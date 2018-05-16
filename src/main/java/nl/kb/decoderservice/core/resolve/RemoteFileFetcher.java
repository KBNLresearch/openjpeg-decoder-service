package nl.kb.decoderservice.core.resolve;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

class RemoteFileFetcher implements ImageFetcher {
    private final String resolveFormat;
    private final HttpClient httpClient;
    private final FileCacher fileCacher;

    RemoteFileFetcher(String resolveFormat, HttpClient httpClient, FileCacher fileCacher) {

        this.resolveFormat = resolveFormat;
        this.httpClient = httpClient;
        this.fileCacher = fileCacher;
    }

    @Override
    public File fetch(String identifier) throws IOException {
        final File file = fileCacher.fetchLocal(identifier);
        if (file.exists()) {
            return file;
        }

        final HttpResponse resp = httpClient.execute(new HttpGet(String.format(resolveFormat, identifier)));
        fileCacher.save(resp.getEntity().getContent(), file);

        return file;
    }
}
