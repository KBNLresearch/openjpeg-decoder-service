package nl.kb.jp2;

public class MatrixRotate {
    static int rotate(int offset, int width, int height, int deg) {
        switch (deg) {
            case 90:
                return rot90(offset, width, height);
            case 180:
                return rot180(offset, width, height);
            case 270:
                return rot270(offset, width, height);
            default:
                return offset;
        }
    }

    /* = 270 counter clockwise (height becomes width) */
    private static int rot90(int offset, int width, int height) {
        int y = (int) Math.floor(offset / width);
        int x = offset - (y * width);
        final int[] rot90 = rot90(x, y, width, height);
        return rot90[1] * height + rot90[0];
    }

    private static int rot180(int offset, int width, int height) {
        int y = (int) Math.floor(offset / width);
        int x = offset - (y * width);
        final int[] rot180 = rot180(x, y, width, height);
        return rot180[1] * width + rot180[0];
    }

    /* = 90 counter clockwise (height becomes width) */
    private static int rot270(int offset, int width, int height) {
        int y = (int) Math.floor(offset / width);
        int x = offset - (y * width);
        final int[] rot270 = rot270(x, y, width, height);

        return rot270[1] * height + rot270[0];
    }

    private static int[] rot90(int x, int y, int width, int height) {
        return new int[] { -y + (height - 1), x };
    }

    private static int[] rot180(int x, int y, int width, int height) {
        return new int[] { -x + (width - 1), -y + (height - 1) };
    }

    private static int[] rot270(int x, int y, int width, int height) {
        return new int[] { y, -x + (width - 1) };
    }
}
