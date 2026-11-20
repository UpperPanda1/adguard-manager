package org.dnsmanager.app.status;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import org.dnsmanager.app.R;
import org.dnsmanager.app.site.Site;
import org.dnsmanager.app.theme.Scheme;
import org.dnsmanager.app.theme.Themes;

import java.util.concurrent.Executor;

/**
 * Keeps the top bar's status icon up to date: "is this phone using the DNS service?" The rules are
 * in {@link StatusRules}; this class gathers the facts.
 *
 * <p><b>How:</b> between {@link #start} and {@link #stop} (the screen is visible) it listens to the
 * phone's active network. Whenever the network or its Private DNS setting changes, it shows the
 * Private DNS state at once and then, on a background thread, runs AdGuard's DNS check
 * ({@link DnsCheck}). Network calls must never run on the main thread (Android throws
 * {@code NetworkOnMainThreadException}), hence the executor; the result is posted back to the main
 * thread, the only thread allowed to touch views.
 * Docs: https://developer.android.com/develop/connectivity/network-ops/reading-network-state</p>
 *
 * <p>The check uses Java's built-in {@code HttpURLConnection}: one tiny request doesn't justify a library.</p>
 */
public final class ConnectionStatus {

    private static final String TAG = "ConnectionStatus";
    /** Several network callbacks arrive in a burst; test once after things settle. */
    private static final long TEST_DELAY_MS = 400;
    /**
     * A "not in use" answer is confirmed once, this much later, with a fresh name before the icon turns red:
     * just after a network switch, DNS can fail for a moment although the phone does use AdGuard DNS.
     */
    private static final long CONFIRM_DELAY_MS = 3000;

    private final Context context;
    private final ImageView icon;
    private final Executor background;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable startTest = () -> runTest(false);
    private final Runnable confirmTest = () -> runTest(true);

    private ConnectivityManager.NetworkCallback callback;
    private StatusRules.State privateDnsState = StatusRules.State.NOT_PROTECTED;
    private StatusRules.State state; // null until the first network callback
    private boolean darkTheme;
    // Counts network changes: a test answer that belongs to an older network is thrown away.
    private int generation;

    /**
     * @param icon       the top bar's status button
     * @param background where the network request runs
     */
    public ConnectionStatus(Context context, ImageView icon, Executor background) {
        this.context = context.getApplicationContext();
        this.icon = icon;
        this.background = background;
    }

    /** Starts listening (MainActivity.onStart). Safe to call twice. */
    public void start() {
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        if (manager == null || callback != null) {
            return;
        }
        callback = new ConnectivityManager.NetworkCallback() {
            // These run on a system thread, so everything is handed to the main thread.
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties link) {
                boolean active = link.isPrivateDnsActive();
                String server = link.getPrivateDnsServerName();
                main.post(() -> onNetwork(StatusRules.fromPrivateDns(active, server)));
            }

            @Override
            public void onLost(Network network) {
                main.post(() -> onNetwork(StatusRules.State.NOT_PROTECTED));
            }
        };
        try {
            // "Default network" = the one the phone uses for the internet right now (Wi-Fi, mobile, VPN).
            manager.registerDefaultNetworkCallback(callback);
        } catch (RuntimeException e) {
            Log.w(TAG, "Couldn't watch the network", e);
            callback = null;
        }
    }

    /** Stops listening (MainActivity.onStop), so nothing runs while the app is in the background. */
    public void stop() {
        main.removeCallbacks(startTest);
        main.removeCallbacks(confirmTest);
        generation++; // an answer still on its way is ignored
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        if (manager != null && callback != null) {
            try {
                manager.unregisterNetworkCallback(callback);
            } catch (RuntimeException e) {
                Log.w(TAG, "Network callback was already gone");
            }
        }
        callback = null;
    }

    /** @return the state on screen, or null while nothing is known yet. */
    public StatusRules.State state() {
        return state;
    }

    /** @return the sentence that explains the current state (also what screen readers say). */
    public String description() {
        if (state == null) {
            return context.getString(R.string.status_unknown);
        }
        switch (state) {
            case SECURE:
                return context.getString(R.string.status_secure, Site.NAME);
            case INSECURE:
                return context.getString(R.string.status_insecure, Site.NAME);
            case OTHER_DNS:
                return context.getString(R.string.status_other_dns, Site.NAME);
            default:
                return context.getString(R.string.status_not_protected, Site.NAME);
        }
    }

    /** Repaints the icon for the theme (MainActivity.applyTheme). */
    public void setDarkTheme(boolean dark) {
        darkTheme = dark;
        show();
    }

    private void onNetwork(StatusRules.State fromPrivateDns) {
        generation++;
        privateDnsState = fromPrivateDns;
        state = fromPrivateDns;
        show();
        main.removeCallbacks(startTest);
        main.removeCallbacks(confirmTest);
        main.postDelayed(startTest, TEST_DELAY_MS);
    }

    /** @param confirming true for the second check that confirms a first "not in use" answer */
    private void runTest(boolean confirming) {
        int mine = generation;
        background.execute(() -> {
            DnsCheck.Result check = DnsCheck.run();
            main.post(() -> {
                if (mine != generation) {
                    return; // the network changed meanwhile: this answer is about the old one
                }
                if (check.outcome == DnsCheck.Outcome.NOT_IN_USE && !confirming) {
                    main.postDelayed(confirmTest, CONFIRM_DELAY_MS); // ask once more before saying "no"
                    return;
                }
                state = StatusRules.combine(privateDnsState, check);
                show();
            });
        });
    }

    private void show() {
        icon.setImageResource(state == StatusRules.State.SECURE
                ? R.drawable.ic_status_ok : R.drawable.ic_status_warn);
        // Nothing known yet (the first moment after start): the quiet grey of the other icons, not red.
        int color = state != null ? StatusRules.color(state, darkTheme)
                : (darkTheme ? Themes.DARK : Themes.LIGHT).get(Scheme.ON_SURFACE_VARIANT);
        icon.setImageTintList(ColorStateList.valueOf(color));
        icon.setContentDescription(description());
    }
}
