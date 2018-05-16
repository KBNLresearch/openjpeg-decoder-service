package nl.kb.decoderservice.api;

import com.google.common.base.Objects;
import nl.kb.jp2.Jp2Header;

public class ParameterCompound {
    private final Jp2Header jp2Header;
    private final Region region;
    private final ScaleDims scaleDims;
    private final int deg;
    private final int cp_reduce;

    public ParameterCompound(Jp2Header jp2Header, Region region, ScaleDims scaleDims, int deg, int cp_reduce) {
        this.jp2Header = jp2Header;
        this.region = region;
        this.scaleDims = scaleDims;
        this.deg = deg;
        this.cp_reduce = cp_reduce;
    }

    public Jp2Header getJp2Header() {
        return jp2Header;
    }

    public Region getRegion() {
        return region;
    }

    public ScaleDims getScaleDims() {
        return scaleDims;
    }

    public int getDeg() {
        return deg;
    }

    public int getCp_reduce() {
        return cp_reduce;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterCompound that = (ParameterCompound) o;
        return deg == that.deg &&
                cp_reduce == that.cp_reduce &&
                Objects.equal(jp2Header, that.jp2Header) &&
                Objects.equal(region, that.region) &&
                Objects.equal(scaleDims, that.scaleDims);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(jp2Header, region, scaleDims, deg, cp_reduce);
    }

    @Override
    public String toString() {
        return "ParameterCompound{" +
                "jp2Header=" + jp2Header +
                ", region=" + region +
                ", scaleDims=" + scaleDims +
                ", deg=" + deg +
                ", cp_reduce=" + cp_reduce +
                '}';
    }
}
