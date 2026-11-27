/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.messenger;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Authenticated encryption for local archive payloads. Key lifecycle is managed by the caller. */
public final class MiugramArchiveCrypto {
    private static final byte[] MAGIC = { 'M', 'I', 'U', 1 };
    private static final int IV_BYTES = 12;
    private static final int TAG_BYTES = 16;
    public static final int MAX_PLAINTEXT_BYTES = 4 * 1024 * 1024;
    private MiugramArchiveCrypto() {}

    private static byte[] aad(long account) throws GeneralSecurityException {
        if (account <= 0) throw new GeneralSecurityException("Invalid account");
        return ByteBuffer.allocate(MAGIC.length + Long.BYTES).put(MAGIC).putLong(account).array();
    }

    public static byte[] seal(SecretKey key, long account, byte[] plaintext) throws GeneralSecurityException {
        if (key == null || plaintext == null || plaintext.length > MAX_PLAINTEXT_BYTES) {
            throw new GeneralSecurityException("Invalid archive payload");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        // Let the provider generate the nonce: Android Keystore requires this for encryption.
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        if (iv == null || iv.length != IV_BYTES) throw new GeneralSecurityException("Unexpected nonce length");
        cipher.updateAAD(aad(account));
        byte[] ciphertext = cipher.doFinal(plaintext);
        return ByteBuffer.allocate(MAGIC.length + IV_BYTES + ciphertext.length)
                .put(MAGIC).put(iv).put(ciphertext).array();
    }

    public static byte[] open(SecretKey key, long account, byte[] sealed) throws GeneralSecurityException {
        if (key == null || sealed == null || sealed.length < MAGIC.length + IV_BYTES + TAG_BYTES
                || sealed.length > MAGIC.length + IV_BYTES + TAG_BYTES + MAX_PLAINTEXT_BYTES
                || !Arrays.equals(MAGIC, Arrays.copyOf(sealed, MAGIC.length))) {
            throw new GeneralSecurityException("Invalid encrypted archive");
        }
        byte[] iv = Arrays.copyOfRange(sealed, MAGIC.length, MAGIC.length + IV_BYTES);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BYTES * 8, iv));
        cipher.updateAAD(aad(account));
        return cipher.doFinal(sealed, MAGIC.length + IV_BYTES, sealed.length - MAGIC.length - IV_BYTES);
    }
}
