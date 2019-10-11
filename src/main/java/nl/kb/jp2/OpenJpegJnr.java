package nl.kb.jp2;

import de.digitalcollections.openjpeg.OpenJpeg;
import de.digitalcollections.openjpeg.lib.callbacks.opj_msg_callback;
import de.digitalcollections.openjpeg.lib.enums.CODEC_FORMAT;
import de.digitalcollections.openjpeg.lib.libopenjp2;
import de.digitalcollections.openjpeg.lib.structs.opj_codestream_info_v2;
import de.digitalcollections.openjpeg.lib.structs.opj_dparameters;
import de.digitalcollections.openjpeg.lib.structs.opj_image;
import de.digitalcollections.openjpeg.lib.structs.opj_image_comp;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.byref.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class OpenJpegJnr {
    private static final Logger LOG = LoggerFactory.getLogger(OpenJpegJnr.class);
    private static final opj_msg_callback infoLogFn = (msg, data) -> LOG.debug(msg.trim());
    private static final opj_msg_callback warnLogFn = (msg, data) -> LOG.warn(msg.trim());
    private static final opj_msg_callback errorLogFn = (msg, data) -> LOG.error(msg.trim());

    private static final libopenjp2 lib = new OpenJpeg().lib;

    private static void setupLogger(Pointer codec) {
        if (LOG.isDebugEnabled()) {
            if (!lib.opj_set_info_handler(codec, infoLogFn)) {
                throw new RuntimeException("Could not set info logging handler");
            }
        }
        if (LOG.isWarnEnabled()) {
            if (!lib.opj_set_warning_handler(codec, warnLogFn)) {
                throw new RuntimeException("Could not set warning logging handler");
            }
        }
        if (LOG.isErrorEnabled()) {
            if (!lib.opj_set_error_handler(codec, errorLogFn)) {
                throw new RuntimeException("Could not set error logging handler");
            }
        }
    }

    private static opj_image getImage(Pointer defaultFileStream, Pointer codec) throws IOException {
        opj_image img = new opj_image(Runtime.getRuntime(lib));
        PointerByReference imgPtr = new PointerByReference();
        if (!lib.opj_read_header(defaultFileStream, codec, imgPtr)) {
            throw new IOException("Error while reading header.");
        }
        img.useMemory(imgPtr.getValue());
        return img;
    }

    private static Pointer getCodec(int reduceFactor) throws IOException {
        Pointer codec = lib.opj_create_decompress(CODEC_FORMAT.OPJ_CODEC_JP2);
        setupLogger(codec);
        opj_dparameters params = new opj_dparameters(Runtime.getRuntime(lib));
        lib.opj_set_default_decoder_parameters(params);
        params.cp_reduce.set(reduceFactor);
        if (!lib.opj_setup_decoder(codec, params)) {
            throw new IOException("Error setting up decoder!");
        }
        return codec;
    }

    static Jp2Header getInfo(String filename) throws IOException {
        Pointer codec = null;
        opj_image img = null;
        Pointer defaultFileStream = null;

        try {
            codec = getCodec(0);
            defaultFileStream = lib.opj_stream_create_default_file_stream(filename, true);

            img = getImage(defaultFileStream, codec);
            final Jp2Header info = getInfo(codec, img);
            info.setFileName(filename);
            return info;
        } finally {
            if (codec != null) {
                lib.opj_destroy_codec(codec);
            }
            if (img != null) {
                img.free(lib);
            }
            if (defaultFileStream != null) {
                lib.opj_stream_destroy(defaultFileStream);
            }
        }
    }


    private static Jp2Header getInfo(Pointer codecPointer, opj_image img) {
        opj_codestream_info_v2 csInfo = null;
        try {
            csInfo = lib.opj_get_cstr_info(codecPointer);
            Jp2Header jp2Header = new Jp2Header();
            jp2Header.setX1(img.x1.intValue());
            jp2Header.setY1(img.y1.intValue());
            jp2Header.setNumComps(csInfo.nbcomps.intValue());
            jp2Header.setTw(csInfo.tw.intValue());
            jp2Header.setTh(csInfo.th.intValue());
            jp2Header.setTdx(csInfo.tdx.intValue());
            jp2Header.setTdy(csInfo.tdy.intValue());
            jp2Header.setNumRes(csInfo.m_default_tile_info.tccp_info.get().numresolutions.intValue());
            return jp2Header;
        } finally {
            if (csInfo != null) {
                csInfo.free(lib);
            }
        }
    }

    static  DecodedImageDims decode(String filename, int x, int y, int w, int h, int reduceFactor, int[][] colorBands)
            throws IOException {
        Pointer codec = null;
        opj_image img = null;
        Pointer defaultFileStream = null;
        try {

            codec = getCodec(reduceFactor);
            defaultFileStream = lib.opj_stream_create_default_file_stream(filename, true);
            img = getImage(defaultFileStream, codec);

            int targetWidth;
            int targetHeight;

            lib.opj_set_decode_area(codec, Struct.getMemory(img), x, y, x + w, y + h);

            if (!lib.opj_decode(codec, defaultFileStream, Struct.getMemory(img))) {
                throw new IOException("Could not decode image!");
            }

            int numcomps = img.numcomps.intValue();
            opj_image_comp[] comps = img.comps.get(numcomps);
            final DecodedImageDims decodedImageDims = new DecodedImageDims();

            targetWidth = comps[0].w.intValue();
            targetHeight = comps[0].h.intValue();
            decodedImageDims.setWidth(targetWidth);
            decodedImageDims.setHeight(targetHeight);

            for (int j = 0; j < numcomps; j++) {
                final Pointer pBand = comps[j].data.get();
                for (int i = 0; i < targetWidth * targetHeight; i++) {
                    colorBands[j][i] = pBand.getInt(i * 4 /* hoping this is sizeof(int) ? */);
                }
            }
            return decodedImageDims;
        } finally {
            if (img != null) {
                img.free(lib);
            }
            if (codec != null) {
                lib.opj_destroy_codec(codec);
            }
            if (defaultFileStream != null) {
                lib.opj_stream_destroy(defaultFileStream);
            }
        }

    }
}
