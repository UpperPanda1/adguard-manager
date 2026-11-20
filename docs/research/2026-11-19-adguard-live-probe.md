# AdGuard DNS: live research (Phase 1)

Phase 1 of [`docs/ADGUARD_PLAN.md`](../ADGUARD_PLAN.md) §6: every **[to verify]** item of the plan (§4, §5), checked on the live site before
any code depends on it. Nothing here is from memory.

- **Date:** 2026-11-19, Claude cloud sandbox (curl 8.5.0, Chromium via Playwright 1.56.1, Node 22).
- **Re-check:** `tools/site-probe/` repeats the sign-in, colour and DNS-check findings with one command each
  (its README). Website builds change; re-run it before Phase 4 (theme), 5 (sign-in) and 6 (status).
- **Builds read:** login site `auth-website-ba07d3f2d716a02aeb9d838f3d4dda6b18061f4e` (`auth.min.js`, `vendor.min.js`);
  dashboard `dns-website-033496342ca3e803dc9c5133cab28ac93e4d8b79` (`dashboard.min.js` 1,433,542 B,
  `dashboard.min.css` 293,146 B); AdGuard DNS server source `AdguardTeam/AdGuardDNS` commit `af31bb8` (2026-10-19).

**How sure each row is:**
- **[live]** = seen in a live HTTP answer or in Chromium on the live site.
- **[bundle]** = read in the website's own JavaScript/CSS.
- **[source]** = read in AdGuard's open-source server.
- **[inference]** = concluded from the above but not seen directly. Treat it as unproven.
- **[phone]** = can only be checked on a phone; it is listed in §4.

No account was signed in: the signed-in dashboard page itself (its HTML) was **not** seen, only its scripts.

## Contents

1. [Facts table](#1-facts-table)
2. [What each later phase must change](#2-what-each-later-phase-must-change)
3. [Owner decisions this research raises](#3-owner-decisions-this-research-raises)
4. [Still open: needs a phone or a signed-in account](#4-still-open)
5. [How the facts were found](#5-how-the-facts-were-found)

## 1. Facts table

### 1.1 Dashboard, login and hosts (→ `site/Site.java`)

| # | Fact | Sure | Source |
|---|---|---|---|
| F1 | `https://adguard-dns.io/en/dashboard/` signed out → `302` to `https://auth.adguardaccount.com/oauth/authorize?aid=…&client_id=adguard-dns-website&redirect_uri=https://adguard-dns.io/oauth/callback.html&response_type=code&scope=trust&state=<dashboard URL>` → `302` to `/login.html?aid=…` | live | curl `-L` |
| F2 | Main-frame hosts of the e-mail login: **`adguard-dns.io`** and **`auth.adguardaccount.com`** only. Sub-resources: `st2.adguardcdn.com` (scripts, CSS, icons). | live | Playwright, all requests recorded |
| F3 | The login page sets `X-Frame-Options: DENY` and `frame-ancestors 'none'` (it can't be framed) and cookies `JSESSIONID` (host-only, `SameSite=Lax`), `user` (`Domain=adguardaccount.com`). The dashboard sets `adga` and `lang` on `adguard-dns.io`. | live | response headers |
| F4 | The dashboard's API is `/api/v1/…` with **`Authorization: Bearer ${AUTH_TOKEN}`** (a value the signed-in page writes into itself), not a cookie. 89 calls use the same header helper. | bundle | `dashboard.min.js` `gf=` |
| F5 | API host = `BACKEND_BASE_URI`, with the code's own fallback `https://${DOMAIN_BASE_URI}`. `adguard-dns.io/api/v1/account` and `api.adguard-dns.io/api/v1/account` both answer `401`. A CORS pre-flight from `https://adguard-dns.io` gets `403` on `api.adguard-dns.io` and `200` **without** any `Access-Control-Allow-*` header on `adguard-dns.io` (so it proves nothing there). Most likely same origin (`adguard-dns.io`, from the fallback); either way the host is under `adguard-dns.io`. | live + inference (same origin) | curl, OPTIONS |
| F6 | **Logout** = a form `POST` to `LOGOUT_PATH` (and API `POST /api/v1/logout`). | bundle | `dashboard.min.js` |
| F7 | Account management (`CABINET_BASE_URI`, the "Manage" links) lives on **`adguardaccount.com`** (`adguardaccount.com` → `/en/account/`; `my.adguard.com` → `301` there). Payment opens `window.checkout_payment_url` (`adguardcheckout.com` serves the checkout). | live (hosts) + inference (which variable holds which host) | curl |
| F8 | The site's own pages use `viewport` `maximum-scale=1, user-scalable=0` (website template); the login page `maximum-scale=1.0`: **pinch zoom is banned**, as on NextDNS → the existing `zoom_viewport.js` is needed. | live | page HTML |
| F9 | The login site loads a telemetry config `window.OBSERVABILITY_CONFIG` (first-party `/-/logs/`, `/-/metrics/` on `auth.adguardaccount.com`). | live | `/params.js` |

### 1.2 Sign-in with Apple, Google, Facebook; captcha (→ Phase 5)

| # | Fact | Sure | Source |
|---|---|---|---|
| S1 | The login page offers e-mail, **Apple, Google, Facebook** (in that order). | live | Playwright screenshot + button texts |
| S2 | A provider button sets `window.location.href = "/auth/<provider>?state=…"`: a **whole-page navigation, no pop-up**. Neither `auth.min.js` nor `vendor.min.js` contains `window.open`; Playwright saw **no pop-up** for any provider. | bundle + live | grep, Playwright `context.on('page')` |
| S3 | Chain (main frame): `auth.adguardaccount.com/auth/<p>` (the app page) → `/social_auth/<p>` (server `302`) → provider. | live | Playwright navigation requests |
| S4 | **Google:** `accounts.google.com/o/oauth2/auth?…&disallow_webview=true` → `accounts.google.com/v3/signin/identifier`. AdGuard **asks Google to refuse WebViews** → `web/SignInUserAgent` (LLM Central) is needed. Sub-resources: `accounts.youtube.com`, `play.google.com`, `www.gstatic.com`, `fonts.gstatic.com`, `lh3.googleusercontent.com`. | live | Playwright; the redirect URL |
| S5 | Headless Chromium got Google's page with **both** the WebView and the disguised user agent: it doesn't send WebView's `X-Requested-With` header, so it can't show the `403 disallowed_useragent` a real WebView gets. | live (limitation) | Playwright both ways |
| S6 | **Apple:** `appleid.apple.com/auth/authorize?…&response_mode=form_post` (the page says "sign in to AdGuard VPN": one AdGuard account for all products). Returns with a **POST** to `https://auth.adguardaccount.com/auth/apple`. Sub-resources: `appleid.cdn-apple.com`, `www.apple.com`, `is1-ssl.mzstatic.com`. `idmsa.apple.com` (in LLM Central's list) was **not** seen at this step. | live | Playwright, curl of `/social_auth/apple` |
| S7 | **Facebook:** `www.facebook.com/v17.0/dialog/oauth` → `m.facebook.com/v17.0/dialog/oauth` → `m.facebook.com/login.php` → `m.facebook.com/unified/login_via/app/…` → `m.facebook.com/?…`. Main-frame hosts: **`www.facebook.com`, `m.facebook.com`**. Sub-resources: `facebook.com`, `scontent.xx.fbcdn.net`, `z-m-static.xx.fbcdn.net`. | live | Playwright |
| S8 | All three return to **`https://auth.adguardaccount.com/auth/<provider>`** (`redirect_uri`), all with `scope=email`. | live | curl of `/social_auth/<p>` (`Location`) |
| S9 | **hCaptcha** is shown **only when the login API asks for it** (an error answer carries the site key; the token goes back in an `x-captcha-token` header). Library loader: `https://js.hcaptcha.com`; its frames and API: `newassets.hcaptcha.com`, `api.hcaptcha.com`, `api2.hcaptcha.com` (in the loader `api.js`). | bundle | `auth.min.js`, `vendor.min.js`, `js.hcaptcha.com/1/api.js` |
| S10 | Login steps in the code: `login`, `sign_up`, `two_factor`, `two_factor_email`, `forgot_password`, `wait_social`, OTP by e-mail (`/api/1.0/web/send_otp`). | bundle | `auth.min.js` |

### 1.3 Private DNS, status check, ping (→ `site/`, `status/`, `ui/PingWindow`)

| # | Fact | Sure | Source |
|---|---|---|---|
| D1 | Personal servers: **`{Your_Device_ID}.d.adguard-dns.com`** (Android Private DNS); the dashboard's template `tls://<id>.d.adguard-dns.com`, DoH `https://d.adguard-dns.com/dns-query/<id>`. | live + bundle | AdGuard KB "Android" page; `dashboard.min.js` |
| D2 | Public servers: `dns.adguard-dns.com`, `family.adguard-dns.com`, `unfiltered.adguard-dns.com`. | live | AdGuard KB "DNS providers" |
| D3 | AdGuard's own "am I using AdGuard DNS?" check: `fetch("https://a<random>-dnscheck.${DNS_SERVER_DOMAIN}/dnscheck/test")`, 10 s timeout; **any JSON answer = in use**, a failure = not in use. | bundle | `dashboard.min.js` `checkDnsUsage` |
| D4 | Those names exist **only in AdGuard DNS**: a fresh random `a<hex>-dnscheck.adguard-dns.com` → `NOERROR` from `dns.adguard-dns.com`, `NXDOMAIN` from Google and Cloudflare. So the check needs no redirect like NextDNS's. | live | DoH queries (`tools/site-probe` `dnscheck`) |
| D5 | Answer (JSON, `Access-Control-Allow-Origin: *`): `client_ip`, `device_id`, `profile_id`, `server_group_name`, `server_name`, **`server_type` = `"private"` / `"public"`**, `node_location`, `node_name`, **`protocol` = `dns` / `doh` / `dot` / `doq` / `dnscrypt` (lower case)**, `tls_curve_id`. `404` when the name never reached AdGuard; `429` when rate-limited. | source | AdGuardDNS `internal/dnscheck/remotekv.go`, `internal/dnsserver/protocol.go` |
| D6 | The JSON answer itself was **not** read live: the sandbox proxy resolves names itself (`502` for a name only AdGuard knows), and an IP can't be forced through it. | live (limitation) | curl |
| D7 | **No ping page**: `ping.adguard-dns.io` / `ping.adguard-dns.com` don't exist, and neither the dashboard nor the website mentions ping, latency or a speed test. | live + bundle | curl, grep |

### 1.4 Theme, links, downloads, background work (→ `theme/Themes`, `Site.LINKS`, `downloads/`)

| # | Fact | Sure | Source |
|---|---|---|---|
| T1 | Page colours computed by Chromium from the real `dashboard.min.css`: **light** page `#ffffff`, text `#333e4c`, accent `#3c81f6`; **dark** page **`#242d38`**, text `#ffffff`, accent `#639af8`. (The body uses `--background-main-default` = `--gray-0` / `--gray-90`; accent `--accent-main-default` = `--product-primary-50` / `-40`.) | live | `tools/site-probe` `colours` |
| T2 | The dashboard's theme is an **account setting** (`theme` = `AUTOMATIC` / `LIGHT` / `DARK`), else `localStorage["account_theme"]`, else **`LIGHT`**. Only `AUTOMATIC` follows `prefers-color-scheme` (also live, by a change listener). Applied as `<html data-theme="light|dark">`. | bundle | `dashboard.min.js` |
| T3 | The login page (`auth.adguardaccount.com`) is a different design: green "Continue" button, white page. | live | screenshot |
| L1 | Privacy: `https://adguard-dns.io/en/privacy.html` (200). Terms: DNS EULA `https://adguard-dns.io/en/eula.html` (200); AdGuard-wide `https://adguard.com/en/terms-and-conditions.html` (200). Service status: `https://status.adguard.com/` (200). The dashboard links `window.DNS_PRIVACY_URI` / `DNS_EULA_URI`. | live + bundle | curl; website footer |
| W1 | Downloads made by the page: **user rules export = `blob:`** (`URL.createObjectURL` + `<a download="rules.txt">`). | bundle | `dashboard.min.js` `t8(…)` |
| W2 | Server files, opened as `<a download href="${BACKEND_BASE_URI}${link}">`: DNS server **settings export** (`settings.json`, from `/api/v1/dns_servers/{id}/settings/export`), **Apple profile** (`adguard-dns.mobileconfig`, from `/api/v1/devices/{id}/mobile_config/download_link`), **query log** (from `/api/v1/query_log/download_link`), a device `config`. The link comes from an authenticated API call. | bundle | `dashboard.min.js` |
| W3 | No `data:` downloads in the dashboard or login scripts (NextDNS had them for 2FA codes; AdGuard's 2FA lives on `adguardaccount.com`, not read). | bundle | grep |
| B1 | No `WebSocket`, no `EventSource`, no `visibilitychange` handling in the dashboard. Timers: custom-domains list every **30 s** (while that page is open); query log **auto-refresh**, off by default, user-chosen 1 or 5 min (`localStorage` `auto_refresh_enabled` / `auto_refresh_rate`); a **1 s** "load more" check of long lists (infinite scrolling; runs while a list still has more to load). | bundle | `dashboard.min.js` |
| B2 | One `window.open(url)`: the footer's link helper (`t ? window.open(e) : location.href = e`). The tabs don't enable multiple windows, so WebView doesn't create a new window for it [Android `WebSettings.setSupportMultipleWindows` docs; behaviour on a phone not checked]. | bundle | `dashboard.min.js` |

## 2. What each later phase must change

| Phase | Change, from the facts above |
|---|---|
| **3 Site** | `NAME` "AdGuard DNS"; `HOME_URL` `https://adguard-dns.io/en/dashboard/`; `DOMAINS` = **`adguard-dns.io`, `adguardaccount.com`** (F1, F2, F5, F7; covers `auth.` and the API). `adguardcheckout.com`: owner decision Q4. `PRIVATE_DNS_MARKER`: match **`adguard-dns.com` as a host suffix** (D1, D2); `.d.adguard-dns.com` = personal server. `LINKS`: L1. Remove `PING_URL` (D7). `SiteTest`: lookalikes (`adguard-dns.io.evil.com`, `evil-adguard-dns.io`, `adguardaccount.com.evil.com`, trailing dot). |
| **4 Theme** | `Themes.java`: page `#ffffff` / `#242d38`, accent from T1 (contrast test decides the exact shade on white). Dark mode of the **page** follows the app only if the account's theme is AUTOMATIC (T2): see Q3. |
| **5 Sign-in** | Sign-in hosts for main-frame decisions only: `accounts.google.com`, `appleid.apple.com` (+ `idmsa.apple.com` kept from LLM Central, unseen here), `www.facebook.com`, `m.facebook.com` (S4, S6, S7). Port `SignInUserAgent` (S4: `disallow_webview=true`). **No `PopupWindows`** needed (S2). Facebook's `login_via/app` step may try to open the Facebook app: the app already drops `intent:`/`fb:` links (only http(s)/mailto/tel leave); check on a phone. hCaptcha: sub-frames already load (only main-frame navigations are filtered, `MainActivity` `TabClient`); third-party cookies are off in tabs (`setAcceptThirdPartyCookies(…, false)`): phone check P3. |
| **6 Status** | `TEST_URL` = `https://a<random>-dnscheck.adguard-dns.com/dnscheck/test` (a new random part each time, 4–63 characters of `a–z A–Z 0–9 -`: AdGuardDNS `dnscheck.go` `validateRandomID`); in use = HTTP 200 with JSON (D3–D5); encrypted = `protocol` in `doh`/`dot`/`doq` (**lower case**; compare without case); `server_type` `private` = the user's own server. The NextDNS fields `status`/`ok` disappear. |
| **7 Ping** | The window shows two service **web pages**; AdGuard has neither (D7). Owner asked why remove it (Q2, open): options are remove, a native "Connection details" view of the DNS check's answer (D5), or an in-app latency test (PROJECT.md §11 Phase 7). |
| **D Downloads** (new step, not in the plan) | `blob:` export (W1) fails today: `downloads/Downloads` shows "download failed" for `blob:`. **Owner Q1: port LLM Central's blob capture.** Server files (W2): whether their links work **without** login (the browser can't send the page's Bearer token) is unproven (P4); if they need the session, the browser hand-off fails too. |
| **8 Texts** | Privacy text: the login page loads AdGuard's own telemetry config (F9) and hCaptcha (S9); the README must say Google's page gets a disguised user agent (plan §11). |

## 3. Owner decisions this research raises

Answers given by the owner on 2026-11-20 (also in PROJECT.md §2.2) are marked **→**.

- **Q1 Downloads.** The rules export is a `blob:` file (W1), which the NextDNS code can't save. Port LLM Central's
  blob capture (`blob_capture.js` + the `blob` message), or accept "Export rules" not working in the app?
  **→ Port it** (Phase D).
- **Q2 Ping.** AdGuard has no ping page (D7): remove the Ping window (recommended), or show `status.adguard.com`
  there instead?
  **→ Open:** the owner asked why remove it. Answer: it shows two service web pages and AdGuard has neither; the
  alternatives ("Connection details" from the DNS check, an in-app latency test) are in PROJECT.md §11 Phase 7.
- **Q3 Page dark mode.** The dashboard follows the phone/app only when the account's theme is "Automatic" (T2; it is a
  setting on AdGuard's server). Leave it to the user (recommended; say so in Settings/README), or make the app set
  the page's theme (an injected script; the server setting would still win when it isn't Automatic)?
  **→ Keep as is** (no script).
- **Q4 Payment and account pages.** Open `adguardaccount.com` inside the account's container (recommended: same
  login, F7) and `adguardcheckout.com` in the browser (like NextDNS's billing), or both inside?
  **→ That split.**
- **Q5 Providers.** Plan §13.3: all three (Apple, Google, Facebook; all seen working up to the provider's page) or
  fewer?
  **→ All three.** Also decided: name *AdGuard Manager*, id `org.rafee_m.adguardmanager`, repository
  `UpperPanda1/adguard-manager`.

## 4. Still open

Needs a phone (WebView) or a signed-in account. None of these can be done in the sandbox.

- **P1** Google answers `403 disallowed_useragent` in a real WebView without the disguise, and works with it (S5).
- **P2** Apple and Facebook sign-in complete inside a WebView (2FA, Facebook's "log in with app" step, Apple's
  `form_post` return).
- **P3** hCaptcha works with third-party cookies off (S9). It only appears when AdGuard's server asks.
- **P4** Whether the settings export, Apple profile and query log links open in the phone's browser without login (W2).
- **P5** The signed-in dashboard HTML: exact `BACKEND_BASE_URI`, `DNS_SERVER_DOMAIN` (assumed `adguard-dns.com`:
  D4 shows that domain answers), `LOGOUT_PATH`, its viewport tag.
- **P6** The dnscheck JSON as AdGuard really sends it from a phone using AdGuard DNS (D5 is from the source code).

## 5. How the facts were found

Re-run with `tools/site-probe` (README there) where possible; the rest is one-off:

```bash
# F1, F3: redirect chain and headers
curl -sS -L -D - -o /dev/null https://adguard-dns.io/en/dashboard/
# F4–F6, D3, T2, W1–W3, B1–B2: read the dashboard bundle (build folder from any public page, e.g. welcome.html)
curl -sS -o dashboard.min.js https://st2.adguardcdn.com/dns-website-<build>/js/dashboard.min.js
grep -oE '.{60}checkDnsUsage.{200}' dashboard.min.js
# D5: AdGuard DNS server source
git clone --depth 1 https://github.com/AdguardTeam/AdGuardDNS.git && grep -n 'json:"' AdGuardDNS/internal/dnscheck/remotekv.go
# S1–S8, T1, D4: tools/site-probe (signin / colours / dnscheck)
```

`dashboard.min.js` / `dashboard.min.css` / `dashboard-vendor.min.js` were found by trying the name next to the public
`landing.min.js` in the same build folder (all `200`); `app.min.js` gave `404`. The signed-in page that loads them was
not seen (P5).
