package nl.kb.decoderservice;

import com.fasterxml.jackson.annotation.JsonProperty;

class DoSFilterConfig {

    @JsonProperty
    private Integer maxRequestsPerSec;
    @JsonProperty
    private Integer delayMs;

    public DoSFilterConfig() {

    }

    public DoSFilterConfig(Integer maxRequestsPerSec, Integer delayMs) {

        this.maxRequestsPerSec = maxRequestsPerSec;
        this.delayMs = delayMs;
    }

    public Integer getDelayMs() {
        return delayMs;
    }

    public Integer getMaxRequestsPerSec() {
        return maxRequestsPerSec;
    }
}
