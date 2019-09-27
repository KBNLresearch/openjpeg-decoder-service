package nl.kb.decoderservice.resources;

import org.apache.commons.io.IOUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;

@Path("/version")
public class VersionResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response version() {
        String version = "version-under-development";
        try {
            version = IOUtils.toString(VersionResource.class.getResourceAsStream("/version.txt"), Charset.defaultCharset()).trim();
        } catch (Exception ignored) {

        }
        return Response.ok(version).build();
    }
}
