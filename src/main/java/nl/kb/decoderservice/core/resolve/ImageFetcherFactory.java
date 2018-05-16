package nl.kb.decoderservice.core.resolve;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.setup.Environment;
import org.apache.http.client.HttpClient;

public class ImageFetcherFactory {
    @JsonProperty
    private String type;

    @JsonProperty
    private String resolveFormat;

    @JsonProperty
    private FileCacher fileCacher;

    @JsonProperty("httpClient")
    private HttpClientConfiguration httpClientConfiguration;


    public ImageFetcher getImageFetcher(Environment environment) {

        switch (type) {
            case "file":
                return new LocalFileFetcher(resolveFormat);
            case "http":
                final HttpClient httpClient = new HttpClientBuilder(environment)
                        .using(httpClientConfiguration)
                        .build("http-client");

                return new RemoteFileFetcher(resolveFormat, httpClient, fileCacher);
            default:
                throw new ExceptionInInitializerError("Image fetcher type not supported");
        }
    }
}
