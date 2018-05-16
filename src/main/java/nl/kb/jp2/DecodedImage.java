package nl.kb.jp2;

public class DecodedImage {
    private final DecodedImageDims decodedImageDims;
    private final int[][] colorBands;

    DecodedImage(DecodedImageDims decodedImageDims, int[][] colorBands) {
        this.decodedImageDims = decodedImageDims;
        this.colorBands = colorBands;
    }

    public DecodedImageDims getDecodedImageDims() {
        return decodedImageDims;
    }

    public int[][] getColorBands() {
        return colorBands;
    }
}
