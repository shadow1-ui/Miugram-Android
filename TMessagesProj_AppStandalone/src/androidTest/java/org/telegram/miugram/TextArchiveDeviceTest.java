/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.miugram;

import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MiugramTextArchive;
import org.telegram.messenger.MiugramArchivePolicy;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

/** Synthetic data only. Does not exercise server deletions or chat rendering. */
@RunWith(AndroidJUnit4.class)
public final class TextArchiveDeviceTest {
    @Test public void keystorePersistenceTamperAndCleanup() throws Exception {
        // Reserved synthetic IDs far outside the ordinary Telegram user-ID range.
        long account = 8500000000000000001L;
        long other = 8500000000000000002L;
        if (ApplicationLoader.applicationContext == null) {
            ApplicationLoader.applicationContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getApplicationContext();
        }
        MiugramTextArchive.clear(account);
        MiugramTextArchive.clear(other);
        try {
            MiugramArchivePolicy.Evidence evidence = new MiugramArchivePolicy.Evidence();
            MiugramTextArchive.Record first = new MiugramTextArchive.Record(100, 200, 3, 1700000000, "first secret");
            try {
                MiugramTextArchive.put(account, first, evidence);
                fail("Missing evidence must be rejected");
            } catch (SecurityException expected) { }
            evidence.optedIn = evidence.serverDeletion = evidence.incoming = true;
            evidence.ordinaryCloudChat = evidence.peerPolicyKnown = evidence.ordinaryTextOnly = true;
            evidence.peerProtected = evidence.messageProtected = evidence.hasExpiry = false;
            MiugramTextArchive.put(account, first, evidence);
            MiugramTextArchive.put(account, new MiugramTextArchive.Record(100, 200, 3, 1700000000, "updated secret"), evidence);
            MiugramTextArchive.put(account, new MiugramTextArchive.Record(101, 201, 4, 1700000001, "different chat"), evidence);
            ArrayList<MiugramTextArchive.Record> rows = MiugramTextArchive.forDialog(account, 100);
            assertEquals(1, rows.size());
            assertEquals("updated secret", rows.get(0).text);
            assertTrue(MiugramTextArchive.forDialog(other, 100).isEmpty());
            File stored = new File(ApplicationLoader.applicationContext.getNoBackupFilesDir(),
                    "miugram-text-archive-" + account + ".bin");
            byte[] encrypted = Files.readAllBytes(stored.toPath());
            assertFalse(new String(encrypted, StandardCharsets.ISO_8859_1).contains("updated secret"));
            encrypted[encrypted.length - 1] ^= 1;
            Files.write(stored.toPath(), encrypted);
            try {
                MiugramTextArchive.forDialog(account, 100);
                fail("Tampered data was accepted");
            } catch (java.security.GeneralSecurityException expected) { }
        } finally {
            MiugramTextArchive.clear(account);
            MiugramTextArchive.clear(other);
        }
        assertTrue(MiugramTextArchive.forDialog(account, 100).isEmpty());
    }
}
