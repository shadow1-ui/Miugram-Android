import java.util.Arrays;
import org.telegram.messenger.MiugramImageBounds;
public final class MiugramImageBoundsTest {
    private static void fit(int w, int h, int max, int x, int y) {
        if (!Arrays.equals(new int[]{x, y}, MiugramImageBounds.fit(w, h, max))) throw new AssertionError();
    }
    public static void main(String[] args) {
        fit(640, 480, 1600, 640, 480);
        fit(8000, 4000, 1600, 1600, 800);
        fit(500, 4000, 1600, 200, 1600);
        fit(Integer.MAX_VALUE, 1, 1600, 1600, 1);
        try { MiugramImageBounds.fit(0, 10, 1600); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Profile image bounds tests passed; bitmap behavior remains untested.");
    }
}
