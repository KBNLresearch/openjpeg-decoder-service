package nl.kb.decoderservice.resources;

import nl.kb.decoderservice.CacheConfig;
import nl.kb.decoderservice.api.Region;
import nl.kb.decoderservice.api.ScaleDims;
import nl.kb.decoderservice.core.ImageDecoder;
import nl.kb.decoderservice.core.ImageFetcher;
import nl.kb.jp2.Jp2Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;

@Path("/imagingService")
public class ImagingServiceResource extends ImageResource {
    private static final Logger LOG = LoggerFactory.getLogger(ImagingServiceResource.class);

    private final ImageFetcher imageFetcher;


    public ImagingServiceResource(ImageFetcher imageFetcher, ImageDecoder imageDecoder, CacheConfig cacheConfig) {
        super(imageDecoder, cacheConfig);
        this.imageFetcher = imageFetcher;
    }

    @GET
    public Response get(
            @QueryParam("id") String identifier,
            @QueryParam("x") Double xParam,
            @QueryParam("y") Double yParam,
            @QueryParam("w") Double wParam,
            @QueryParam("h") Double hParam,
            @QueryParam("s") Double sParam,
            @QueryParam("r") Double rParam,
            @QueryParam("c") String cParam,
            @QueryParam("f") String fParam,
            @QueryParam("q") Integer qParam
    ) {

        try {
            final File imageFile = imageFetcher.fetch(identifier);
            final Jp2Header jp2Header = Jp2Header.read(imageFile);
            final ScaleDims scaleDims = new ScaleDims(jp2Header);
            final Region region = new Region(jp2Header);

            if (cParam != null && cParam.equals("imghead")) {
                return metadataResponse(jp2Header, fParam);
            }

            interpretParams(scaleDims, region,
                    xParam == null ? null : (int) Math.round(xParam),
                    yParam == null ? null : (int) Math.round(yParam),
                    (wParam == null || wParam == 0) ? null : (int) Math.round(wParam),
                    (hParam == null || hParam == 0) ? null : (int) Math.round(hParam),
                    sParam,
                    rParam == null ? null : rParam.intValue(),
                    jp2Header);

            return getJpegResponse(jp2Header, region, scaleDims, rParam == null ? 0 : rParam.intValue(), qParam == null ? 1f : (float)qParam / 100f);
        } catch (FileNotFoundException e) {
            LOG.warn("File not found ", e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response metadataResponse(Jp2Header jp2Header, String fParam) throws Exception {
            return Response.ok(
                    String.format("width: %d\n" +
                            "height: %d\n" +
                            "levels: %d\n" +
                            "tilex: 0\n" +
                            "tiley: 0\n" +
                            "tilewidth: %d\n" +
                            "tileheight: %d\n" +
                            "colorspace: %s\n", jp2Header.getX1(), jp2Header.getY1(), jp2Header.getNumRes() - 1, jp2Header.getTdx(), jp2Header.getTdy(), jp2Header.getColorSpace()
                    )).header("Content-type", "text/plain; charset=utf-8").build();
    }

    private void interpretParams(ScaleDims scaleDims, Region region,
                                 Integer xParam, Integer yParam, Integer wParam, Integer hParam, Double sParam,
                                 Integer rParam, Jp2Header jp2Header) {

        if (xParam == null && yParam == null && sParam == null) {
            interpretFullRegionParams(scaleDims, wParam, hParam, rParam == null ? 0 : rParam, jp2Header);
        } else {
            // Applies default values to client GET parameters, fixes negative values
            rParam = rParam == null ? 0 : rParam;
            sParam = sParam == null || sParam == 0 ? 1.0 : sParam;
            final int requestedX = xParam == null || xParam < 0 ? 0 : xParam;
            final int requestedY = yParam == null || yParam < 0 ? 0 : yParam;
            final int requestedW = wParam == null ? (int) Math.round(jp2Header.getX1() * sParam) : wParam;
            final int requestedH = hParam == null ? (int) Math.round(jp2Header.getY1() * sParam) : hParam;

            // Determines the region on the the source image the client wants returned
            final Region regionSelectedOnSourceImage = determineRegionOnSourceImage(
                    xParam, yParam, wParam, hParam, sParam, rParam, jp2Header,
                            requestedX, requestedY, requestedW, requestedH);


            // Ensures that all positions are with in the full dimensions of the source image
            int boundedSourceRegionW = Math.min(regionSelectedOnSourceImage.getW(), jp2Header.getX1());
            int boundedSourceRegionH = Math.min(regionSelectedOnSourceImage.getH(), jp2Header.getY1());
            int boundedSourceRegionX;
            int boundedSourceRegionY;
            if (regionSelectedOnSourceImage.getX() + boundedSourceRegionW > jp2Header.getX1()) {
                if (jp2Header.getX1() - regionSelectedOnSourceImage.getW() >= 0) {
                    boundedSourceRegionX = jp2Header.getX1() - regionSelectedOnSourceImage.getW();
                    boundedSourceRegionW = regionSelectedOnSourceImage.getW();
                } else {
                    boundedSourceRegionX = 0;
                }
            } else {
                boundedSourceRegionX = regionSelectedOnSourceImage.getX();
            }
            if (regionSelectedOnSourceImage.getY() + boundedSourceRegionH > jp2Header.getY1()) {
                if (jp2Header.getY1() - regionSelectedOnSourceImage.getH() >= 0) {
                    boundedSourceRegionY = jp2Header.getY1() - regionSelectedOnSourceImage.getH();
                    boundedSourceRegionH = regionSelectedOnSourceImage.getH();
                } else {
                    boundedSourceRegionY = 0;
                }
            } else {
                boundedSourceRegionY = regionSelectedOnSourceImage.getY();
            }
            // end bounds validation

            // Sets the exact dimensions for scaled result image based on the bounded source region
            scaleDims.setW((int) Math.round(boundedSourceRegionW * sParam));
            scaleDims.setH((int) Math.round(boundedSourceRegionH * sParam));

            // Copies bounded region into Region class-instance for further processing
            region.setW(boundedSourceRegionW);
            region.setH(boundedSourceRegionH);
            region.setX(boundedSourceRegionX);
            region.setY(boundedSourceRegionY);

        }
    }

    private Region determineRegionOnSourceImage(Integer xParam, Integer yParam, Integer wParam, Integer hParam,
                                                Double sParam, Integer rParam, Jp2Header jp2Header, int requestedX,
                                                int requestedY, int requestedW, int requestedH) {

        if (wParam == null && hParam == null) {
            return new Region(0, 0, jp2Header.getX1(), jp2Header.getY1());
        } else {
            return new Region(
                    (int) Math.round(requestedX / sParam),
                    (int) Math.round(requestedY / sParam),
                    (int) Math.round(requestedW / sParam),
                    (int) Math.round(requestedH / sParam)
            ).rotatedForRequest(jp2Header, rParam);
        }
    }

    private void interpretFullRegionParams(ScaleDims scaleDims, Integer wParam, Integer hParam, Integer rParam, Jp2Header jp2Header) {
        if (hParam == null && wParam == null) {
            return;
        }
        if (hParam == null) {
            if (rParam == 90 || rParam == 270) {
                setScaleFromHeightParam(scaleDims, wParam, jp2Header);
            } else {
                setScaleFromWidthParam(scaleDims, wParam, jp2Header);
            }
        } else if (wParam == null) {
            if (rParam == 90 || rParam == 270) {
                setScaleFromWidthParam(scaleDims, hParam, jp2Header);
            } else {
                setScaleFromHeightParam(scaleDims, hParam, jp2Header);
            }
        } else {
            if (hParam < wParam) {
                if (rParam == 90 || rParam == 270) {
                    setScaleFromWidthParam(scaleDims, hParam, jp2Header);
                } else {
                    setScaleFromHeightParam(scaleDims, hParam, jp2Header);
                }
            } else {
                if (rParam == 90 || rParam == 270) {
                    setScaleFromHeightParam(scaleDims, wParam, jp2Header);
                } else {
                    setScaleFromWidthParam(scaleDims, wParam, jp2Header);
                }
            }
        }
    }

    private void setScaleFromHeightParam(ScaleDims scaleDims, Integer hParam, Jp2Header jp2Header) {
        scaleDims.setH(hParam);
        scaleDims.setW((int) Math.round(jp2Header.getX1() * ((double) hParam / (double) jp2Header.getY1())));
    }

    private void setScaleFromWidthParam(ScaleDims scaleDims, Integer wParam, Jp2Header jp2Header) {
        scaleDims.setW(wParam);
        scaleDims.setH((int) Math.round(jp2Header.getY1() * ((double) wParam / (double) jp2Header.getX1())));
    }
}
