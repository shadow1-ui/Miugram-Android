/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.messenger;

/** Local visual-detail heuristic for an initial, editable profile-header crop. */
public final class MiugramAutoFrame {
    private MiugramAutoFrame() {}

    private static int gray(int pixel) {
        return (((pixel >> 16) & 255) * 299 + ((pixel >> 8) & 255) * 587 + (pixel & 255) * 114) / 1000;
    }

    /** Returns left, top, right, bottom in [0,1]. No network or face recognition. */
    public static float[] choose(int[] pixels, int gridWidth, int gridHeight, int imageWidth, int imageHeight) {
        if (pixels == null || gridWidth < 2 || gridHeight < 2 || imageWidth < 1 || imageHeight < 1
                || (long) gridWidth * gridHeight > pixels.length) {
            return new float[] {0f, 0f, 1f, 1f};
        }
        double total = 0, xsum = 0, ysum = 0;
        long edgeTotal = 0;
        for (int y = 1; y < gridHeight; y++) {
            for (int x = 1; x < gridWidth; x++) {
                int at = y * gridWidth + x;
                int value = gray(pixels[at]);
                int edge = Math.abs(value - gray(pixels[at - 1])) + Math.abs(value - gray(pixels[at - gridWidth]));
                edgeTotal += edge;
                double nx = (x + .5) / gridWidth, ny = (y + .5) / gridHeight;
                double centerBias = 1.0 - .18 * (Math.abs(nx - .5) + Math.abs(ny - .5));
                double weight = (edge + 2) * centerBias;
                total += weight;
                xsum += nx * weight;
                ysum += ny * weight;
            }
        }
        double cx = edgeTotal > 0 && total > 0 ? xsum / total : .5;
        double cy = edgeTotal > 0 && total > 0 ? ysum / total : .5;
        final double desiredRatio = 1.8;
        double imageRatio = (double) imageWidth / imageHeight;
        double cropW = imageRatio > desiredRatio ? desiredRatio / imageRatio : 1.0;
        double cropH = imageRatio > desiredRatio ? 1.0 : imageRatio / desiredRatio;
        double left = Math.max(0, Math.min(1.0 - cropW, cx - cropW / 2.0));
        double top = Math.max(0, Math.min(1.0 - cropH, cy - cropH / 2.0));
        return new float[] {(float) left, (float) top, (float) (left + cropW), (float) (top + cropH)};
    }
}
