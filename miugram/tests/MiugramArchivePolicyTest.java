import org.telegram.messenger.MiugramArchivePolicy;

/** Exhaustive fail-closed test of the policy, not a UI or integration test. */
public final class MiugramArchivePolicyTest {
    public static void main(String[] args) {
        if (MiugramArchivePolicy.mayRetainText((MiugramArchivePolicy.Evidence) null)
                || MiugramArchivePolicy.mayRetainText(new MiugramArchivePolicy.Evidence())) {
            throw new AssertionError("Missing evidence must deny retention");
        }
        MiugramArchivePolicy.Evidence display = new MiugramArchivePolicy.Evidence();
        if (MiugramArchivePolicy.mayDisplay(display)) throw new AssertionError("Unverified display policy");
        display.optedIn = display.ordinaryCloudChat = display.peerPolicyKnown = true;
        display.peerProtected = display.hasExpiry = false;
        if (!MiugramArchivePolicy.mayDisplay(display)) throw new AssertionError("Safe display rejected");
        display.peerProtected = true;
        if (MiugramArchivePolicy.mayDisplay(display)) throw new AssertionError("Protected chat displayed");
        display.peerProtected = false; display.hasExpiry = true;
        if (MiugramArchivePolicy.mayDisplay(display)) throw new AssertionError("Timed chat displayed");
        int permitted = 0;
        for (int bits = 0; bits < (1 << 9); bits++) {
            boolean[] b = new boolean[9];
            for (int i = 0; i < b.length; i++) b[i] = (bits & (1 << i)) != 0;
            boolean result = MiugramArchivePolicy.mayRetainText(
                    b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8]);
            boolean expected = b[0] && b[1] && b[2] && b[3] && b[4]
                    && !b[5] && !b[6] && !b[7] && b[8];
            if (result != expected) throw new AssertionError("state " + bits);
            if (result) permitted++;
        }
        if (permitted != 1) throw new AssertionError("unexpected allowed states: " + permitted);
        System.out.println("Archive safety policy: 512 states checked; integration not implemented.");
    }
}
