/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.messenger;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/** Encrypted, per-account local store for eligible text snapshots.
 * Not wired to Telegram updates or the chat UI yet; callers must establish policy evidence.
 * All methods do disk/keystore work and must run off the main thread. */
public final class MiugramTextArchive {
    private static final int MAX_RECORDS = 2000;
    private static final int MAX_TEXT_BYTES = 16 * 1024;
    private static final int MAX_SEALED_BYTES = MiugramArchiveCrypto.MAX_PLAINTEXT_BYTES + 32;
    private MiugramTextArchive() {}

    public static final class Record {
        public final long dialogId, senderId;
        public final int messageId, date;
        public final String text;
        public Record(long dialogId, long senderId, int messageId, int date, String text) {
            this.dialogId = dialogId; this.senderId = senderId; this.messageId = messageId;
            this.date = date; this.text = text;
        }
    }

    private static String alias(long account) throws GeneralSecurityException {
        if (account <= 0) throw new GeneralSecurityException("Invalid account");
        return "miugram.text-archive.v1." + account;
    }

    private static AtomicFile file(long account) {
        return new AtomicFile(new File(ApplicationLoader.applicationContext.getNoBackupFilesDir(),
                "miugram-text-archive-" + account + ".bin"));
    }

    private static boolean fileExists(AtomicFile atomic) {
        File base = atomic.getBaseFile();
        return base.exists() || new File(base.getPath() + ".bak").exists();
    }

    private static SecretKey key(long account, boolean create) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        String alias = alias(account);
        java.security.Key existing = store.getKey(alias, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        if (existing != null || !create) throw new GeneralSecurityException("Archive key unavailable");
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }

    private static byte[] readBounded(AtomicFile atomic) throws IOException {
        try (FileInputStream in = atomic.openRead(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > MAX_SEALED_BYTES) throw new IOException("Archive too large");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static JSONArray read(long account, AtomicFile atomic) throws Exception {
        if (!fileExists(atomic)) return new JSONArray();
        byte[] plain = MiugramArchiveCrypto.open(key(account, false), account, readBounded(atomic));
        try {
            JSONArray result = new JSONArray(new String(plain, StandardCharsets.UTF_8));
            if (result.length() > MAX_RECORDS) throw new JSONException("Too many archive records");
            return result;
        } finally {
            java.util.Arrays.fill(plain, (byte) 0);
        }
    }

    /** Save only after the caller has positively established all policy preconditions. */
    public static synchronized void put(long account, Record record, MiugramArchivePolicy.Evidence evidence) throws Exception {
        if (!MiugramArchivePolicy.mayRetainText(evidence)) throw new SecurityException("Ineligible archive snapshot");
        alias(account);
        if (record == null || record.dialogId == 0 || record.messageId <= 0 || record.date <= 0
                || record.text == null || record.text.isEmpty()
                || record.text.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES) {
            throw new IllegalArgumentException("Invalid text snapshot");
        }
        AtomicFile atomic = file(account);
        boolean existed = fileExists(atomic);
        JSONArray records = read(account, atomic);
        for (int i = records.length() - 1; i >= 0; --i) {
            JSONObject old = records.getJSONObject(i);
            if (old.getLong("dialog") == record.dialogId && old.getInt("id") == record.messageId) records.remove(i);
        }
        if (records.length() >= MAX_RECORDS) throw new IOException("Archive is full");
        JSONObject entry = new JSONObject();
        entry.put("dialog", record.dialogId); entry.put("sender", record.senderId);
        entry.put("id", record.messageId); entry.put("date", record.date); entry.put("text", record.text);
        records.put(entry);
        write(account, atomic, records, !existed);
    }

    private static void write(long account, AtomicFile atomic, JSONArray records, boolean createKey) throws Exception {
        byte[] plain = records.toString().getBytes(StandardCharsets.UTF_8);
        byte[] sealed;
        try {
            sealed = MiugramArchiveCrypto.seal(key(account, createKey), account, plain);
        } finally {
            java.util.Arrays.fill(plain, (byte) 0);
        }
        FileOutputStream out = null;
        try {
            out = atomic.startWrite(); out.write(sealed); atomic.finishWrite(out);
        } catch (Exception e) {
            if (out != null) atomic.failWrite(out);
            throw e;
        }
    }

    /** Remove a single chat without modifying other chats. No data is overwritten on read failure. */
    public static synchronized void removeDialog(long account, long dialogId) throws Exception {
        alias(account);
        if (dialogId == 0) throw new IllegalArgumentException("Invalid dialog");
        AtomicFile atomic = file(account);
        if (!fileExists(atomic)) return;
        JSONArray records = read(account, atomic);
        boolean changed = false;
        for (int i = records.length() - 1; i >= 0; --i) {
            if (records.getJSONObject(i).getLong("dialog") == dialogId) {
                records.remove(i);
                changed = true;
            }
        }
        if (!changed) return;
        if (records.length() == 0) clear(account);
        else write(account, atomic, records, false);
    }

    public static synchronized ArrayList<Record> forDialog(long account, long dialogId) throws Exception {
        alias(account);
        ArrayList<Record> result = new ArrayList<>();
        JSONArray records = read(account, file(account));
        for (int i = 0; i < records.length(); i++) {
            JSONObject entry = records.getJSONObject(i);
            if (entry.getLong("dialog") == dialogId) result.add(new Record(dialogId,
                    entry.getLong("sender"), entry.getInt("id"), entry.getInt("date"), entry.getString("text")));
        }
        return result;
    }

    /** Discard all local snapshots for this account and the associated keystore key. */
    public static synchronized void clear(long account) throws Exception {
        String name = alias(account);
        AtomicFile atomic = file(account);
        atomic.delete();
        if (fileExists(atomic)) throw new IOException("Could not delete archive");
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(name)) store.deleteEntry(name);
    }
}
