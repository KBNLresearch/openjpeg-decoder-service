package nl.kb.jp2;

import de.digitalcollections.openjpeg.OpenJpeg;

class OpenJpegJnr {
    private static final OpenJpeg openJpeg = new OpenJpeg();

    static OpenJpeg getOpenJpeg() { return openJpeg; }

}
