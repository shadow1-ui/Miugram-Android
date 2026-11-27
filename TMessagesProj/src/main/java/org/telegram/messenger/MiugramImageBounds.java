/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.messenger;

/** Fit an image within a pixel bound without enlarging it. */
public final class MiugramImageBounds {
    private MiugramImageBounds() {}
    public static int[] fit(int width, int height, int limit) {
        if (width <= 0 || height <= 0 || limit <= 0) throw new IllegalArgumentException("Invalid image bounds");
        int longest = Math.max(width, height);
        if (longest <= limit) return new int[] {width, height};
        return new int[] {
            (int) Math.max(1L, ((long) width * limit + longest / 2L) / longest),
            (int) Math.max(1L, ((long) height * limit + longest / 2L) / longest)
        };
    }
}
