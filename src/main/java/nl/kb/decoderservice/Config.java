package nl.kb.decoderservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

class Config extends Configuration {

    @JsonProperty("threadPool")
    private int threadPoolSize = 24;
    @JsonProperty
    private int numThreads = 8;
    @JsonProperty
    private String mountPoint;

    @JsonProperty
    private CacheConfig decodedImageCache = new CacheConfig(500, 60);

    @JsonProperty
    private CacheConfig responseImageCache = new CacheConfig(2000, 10);

    @JsonProperty("DoSFilter")
    private DoSFilterConfig doSFilterConfig = new DoSFilterConfig(20, 100);

    int getThreadPoolSize() {
        return threadPoolSize;
    }

    int getNumThreads() {
        return numThreads;
    }

    String getMountPoint() {
        return mountPoint;
    }

    public CacheConfig getDecodedImageCache() {
        return decodedImageCache;
    }

    public CacheConfig getResponseImageCache() {
        return responseImageCache;
    }

    public DoSFilterConfig getDoSFilterConfig() {
        return doSFilterConfig;
    }
}
