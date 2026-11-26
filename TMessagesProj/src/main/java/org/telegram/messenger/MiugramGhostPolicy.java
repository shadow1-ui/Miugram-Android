/* Miugram ghost-mode policy. This file is licensed under GPL-2.0-or-later. */
package org.telegram.messenger;

/** Pure policy; network timing, other sessions and all client paths still need integration tests. */
public final class MiugramGhostPolicy {
    private MiugramGhostPolicy() {}

    public static boolean suppress(boolean enabled, boolean encrypted, boolean expiring) {
        return enabled && !encrypted && !expiring;
    }
}
