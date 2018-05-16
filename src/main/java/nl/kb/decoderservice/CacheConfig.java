package nl.kb.decoderservice;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CacheConfig {

    @JsonProperty
    private Integer maxEntries;

    @JsonProperty
    private Integer expireAfterAccessMinutes;

    public CacheConfig() {

    }

    public CacheConfig(Integer maxEntries, Integer expireAfterAccessMinutes) {
        this.maxEntries = maxEntries;
        this.expireAfterAccessMinutes = expireAfterAccessMinutes;
    }

    public Integer getMaxEntries() {
        return maxEntries;
    }

    public Integer getExpireAfterAccessMinutes() {
        return expireAfterAccessMinutes;
    }
}
