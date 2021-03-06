package nl.kb.decoderservice;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import nl.kb.decoderservice.core.ImageDecoder;
import nl.kb.decoderservice.core.ImageFetcher;
import nl.kb.decoderservice.resources.IIIFServiceResource;
import nl.kb.decoderservice.resources.ImagingServiceResource;
import nl.kb.decoderservice.resources.VersionResource;
import nl.kb.utils.NativeUtils;
import org.eclipse.jetty.servlets.DoSFilter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.io.IOException;
import java.util.EnumSet;
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
        final ImageFetcher imageFetcher = new ImageFetcher(config.getMountPoint());

        final ExecutorService executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
        final ImageDecoder imageDecoder = new ImageDecoder(executorService, config.getNumThreads(), config.getDecodedImageCache());

        final FilterRegistration.Dynamic doSFilter = environment.servlets().addFilter("DoSFilter", DoSFilter.class);
        doSFilter.setInitParameter("delayMs", String.valueOf(config.getDoSFilterConfig().getDelayMs()));
        doSFilter.setInitParameter("maxRequestsPerSec", String.valueOf(config.getDoSFilterConfig().getMaxRequestsPerSec()));
        doSFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        environment.jersey().register(new ImagingServiceResource(imageFetcher, imageDecoder, config.getResponseImageCache()));
        environment.jersey().register(new IIIFServiceResource(imageFetcher, imageDecoder, config.getResponseImageCache()));
        environment.jersey().register(new VersionResource());
    }


}
