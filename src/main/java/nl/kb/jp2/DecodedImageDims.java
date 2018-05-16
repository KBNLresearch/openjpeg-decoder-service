package nl.kb.jp2;

public class DecodedImageDims {

    private int width;
    private int height;

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    @Override
    public String toString() {
        return "DecodedImageDims{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
