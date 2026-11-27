import org.telegram.messenger.MiugramAutoFrame;
import java.util.Arrays;

public class MiugramAutoFrameTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        float[] invalid = MiugramAutoFrame.choose(null, 32, 32, 100, 100);
        check(invalid[0] == 0f && invalid[3] == 1f);
        int[] image = new int[32 * 32];
        Arrays.fill(image, 0xff808080);
        float[] portrait = MiugramAutoFrame.choose(image, 32, 32, 1000, 1800);
        check(portrait[0] == 0f && portrait[2] == 1f);
        check(portrait[1] >= 0f && portrait[3] <= 1f && portrait[3] > portrait[1]);
        check(Math.abs((portrait[1] + portrait[3]) / 2f - .5f) < 1e-6);
        float[] wide = MiugramAutoFrame.choose(image, 32, 32, 3600, 1000);
        check(wide[0] >= 0f && wide[2] <= 1f && wide[2] > wide[0]);
        check(wide[1] == 0f && wide[3] == 1f);
        for (int y = 3; y < 12; y++) for (int x = 3; x < 12; x++) image[y * 32 + x] = ((x + y) % 2 == 0) ? 0xff000000 : 0xffffffff;
        float[] detail = MiugramAutoFrame.choose(image, 32, 32, 1000, 1800);
        check(detail[1] < portrait[1]);
        check(detail[1] >= 0f && detail[3] <= 1f);
        System.out.println("Miugram automatic frame bounds and visual-detail tests passed.");
    }
}
