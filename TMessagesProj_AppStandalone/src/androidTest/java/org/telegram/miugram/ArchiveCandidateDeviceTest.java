/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.miugram;

import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.telegram.messenger.MiugramArchiveCandidate;
import org.telegram.messenger.MiugramArchivePolicy;
import org.telegram.messenger.MiugramTextArchive;
import org.telegram.messenger.ApplicationLoader;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import org.telegram.tgnet.TLRPC;

/** Exercises candidate extraction with synthetic objects, not server updates or chat rendering. */
@RunWith(AndroidJUnit4.class)
public final class ArchiveCandidateDeviceTest {
    @Test public void rejectProtectedTimedUnknownAndNonTextMessages() {
        TLRPC.TL_dialog dialog = new TLRPC.TL_dialog(); dialog.id = -123;
        TLRPC.TL_chat chat = new TLRPC.TL_chat(); chat.id = 123;
        TLRPC.TL_chatFull full = new TLRPC.TL_chatFull(); full.id = 123;
        TLRPC.TL_message msg = new TLRPC.TL_message();
        msg.id = 42; msg.date = 1700000000; msg.dialog_id = -123; msg.message = "synthetic text";
        TLRPC.TL_peerUser sender = new TLRPC.TL_peerUser(); sender.user_id = 456; msg.from_id = sender;
        msg.media = new TLRPC.TL_messageMediaEmpty();
        assertNotNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true));
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, false, true));
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, false));
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, null, true, true));
        chat.noforwards = true;
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true));
        chat.noforwards = false; msg.noforwards = true;
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true));
        msg.noforwards = false; full.ttl_period = 60;
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true));
        full.ttl_period = 0; msg.ttl_period = 60;
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true));
        msg.ttl_period = 0; msg.out = true;
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true));
        msg.out = false; msg.entities.add(new TLRPC.TL_messageEntityBold());
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true));
        msg.entities.clear(); msg.fwd_from = new TLRPC.TL_messageFwdHeader();
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true));
        msg.fwd_from = null; dialog.id = -124;
        assertNull(MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true));
    }
    @Test public void storeCandidateBatchAtomically() throws Exception {
        long account = 8500000000000000003L;
        if (ApplicationLoader.applicationContext == null) {
            ApplicationLoader.applicationContext = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        }
        TLRPC.TL_dialog dialog = new TLRPC.TL_dialog(); dialog.id = -123;
        TLRPC.TL_chat chat = new TLRPC.TL_chat(); chat.id = 123;
        TLRPC.TL_chatFull full = new TLRPC.TL_chatFull(); full.id = 123;
        TLRPC.TL_peerUser sender = new TLRPC.TL_peerUser(); sender.user_id = 456;
        TLRPC.TL_message msg = new TLRPC.TL_message();
        msg.id = 42; msg.date = 1700000000; msg.dialog_id = -123;
        msg.message = "synthetic first"; msg.from_id = sender;
        MiugramArchiveCandidate.Candidate first = MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true);
        msg.id = 43; msg.message = "synthetic second";
        MiugramArchiveCandidate.Candidate second = MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true);
        assertNotNull(first); assertNotNull(second);
        ArrayList<MiugramArchiveCandidate.Candidate> batch = new ArrayList<>();
        batch.add(first); batch.add(second);
        MiugramTextArchive.clear(account);
        try {
            MiugramTextArchive.putCandidates(account, batch);
            MiugramArchivePolicy.Evidence visible = first.evidence;
            assertEquals(2, MiugramTextArchive.forDialog(account, -123, visible).size());
            second.evidence.peerProtected = true;
            try { MiugramTextArchive.putCandidates(account, batch); fail("protected candidate accepted"); }
            catch (SecurityException expected) { }
            assertEquals(2, MiugramTextArchive.forDialog(account, -123, visible).size());
        } finally { MiugramTextArchive.clear(account); }
    }

    @Test public void archiveRetainsNewestWithinCountAndEscapedByteLimits() throws Exception {
        final long account = 8500000000000000004L;
        if (ApplicationLoader.applicationContext == null) {
            ApplicationLoader.applicationContext = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        }
        TLRPC.TL_dialog dialog = new TLRPC.TL_dialog(); dialog.id = -123;
        TLRPC.TL_chat chat = new TLRPC.TL_chat(); chat.id = 123;
        TLRPC.TL_chatFull full = new TLRPC.TL_chatFull(); full.id = 123;
        TLRPC.TL_peerUser sender = new TLRPC.TL_peerUser(); sender.user_id = 456;
        TLRPC.TL_message msg = new TLRPC.TL_message();
        msg.date = 1700000000; msg.dialog_id = -123; msg.from_id = sender;
        ArrayList<MiugramArchiveCandidate.Candidate> batch = new ArrayList<>();
        MiugramArchivePolicy.Evidence evidence = null;
        MiugramTextArchive.clear(account);
        try {
            msg.message = "short";
            for (int id = 1; id <= 2010; ++id) {
                msg.id = id;
                MiugramArchiveCandidate.Candidate c = MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true);
                assertNotNull(c); batch.add(c); evidence = c.evidence;
            }
            MiugramTextArchive.putCandidates(account, batch);
            ArrayList<MiugramTextArchive.Record> rows = MiugramTextArchive.forDialog(account, -123, evidence);
            assertEquals(2000, rows.size());
            assertEquals(11, rows.get(0).messageId);
            assertEquals(2010, rows.get(rows.size() - 1).messageId);

            MiugramTextArchive.clear(account); batch.clear();
            // Quotes double in JSON and must be counted as serialized UTF-8 bytes.
            char[] characters = new char[8000]; java.util.Arrays.fill(characters, '"');
            msg.message = new String(characters);
            for (int id = 1; id <= 300; ++id) {
                msg.id = id;
                MiugramArchiveCandidate.Candidate c = MiugramArchiveCandidate.fromKnownState(7, -123, msg, dialog, null, chat, full, true, true);
                assertNotNull(c); batch.add(c);
            }
            MiugramTextArchive.putCandidates(account, batch);
            rows = MiugramTextArchive.forDialog(account, -123, evidence);
            assertTrue(rows.size() > 0 && rows.size() < 300);
            assertTrue(rows.get(0).messageId > 1);
            assertEquals(300, rows.get(rows.size() - 1).messageId);
            assertEquals(msg.message, rows.get(rows.size() - 1).text);
        } finally { MiugramTextArchive.clear(account); }
    }

}
