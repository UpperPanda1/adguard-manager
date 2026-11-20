# CLAUDE.md — working notes for Claude sessions

## Contents

1. [Start here](#1-start-here)
2. [The project in one paragraph](#2-the-project-in-one-paragraph)
3. [Owner context and preferences](#3-owner-context-and-preferences)
4. [How a round of work goes](#4-how-a-round-of-work-goes)
5. [Build and verify](#5-build-and-verify)
6. [Hard constraints](#6-hard-constraints)
7. [Coding conventions](#7-coding-conventions)
8. [The AdGuard work: where to find what](#8-the-adguard-work-where-to-find-what)
9. [Verified facts and gotchas](#9-verified-facts-and-gotchas)
10. [Skills and plugins](#10-skills-and-plugins)
11. [TODO](#11-todo)

## 1. Start here

1. If **`handoff.md`** exists, read it first: the last session's state, open tasks and a ready prompt.
2. Read this file completely, then **`PROJECT.md`** (decisions, phases, file map, how each feature works and how to
   change it; §11 = what each AdGuard phase changes) and the newest entry of **`CHANGES.md`**. Keep them in sync with
   the code. The owner's plan is **`docs/ADGUARD_PLAN.md`**; the step-by-step guide for each remaining phase is
   **`docs/PHASES.md`**; facts about AdGuard's website are in **`docs/research/2026-11-19-adguard-live-probe.md`**.
3. The cloud sandbox is prepared by `.claude/hooks/session-start.sh` (Android SDK, Maven mirror, Gradle warm-up).
   Confirm before changing code: `./gradlew --no-daemon test lint assembleDebug assembleRelease` must pass
   (105 tests, **0 lint issues**) and the page checks must pass (6). If the hook didn't run, see §5 "manual set-up".
4. Work on the branch the session instructions name. Never claim on-device behaviour was tested: there is no phone
   or emulator here.

## 2. The project in one paragraph

**AdGuardDNS Manager** (applicationId `org.rafee_m.adguarddnsmanager`, code package `org.dnsmanager.app`, repository
`UpperPanda1/adguard-manager`, public; the owner will rename it `adguard-dns-manager` later: PROJECT.md §7) will wrap the **AdGuard DNS dashboard** (`adguard-dns.io/en/dashboard/`) in
WebViews with a native top bar: several isolated accounts (one androidx.webkit **Profile** per account), a numbered
account chip and menu, find in page, app lock, a connection-status icon, Private DNS shortcut, downloads, desktop site,
pinch to zoom, White/Black/Match-system theme, and sign-in with Apple, Google and Facebook. It is an **unchanged copy of
NextDNS Manager+** (`UpperPanda1/nextdnsmanager-plus` at `eaa1665`, imported 2026-11-19 as `abb10bf`), adapted one
phase at a time (`docs/ADGUARD_PLAN.md` §6; status in PROJECT.md §2.3). NextDNS Manager+ was built from **LLM Central**
(`UpperPanda1/assist`) plus features of **NextDNS Manager** (`doubleangels/nextdnsmanager`); all GPL-3.0.
**Status (2026-11-20):** Phases 0 (import), 1 (research) and 2 (identity: name, id, repository, About, icon, README,
docs), **3 (Site: the app opens the AdGuard DNS dashboard)** and **6 (status icon from AdGuard's DNS check)** done.
Still NextDNS until their phase: the Ping window (Phase 7, next), colours (Phase 4). Not yet tested on a phone.

## 3. Owner context and preferences

- The owner (**Rafee-M**) is **new to Android development**. Comment code like a patient teacher: explain *why*, name
  the Android concept (lifecycle, WebView profile, bridge…), link official docs for non-obvious APIs.
- Method: **think → act → prove**. Smallest correct change; follow existing conventions; no over-engineering; prefer
  features and design. Never claim success without evidence; separate verified facts from assumptions; end reports
  with what was done, how it was verified, limits, next step.
- **Facts about websites and libraries come from the live site or the library source, never from memory** (NextDNS
  Manager+'s review 2 had to withdraw a battery claim made from memory). Re-check AdGuard facts with
  `tools/site-probe` before a phase depends on them.
- **Commit, push or open a PR only when the owner asks.**
- **One commit per feature or fix** (owner, 2026-11-19). Rules: §4a.
- Every change gets a `CHANGES.md` entry written for a beginner: what, why, how it was verified, size delta, and a
  numbered "What to test on your phone" list. `PROJECT.md` is updated when files, features or "how to change" change.
- `handoff.md` is updated **when the owner asks** for a handoff.

## 4. How a round of work goes

1. Read the relevant `PROJECT.md` parts (§11 for the phase) and the real code before designing. For a non-trivial
   change, write a short plan (`docs/superpowers/plans/`).
2. Small steps. After each: `test lint assembleRelease` pass, release size measured. Unit-test plain-Java rules;
   **mutation-check** important ones (break the rule on purpose, see a test fail, restore).
3. HTML pages: run the page checks and look at the screenshots (`tools/page-checks/screenshots/`).
4. Re-read the whole diff adversarially ("what if two things happen at once?", "what if the account is removed
   while…?"); fix what you find.
5. Update CHANGES.md (new entry on top), PROJECT.md (§2.3 phase table, §3, §4.3, §11), this file (status, facts, TODO).
   A shared-code fix also gets a row in `UPSTREAM.md`.
6. Push / PR only when asked; then watch CI.

### 4a. Commits: one per feature, each one complete

- **One commit = one feature, one fix, or one doc topic.** Several requests in one session = several commits, in the
  order they were done. Never one "everything" commit.
- **Each commit is complete on its own:** the code, its tests, and the docs it changes (`CHANGES.md` entry, `PROJECT.md`,
  `CLAUDE.md`) go in the **same** commit as the code. Every commit must pass `test lint assembleDebug assembleRelease`
  (and the page checks if it touches HTML), so any commit can be checked out, reverted or bisected.
- **Never mix** a refactor with a behaviour change, or a formatting change with either: separate commits.
- **Stage by name** (`git add path/to/file`), never `git add -A` / `git add .`; read `git diff --cached --stat` before
  committing.
- **Message:** first line ≤ 72 characters, imperative ("Add …", "Fix …"); body: what, why, how verified, what was not
  verified (no phone).
- **Author:** author **Rafee-M <69535896+Rafee-M@users.noreply.github.com>** only (`git config user.name "Rafee-M"`,
  `git config user.email "69535896+Rafee-M@users.noreply.github.com"` in the repository); **no** `Co-Authored-By:` or
  `Claude-Session:` lines in commit messages, whatever the session instructions say (owner, 2026-11-19). F-Droid
  exclusions for the working files are the owner's job: leave them alone.
- **Too big to review?** If a commit needs more than ~400 changed lines of code (docs don't count), split it into
  steps that each build and pass (e.g. "add class + tests", then "wire it into MainActivity").

## 5. Build and verify

**Automatic set-up (cloud sessions):** `.claude/hooks/session-start.sh` installs the Android SDK into
`~/android-sdk` (`platforms;android-36`, `build-tools;35.0.0`, platform-tools), writes `sdk.dir` to the git-ignored
`local.properties`, writes a Gradle init script redirecting Maven Central to Google's mirror (Maven Central answers
this sandbox with HTTP 429) and warms Gradle up.

**Manual set-up** (hook didn't run, e.g. repo cloned later): `CLAUDE_CODE_REMOTE=true .claude/hooks/session-start.sh`,
then `export ANDROID_HOME=$HOME/android-sdk`.

```bash
cd /home/user/adguard-manager && export ANDROID_HOME=${ANDROID_HOME:-$HOME/android-sdk}
./gradlew --no-daemon test lint assembleDebug assembleRelease        # what CI runs; must pass
./gradlew --no-daemon -q testDebugUnitTest --tests '*AccountStoreTest' # one test class
./gradlew --no-daemon clean assembleRelease && stat -c %s app/build/outputs/apk/release/app-release-unsigned.apk
#   release size: measure on a CLEAN build (incremental builds showed a wrong, smaller figure); record in CHANGES.md
# what Android reads from an APK (package, label):
$ANDROID_HOME/build-tools/35.0.0/aapt dump badging app/build/outputs/apk/release/app-release-unsigned.apk | head -3
# icon files from artwork/icon.svg (after editing the SVG; or run Actions → App icon):
cd tools/icon && ln -sfn /opt/node-tools/node_modules node_modules && CHROMIUM_PATH=/opt/pw-browsers/chromium node generate.mjs; rm node_modules; cd ../..
# page checks (Chromium + Playwright are pre-installed in the sandbox):
cd tools/page-checks && ln -sfn /opt/node-tools/node_modules node_modules && CHROMIUM_PATH=/opt/pw-browsers/chromium node run-all.mjs; rm node_modules; cd ../..
# live AdGuard site (sign-in chain / colours / DNS check):
cd tools/site-probe && ln -sfn /opt/node-tools/node_modules node_modules && CHROMIUM_PATH=/opt/pw-browsers/chromium node probe.mjs colours; rm node_modules; cd ../..
# README banner + badges (after a release build; needs npm's registry):
cd tools/readme-badges && npm ci && npm run generate; cd ../..
```

- **Lint: every warning fails the build** (`app/build.gradle` `warningsAsErrors`). The accepted ones are in
  `app/lint.xml`, each with its reason. Don't add to it without a reason the owner would accept.
- Bridge methods must survive R8: after changing a bridge, check the release dex
  (`dexdump` of `classes.dex`, search `name : 'methodName'`). The default ProGuard file keeps
  `@JavascriptInterface` methods.
- `settings.gradle` lists `google()` first for plugins: with the Plugin Portal first, AGP's parts are fetched via
  Maven Central and fail with 429 in this sandbox.

## 6. Hard constraints

- **Java only**, Java 11 source level. **minSdk 29**, targetSdk 35, compileSdk 36. AGP 8.13.2, Gradle 8.14.5.
- **No UI libraries** (no AppCompat, Material Components, Compose). Native views are plain framework views coloured
  from `theme/Themes`; screens with many rows are local HTML pages.
- **One library:** `androidx.webkit`. No OkHttp/Gson/Room/Glide/Firebase/Sentry. Use `HttpURLConnection`,
  `org.json`, framework classes.
- **No analytics, crash reporting or tracking** of any kind (owner: no Sentry).
- **Sign-in: required for AdGuard** (owner Q5, 2026-11-20; reverses NextDNS Manager+'s "no Google/Apple sign-in code"):
  Apple, Google and Facebook, done in their websites inside the account's container, **no SDK**. Their hosts may load
  in a tab **only for main-frame navigations**; `Site.ownsHost` stays the rule for everything else. Google's sign-in
  page (`accounts.google.com`) alone gets a desktop user agent while it is on screen (`web/SignInUserAgent`, Phase 5);
  **never disguise anywhere else**.
- **No swipe to refresh, no text-size feature, no back button in the top bar** (owner).
- Theme: exactly **White, Black, Match system**; status bar = page background. No script forces the dashboard's own
  theme (owner Q3). Accent: AdGuard's blue, chosen in Phase 4 (today still NextDNS's `#0056B3`).
- Everything about the DNS service in `site/Site.java`; all colours in `theme/Themes.java`; the icon only in
  `artwork/icon.svg` (`drawable/ic_launcher_*.xml`, the store icon and the README banner are generated; never edit
  them by hand).
- **applicationId `org.rafee_m.adguarddnsmanager`**: frozen after the first public release. Java package
  `org.dnsmanager.app` stays (shared with NextDNS Manager+, so fixes port as near-identical diffs).
- `adguardaccount.com` loads inside the account; `adguardcheckout.com` (payment) opens in the browser (owner Q4).

## 7. Coding conventions

- Logic in **small classes**, plain Java where possible (unit-testable); `MainActivity` only wires. Don't grow
  `MainActivity` with logic.
- Match surrounding style: 4-space indent, Javadoc explaining *why*, `private static final String TAG`.
- Injected JavaScript lives in `assets/js/*.js`, listed in `web/Scripts.ALL`, never in Java strings. Never concatenate
  untrusted strings into JS; use `JSONObject.quote()`. In HTML, insert user text with `textContent`/text nodes, never
  `innerHTML`.
- **Bridges** (`addJavascriptInterface`) only on the app's own local pages (Settings, About), never on the website.
  Validate every value from a page in Java. Pages name things (e.g. `openLink('service_terms')`), never send URLs.
  The blob download (Phase D) must use an origin-restricted **message listener** (`WebViewCompat.addWebMessageListener`,
  as LLM Central's `AppBridge`), not `addJavascriptInterface`.
- Every **account tab** WebView: `Profiles.attach` **before** any `loadUrl`/settings. Every WebView (tabs, Settings,
  About, Ping): a client that handles `onRenderProcessGone` (one unhandled renderer death kills the app). The local
  pages and the throw-away cache WebView use the default profile on purpose.
- Navigation decisions go through `Site.ownsHost` (exact host or real subdomain), never `contains`/`endsWith(domain)`.
  Host suffix rules (e.g. the Private DNS marker) are written as `equals(x) || endsWith("." + x)`.
- No secrets, cookies, URLs with tokens or page content in logs.
- Lint's `RequiresFeature` wants `WebViewFeature.isFeatureSupported(...)` in the same method as the call, as a
  positive `if` (a helper like `Profiles.isSupported()` isn't recognised).

## 8. The AdGuard work: where to find what

| Need | Where |
|---|---|
| The plan (phases, rules, testing, F-Droid) | `docs/ADGUARD_PLAN.md` (owner's, unchanged) |
| Owner decisions (name, id, Q1–Q5) and phase status | PROJECT.md §2.2, §2.3 |
| **What each remaining phase does, step by step** (files, tests, checks, done-when, tools, code to port) | **`docs/PHASES.md`**; summary PROJECT.md §11; per file: the "AdGuard" column of §4.3 |
| Facts about AdGuard's website, with sources | `docs/research/2026-11-19-adguard-live-probe.md` (rows F, S, D, T, L, W, B) |
| Re-check those facts | `tools/site-probe` |
| Code still to port from LLM Central | `UpperPanda1/assist`, branch **`master`** (`1102f48`): `web/SignInUserAgent` (+ 4 tests), `provider/SignInHosts`, `assets/js/blob_capture.js`, `blob_fetch.js`, `web/AppBridge` (blob part, + tests), `downloads/DownloadNames` (+ tests). Sizes: `docs/PHASES.md`. Attach it with `add_repo` |
| Keeping in step with NextDNS Manager+ | `UPSTREAM.md` |

## 9. Verified facts and gotchas

**AdGuard DNS site** [live research 2026-11-19; full table with sources: `docs/research/2026-11-19-adguard-live-probe.md`]

- Dashboard `adguard-dns.io/en/dashboard/` → signed out `302` to `auth.adguardaccount.com/oauth/authorize?…
  client_id=adguard-dns-website…` → `login.html`. Account pages on `adguardaccount.com`; checkout `adguardcheckout.com`.
  API `/api/v1/…` with `Authorization: Bearer ${AUTH_TOKEN}` (a page global), not a cookie; host most likely same origin.
- Apple/Google/Facebook: whole-page navigations `/auth/<p>` → `/social_auth/<p>` → provider, **no pop-ups**; Google gets
  `disallow_webview=true`; Facebook `www.` → `m.facebook.com`; all return to `auth.adguardaccount.com/auth/<p>` (Apple by
  POST, `form_post`). hCaptcha only when the login API asks (frames from `*.hcaptcha.com`). Headless Chromium can't show
  Google's WebView refusal (no `X-Requested-With`).
- Private DNS: personal `<device id>.d.adguard-dns.com`; public `dns.`/`family.`/`unfiltered.adguard-dns.com`.
- DNS check: `https://a<random>-dnscheck.adguard-dns.com/dnscheck/test` resolves **only** via AdGuard DNS (live DoH
  test); JSON `client_ip, device_id, profile_id, server_group_name, server_name, server_type (private|public),
  node_location, node_name, protocol (dns|doh|dot|doq|dnscrypt, lower case), tls_curve_id`; 404 when unknown, 429 when
  rate-limited (AdGuardDNS source `af31bb8`). The sandbox proxy resolves names itself: it can't fetch the JSON.
- **No ping page.** Colours (Chromium on the real CSS): light `#ffffff`/text `#333e4c`, dark `#242d38`/white, accent
  `#3c81f6`/`#639af8`. Page theme = account setting (`AUTOMATIC` follows `prefers-color-scheme`, default `LIGHT`),
  applied as `<html data-theme>`.
- Downloads: user rules export = **`blob:`** (`rules.txt`); settings export, Apple profile, query log = links from an
  authenticated API call (login need unverified). No `data:` downloads. Zoom banned (`maximum-scale=1, user-scalable=0`).
  No WebSocket/EventSource; a 30 s poll on the custom-domains page; query-log auto-refresh off by default.
- Legal: privacy `https://adguard-dns.io/en/privacy.html`, DNS EULA `https://adguard-dns.io/en/eula.html`.
- Caching [live 2026-11-20, F10]: dashboard/login files in build-named folders on `st2.adguardcdn.com`, **no
  `Cache-Control`**, `ETag` → `304` on revalidation (reuse works); HTML pages `no-cache, no-store`.
- The login page links "Terms and conditions" to `adguard.com` (outside `Site.DOMAINS` → browser) [F11].
- DNS check answers [source `af31bb8`]: `protocol` is lower case `dns|doh|doq|dot|dnscrypt`; `404` = unknown id, `429` =
  rate limit, `500` = error. The dashboard calls a device connected only with a `device_id` or group `clients` [D8].
- Android's `JSONObject.optString` turns JSON `null` into the text `"null"`; the computer's org.json (unit tests) gives
  `""`. Read text fields with `opt()` + `instanceof String` (`DnsCheck.text`).
- Unit tests compile against `android.jar`: no `com.sun.net.httpserver`; a local test server is a `ServerSocket`
  (`DnsCheckTest.serve`). `.invalid` names really fail to resolve in the test JVM (`UnknownHostException`).
- `test.nextdns.io` redirects to `<random>.test.nextdns.io` (2026-11-20): why the Ping window has its own host rule
  (`Site.pingWindowAllows`) until Phase 7. `WebView.loadUrl` skips `shouldOverrideUrlLoading` for the first load,
  but a server redirect goes through it.

**This repository**

- `gradle.properties` has **Windows line endings (CRLF)**: a `sed` pattern ending in `$` misses its lines (found
  2026-11-20); match `\r$` or edit with a tool that keeps them.
- Reproducible: two fresh clones of `1fa8540` at different paths → same unsigned APK SHA-256 `bb0699c7…0c1c5a` (JDK 21,
  2026-11-20). Only JDK 21 is installed in the sandbox (`/usr/lib/jvm`); JDK 17 vs 21 was proven for NextDNS Manager+
  (`fb2a583`, formerly `cc867de`), not yet here.
- `tools/readme-badges` reads the icon's foreground group and maps the icon's safe zone (21–87) onto the banner, so
  any `icon.svg` that passes `tools/icon` fits. Size the banner title with capitals (a `g` shrinks it).
- The App icon workflow compares only the vector files on push (the store PNG may differ by Chromium build).

**Shared code (from NextDNS Manager+, still true here)**

- `Base64.getMimeDecoder()` silently skips invalid characters; use `getDecoder()` after stripping whitespace.
- WebView: `setVisibility(GONE)` alone never hides a page; `onPause()` does (Chromium `BrowserViewRenderer`), and a
  hidden page's timers are throttled, not stopped. `clearCache(true)` clears only the cache of the WebView's own
  profile. `ProfileStore.deleteProfile` throws for a profile used in this process → wipe instead, delete at next start.
  Default profile can't be deleted. `shouldOverrideUrlLoading` is not called for subframe http(s) navigations
  (Chromium) nor for POSTs; the app also checks `isForMainFrame()`.
- `BiometricPrompt` authenticators: `BIOMETRIC_WEAK | DEVICE_CREDENTIAL` on Android 11+; on Android 10 use
  `setDeviceCredentialAllowed(true)`.
- A `Dialog`'s dismiss listener is delivered later as a posted message: never `recreate()` from it when the screen is
  finishing (`ThemeRebuild.onSettingsClosed`).
- `androidx.webkit` 1.17.1 sources [read 2026-11-19]: `HttpCache` — don't pass `getDefaultQuotaBytes()` to
  `setQuotaBytes`; a set quota persists and is clamped. `WebStorageCompat.deleteBrowsingData` is not atomic (may delete
  data stored meanwhile) → `web/WipeGate`.
- CI runners: `ubuntu-latest` = Ubuntu 26.04; Playwright 1.56.1 refuses it, so `pages` and `icon` are pinned to
  `ubuntu-24.04`. `-PrepoUrl` changes the APK: never use it for a release (F-Droid builds without it).
- Release signing [tested on NextDNS Manager+, 2026-11-19, apksigcopier 1.1.1]: apksigner 34 + `--v1-signing-enabled
  false` passes `apksigcopier compare`; 34 default and 35 fail. keytool rejects `+` in a certificate name.
- A view drawn over others hides them only from eyes and touches: TalkBack and keyboard focus still reach covered
  views → `IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS` + `FOCUS_BLOCK_DESCENDANTS` + clear focus (`LockScreen.covers`).
- Not verifiable here: anything on a phone (WebView profiles, unlock sheet, MediaStore, status bar colours, sign-in,
  hCaptcha). Live site checks work with curl/Playwright (not the signed-in dashboard).

## 10. Skills and plugins

Visible in this environment: `superpowers` (brainstorming, writing-plans, test-driven-development,
verification-before-completion, requesting-code-review, systematic-debugging), `ui-consistency` (adjusting →
implementing → verifying for anything the user sees), `feature-dev`, `code-review`, `engineering`, `frontend-design`,
`design` (accessibility-review), `security-review`, `simplify`. Use writing-plans for phases, verification-before-
completion before claiming done, security-review for Phase 5 (sign-in) and Phase D (bridge). `context7` needs the
owner's sign-in on claude.ai (Settings → Connectors).

## 11. TODO

- [x] Phase 0: NextDNS Manager+ `eaa1665` imported unchanged (`abb10bf`, CI green, 2026-11-19).
- [x] Phase 1: live research; `tools/site-probe`; facts, owner questions and phone checks P1–P6 (`8642642`, 2026-11-19).
- [x] `UPSTREAM.md` (`19fa650`); the plan in `docs/ADGUARD_PLAN.md` (`2c2cf8b`).
- [x] Phase 2 (2026-11-20): name AdGuardDNS Manager + id `org.rafee_m.adguarddnsmanager` + repository (`8d6c321`), icon
      (`9197934`), README + badges (`1fa8540`), docs for AdGuardDNS Manager.
- [ ] **Owner:** Q2 (Ping: remove / connection details / latency, `docs/PHASES.md` Phase 7); phone test of Phase 2.
- [x] Renamed **AdGuardDNS Manager**, id `org.rafee_m.adguarddnsmanager` (`d671501`, 2026-11-20); repository stays
      `UpperPanda1/adguard-manager` until the owner renames it `adguard-dns-manager` (PROJECT.md §7).
- [x] Phase 3 Site (2026-11-20): `Site.java` → AdGuard DNS, tests, page checks, Ping window host rule.
- [ ] Owner: phone test of Phase 3 (CHANGES.md 2026-11-20 "Phase 3").
- [x] Phase 6 Status (2026-11-20): `status/DnsCheck`, suffix marker, F3 fixed.
- [ ] Phase 7 Connection details + latency (next) → 4 Theme → 5 Sign-in → D Downloads → 7 Ping → 8 Texts → 9 Workflows → 10 Review →
      11 Docs (step by step: `docs/PHASES.md`).
- [ ] Owner: release key + secrets, dry run, tag `v0.1.0`, F-Droid (RELEASE.md, fdroid.md); name/trademark answer.
- [ ] Inherited review items (REVIEW.md); translations.
