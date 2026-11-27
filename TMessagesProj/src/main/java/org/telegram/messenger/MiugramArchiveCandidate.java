/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;
import java.nio.charset.StandardCharsets;

/** Conservative extraction from already-cached Telegram messages.
 * Experimental update capture uses this helper behind a disabled internal gate;
 * chat UI and policy-change cleanup remain unconnected.
 * Call only on the UI thread, where MessagesController's peer caches are accessed. */
public final class MiugramArchiveCandidate {
    private MiugramArchiveCandidate() {}

    public static final class Candidate {
        public final MiugramTextArchive.Record record;
        public final MiugramArchivePolicy.Evidence evidence;
        private Candidate(MiugramTextArchive.Record record, MiugramArchivePolicy.Evidence evidence) {
            this.record = record;
            this.evidence = evidence;
        }
    }

    public static Candidate fromServerDeletion(MessagesController controller,
            long selfId, long dialogId, TLRPC.Message message, boolean optedIn, boolean serverDeletion) {
        if (controller == null || dialogId == 0) return null;
        return fromKnownState(selfId, dialogId, message, controller.dialogs_dict.get(dialogId),
                dialogId > 0 ? controller.getUserFull(dialogId) : null,
                dialogId < 0 ? controller.getChat(-dialogId) : null,
                dialogId < 0 ? controller.getChatFull(-dialogId) : null,
                optedIn, serverDeletion);
    }

    /** The caller must supply current, authoritative peer objects for this same dialog. */
    public static Candidate fromKnownState(long selfId, long dialogId, TLRPC.Message message,
            TLRPC.Dialog dialog, TLRPC.UserFull userFull, TLRPC.Chat chat, TLRPC.ChatFull chatFull,
            boolean optedIn, boolean serverDeletion) {
        if (message == null || selfId <= 0 || dialogId == 0 || dialogId == selfId
                || DialogObject.isEncryptedDialog(dialogId) || DialogObject.isFolderDialogId(dialogId)
                || MessageObject.getDialogId(message) != dialogId || dialog == null || dialog.id != dialogId) return null;

        MiugramArchivePolicy.Evidence evidence = currentDialogEvidence(
                selfId, dialogId, dialog, userFull, chat, chatFull, optedIn);
        evidence.serverDeletion = serverDeletion;
        evidence.incoming = !message.out;
        evidence.messageProtected = message.noforwards;
        evidence.hasExpiry = evidence.hasExpiry || message.ttl != 0 || message.ttl_period != 0
                || message.expire_date != 0 || message.destroyTime != 0 || message.destroyTimeMillis != 0
                || (message.media != null && message.media.ttl_seconds != 0);
        // Exclude entities/spoilers and forwarded content until provenance and formatting are preserved.
        evidence.ordinaryTextOnly = message instanceof TLRPC.TL_message && message.action == null
                && !message.from_scheduled && message.fwd_from == null && message.from_id != null
                && (message.media == null || message.media instanceof TLRPC.TL_messageMediaEmpty)
                && (message.entities == null || message.entities.isEmpty())
                && message.message != null && !message.message.isEmpty()
                && message.message.getBytes(StandardCharsets.UTF_8).length <= 16 * 1024;
        if (!MiugramArchivePolicy.mayRetainText(evidence) || message.id <= 0 || message.date <= 0) return null;
        long sender = MessageObject.getPeerId(message.from_id);
        if (sender == 0) return null;
        return new Candidate(new MiugramTextArchive.Record(dialogId, sender, message.id, message.date, message.message), evidence);
    }
    /** Snapshot current authoritative dialog policy for a future archive read.
     * Unknown or mismatched peers are denied, not treated as unprotected. */
    public static MiugramArchivePolicy.Evidence currentDialogEvidence(long selfId, long dialogId,
            TLRPC.Dialog dialog, TLRPC.UserFull userFull, TLRPC.Chat chat, TLRPC.ChatFull chatFull,
            boolean optedIn) {
        MiugramArchivePolicy.Evidence evidence = new MiugramArchivePolicy.Evidence();
        evidence.optedIn = optedIn;
        if (selfId <= 0 || dialogId == 0 || dialogId == selfId
                || DialogObject.isEncryptedDialog(dialogId) || DialogObject.isFolderDialogId(dialogId)
                || dialog == null || dialog.id != dialogId) return evidence;
        evidence.ordinaryCloudChat = true;
        if (dialogId > 0 && userFull != null && userFull.id == dialogId) {
            evidence.peerPolicyKnown = true;
            evidence.peerProtected = userFull.noforwards_peer_enabled || userFull.noforwards_my_enabled;
            evidence.hasExpiry = dialog.ttl_period != 0 || userFull.ttl_period != 0;
        } else if (dialogId < 0 && chat != null && chatFull != null && chat.id == -dialogId
                && chatFull.id == -dialogId && chat.migrated_to == null) {
            evidence.peerPolicyKnown = true;
            evidence.peerProtected = chat.noforwards;
            evidence.hasExpiry = dialog.ttl_period != 0 || chatFull.ttl_period != 0;
        }
        return evidence;
    }

}
