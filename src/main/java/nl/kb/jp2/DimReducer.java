package nl.kb.jp2;

public class DimReducer {
    private DimReducer() { }

    public static int reduce(int size, int cp_reduce) {
        if (cp_reduce == 0) {
            return size;
        } else {
            return reduce((int)Math.round(size / 2d), cp_reduce - 1);
        }
    }
}
