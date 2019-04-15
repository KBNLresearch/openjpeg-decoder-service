package nl.kb.jp2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Jp2Decode {

    private Jp2Decode() { }

    public static List<DecodedImage> mtDecodeArea(
            Jp2Header jp2Header, int x, int y, int w, int h, int cp_reduce, int numThreads, ExecutorService executorService) throws IOException, InterruptedException, ExecutionException {
        final List<DecodedImage> result = new ArrayList<>();

        if (numThreads > 1 && h > numThreads && DimReducer.reduce(w, cp_reduce) * DimReducer.reduce(h, cp_reduce) > 500_000) {
            final List<Callable<DecodedImage>> callables = new ArrayList<>();

            final int divH = (int) Math.ceil(h / numThreads);
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

    public static DecodedImage decodeArea(Jp2Header jp2Header, int x, int y, int w, int h, int cp_reduce) throws IOException {
        int size = DimReducer.reduce(w, cp_reduce) * DimReducer.reduce(h, cp_reduce);
        int[][] colorBands = new int[jp2Header.getNumComps()][];
        for (int i = 0; i < jp2Header.getNumComps(); i++) {
            colorBands[i] = new int[size];
        }

        final DecodedImageDims decodedImageDims = decodeJp2Area(jp2Header.getFileName(), x, y, w, h, cp_reduce, colorBands);
        return new DecodedImage(decodedImageDims, colorBands);
    }

    private static native DecodedImageDims decodeJp2Area(String filename,
                                             int x, int y, int w, int h, int cp_reduce,
                                             int[][] colorBands) throws IOException;
}
