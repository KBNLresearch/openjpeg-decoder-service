package nl.kb.decoderservice.resources;

import nl.kb.decoderservice.CacheConfig;
import nl.kb.decoderservice.api.Region;
import nl.kb.decoderservice.api.ScaleDims;
import nl.kb.decoderservice.core.ImageDecoder;
import nl.kb.decoderservice.core.resolve.ImageFetcher;
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
                return metadataResponse(jp2Header);
            }

            interpretParams(scaleDims, region,
                    xParam == null ? null : (int) Math.round(xParam),
                    yParam == null ? null : (int) Math.round(yParam),
                    wParam == null ? null : (int) Math.round(wParam),
                    hParam == null ? null : (int) Math.round(hParam),
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

    private Response metadataResponse(Jp2Header jp2Header) {
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
            rParam = rParam == null ? 0 : rParam;
            sParam = sParam == null ? 1.0 : sParam;
            final int requestedX = xParam == null || xParam < 0 ? 0 : xParam;
            final int requestedY = yParam == null || yParam < 0 ? 0 : yParam;
            final int requestedW = wParam == null ? (int) Math.round(jp2Header.getX1() * sParam) : wParam;
            final int requestedH = hParam == null ? (int) Math.round(jp2Header.getY1() * sParam) : hParam;

            final Region rotatedRegion = new Region(
                    (int) Math.round(requestedX / sParam),
                    (int) Math.round(requestedY / sParam),
                    (int) (xParam == null && yParam == null && wParam == null && hParam == null ? jp2Header.getX1() : Math.round(requestedW / sParam)),
                    (int) (xParam == null && yParam == null && wParam == null && hParam == null ? jp2Header.getY1() : Math.round(requestedH / sParam)))
                    .rotatedForRequest(jp2Header, rParam);

            final int scaledRequestedW = rotatedRegion.getW();
            final int scaledRequestedH = rotatedRegion.getH();
            final int requestedRegionX = rotatedRegion.getX();
            final int requestedRegionY = rotatedRegion.getY();

            int derivedRegionW = Math.min(scaledRequestedW, jp2Header.getX1());
            int derivedRegionH = Math.min(scaledRequestedH, jp2Header.getY1());
            int derivedRegionX;
            int derivedRegionY;


            if (requestedRegionX + derivedRegionW > jp2Header.getX1()) {
                if (jp2Header.getX1() - scaledRequestedW >= 0) {
                    derivedRegionX = jp2Header.getX1() - scaledRequestedW;
                    derivedRegionW = scaledRequestedW;
                } else {
                    derivedRegionX = 0;
                }
            } else {
                derivedRegionX = requestedRegionX;
            }

            if (requestedRegionY + derivedRegionH > jp2Header.getY1()) {
                if (jp2Header.getY1() - scaledRequestedH >= 0) {
                    derivedRegionY = jp2Header.getY1() - scaledRequestedH;
                    derivedRegionH = scaledRequestedH;
                } else {
                    derivedRegionY = 0;
                }
            } else {
                derivedRegionY = requestedRegionY;
            }

            scaleDims.setW((int) Math.round(derivedRegionW * sParam));
            scaleDims.setH((int) Math.round(derivedRegionH * sParam));
            region.setW(derivedRegionW);
            region.setH(derivedRegionH);
            region.setX(derivedRegionX);
            region.setY(derivedRegionY);
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
