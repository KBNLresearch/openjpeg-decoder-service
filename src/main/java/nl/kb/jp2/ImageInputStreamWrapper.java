package nl.kb.jp2;

import de.digitalcollections.openjpeg.InStreamWrapper;
import de.digitalcollections.openjpeg.OpenJpeg;
import jnr.ffi.Pointer;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

public class ImageInputStreamWrapper extends InStreamWrapper {
    private ImageInputStream is;

    ImageInputStreamWrapper(ImageInputStream is, OpenJpeg lib) {
        super(lib.lib);
        this.is = is;
    }

    protected long read(Pointer outBuffer, long numBytes, Pointer userData) {
        byte[] buf = new byte[(int) numBytes];
        try {
            int read = is.read(buf, 0, (int) numBytes);
            if (read > 0) {
                outBuffer.put(0, buf, 0, read);
            } else {
                return read;
            }
            return read;
        } catch (IOException e) {
            return -1;
        }
    }

    protected long skip(long numBytes, Pointer userData) {
        try {
            return this.is.skipBytes(numBytes);
        } catch (IOException e) {
            return -1;
        }
    }
}
