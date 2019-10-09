package nl.kb.jp2;

import nl.kb.utils.NativeUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SampleTestRunner {

    public static void main(String... args) {
        final File[] aFiles = new File(args[0]).listFiles();
        final List<File> files = Arrays.asList(aFiles != null ? aFiles : new File[0]);
        files.sort(Comparator.comparing(File::getAbsolutePath));

        for (File file : files) {
            if (file.isFile() && !file.getAbsolutePath().endsWith(".xml")) {
                try {
                    testRun(file);
                } catch (IOException/* | InterruptedException | ExecutionException */e) {
                    System.err.println("Failed to decode file " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }

    }

    private static void testRun(File file) throws IOException {
        final int cp_reduce = 0;
        try (final FileInputStream fis = new FileInputStream(file); final ImageInputStream ims = ImageIO.createImageInputStream(fis)) {
            final Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByFormatName("JPEG2000");
            if (imageReaders.hasNext()) {
                final ImageReader reader = imageReaders.next();

                reader.setInput(ims);
                final ImageReadParam imageReadParam = new ImageReadParam();
                final Rectangle rectangle = new Rectangle();
                rectangle.width = 100;
                rectangle.height = 100;
                rectangle.x = 10;
                rectangle.y = 10;
                imageReadParam.setSourceRegion(rectangle);

                final BufferedImage img = reader.read(3, imageReadParam);
                ImageIO.write(img, "jpg", new File("output/" +
                        file.getName()
                                .replaceAll("\\..*", "") + "-" + cp_reduce +".jpg"));

                reader.dispose();

            } else {
                throw new IllegalStateException("this should not happen");
            }
        }
    }

    private static void testRun2(File file) throws IOException, ExecutionException, InterruptedException {
        long before = System.currentTimeMillis();
        final Jp2Header jp2Header = Jp2Header.read(file);

        final ExecutorService executorService = Executors.newFixedThreadPool(24);


        System.out.println(file + ": " + (System.currentTimeMillis() - before) + "ms: " + jp2Header);

        for (int cp_reduce = 0; cp_reduce < jp2Header.getNumRes(); cp_reduce++) {
            before = System.currentTimeMillis();

            final int w = jp2Header.getX1();
            final int h = jp2Header.getY1();
            final List<DecodedImage> decodedImages = Jp2Decode.mtDecodeArea(jp2Header, 0, 0, w, h,
                    cp_reduce, 4, executorService);
            System.out.println(file + ": " + (System.currentTimeMillis() - before) + "ms: decode");

            before = System.currentTimeMillis();

/*
            final int reducedW = reduce(w, cp_reduce);
            final int reducedH = reduce(h, cp_reduce);
*/
            BufferedImage img = BufferedImageWriter
                    .fromRaw(decodedImages, decodedImages.get(0).getDecodedImageDims().getWidth(),
                            decodedImages.stream().mapToInt(x -> x.getDecodedImageDims().getHeight()).sum(), 90,
                            executorService);

            System.out.println(file + ": " + (System.currentTimeMillis() - before) + "ms: to buffered image");
            ImageIO.write(img, "jpg", new File("output/" +
                    file.getName()
                            .replaceAll("\\..*", "") + "-" + cp_reduce +".jpg"));

        }
        executorService.shutdown();
    }

}
