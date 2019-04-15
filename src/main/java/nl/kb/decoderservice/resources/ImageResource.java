package nl.kb.decoderservice.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import nl.kb.decoderservice.CacheConfig;
import nl.kb.decoderservice.api.ParameterCompound;
import nl.kb.decoderservice.api.Region;
import nl.kb.decoderservice.api.ScaleDims;
import nl.kb.decoderservice.core.ImageDecoder;
import nl.kb.jp2.Jp2Header;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class ImageResource extends CacheLoader<ParameterCompound, BufferedImage> {

    private final ImageDecoder imageDecoder;
    private final LoadingCache<ParameterCompound, BufferedImage> bufferedImageCache;


    ImageResource(ImageDecoder imageDecoder, CacheConfig cacheConfig) {
        this.imageDecoder = imageDecoder;
        bufferedImageCache = CacheBuilder.newBuilder()
                .softValues()
                .maximumSize(cacheConfig.getMaxEntries())
                .expireAfterAccess(cacheConfig.getExpireAfterAccessMinutes(), TimeUnit.MINUTES)
                .build(this);
    }

    Response getJpegResponse(Jp2Header jp2Header, Region region, ScaleDims scaleDims, int deg, float quality) throws IOException, ExecutionException, InterruptedException {

        if (!region.isValid() || !scaleDims.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final double scale = (double) scaleDims.getW() / (double) region.getW();
        int cp_reduce = 0;

        if (scale < 0.75) {
            for (double s = 0.75; scale <= s && cp_reduce < jp2Header.getNumRes() - 1; cp_reduce++, s *= 0.5) {
                // Leave empty
            }
        }

        final ParameterCompound cacheKey = new ParameterCompound(jp2Header, region, scaleDims, deg, cp_reduce);

        final BufferedImage image =  bufferedImageCache.get(cacheKey);

        final JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpegParams.setCompressionQuality(quality);


        final StreamingOutput stream = os -> {
            final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(os);
            writer.setOutput(ios);
            try {
                writer.write(null, new IIOImage(image, null, null), jpegParams);
                ios.flush();
                os.flush();
                ios.close();
                os.close();
                writer.dispose();
            } catch (IOException ignored) {
                // ignores broken pipes when peer closes connection early
            }
        };
        return Response
                .ok(stream)
                .header("Content-type", "image/jpeg")
                .header("Cache-Control", "public, max-age=3600")
                .header("Connection", "close")
                .header("Expires", ZonedDateTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("EEE, dd-MMM-yyyy HH:mm:ss zzz")))
                .build();
    }


    @Override
    public BufferedImage load(ParameterCompound key) throws Exception {
        return  imageDecoder.getBufferedImageFromJp2File(key.getJp2Header(), key.getRegion(), key.getScaleDims(), key.getDeg(), key.getCp_reduce());
    }
}
