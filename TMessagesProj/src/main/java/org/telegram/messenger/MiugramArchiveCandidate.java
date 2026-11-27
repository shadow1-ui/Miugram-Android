/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

/** Conservative extraction from already-cached Telegram messages.
 * This helper is not connected to updates, persistence, or the chat UI yet.
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

        boolean policyKnown = false;
        boolean peerProtected = true;
        boolean dialogExpires = true;
        if (dialogId > 0) {
            if (userFull != null && userFull.id == dialogId) {
                policyKnown = true;
                peerProtected = userFull.noforwards_peer_enabled || userFull.noforwards_my_enabled;
                dialogExpires = dialog.ttl_period != 0 || userFull.ttl_period != 0;
            }
        } else {
            // A migrated peer's protection needs its own verified state; omit it for now.
            if (chat != null && chatFull != null && chat.id == -dialogId && chatFull.id == -dialogId && chat.migrated_to == null) {
                policyKnown = true;
                peerProtected = chat.noforwards;
                dialogExpires = dialog.ttl_period != 0 || chatFull.ttl_period != 0;
            }
        }
        MiugramArchivePolicy.Evidence evidence = new MiugramArchivePolicy.Evidence();
        evidence.optedIn = optedIn;
        evidence.serverDeletion = serverDeletion;
        evidence.incoming = !message.out;
        evidence.ordinaryCloudChat = true;
        evidence.peerPolicyKnown = policyKnown;
        evidence.peerProtected = peerProtected;
        evidence.messageProtected = message.noforwards;
        evidence.hasExpiry = dialogExpires || message.ttl != 0 || message.ttl_period != 0
                || message.expire_date != 0 || message.destroyTime != 0 || message.destroyTimeMillis != 0
                || (message.media != null && message.media.ttl_seconds != 0);
        // Exclude entities/spoilers and forwarded content until provenance and formatting are preserved.
        evidence.ordinaryTextOnly = message instanceof TLRPC.TL_message && message.action == null
                && !message.from_scheduled && message.fwd_from == null && message.from_id != null
                && (message.media == null || message.media instanceof TLRPC.TL_messageMediaEmpty)
                && (message.entities == null || message.entities.isEmpty())
                && message.message != null && !message.message.isEmpty();
        if (!MiugramArchivePolicy.mayRetainText(evidence) || message.id <= 0 || message.date <= 0) return null;
        long sender = MessageObject.getPeerId(message.from_id);
        if (sender == 0) return null;
        return new Candidate(new MiugramTextArchive.Record(dialogId, sender, message.id, message.date, message.message), evidence);
    }
}
