/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.miugram;

import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.telegram.messenger.MiugramArchiveCandidate;
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
}
