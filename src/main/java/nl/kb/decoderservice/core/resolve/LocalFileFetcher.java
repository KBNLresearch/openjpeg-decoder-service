package nl.kb.decoderservice.core.resolve;

import java.io.File;
import java.io.FileNotFoundException;

class LocalFileFetcher implements ImageFetcher {
    private final String pathFormat;

    public LocalFileFetcher(String pathFormat) {
        this.pathFormat = pathFormat;
    }

    public File fetch(String identifier) throws FileNotFoundException {

        final File file = new File(String.format(pathFormat, identifier));
        if (file.canRead()) {
            return file;
        } else {
            throw new FileNotFoundException("Could not resolve identifier to a file: " + identifier);
        }
    }
}
