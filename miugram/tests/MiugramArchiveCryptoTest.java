import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.telegram.messenger.MiugramArchiveCrypto;

/** Tests a codec only; Android Keystore, persistence, capture and UI remain untested. */
public final class MiugramArchiveCryptoTest {
    interface Operation { void run() throws Exception; }
    private static void mustReject(Operation op) throws Exception {
        try { op.run(); } catch (GeneralSecurityException expected) { return; }
        throw new AssertionError("Rejected input was accepted");
    }
    public static void main(String[] args) throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        byte[] text = "private message".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] first = MiugramArchiveCrypto.seal(key, 17, text);
        byte[] second = MiugramArchiveCrypto.seal(key, 17, text);
        if (Arrays.equals(first, second)) throw new AssertionError("nonce reuse");
        if (!Arrays.equals(text, MiugramArchiveCrypto.open(key, 17, first))) throw new AssertionError("round trip");
        mustReject(() -> MiugramArchiveCrypto.open(key, 18, first));
        mustReject(() -> MiugramArchiveCrypto.open(generator.generateKey(), 17, first));
        mustReject(() -> MiugramArchiveCrypto.open(key, 17, Arrays.copyOf(first, first.length - 1)));
        for (int i = 0; i < first.length; i++) {
            byte[] modified = first.clone(); modified[i] ^= 1;
            mustReject(() -> MiugramArchiveCrypto.open(key, 17, modified));
        }
        mustReject(() -> MiugramArchiveCrypto.seal(key, 0, text));
        mustReject(() -> MiugramArchiveCrypto.seal(key, 17, new byte[MiugramArchiveCrypto.MAX_PLAINTEXT_BYTES + 1]));
        System.out.println("Archive encryption codec: round trip, account binding and tamper rejection passed.");
    }
}
