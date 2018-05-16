package nl.kb.decoderservice;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import nl.kb.decoderservice.core.ImageDecoder;
import nl.kb.decoderservice.core.resolve.ImageFetcher;
import nl.kb.decoderservice.resources.IIIFServiceResource;
import nl.kb.utils.NativeUtils;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application<Config> {
    static {
        try {
            NativeUtils.loadLibraryFromJar("/native/libjp2j.so");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... args) throws Exception {
        new Main().run(args);
    }

    @Override
    public void run(Config config, Environment environment) {
        final ImageFetcher imageFetcher = config.getImageFetcherFactory().getImageFetcher(environment);

        final ExecutorService executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
        final ImageDecoder imageDecoder = new ImageDecoder(executorService, config.getNumThreads(), config.getDecodedImageCache());

        environment.jersey().register(new IIIFServiceResource(imageFetcher, imageDecoder, config.getResponseImageCache()));

    }


}
