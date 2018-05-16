package nl.kb.decoderservice.api;

import com.google.common.base.Objects;
import nl.kb.jp2.Jp2Header;

public class Region {
    private int x;
    private int y;
    private int w;
    private int h;

    @Override
    public String toString() {
        return "Region{" +
                "x=" + x +
                ", y=" + y +
                ", w=" + w +
                ", h=" + h +
                '}';
    }

    public Region(Jp2Header jp2Header) {
        x = 0;
        y = 0;
        w = jp2Header.getX1();
        h = jp2Header.getY1();
    }

    public Region(int x, int y, int w, int h) {

        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static Region parseAndDetermine(String raw, int imgW, int imgH) {
        if (raw.equals("square")) {
            return getSquare(imgW, imgH);
        } else if (raw.startsWith("pct:") && raw.matches("^pct:[0-9.]+,[0-9.]+,[0-9.]+,[0-9.]+$")) {
            final String[] dims = raw.replaceAll("^pct:","").split(",");
            return new Region(
                    (int) ((Double.parseDouble(dims[0]) / 100.0) * imgW),
                    (int) ((Double.parseDouble(dims[1]) / 100.0)  * imgH),
                    (int) ((Double.parseDouble(dims[2]) / 100.0)  * imgW),
                    (int) ((Double.parseDouble(dims[3]) / 100.0)  * imgH));

        } else if (raw.matches("^[0-9]+,[0-9]+,[0-9]+,[0-9]+$")) {
            final String[] dims = raw.split(",");
            return new Region(Integer.parseInt(dims[0]),
                    Integer.parseInt(dims[1]),
                    Integer.parseInt(dims[2]),
                    Integer.parseInt(dims[3]));

        } else /* if raw.equals("full") */{
            return new Region(0,0, imgW, imgH);
        }
    }

    private static Region getSquare(int imgW, int imgH) {
        return imgH < imgW
               ? new Region((int) ((imgW - imgH) * 0.5), 0, imgH, imgH)
               : new Region(0, (int) ((imgH - imgW) * 0.5), imgW, imgW);

    }

    public int getW() {
        return w;
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public int getH() {
        return h;
    }

    public boolean isValid() {
        return x >= 0 && y >= 0 && w > 0 && h > 0;
    }

    public void setW(int w) {
        this.w = w;
    }

    public void setH(int h) {
        this.h = h;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Region rotatedForRequest(Jp2Header jp2Header, int rParam) {
        switch (rParam) {
            case 90:
                final int newY90 = jp2Header.getY1() - (this.x + this.w);
                return new Region(
                        this.y,
                        newY90 < 0 ? 0 : newY90,
                        this.h,
                        this.w
                );
            case 180:
                final int newX180 = jp2Header.getX1() - (this.x + this.w);
                final int newY180 = jp2Header.getY1() - (this.y + this.h);
                return new Region(
                        newX180 < 0 ? 0 : newX180,
                        newY180 < 0 ? 0 : newY180,
                        this.w,
                        this.h
                );
            case 270:
                final int newX270 = jp2Header.getX1() - (this.y + this.h);
                return new Region(
                        newX270 < 0 ? 0 : newX270,
                        this.x,
                        this.h,
                        this.w
                );
            default:
                return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Region region = (Region) o;
        return x == region.x &&
                y == region.y &&
                w == region.w &&
                h == region.h;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y, w, h);
    }
}
