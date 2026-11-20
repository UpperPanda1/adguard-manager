package org.dnsmanager.app.status;

import org.dnsmanager.app.site.Site;

/**
 * Decides what the top bar's status icon shows: is this phone's DNS going through the service, and
 * is that connection encrypted? Plain Java (StatusRulesTest); status/ConnectionStatus collects the
 * two facts from Android and the network and asks this class.
 *
 * <p><b>Two sources</b></p>
 * <ol>
 *   <li>Android's <b>Private DNS</b> setting (Settings → Network → Private DNS): on with a host name
 *       of the service, on with something else, or off. Known at once, without the network.</li>
 *   <li>AdGuard's <b>DNS check</b> ({@link DnsCheck}): did a lookup of a name only AdGuard knows go
 *       through AdGuard, and with which protocol? This also catches set-ups Android can't see (AdGuard
 *       DNS configured on the router, or in a VPN app), so a clear answer wins over source 1.</li>
 * </ol>
 */
public final class StatusRules {

    /** What the icon can say, from best to worst. */
    public enum State {
        /** Using the service over an encrypted protocol. Green shield with a tick. */
        SECURE,
        /** Using the service, but unencrypted (plain DNS). Orange warning. */
        INSECURE,
        /** Private DNS is on but points somewhere else. Yellow warning. */
        OTHER_DNS,
        /** Not using the service. Red warning. */
        NOT_PROTECTED
    }

    private StatusRules() {
        // Only static methods.
    }

    /**
     * @param active     Android's {@code LinkProperties.isPrivateDnsActive()}
     * @param serverName Android's {@code getPrivateDnsServerName()}; null in "automatic" mode
     */
    public static State fromPrivateDns(boolean active, String serverName) {
        if (!active) {
            return State.NOT_PROTECTED;
        }
        return Site.isPrivateDnsHost(serverName) ? State.SECURE : State.OTHER_DNS;
    }

    /**
     * @return what the DNS check alone says: SECURE or INSECURE when AdGuard saw the lookup (by its
     *         protocol), NOT_PROTECTED when it didn't, null when the check gave no verdict
     */
    public static State fromCheck(DnsCheck.Result check) {
        if (check == null) {
            return null;
        }
        switch (check.outcome) {
            case IN_USE:
                return check.encrypted() ? State.SECURE : State.INSECURE;
            case NOT_IN_USE:
                return State.NOT_PROTECTED;
            default:
                return null;
        }
    }

    /**
     * @return what to show. The check wins when it has a verdict, with one exception: if it says "not in
     *         use" while Private DNS points to another service, "another DNS" (yellow) stays, because it says
     *         more. Without a verdict (no network, timeout, rate limit) the Private DNS state stands.
     */
    public static State combine(State privateDns, DnsCheck.Result check) {
        State fromCheck = fromCheck(check);
        if (fromCheck == null) {
            return privateDns;
        }
        if (fromCheck == State.NOT_PROTECTED && privateDns == State.OTHER_DNS) {
            return State.OTHER_DNS;
        }
        return fromCheck;
    }

    /**
     * @return the icon's colour (0xAARRGGBB) for a state, readable on the top bar in that theme
     *         (StatusRulesTest checks the contrast). The icon's shape differs too (tick vs. warning
     *         sign), so the state never depends on colour alone.
     */
    public static int color(State state, boolean darkTheme) {
        switch (state) {
            case SECURE:
                return darkTheme ? 0xFF75B798 : 0xFF198754;
            case INSECURE:
                return darkTheme ? 0xFFFD9843 : 0xFFC2570A;
            case OTHER_DNS:
                return darkTheme ? 0xFFFFDA6A : 0xFF946C00;
            default:
                return darkTheme ? 0xFFEA868F : 0xFFDC3545;
        }
    }
}
