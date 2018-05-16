package nl.kb.jp2;

import com.google.common.base.Objects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Jp2Header {
    private Object scaleFactors;

    private Jp2Header() { }
    private int x1;
    private int y1;
    private int tw;
    private int th;
    private int tdx;
    private int tdy;
    private int numRes;
    private int numComps;
    private String fileName;

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getTw() {
        return tw;
    }

    public void setTw(int tw) {
        this.tw = tw;
    }

    public int getTh() {
        return th;
    }

    public void setTh(int th) {
        this.th = th;
    }

    public int getTdx() {
        return tdx;
    }

    public void setTdx(int tdx) {
        this.tdx = tdx;
    }

    public int getTdy() {
        return tdy;
    }

    public void setTdy(int tdy) {
        this.tdy = tdy;
    }

    public int getNumRes() {
        return numRes;
    }

    public void setNumRes(int numRes) {
        this.numRes = numRes;
    }

    public int getNumComps() {
        return numComps;
    }

    public void setNumComps(int numComps) {
        this.numComps = numComps;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "Jp2Header{" +
                "x1=" + x1 +
                ", y1=" + y1 +
                ", tw=" + tw +
                ", th=" + th +
                ", tdx=" + tdx +
                ", tdy=" + tdy +
                ", numRes=" + numRes +
                ", numComps=" + numComps +
                '}';
    }

    public static Jp2Header read(File file) throws FileNotFoundException {
        try {
            final Jp2Header jp2Header = fromFile(file.getAbsolutePath());
            jp2Header.setFileName(file.getAbsolutePath());
            return jp2Header;
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to open file at: " + file.getAbsolutePath());
        }
    }

    private static native Jp2Header fromFile(String filename) throws IOException;

    public String getColorSpace() {
        return numComps > 1 ? "RGB" : "GrayScale";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Jp2Header jp2Header = (Jp2Header) o;
        return Objects.equal(fileName, jp2Header.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fileName);
    }
}
