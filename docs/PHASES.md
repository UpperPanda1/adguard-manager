# AdGuardDNS Manager — the remaining phases, step by step

The working guide for every phase still to do. Each phase lists: **goal**, what the owner has decided or still must
decide, **files**, **steps**, **tests**, **how to verify**, **done when**, and the facts it rests on (row ids such as
F1, S4, D5 refer to [`research/2026-11-19-adguard-live-probe.md`](research/2026-11-19-adguard-live-probe.md)).

The owner's plan is [`ADGUARD_PLAN.md`](ADGUARD_PLAN.md) (§6 phases, §7 rules, §8 testing); the project map is
[`../PROJECT.md`](../PROJECT.md); the working rules are [`../CLAUDE.md`](../CLAUDE.md). Written 2026-11-20 after Phase 2;
**re-check every website fact with `tools/site-probe` before code depends on it** (websites change).

## Contents

- [Rules for every phase](#rules-for-every-phase)
- [Tools and packages each phase needs](#tools-and-packages-each-phase-needs)
- [Code to port from LLM Central](#code-to-port-from-llm-central)
- [Phase 3 — Site: point the app at AdGuard DNS](#phase-3--site-point-the-app-at-adguard-dns) ✅
- [Phase 4 — Theme: AdGuard's colours](#phase-4--theme-adguards-colours)
- [Phase 5 — Sign-in with Apple, Google, Facebook](#phase-5--sign-in-with-apple-google-facebook)
- [Phase 6 — Status icon from AdGuard's DNS check](#phase-6--status-icon-from-adguards-dns-check) ✅
- [Phase D — Downloads: save blob: files](#phase-d--downloads-save-blob-files)
- [Phase 7 — Ping window (owner decision first)](#phase-7--ping-window-owner-decision-first)
- [Phase 8 — Texts and store files](#phase-8--texts-and-store-files)
- [Phase 9 — Release workflow](#phase-9--release-workflow)
- [Phase 10 — Review](#phase-10--review)
- [Phase 11 — Docs](#phase-11--docs)
- [Phase 12 — Owner: phone, key, release, F-Droid](#phase-12--owner-phone-key-release-f-droid)

**Suggested order:** ~~3~~ ✅ → ~~6~~ ✅ → **7** (owner: together with 6) → 4 → 5 → D → 7 → 8 → 9 → 10 → 11 → 12. Phase 6 moves up because after Phase 3 the
status icon mixes AdGuard's Private DNS marker with NextDNS's test page (harmless in a development build, but wrong
until Phase 6). Every other dependency is noted in its phase.

---

## Rules for every phase

- **Before starting:** baseline green (`./gradlew --no-daemon test lint assembleDebug assembleRelease`, page checks),
  latest CI run green, the phase's facts re-checked live.
- **One commit per feature or fix**, each complete (code + tests + `CHANGES.md` entry + `PROJECT.md`/`CLAUDE.md`
  updates) and passing `test lint assembleDebug assembleRelease` (+ page checks when HTML/JS changes). Author
  **Rafee-M <69535896+Rafee-M@users.noreply.github.com>**, no `Co-Authored-By`/`Claude-Session` lines (CLAUDE.md §4a).
- Plain-Java rules get unit tests; important rules get a **mutation check** (break the rule on purpose, see a test
  fail, restore) and the result goes into the commit message.
- Measure the release APK on a **clean** build and record the size delta in `CHANGES.md`.
- Each `CHANGES.md` entry ends with a numbered **"What to test on your phone"** list.
- Re-read the diff adversarially; shared-code fixes get a row in `UPSTREAM.md`.
- Update `PROJECT.md` §2.3 (phase table) and the "AdGuard" column of §4.3 when a phase is done.

## Tools and packages each phase needs

| Tool | Where it comes from | Needed in |
|---|---|---|
| JDK 17+ (21 in the sandbox), Gradle 8.14.5 (wrapper), AGP 8.13.2 | sandbox / `./gradlew` downloads Gradle | all |
| Android SDK: `platforms;android-36`, `build-tools;35.0.0`, `platform-tools` | `.claude/hooks/session-start.sh` (or BUILDING.md) | all |
| `aapt` (in build-tools 35) | SDK | identity checks |
| Maven mirror init script (Maven Central answers the sandbox with 429) | written by the session hook into `~/.gradle/init.d/` | all |
| Node 18+ and Playwright 1.56.1 + Chromium | pre-installed in the sandbox (`/opt/node-tools/node_modules`, `/opt/pw-browsers/chromium`) | page checks, `tools/site-probe`, `tools/icon` |
| `tools/readme-badges` npm packages (opentype.js 1.3.4, MCU 0.3.0, simple-icons 16.34.0, Google Sans 5.3.2) | `npm ci` in the folder (npm registry is reachable) | README after a version/size change |
| `curl`, Python 3 | sandbox | live checks (DoH, headers) |
| **`build-tools;34.0.0`** (apksigner 34) | `sdkmanager "build-tools;34.0.0"` (not installed by the hook) | Phase 9 |
| **`apksigcopier` 1.1.1** | `pip install apksigcopier==1.1.1` (in a venv) | Phase 9 |
| `fdroidserver` (optional) | `pip install fdroidserver` + fdroiddata's `config/` files | Phase 9 / 12 |
| JDK 17 (for the 17-vs-21 reproducibility check) | not installed in the sandbox (`apt-get install openjdk-17-jdk-headless` if allowed) | Phase 9 |
| LLM Central source | `add_repo` `UpperPanda1/assist` (in this session it was at `/home/user/assist`) | Phases 5, D |
| NextDNS Manager+ source | `add_repo` `UpperPanda1/nextdnsmanager-plus` | `UPSTREAM.md` ports |

No new **app** library may be added in any phase (CLAUDE.md §6: `androidx.webkit` only).

## Code to port from LLM Central

`UpperPanda1/assist`, branch **`master`** (commit `1102f48`, 2026-11-14; package `org.llmcentral.app` → rename to
`org.dnsmanager.app`). Read the file, port only what this app needs, keep the teacher-style comments.

| File (LLM Central) | Lines / tests | For | What to take |
|---|---|---|---|
| `web/SignInUserAgent.java` + `SignInUserAgentTest.java` | 116 / 4 tests | Phase 5 | all: `disguise` (first bracket → `(X11; Linux <arch>)`), `change`/`apply` with a view tag, restore on leave |
| `provider/SignInHosts.java` | 46 | Phase 5 | the host list idea; here it becomes a list in `site/Site.java` + `Site.loadsInApp(host)` |
| `web/AppBridge.java` + `AppBridgeTest.java` | 297 / 7 tests | Phase D | only the `blob` message part and its origin/scope rules (`Scope.downloads`), not duck.ai's messages |
| `assets/js/blob_capture.js`, `blob_fetch.js`, `download_name.js` | 64 / 50 / 9 | Phase D | capture the page's `blob:` URLs and `<a download>` names; send bytes to the app |
| `downloads/Downloader.java`, `DownloadNames.java` + `DownloadNamesTest.java` | 404 / 209 / 6 tests | Phase D | only the MediaStore save of received bytes and name cleaning; this app already has `downloads/Downloads` + `DataUrl` for `data:` |
| `web/PopupWindows.java`, `ThirdPartyCookies.java` | — | not needed | AdGuard's sign-in has no pop-ups (S2); third-party cookies stay off unless P3 fails |

---

## Phase 3 — Site: point the app at AdGuard DNS

> **✅ Done 2026-11-20** (CHANGES.md entry of that day). Result: `Site.java` names AdGuard DNS and opens
> `https://adguard-dns.io/en/dashboard/`; `DOMAINS` = `adguard-dns.io` + `adguardaccount.com`; marker
> `adguard-dns.com`; AdGuard's legal links. Tests: 93 (SiteTest rewritten + 1 test), 0 lint issues, page checks 6/6;
> mutation checks M1–M5 all caught; release APK 187,626 bytes (+212). Facts re-checked live: F1, F2, F7, F8, L1
> unchanged; new F10 (caching: ETag/304, no `Cache-Control`) and F11 (login page links `adguard.com`).
> **One addition not in the steps below:** `test.nextdns.io` redirects to a random subdomain, which the Ping window
> would have sent to the browser once `nextdns.io` left `DOMAINS`. Fix: `Site.PING_DOMAINS` + `Site.pingWindowAllows`
> (Ping window only; account tabs still use `ownsHost`); **Phase 7 removes both.**
> Comments naming NextDNS (step 6) are a separate commit, "Update comments for AdGuard DNS".

**Goal:** the app opens the AdGuard DNS dashboard and keeps AdGuard's login pages inside the account; everything that
names the service says "AdGuard DNS".

**Decided:** domains `adguard-dns.io` + `adguardaccount.com` inside, `adguardcheckout.com` in the browser (owner Q4).

**Files:** `app/src/main/java/org/dnsmanager/app/site/Site.java` (+ its Javadoc, which still says "the planned AdGuard
DNS Manager"); tests `site/SiteTest.java`, `web/TabManagerTest.java`, `downloads/DownloadsTest.java`,
`status/StatusRulesTest.java` (marker lines 17–18); cosmetic: `web/LoadErrorsTest.java`, `downloads/DataUrlTest.java`
(NextDNS addresses used as sample text); comments naming NextDNS in `web/Profiles.java`, `theme/ThemeManager.java`,
`res/layout/top_bar.xml`, `AndroidManifest.xml`, `assets/js/zoom_viewport.js`, `tools/page-checks/zoom-viewport.mjs`,
`tools/page-checks/common.mjs` (stand-in `service: 'NextDNS'`), `settings-options.mjs` (expects "NextDNS privacy policy").

**Steps:**
1. Re-check live: `curl -sSL -D - -o /dev/null https://adguard-dns.io/en/dashboard/` (F1 redirect chain); read the
   dashboard files' `Cache-Control` (`curl -sI https://st2.adguardcdn.com/dns-website-<build>/js/dashboard.min.js`; build
   folder from `https://adguard-dns.io/en/welcome.html`) and note it in PROJECT.md §6 rule 2.
2. `Site.java`: `NAME = "AdGuard DNS"`; `HOME_URL = "https://adguard-dns.io/en/dashboard/"`; `DOMAINS =
   ["adguard-dns.io", "adguardaccount.com"]` (with a comment: why each, why not `adguardcheckout.com`);
   `PRIVATE_DNS_MARKER = "adguard-dns.com"` (the suffix *rule* comes in Phase 6); `LINKS`: `service_privacy` →
   `https://adguard-dns.io/en/privacy.html`, `service_terms` → `https://adguard-dns.io/en/eula.html` (L1). Leave
   `TEST_URL`/test fields (Phase 6) and `PING_URL` (Phase 7), each with a "changes in Phase N" comment.
3. Tests: `SiteTest` with AdGuard hosts — inside: `adguard-dns.io`, `ADGUARD-DNS.IO`, `auth.adguardaccount.com`,
   `adguardaccount.com`; outside: `evil-adguard-dns.io`, `adguard-dns.io.evil.com`, `adguardaccount.com.evil.com`,
   `adguardcheckout.com`, `adguard-dns.com`, `adguard-dns.io.` (trailing dot), empty/null; origin rules for both
   domains; `LINKS` are https on AdGuard's own domain. `TabManagerTest`: AdGuard start URLs incl. `@`/port tricks.
   `DownloadsTest`: "AdGuard DNS download.txt". `StatusRulesTest`: `abc123.d.adguard-dns.com`, `dns.adguard-dns.com`.
4. **Mutation check:** change `ownsHost` to `h.endsWith(domain)` → `SiteTest`/`TabManagerTest` must fail; restore.
5. Page checks: stand-in `service: 'AdGuard DNS'` in `common.mjs`, expected link texts in `settings-options.mjs`, the
   zoom check's sample URL; run all; look at `settings-*` screenshots.
6. Update comments naming NextDNS (list above) — a separate commit if it grows ("Update comments for AdGuard DNS").

**Verify:** `test lint assembleDebug assembleRelease`; page checks; `grep -rn "nextdns" app/src` = only
Phase 6/7 leftovers (`TEST_URL`, `PING_URL`) and their comments; `aapt` unchanged id; clean APK size.

**Done when:** a debug APK opens `adguard-dns.io/en/dashboard/` (redirect to the AdGuard login stays in the app),
Settings says "AdGuard DNS privacy policy" etc., tests and mutation check pass.

**Phone test to write:** open the app → AdGuard login page inside the app; log in with e-mail; the dashboard loads;
a link to `adguardcheckout.com` (Upgrade) opens the browser; Settings → About & legal links open AdGuard's pages.

---

## Phase 4 — Theme: AdGuard's colours

**Goal:** the app's surfaces, status bar and accent match the AdGuard dashboard in light and dark.

**Decided:** keep the page's own theme logic (owner Q3: no injected theme script).

**Files:** `theme/Themes.java`, `theme/ContrastTest.java` (its NextDNS comment and expectations), `res/values/colors.xml`,
`res/values-night/colors.xml` (start-up colours), the fallback colours at the top of `assets/settings.html` and
`assets/about.html`, `tools/page-checks/settings-dark.mjs` (compares page colours with `Themes.java`).

**Steps:**
1. `node tools/site-probe/probe.mjs colours` (T1): today light `#ffffff`/`#333e4c`, dark `#242d38`/white, accent
   `#3c81f6`/`#639af8`.
2. Surfaces: light `#ffffff`, dark `#242d38`. Text: light `#333e4c`, dark white (check `ContrastTest`).
3. Accent: `#3c81f6` has only 3.71:1 with white text; AdGuard's next step `#1a6bf4` (4.72:1) is also the icon colour.
   Let `ContrastTest` decide (WCAG 4.5:1 for text on accent buttons, 3:1 for icons).
4. Update start-up colours and HTML fallbacks; run page checks; look at light/dark screenshots.

**Verify:** `ContrastTest` green; mutation check (an unreadable pair fails it); screenshots light/dark; the README
badges keep `#1A6BF4` (`tools/readme-badges` `BRAND`) unless the accent changes.

**Done when:** colours from AdGuard's CSS, contrast proven, CLAUDE.md §6 accent line updated.

**Phone test to write:** White, Black, Match system each with the dashboard; status bar = page colour; dashboard
dark only when its account theme is Automatic/Dark (T2).

---

## Phase 5 — Sign-in with Apple, Google, Facebook

**Goal:** the three social logins work inside the account's container; Google's page alone gets a desktop user agent.

**Decided:** all three providers (Q5); no SDK; no pop-ups needed (S2).

**Files:** `site/Site.java` (sign-in host list + `loadsInApp`), new `web/SignInUserAgent.java` + test, `MainActivity`
(`TabClient.shouldOverrideUrlLoading` and `onPageStarted`: wiring only), `web/DesktopMode.java` (must not override the
sign-in user agent while it is active, and must restore correctly), README (already explains the user agent).

**Steps:**
1. Re-check live: `node tools/site-probe/probe.mjs signin Google`, `… Apple`, `… Facebook`, `… Google --disguised`
   (S3–S8). Note any new host in the research file.
2. `Site.SIGN_IN_HOSTS` = `accounts.google.com`, `appleid.apple.com`, `idmsa.apple.com`, `www.facebook.com`,
   `m.facebook.com`; `Site.loadsInApp(host)` = `ownsHost(host)` or exact match in that list. Use it **only** in the tab's
   main-frame decision; `ownsHost` stays for scripts, start URLs and everything else.
3. Port `SignInUserAgent` (+ 4 tests) from LLM Central; apply it in `shouldOverrideUrlLoading` (restart the GET after a
   switch, as LLM Central does) with an `onPageStarted` backstop; restore when leaving `accounts.google.com`.
4. Make sure Desktop site and the sign-in user agent don't fight (one owner of the UA string at a time).
5. Facebook's "log in with app" step: `fb:`/`intent:` must stay blocked (`ExternalLinks`); test it.
6. Security review (`security-review` skill): host list exact-match only, UA scope, no new bridge.

**Tests:** `SiteTest` (`loadsInApp`: the five hosts yes; `accounts.google.com.evil.com`, `evil-facebook.com`,
`facebook.com` (not in list), `google.com` no); `SignInUserAgentTest`; mutation checks (widen the list to a suffix
rule → test fails; disguise on a non-Google host → test fails).

**Verify:** unit tests; the probe; lint; page checks unchanged. **Not verifiable here:** the real WebView refusal
(`403 disallowed_useragent`) and the full logins (P1, P2) — phone.

**Done when:** tests + mutation checks pass; CHANGES.md phone list covers all three providers, 2FA, hCaptcha (P3).

---

## Phase 6 — Status icon from AdGuard's DNS check

> **✅ Done 2026-11-20** (CHANGES.md; plan and result: `superpowers/plans/2026-11-20-phases-6-7-status-and-connection.md`).
> New `status/DnsCheck` (address, request, answer → in use / not in use / unknown, validated details); `StatusRules`
> suffix marker + new `combine` (REVIEW F3 fixed); `ConnectionStatus` uses it and confirms a "not in use" once, 3 s
> later. **Decided:** `dnscrypt` counts as encrypted (it is: every query encrypted and signed); 404 = not in use,
> like the dashboard. Tests 105 (+12), mutation checks M1–M7 all caught. Facts D3–D6 re-checked; new D8.

**Goal:** the top-bar icon says correctly whether this phone uses AdGuard DNS, encrypted or not.

**Files:** `status/StatusRules.java` (+ test), `status/ConnectionStatus.java` (fetch), `site/Site.java` (test address
parts, protocol names, marker rule), `res/values/strings.xml` (texts if "own server" is shown).

**Steps:**
1. Re-check live: `node tools/site-probe/probe.mjs dnscheck` (D4); re-read AdGuardDNS `internal/dnscheck/remotekv.go`
   (`git clone --depth 1 https://github.com/AdguardTeam/AdGuardDNS.git`) for the JSON fields (D5).
2. Test address: `https://a<random>-dnscheck.adguard-dns.com/dnscheck/test`, a **new random part per check** (4–63 of
   `a–z A–Z 0–9 -`; e.g. `a` + 16 hex), 10 s timeout (as the dashboard). `HttpURLConnection` only.
3. Rules (plain Java, tested): HTTP 200 + JSON = in use; `protocol` compared case-insensitively with `doh`/`dot`/`doq`
   = secure; `dns`/`dnscrypt` = insecure (decide `dnscrypt` with the owner or treat as insecure); lookup failure / 404 =
   not in use; 429 or other errors = unknown (fall back to Private DNS). `server_type` `private` vs `public` can go into
   the explanation text.
4. Private DNS marker as a **suffix rule** (`equals("adguard-dns.com") || endsWith(".adguard-dns.com")`): fixes inherited
   finding F3/4.5 (REVIEW.md).
5. Remove the NextDNS test fields from `Site.java`.
6. (Left by Phase 3) Settings → Privacy says the test request goes to "the AdGuard DNS test address": true only once
   this phase is done. Check the sentence still matches what `ConnectionStatus` sends (address, no account data).

**Tests:** every answer: secure DoH/DoT/DoQ (upper and lower case), plain DNS, DNSCrypt, malformed JSON, empty body, 404,
429, timeout; marker: `abc.d.adguard-dns.com` yes, `adguard-dns.com.evil.com` no, `evil-adguard-dns.com` no. Mutation
check on the suffix rule.

**Done when:** tests pass; no NextDNS address left in `status/`; phone list: Private DNS on with the personal
`<device id>.d.adguard-dns.com` → green; off → red; a public `dns.adguard-dns.com` → green; Wi-Fi ↔ mobile switch.
**Not verifiable here:** the JSON from a phone that uses AdGuard DNS (P6).

---

## Phase D — Downloads: save blob: files

**Goal:** "Export rules" (and any other file the dashboard makes in memory) is saved to Downloads.

**Decided:** port LLM Central's blob capture (owner Q1). Bridge rule: an origin-restricted
`WebViewCompat.addWebMessageListener`, **not** `addJavascriptInterface` (CLAUDE.md §7).

**Files:** new `assets/js/blob_capture.js` (+ `Scripts.ALL`), new `web/BlobBridge.java` (or a small port of
`AppBridge`'s blob part) + tests, `downloads/Downloads.java` (blob path; route web addresses through
`ExternalLinks.open`, inherited R6), `downloads/DataUrl.java` (unknown MIME → `application/octet-stream`, inherited F2),
`MainActivity` (register the listener per tab: wiring only).

**Steps:**
1. Re-read the dashboard bundle for every `createObjectURL` / `download=` use (W1–W3).
2. Port: the script records blobs the page creates and the `<a download>` name; on a download of a `blob:` URL it reads
   the bytes and posts them (base64, size-capped, e.g. 10 MB) to the listener, which accepts only
   `https://adguard-dns.io` (+ `https://*.adguard-dns.io`), main frame only.
3. Java validates size, name (clean, extension from type), type; saves through MediaStore (existing code path).
4. Server files (W2): keep the browser hand-off until the phone check P4; if links need the login, download in the app
   with the account's cookies (`DownloadManager` + `CookieManager` of the account's profile) in a later step.
5. Security review: origin list, size cap, name sanitising, no URL accepted from the page.

**Tests:** message validation (wrong origin, sub-frame, too big, bad base64, bad name), name cleaning, MIME mapping;
page check for the script (a page that makes a blob and clicks a download link; assert the posted message).

**Done when:** tests + page check pass; phone list: Export rules → `rules.txt` in Downloads with the right content.

---

## Phase 7 — Ping window (owner decision first)

**Goal:** decide what the three-dots → "Ping test" item becomes. The window shows two of the service's **web pages**
(`ping.nextdns.io`, `test.nextdns.io`); AdGuard has neither (D7).

**Whatever the option:** remove `Site.PING_DOMAINS` and `Site.pingWindowAllows` (added in Phase 3 only so the
NextDNS pages and their redirect stay in the window) and their part of `SiteTest`; a Ping window that keeps web pages
goes back to `Site.ownsHost`.

**Options (owner Q2, open):**
1. **Remove** the menu item, `ui/PingWindow.java`, its strings (`menu_ping`, `ping_title`), `Site.PING_URL`, and any
   now-unused resources (lint fails on unused resources). Smallest.
2. **"Connection details" (recommended)**: a small native window showing the DNS check's answer (Phase 6) as text:
   server location (`node_location`), protocol, own server or public, device id. Same information `test.nextdns.io`
   gave. Reuses Phase 6's request and rules; plain views; no web page.
3. **Latency test**: time a few DoH requests to `dns.adguard-dns.com` / `d.adguard-dns.com`. Real numbers, but new
   networking code and only the server location the phone is routed to.

**Tests:** option 2: formatting rules (plain Java); option 1: lint proves nothing unused is left.

**Done when:** the owner's option is built, tested, and the menu matches it.

---

## Phase 8 — Texts and store files

**Files:** `assets/settings.html` (Privacy, Permissions, Disclaimer: sign-in pages of Apple/Google/Facebook, hCaptcha
frames, AdGuard's own telemetry (F9), the DNS check request), `fastlane/metadata/android/en-US/title.txt`
("AdGuardDNS Manager"), `short_description.txt` (≤ 80), `full_description.txt` (≤ 4000: unofficial, needs an AdGuard
account, the Google user-agent note, WebView 119+ for several accounts), `changelogs/1.txt` (≤ 500, rewrite), README
features (remove "planned" marks that are done).

**Verify:** page checks (incl. the texts' sub-screens), character limits by script, `ui-consistency` adjusting →
verifying for the Settings sub-screens.

---

## Phase 9 — Release workflow

**Files:** `.github/workflows/release.yml`: APK `AdGuardDNS-Manager-<version>.apk`, release title "AdGuardDNS Manager
<version>", dry-run certificate `CN=AdGuardDNS Manager dry run (not a release key)`; `RELEASE.md`, `fdroid.md` §3.5.

**Steps:** install `build-tools;34.0.0` and `apksigcopier` 1.1.1 (tools table); extract the `run:` blocks and run them
locally with a throw-away key (check step: good tag, wrong tag, moved repository, fork dry run; sign → compare →
summary); repeat the reproducibility check (two clones; JDK 17 vs 21 if JDK 17 can be installed); `actionlint` if
available.

**Done when:** local rehearsal passes; names match the recipe's `Binaries:` line.

---

## Phase 10 — Review

Adversarial review of everything since the import (`git diff abb10bf..HEAD`), using the checklist in
[`../REVIEW.md`](../REVIEW.md) §4 (sign-in hosts and user-agent scope, blob bridge, status check, links leaving the app,
"two things at once", battery). Use the `security-review` and `code-review` skills. Record findings in `REVIEW.md`; fix
or list them for the owner.

## Phase 11 — Docs

Every number in the docs matches a run (tests, lint, APK size, hashes); `PROJECT.md` (file map, features, §11),
`CLAUDE.md` (facts, TODO), `CHANGES.md`, `fdroid.md`, `README.md` (no "planned" left for done work), `handoff.md` (when
asked). Link/anchor check by script (as in the 2026-11-20 docs commit).

## Phase 12 — Owner: phone, key, release, F-Droid

Phone tests from `CHANGES.md`; the name/trademark answer (`fdroid.md` §3.2); release key + 4 secrets (`RELEASE.md` §3);
dry run (test key, then real key); rename the repository to `adguard-dns-manager` if wanted (PROJECT.md §7 lists the
follow-up edits); tag `v0.1.0`; the F-Droid recipe (`fdroid.md` §3.5) and `fdroid lint/build/scanner`.
