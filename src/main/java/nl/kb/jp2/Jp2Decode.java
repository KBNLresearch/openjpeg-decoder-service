package nl.kb.jp2;

import de.digitalcollections.openjpeg.InStreamWrapper;
import de.digitalcollections.openjpeg.OpenJpeg;
import de.digitalcollections.openjpeg.lib.callbacks.opj_msg_callback;
import de.digitalcollections.openjpeg.lib.enums.CODEC_FORMAT;
import de.digitalcollections.openjpeg.lib.libopenjp2;
import de.digitalcollections.openjpeg.lib.structs.opj_dparameters;
import de.digitalcollections.openjpeg.lib.structs.opj_image;
import de.digitalcollections.openjpeg.lib.structs.opj_image_comp;
import jnr.ffi.Pointer;
import jnr.ffi.Struct;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.Runtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Jp2Decode {
    private static final Logger LOG = LoggerFactory.getLogger(Jp2Decode.class);
    private static final opj_msg_callback infoLogFn = (msg, data) -> LOG.debug(msg.trim());
    private static final opj_msg_callback warnLogFn = (msg, data) -> LOG.warn(msg.trim());
    private static final opj_msg_callback errorLogFn = (msg, data) -> LOG.error(msg.trim());

    private static final OpenJpeg openJpeg = OpenJpegJnr.getOpenJpeg();
    private static final libopenjp2 lib = openJpeg.lib;


    private Jp2Decode() { }

    private static void setupLogger(Pointer codec) {
        if (LOG.isInfoEnabled()) {
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

    public static List<DecodedImage> mtDecodeArea(
            Jp2Header jp2Header, int x, int y, int w, int h, int cp_reduce, int numThreads, ExecutorService executorService) throws IOException, InterruptedException, ExecutionException {
        final List<DecodedImage> result = new ArrayList<>();

        if (numThreads > 1 && h > numThreads && DimReducer.reduce(w, cp_reduce) * DimReducer.reduce(h, cp_reduce) > 500_000) {
            final List<Callable<DecodedImage>> callables = new ArrayList<>();

            final int divH = h / numThreads;
            for (int i = 0; i < numThreads; i++) {
                final int newY = y + (i * divH);
                final int finalH = i == numThreads - 1
                        ? h - ((numThreads - 1) * divH)
                        : divH;

                callables.add(() -> decodeArea(jp2Header, x, newY, w, finalH, cp_reduce));
            }
            final List<Future<DecodedImage>> futures = executorService.invokeAll(callables);
            for (Future<DecodedImage> future : futures) {
                result.add(future.get());
            }

        } else {
            result.add(decodeArea(jp2Header, x, y, w, h, cp_reduce));
        }

        return result;
    }

    private static DecodedImage decodeArea(Jp2Header jp2Header, int x, int y, int w, int h, int cp_reduce) throws IOException {
        int size = DimReducer.reduce(w, cp_reduce) * DimReducer.reduce(h, cp_reduce);
        int[][] colorBands = new int[jp2Header.getNumComps()][];
        for (int i = 0; i < jp2Header.getNumComps(); i++) {
            colorBands[i] = new int[size];
        }

        try (final FileInputStream fis = new FileInputStream(jp2Header.getFileName());
             final ImageInputStream ims = ImageIO.createImageInputStream(fis)) {
            final DecodedImageDims decodedImageDims = decode(new ImageInputStreamWrapper(ims, openJpeg),
                    new Rectangle(x, y, w, h), cp_reduce, colorBands);
            return new DecodedImage(decodedImageDims, colorBands);
        }
    }

    private static native DecodedImageDims decodeJp2Area(String filename,
                                             int x, int y, int w, int h, int cp_reduce,
                                             int[][] colorBands) throws IOException;



    private static opj_image getImage(InStreamWrapper wrapper, Pointer codec) throws IOException {
        opj_image img = new opj_image(Runtime.getRuntime(lib));
        PointerByReference imgPtr = new PointerByReference();
        if (!lib.opj_read_header(wrapper.getNativeStream(), codec, imgPtr)) {
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

    private static  DecodedImageDims decode(InStreamWrapper wrapper, Rectangle area, int reduceFactor, int[][] colorBands)
            throws IOException {
        Pointer codec = null;
        opj_image img = null;
        try {
            codec = getCodec(reduceFactor);
            img = getImage(wrapper, codec);

            // Configure decoding area
            int targetWidth;
            int targetHeight;
            if (area == null) {
                if (!lib.opj_set_decode_area(codec, Struct.getMemory(img), img.x0.intValue(), img.y0.intValue(),
                        img.x1.intValue(), img.y1.intValue())) {
                    throw new IOException("Could not set decoding area!");
                }
            } else {
                lib.opj_set_decode_area(
                        codec, Struct.getMemory(img),
                        area.x, area.y, area.x + area.width, area.y + area.height);
            }

            if (!lib.opj_decode(codec, wrapper.getNativeStream(), Struct.getMemory(img))) {
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
            wrapper.close();
        }

    }
}
