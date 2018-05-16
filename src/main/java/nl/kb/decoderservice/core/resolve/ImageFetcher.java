package nl.kb.decoderservice.core.resolve;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface ImageFetcher {

    File fetch(String identifier) throws IOException;
}
