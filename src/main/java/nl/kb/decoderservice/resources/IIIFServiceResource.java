package nl.kb.decoderservice.resources;

import nl.kb.decoderservice.CacheConfig;
import nl.kb.decoderservice.api.ImageInfo;
import nl.kb.decoderservice.api.Region;
import nl.kb.decoderservice.api.ScaleDims;
import nl.kb.decoderservice.core.ImageDecoder;
import nl.kb.decoderservice.core.resolve.ImageFetcher;
import nl.kb.jp2.Jp2Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

@Path("/iiif-service/{identifier}")
public class IIIFServiceResource extends ImageResource {
    private static final Logger LOG = LoggerFactory.getLogger(IIIFServiceResource.class);

    private final ImageFetcher imageFetcher;
    private final String allowOrigin;

    public IIIFServiceResource(ImageFetcher imageFetcher, ImageDecoder imageDecoder, CacheConfig cacheConfig, String allowOrigin) {
        super(imageDecoder, cacheConfig);
        this.imageFetcher = imageFetcher;
        this.allowOrigin = allowOrigin;
    }

    @GET
    @Path("/")
    public Response redirectToInfo(@PathParam("identifier") String identifier) {
        final URI uri = UriBuilder.fromPath(String.format("/iiif-service/%s/info.json", identifier)).build();
        return Response.seeOther(uri).build();
    }

    @GET
    @Path("/info.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response info(@Context UriInfo uriInfo, @PathParam("identifier") String identifier) {
        try {
            final File cached = imageFetcher.fetch(identifier);
            final Jp2Header jp2Header = Jp2Header.read(cached);

            final Response.ResponseBuilder responseBuilder = Response.ok(new ImageInfo(jp2Header, uriInfo));

            if (allowOrigin != null) {
                return responseBuilder.header("Access-Control-Allow-Origin", allowOrigin).build();
            } else {
                return responseBuilder.build();
            }
        } catch (FileNotFoundException e) {
            LOG.warn("File not found ", e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{region}/{size}/{rotation}/{format}.jpg")
    @Produces("image/jpeg")
    public Response decode(@PathParam("identifier") String identifier,
        @PathParam("region") String regionParam,
        @PathParam("size") String sizeParam,
        @PathParam("rotation") String rotation
    ) {
        try {
            final File imageFile = imageFetcher.fetch(identifier);
            final Jp2Header jp2Header = Jp2Header.read(imageFile);
            final Region region = Region.parseAndDetermine(regionParam, jp2Header.getX1(), jp2Header.getY1());
            final ScaleDims scaleDims = ScaleDims.parseAndDetermine(sizeParam, region);
            final int deg = rotation.matches("^(90|180|270)$") ? Integer.parseInt(rotation) : 0;

            return getJpegResponse(jp2Header, region, scaleDims, deg);
        } catch (FileNotFoundException e) {
            LOG.warn("File not found ", e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


}
