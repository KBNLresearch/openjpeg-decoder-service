package nl.kb.decoderservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import nl.kb.decoderservice.core.resolve.ImageFetcherFactory;

class Config extends Configuration {

    @JsonProperty("threadPool")
    private int threadPoolSize = 24;
    @JsonProperty
    private int numThreads = 8;
    @JsonProperty
    private String pathFormat;

    @JsonProperty
    private CacheConfig decodedImageCache = new CacheConfig(500, 60);

    @JsonProperty
    private CacheConfig responseImageCache = new CacheConfig(2000, 10);

    @JsonProperty("imageFetcher")
    private ImageFetcherFactory imageFetcherFactory;

    @JsonProperty("allowCorsDomain")
    private String allowOrigin = null;

    int getThreadPoolSize() {
        return threadPoolSize;
    }

    int getNumThreads() {
        return numThreads;
    }

    String getPathFormat() {
        return pathFormat;
    }

    public CacheConfig getDecodedImageCache() {
        return decodedImageCache;
    }

    public CacheConfig getResponseImageCache() {
        return responseImageCache;
    }

    public ImageFetcherFactory getImageFetcherFactory() {
        return imageFetcherFactory;
    }

    public String getAllowOrigin() {
        return allowOrigin;
    }
}
