package org.dnsmanager.app.status;

import org.dnsmanager.app.site.Site;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds what the Connection details page ({@code assets/connection.html}) receives: the status and what
 * AdGuard's DNS check saw, and the latency rows. Plain Java (ConnectionReportTest).
 *
 * <p>The page gets <b>codes and numbers</b>, never sentences built from network data: {@code "dot"},
 * {@code "private"}, {@code 23} (ms). It turns them into words itself and inserts them with
 * {@code textContent}. Every code from AdGuard was already checked by {@link DnsCheck}.</p>
 */
public final class ConnectionReport {

    private ConnectionReport() {
        // Only static methods.
    }

    /**
     * @param state       the icon's state, or null while nothing is known yet
     * @param sentence    the icon's explanation (ConnectionStatus.description)
     * @param color       the icon's colour (0xAARRGGBB; 0 = none yet)
     * @param check       the latest DNS check, or null
     * @param privateDnsHost Android's Private DNS host name; null in "automatic" mode or when off
     * @return {state, sentence, color, checking, check:{outcome, protocol, encrypted, serverType, deviceId,
     *         location, node} | null, privateDns:{active, host}, servers:{name: host}}
     */
    public static String status(StatusRules.State state, String sentence, int color, boolean checking,
                                DnsCheck.Result check, boolean privateDnsActive, String privateDnsHost) {
        try {
            JSONObject o = new JSONObject();
            o.put("state", state == null ? JSONObject.NULL : state.name());
            o.put("sentence", sentence);
            o.put("color", color == 0 ? JSONObject.NULL : hexColor(color));
            o.put("checking", checking);
            if (check == null) {
                o.put("check", JSONObject.NULL);
            } else {
                JSONObject c = new JSONObject();
                c.put("outcome", check.outcome.name());
                c.put("protocol", check.protocol);
                c.put("encrypted", check.encrypted());
                c.put("serverType", check.serverType);
                c.put("deviceId", check.deviceId);
                c.put("location", check.location);
                c.put("node", check.node);
                o.put("check", c);
            }
            JSONObject p = new JSONObject();
            p.put("active", privateDnsActive);
            p.put("host", privateDnsHost == null ? JSONObject.NULL : privateDnsHost);
            o.put("privateDns", p);
            JSONObject servers = new JSONObject();
            for (Map.Entry<String, String> s : Site.LATENCY_SERVERS.entrySet()) {
                servers.put(s.getKey(), s.getValue());
            }
            o.put("servers", servers);
            return o.toString();
        } catch (JSONException e) {
            return "{}"; // can't happen with these value types; the page shows "Checking…"
        }
    }

    /**
     * @param rows what to list (Latency.forDisplay), possibly while measuring is still going on
     * @param done true when every server has been measured
     * @return {done, ipv6:(any IPv6 row listed), rows:[{server, ip, ms (number, or null = no answer)}]}
     */
    public static String latency(List<Latency.Row> rows, boolean done) {
        try {
            JSONObject o = new JSONObject();
            o.put("done", done);
            JSONArray list = new JSONArray();
            boolean ipv6 = false;
            for (Latency.Row r : rows) {
                JSONObject row = new JSONObject();
                row.put("server", r.server);
                row.put("ip", r.ipVersion);
                row.put("ms", r.answered() ? (Object) milliseconds(r.micros) : JSONObject.NULL);
                list.put(row);
                ipv6 |= r.ipVersion == 6;
            }
            o.put("ipv6", ipv6);
            o.put("rows", list);
            return o.toString();
        } catch (JSONException e) {
            return "{\"done\":true,\"ipv6\":false,\"rows\":[]}";
        }
    }

    /** @return microseconds as milliseconds with one decimal below 10 ms (3.4), whole above (23). */
    static double milliseconds(long micros) {
        double ms = micros / 1000.0;
        return ms < 10 ? Math.round(ms * 10) / 10.0 : Math.round(ms);
    }

    /** @return "#rrggbb" for an 0xAARRGGBB colour (alpha dropped: the icon colours are opaque). */
    static String hexColor(int argb) {
        return String.format(Locale.ROOT, "#%06x", argb & 0xFFFFFF);
    }
}
