package nl.kb.decoderservice.core;

import java.io.File;
import java.io.FileNotFoundException;

public class ImageFetcher {
    private final String mountPoint;

    public ImageFetcher(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    public File fetch(String identifier) throws FileNotFoundException {
	final File f = new File(mountPoint + "/" + identifier);
	if (f.exists() && f.canRead()) {
	    return f;
	} else {
	    throw new FileNotFoundException("File not found for identifier: " + identifier);
	}
    }
}
