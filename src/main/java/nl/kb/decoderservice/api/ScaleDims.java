package nl.kb.decoderservice.api;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import nl.kb.jp2.Jp2Header;

public class ScaleDims {
    @Override
    public String toString() {
        return "ScaleDims{" +
                "h=" + h +
                ", w=" + w +
                '}';
    }

    private int h;
    private int w;

    public ScaleDims(Jp2Header jp2Header) {
        h = jp2Header.getY1();
        w = jp2Header.getX1();
    }

    private ScaleDims(int w, int h) {
        this.w = w;
        this.h = h;
    }

    private ScaleDims(int... dims) {
        this.w = dims[0];
        this.h = dims[1];
    }

    public static ScaleDims parseAndDetermine(String sizeParam, Region region) {
        if (sizeParam.matches("^pct:[0-9.]+$")) {
            final double s = Double.parseDouble(sizeParam.replaceAll("^pct:", "")) / 100.0;
            return new ScaleDims((int) Math.round(s * region.getW()), (int) Math.round(s * region.getH()));
        } else if (sizeParam.matches("^[0-9]+,[0-9]+$")) {
            final int[] dims = Lists.newArrayList(sizeParam.split(","))
                    .stream().mapToInt(Integer::parseInt).toArray();
            return new ScaleDims(dims);
        } else if (sizeParam.matches("^[0-9]+,$")) {
            final int w = Integer.parseInt(sizeParam.replaceAll(",", ""));
            final int h = (int) Math.ceil(((double) w / (double) region.getW()) * region.getH());
            return new ScaleDims(w, h);
        } else if (sizeParam.matches("^,[0-9]+$")) {
            final int h = Integer.parseInt(sizeParam.replaceAll(",", ""));
            final int w = (int) Math.ceil(((double) h / (double) region.getH()) * region.getW());
            return new ScaleDims(w, h);
        } else /* sizeParam = full or max*/ {
          return new ScaleDims(region.getW(), region.getH());
        }
    }

    public int getH() {
        return h;
    }

    public int getW() {
        return w;
    }

    public boolean isValid() {
        return w > 0 && h > 0;
    }

    public void setW(Integer w) {
        this.w = w;
    }

    public void setH(int h) {
        this.h = h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScaleDims scaleDims = (ScaleDims) o;
        return h == scaleDims.h &&
                w == scaleDims.w;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(h, w);
    }
}
