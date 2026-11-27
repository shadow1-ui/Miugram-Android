/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.messenger;

/** Fail-closed eligibility rules for a future local deletion archive.
 *  This policy alone does not capture, store, or display any messages. */
public final class MiugramArchivePolicy {
    private MiugramArchivePolicy() {}

    /** Defaults deny retention until each property has been positively established. */
    public static final class Evidence {
        public boolean optedIn, serverDeletion, incoming, ordinaryCloudChat, peerPolicyKnown;
        public boolean peerProtected = true, messageProtected = true, hasExpiry = true;
        public boolean ordinaryTextOnly;
    }

    public static boolean mayRetainText(Evidence evidence) {
        return evidence != null && mayRetainText(evidence.optedIn, evidence.serverDeletion,
                evidence.incoming, evidence.ordinaryCloudChat, evidence.peerPolicyKnown,
                evidence.peerProtected, evidence.messageProtected, evidence.hasExpiry,
                evidence.ordinaryTextOnly);
    }

    /** Caller must positively establish every safety property, not treat unknown as safe. */
    public static boolean mayRetainText(boolean optedIn, boolean serverDeletion,
            boolean incoming, boolean ordinaryCloudChat, boolean peerPolicyKnown,
            boolean peerProtected, boolean messageProtected, boolean hasExpiry,
            boolean ordinaryTextOnly) {
        return optedIn && serverDeletion && incoming && ordinaryCloudChat && peerPolicyKnown
                && !peerProtected && !messageProtected && !hasExpiry && ordinaryTextOnly;
    }
}
