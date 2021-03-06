package nl.kb.decoderservice.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import nl.kb.decoderservice.CacheConfig;
import nl.kb.decoderservice.api.ParameterCompound;
import nl.kb.decoderservice.api.Region;
import nl.kb.decoderservice.api.ScaleDims;
import nl.kb.jp2.BufferedImageWriter;
import nl.kb.jp2.DecodedImage;
import nl.kb.jp2.Jp2Decode;
import nl.kb.jp2.Jp2Header;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ImageDecoder extends CacheLoader<ParameterCompound, List<DecodedImage>> {

    private final LoadingCache<ParameterCompound,  List<DecodedImage>> decodedImageCache;
    private final ExecutorService executorService;
    private final int numThreads;


    public ImageDecoder(ExecutorService executorService, int numThreads, CacheConfig cacheConfig) {
        this.executorService = executorService;
        this.numThreads = numThreads;
        this.decodedImageCache = CacheBuilder.newBuilder()
                .softValues()
                .maximumSize(cacheConfig.getMaxEntries())
                .expireAfterAccess(cacheConfig.getExpireAfterAccessMinutes(), TimeUnit.MINUTES)
                .build(this);
    }

    public BufferedImage getBufferedImageFromJp2File(Jp2Header jp2Header, Region region, ScaleDims scaleDims, int deg, int cp_reduce) throws IOException, ExecutionException, InterruptedException {
        final List<DecodedImage> decodedImages = decodedImageCache.get(new ParameterCompound(jp2Header, region, null, 0, cp_reduce));

        return BufferedImageWriter.fromRaw(decodedImages,scaleDims.getW(), scaleDims.getH(), deg, executorService);
    }

    @Override
    public List<DecodedImage> load(ParameterCompound key) throws Exception {
        return Jp2Decode.mtDecodeArea(key.getJp2Header(),
                key.getRegion().getX(),
                key.getRegion().getY(),
                key.getRegion().getW(),
                key.getRegion().getH(),
                key.getCp_reduce(), numThreads, executorService);
    }
}
