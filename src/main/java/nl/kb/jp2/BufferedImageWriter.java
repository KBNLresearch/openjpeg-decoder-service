package nl.kb.jp2;

import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class BufferedImageWriter {
    private BufferedImageWriter() { }

    
    public static BufferedImage fromRaw(List<DecodedImage> decodedImages, int newWidth, int newHeight, int deg,
                                        ExecutorService executorService) throws InterruptedException {

        final int width = decodedImages.get(0).getDecodedImageDims().getWidth();
        final int height = decodedImages.stream().mapToInt(x -> x.getDecodedImageDims().getHeight()).sum();

        final int outSize = height * width;
        final int[] remapped = new int[outSize];

        final List<Callable<Boolean>> callables = new ArrayList<>();
        for (int i = 0; i < decodedImages.size(); i++) {
            final DecodedImage decodedImage = decodedImages.get(i);
            final int[][] colorBands = decodedImage.getColorBands();
            final int inHeight = decodedImage.getDecodedImageDims().getHeight();
            final int inWidth = decodedImage.getDecodedImageDims().getWidth();
            final int curIndex = i;

            callables.add(() -> {
                int inOffset = 0;
                int combinedOffset = decodedImages.subList(0, curIndex).stream().mapToInt(x -> x.getDecodedImageDims().getHeight()).sum() * width;
                for (int y = curIndex * inHeight; y < (curIndex + 1) * inHeight; y++) {
                    for (int x = 0; x < inWidth; x++) {
                        int r = colorBands[0][inOffset];
                        int g = colorBands[colorBands.length < 3 ? 0 : 1][inOffset];
                        int b = colorBands[colorBands.length < 3 ? 0 : 2][inOffset];
                        int p = (r << 16) | (g << 8) | b; //pixel
                        remapped[MatrixRotate.rotate(combinedOffset, inWidth, height, deg)] = p;
                        inOffset++;
                        combinedOffset++;
                    }
                }
                return true;
            });
        }
        executorService.invokeAll(callables);

        if (deg == 0 || deg == 180) {
            final BufferedImage inImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            inImage.getRaster().setDataElements(0,0, width, height, remapped);
            if (width == newWidth && height == newHeight) {
                return inImage;
            }

            final ResampleOp resizeOp = new ResampleOp(newWidth, newHeight);
            resizeOp.setFilter(ResampleFilters.getLanczos3Filter());

            return resizeOp.filter(inImage, null);
        } else {
            final BufferedImage inImage = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);
            inImage.getRaster().setDataElements(0,0, height, width, remapped);


            if (width == newWidth && height == newHeight) {

                return inImage;
            }

            final ResampleOp resizeOp = new ResampleOp(newHeight, newWidth);
            resizeOp.setFilter(ResampleFilters.getLanczos3Filter());

            return resizeOp.filter(inImage, null);
        }
    }

}
