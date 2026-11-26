import org.telegram.messenger.MiugramGhostPolicy;

public class MiugramGhostPolicyTest {
    public static void main(String[] args) {
        for (boolean enabled : new boolean[]{false, true}) {
            for (boolean encrypted : new boolean[]{false, true}) {
                for (boolean expiring : new boolean[]{false, true}) {
                    boolean expected = enabled && !encrypted && !expiring;
                    if (MiugramGhostPolicy.suppress(enabled, encrypted, expiring) != expected) {
                        throw new AssertionError("ghost policy: " + enabled + ", " + encrypted + ", " + expiring);
                    }
                }
            }
        }
        System.out.println("Ghost policy: all eight state combinations passed.");
    }
}
