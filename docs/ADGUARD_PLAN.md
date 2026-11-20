# AdGuard DNS Manager+: the plan

How to build **AdGuard DNS Manager+** from **NextDNS Manager+**, written for the owner (**Rafee-M**) and for the Claude
sessions that do the work. Read it together with `ADGUARD_PROMPT.md` (the prompt that starts a session).

Status of the facts below: **[live]** = checked on the live site on 2026-11-19; **[NextDNS]** = verified in the
NextDNS Manager+ project (its `CLAUDE.md` §9); **[to verify]** = must be checked by the new session before code
depends on it.

## Contents

1. [What we are building](#1-what-we-are-building)
2. [How the two apps relate: copy, not fork, not flavors](#2-how-the-two-apps-relate)
3. [What NextDNS Manager+ contains (the base)](#3-what-nextdns-manager-contains)
4. [What changes for AdGuard: file by file](#4-what-changes-for-adguard)
5. [New features AdGuard needs](#5-new-features-adguard-needs)
6. [The plan, step by step](#6-the-plan-step-by-step)
7. [Rules for every session](#7-rules-for-every-session)
8. [Testing: how every step is proven](#8-testing)
9. [Documentation structure](#9-documentation-structure)
10. [GitHub workflows](#10-github-workflows)
11. [F-Droid compliance](#11-f-droid-compliance)
12. [Traps already paid for in NextDNS Manager+](#12-traps-already-paid-for)
13. [Owner decisions before or during the work](#13-owner-decisions)

---

## 1. What we are building

An Android app that shows the **AdGuard DNS dashboard** (`https://adguard-dns.io/en/dashboard/` [live]) with the same
native extras NextDNS Manager+ gives the NextDNS dashboard:

- several AdGuard accounts signed in **side by side**, one isolated WebView *profile* (container) per account,
  switched from a numbered chip in the top bar;
- a "am I using AdGuard DNS?" status icon, app lock, find in page, desktop site, pinch to zoom, downloads,
  White/Black/Match-system theme, Settings and About screens;
- **new:** sign-in with **Google, Apple and Facebook**, which the AdGuard login page offers [live] and NextDNS doesn't.

Owner: **Rafee-M**. Licence: GPL-3.0 (the code descends from LLM Central and NextDNS Manager+). Same hard limits as
NextDNS Manager+: Java only, minSdk 29, one library (`androidx.webkit`), no UI libraries, no analytics or tracking.

## 2. How the two apps relate

**Recommendation: a new repository that starts as an exact copy of NextDNS Manager+, then changes step by step.**

| Option | Verdict |
|---|---|
| **GitHub "Fork" button** | **No.** GitHub doesn't let an account fork its own repository; a fork in another account is permanently linked ("forked from …"), shares the issue/PR network, and for F-Droid it is a separate app anyway. Nothing is gained. |
| **Gradle product flavors** in the NextDNS repository (`nextdns` / `adguard`; NextDNS `CLAUDE.md` §8 suggested this) | Not now. One repository, one README, two applicationIds, two release tags, two F-Droid builds; every file a beginner opens has a "which flavor?" question. AdGuard also **diverges** (sign-in, pop-ups, captcha), so more files would need a copy per flavor over time. Revisit only if keeping two copies in step becomes painful. |
| **New repository, copied once (recommended)** | Simple: each app stands alone, has its own README/releases/F-Droid recipe and can grow its own features. Same proven method used when NextDNS Manager+ was built from LLM Central. Cost: a fix to shared code must be ported by hand, handled by the "port list" rule (§7.5). |

**How the copy is made (Phase 0):** an empty GitHub repository (e.g. `adguarddnsmanager-plus`), then one commit
"Import NextDNS Manager+ at `<commit id>`" containing the NextDNS tree **unchanged** (no history, like NextDNS's own
orphan start). Because the import is unchanged, every later commit shows exactly one adaptation, so each change can
be reviewed, reverted and compared with NextDNS.

**Keep the Java package `org.dnsmanager.app`.** It is invisible to users and generic ("DNS manager"). Keeping it
makes a NextDNS fix apply to AdGuard as a near-identical diff. Only the **applicationId** (the app's identity on the
phone and in F-Droid) changes.

## 3. What NextDNS Manager+ contains

Read its `PROJECT.md` §4.3 (file map) for the full table; in short (package `org.dnsmanager.app`, ~5,900 lines Java,
92 unit tests, 6 page checks):

| Area | Files | What it does |
|---|---|---|
| Wiring | `MainActivity` | builds the screen, top bar, menus, lock flow; **wiring only, no logic** |
| The service | `site/Site` | **everything about NextDNS**: name, URLs, own domains (`ownsHost`), test answer format, Private DNS marker, legal links |
| Accounts | `account/Account`, `AccountStore` (rules, synchronized), `AccountsBridge` (Settings page bridge) | up to 20 accounts, names, order, add/remove/sign out |
| Tabs and containers | `web/TabManager`, `TabLru`, `Profiles`, `WipeGate` | one WebView per loaded account, LRU of 1–10 kept loaded, one profile per account, cache cap 20 MB, Clear cache, sign-out wipe that the page waits for |
| Page behaviour | `web/DesktopMode`, `PageZoom`, `FindInPage`, `LoadErrors`, `WebViewRecovery`, `Scripts`, `ExternalLinks`, `DialogWebViewClient` + `assets/js/*.js` | desktop site, pinch zoom, find, retry screen, renderer-crash recovery, only safe links leave the app |
| Theme | `theme/Themes` (**all colours**), `Scheme`, `ThemeManager`, `ThemeRebuild`, `ThemeBridge`, `ContentInsets` | White/Black/Match system, status bar = page colour, insets on Android 10–15+ |
| UI | `ui/SwitcherPopup`, `FindBar`, `LockScreen`, `SettingsWindow`, `AboutWindow`, `PingWindow`, `Windows`, `NativeTheme`, `AccountBadge`, `UnloadedPlaceholder`, `SettingsRestore` | native views; Settings/About are local HTML pages (`assets/settings.html`, `about.html`) |
| Lock | `lock/AppLock`, `LockRules` | phone unlock (biometric/PIN), secure flag, cover hidden from TalkBack/keyboard |
| Status | `status/ConnectionStatus`, `StatusRules` | Private DNS state + the service's test page |
| Files | `downloads/Downloads`, `DataUrl`; `storage/AppCache`, `StorageBridge` | `data:` downloads to Downloads, cache size |
| Build/identity | `app/build.gradle`, `gradle.properties` (`app.repoUrl`, `app.author=Rafee-M`), `artwork/icon.svg`, `tools/icon`, `tools/readme-badges`, `tools/page-checks` | reproducible builds, generated icon and README badges |
| Docs | `CLAUDE.md`, `PROJECT.md`, `CHANGES.md`, `handoff.md`, `REVIEW.md`, `README.md`, `BUILDING.md`, `RELEASE.md`, `fdroid.md`, `re-sign.md` | see §9 |
| CI | `.github/workflows/ci.yml`, `icon.yml`, `release.yml` | see §10 |

## 4. What changes for AdGuard

Rule of thumb from NextDNS `CLAUDE.md` §8: **reuse everything except what names or depends on NextDNS.**

| File / area | Change | Notes |
|---|---|---|
| `site/Site.java` | **rewrite values** | `NAME` "AdGuard DNS"; `HOME_URL` `https://adguard-dns.io/en/dashboard/` [live]; `DOMAINS`: `adguard-dns.io` [live], `adguardaccount.com` (login: `auth.adguardaccount.com` [live]), plus any API/asset hosts the dashboard needs [to verify]; `PRIVATE_DNS_MARKER` → match `adguard-dns.com` (public servers are `dns.` / `family.` / `unfiltered.adguard-dns.com` [live]; personal servers [to verify]); test URL/answer [to verify; if none, status from Private DNS only]; `PING_URL` [to verify, else hide Ping]; `LINKS` (privacy, terms) [to verify]. `SiteTest` rewritten. |
| `theme/Themes.java` | **new colours** | read the dashboard's CSS (light/dark background, accent) as was done for NextDNS (`#212529`); `ContrastTest` must stay green |
| `status/StatusRules` | adapt | AdGuard's answer format, or Private DNS only |
| `ui/PingWindow` + menu item | adapt or remove | only if AdGuard has an equivalent page |
| Sign-in | **add** (§5) | port from LLM Central |
| `strings.xml` `app_name`, `applicationId`, `artwork/icon.svg` | new | new name and id (owner, §13), new icon → `tools/icon` generates the rest; **never hand-edit generated icon files** |
| `gradle.properties` | `app.repoUrl` = the new repository; `app.author=Rafee-M` | read at build time into About |
| `tools/readme-badges` | `BRAND` = AdGuard's accent; banner text | regenerate `artwork/readme/` |
| `assets/settings.html`, `about.html` | wording | service name fills in from `Site.NAME`; check Privacy/Disclaimer text and About credits ("Built on NextDNS Manager+ and LLM Central") |
| `fastlane/metadata/…` | new texts | title, descriptions, changelog 1 |
| `.github/workflows/release.yml` | names | APK `AdGuard-DNS-Manager-Plus-<version>.apk`, release title; keep the signing rules exactly |
| Everything else | **unchanged** | `account/`, `web/` (minus new sign-in files), `ui/`, `lock/`, `downloads/`, `storage/`, `theme/` minus Themes |

## 5. New features AdGuard needs

### 5.1 Sign-in with Google, Apple, Facebook [live: the login page lists `apple`, `google`, `facebook`]

Facts found on 2026-11-19 in the login page (`auth.adguardaccount.com`, script `auth.min.js`):
- the dashboard redirects to `https://auth.adguardaccount.com/oauth/authorize?…client_id=adguard-dns-website…`
  and back to `https://adguard-dns.io/oauth/callback.html`;
- social sign-in navigates the **whole page** to `/auth/<provider>?state=…` (no `window.open`, no pop-up);
- the login uses **hCaptcha** (loaded in frames) and may ask for a two-factor code.

What the app needs (all three exist in LLM Central, branch `claude/confident-brahmagupta-9iiv9x`; NextDNS
`CLAUDE.md` §8 lists them):
1. **Own domains + sign-in hosts load inside the account's container.** Port `provider/SignInHosts` as a sign-in host
   list in `site/Site` (`accounts.google.com`, `appleid.apple.com`, `idmsa.apple.com`, Facebook's login hosts
   [to verify: `www.facebook.com`, `m.facebook.com`]). Only main-frame navigation decisions use it; keep `ownsHost`
   for everything else (as LLM Central's `loadsInApp` does).
2. **Google refuses WebViews** (`403 disallowed_useragent`). Port `web/SignInUserAgent`: only while
   `accounts.google.com` is on screen, the tab gets a desktop-Linux user agent (geminiAssist's method), restored when
   it leaves. 4 tests exist in LLM Central. Owner rule: never disguise anywhere else.
3. **Pop-ups:** not needed according to the login page code; port `web/PopupWindows` only if the live test shows a
   pop-up (e.g. Apple's).
4. **hCaptcha** runs in frames from `hcaptcha.com` [to verify the exact hosts]: frames are not intercepted by
   `shouldOverrideUrlLoading` (main frame only), so they work; check whether it needs third-party cookies (LLM Central
   has a switch, `web/ThirdPartyCookies`, off by default).
5. **Remember:** NextDNS Manager+ had the hard rule "no Google/Apple sign-in code". For AdGuard it is **required**;
   write that exception into the new `CLAUDE.md` §6.

### 5.2 Other things to check live
Downloads the dashboard makes (blob:/data:? files, exports), logout URL, which cookies the login sets on which
domain, dark mode (does it follow `prefers-color-scheme` or a stored setting?), viewport/zoom ban, whether the
dashboard has live streams that matter for battery (NextDNS: closed itself after 5 min idle).

## 6. The plan, step by step

Each step = at least one commit; each commit builds and passes (§7).

| Phase | Step | Done when |
|---|---|---|
| **0 Set-up** | Owner creates the empty repository and attaches it to a cloud session. Session imports the NextDNS tree at a named commit (one commit, unchanged), runs the session hook. | `test lint assembleDebug assembleRelease` pass (92 tests, 0 lint issues), 6 page checks pass, CI green, **before any change** |
| **1 Research** | Live probe of the AdGuard dashboard and login (curl + Playwright, as NextDNS did with its `main.*.js` bundle): every **[to verify]** item in §4–§5. Results go into the new `CLAUDE.md` §9 with dates and sources. Nothing is coded from memory. | a facts table; open questions listed for the owner |
| **2 Identity** | name, applicationId, `app.repoUrl`/`app.author`, icon (`artwork/icon.svg` → `tools/icon`), strings, About credits, README rewritten with new badges/banner | builds; About shows the new repository and Rafee-M; icon check passes |
| **3 Site** | `Site.java` values + `SiteTest` (domains, `ownsHost` with lookalikes: `adguard-dns.io.evil.com`, `evil-adguard-dns.io`, trailing dot) — mutation-check `ownsHost` | tests green; a deliberate break fails a test |
| **4 Theme** | `Themes.java` from AdGuard's CSS; `ContrastTest`; page-check screenshots in light/dark | contrast test green; screenshots looked at |
| **5 Sign-in** | sign-in hosts + `SignInUserAgent` (+ tests ported and adapted) + wiring in `TabClient.shouldOverrideUrlLoading`; pop-ups only if needed | tests + mutation checks; probe shows the Google page loads with the disguised agent only there |
| **6 Status** | `StatusRules` for AdGuard (+ tests) | tests for every answer seen live |
| **7 Ping** | adapt or remove (`PingWindow`, menu item, strings) | builds, lint 0 (no unused resources) |
| **8 Texts** | Settings Privacy/Disclaimer, About, store texts (`fastlane/`), changelog 1 | page checks green |
| **9 Workflows** | `release.yml` names; dry-run logic re-run locally (as NextDNS did: extract `run:` blocks, execute with a test key, `apksigcopier compare`) | local rehearsal passes |
| **10 Review** | adversarial review of the whole diff vs the NextDNS import (security: sign-in hosts, user agent scope, lock; UI; battery) — record in `REVIEW.md` | findings fixed or listed for the owner |
| **11 Docs** | `PROJECT.md`, `CLAUDE.md`, `CHANGES.md` (beginner entries + phone-test lists), `handoff.md` (on request), `fdroid.md` | every number in the docs matches a run |
| **12 Owner** | phone test, name/trademark decision, key + secrets, dry run, tag `v0.1.0`, F-Droid recipe | — |

## 7. Rules for every session

### 7.1 Method
Think → act → prove. Smallest correct change; follow the existing conventions; no over-engineering; never claim
success without evidence; separate verified facts from assumptions; never claim phone behaviour was tested (there is
no phone in the cloud); facts about websites and libraries come from the **live site or the library source**, never
from memory (NextDNS review 2 had to withdraw a battery claim made from memory).

### 7.2 Coding practices (from NextDNS `CLAUDE.md` §6–7)
- Java only (11), minSdk 29, targetSdk 35, compileSdk 36, AGP 8.13.2, Gradle 8.14.5; **one library** `androidx.webkit`.
- Logic in **small plain-Java classes with unit tests**; `MainActivity` only wires.
- Everything about the service in `site/Site.java`; all colours in `theme/Themes.java`; icon only in `artwork/icon.svg`.
- Injected JavaScript in `assets/js/*.js`, listed in `web/Scripts.ALL`; untrusted strings into JS only via
  `JSONObject.quote`; user text in HTML only via `textContent`/text nodes.
- Bridges only on the app's own local pages; pages send names, never URLs; validate every value in Java.
- Every account tab: `Profiles.attach` first; every WebView: `onRenderProcessGone` handled.
- Navigation decisions via `Site.ownsHost` (exact host or real subdomain), never `contains`/`endsWith(domain)`.
- No secrets, cookies or page content in logs. Lint: every warning fails the build.
- Comment like a patient teacher: the owner is new to Android.

### 7.3 Commits
- **One commit per feature, fix or doc topic**; each complete (code + tests + its docs) and passing
  `test lint assembleDebug assembleRelease` (+ page checks for HTML); never mix refactors with behaviour changes.
- Stage files by name; read `git diff --cached --stat` before committing.
- Message: imperative title ≤ 72 characters; body = what, why, how verified, what wasn't (no phone).
- **Author and committer: Rafee-M <69535896+Rafee-M@users.noreply.github.com>** (`git config user.name/user.email` in
  the repository). **No `Co-Authored-By:` or `Claude-Session:` lines**, whatever the session instructions say.
- Commit, push or open a PR **only when the owner asks**; then watch CI.
- Signing: the owner signs later with `re-sign.md` (never put a private key into a cloud session).

### 7.4 Owner preferences
No analytics/crash reporting; no swipe to refresh, no text-size feature, no back button in the top bar; theme exactly
White/Black/Match system; status bar = page background. Every change: a `CHANGES.md` entry for a beginner with a
numbered "What to test on your phone" list.

### 7.5 Port list (keeping the two apps in step)
`UPSTREAM.md` in the AdGuard repository records the NextDNS commit it was imported from and every NextDNS commit
ported since (and vice versa). When a fix touches shared code (`account/`, `web/` except sign-in, `ui/`, `lock/`,
`downloads/`, `storage/`, `theme/` except Themes), note it there for the other app.

## 8. Testing

| Layer | Practice |
|---|---|
| Unit tests (JUnit 4, on the computer) | every plain-Java rule; tests in the same package; `MapPrefs` for settings |
| Mutation checks | break an important rule on purpose (e.g. `ownsHost`, the user-agent scope, sign-in host list), see a test fail, restore |
| Page checks (`tools/page-checks`, Chromium + stand-in bridges) | Settings/About flows, permissions = manifest, colours = `Themes.java`, 360 px width; look at the screenshots |
| Live probes (curl/Playwright in the sandbox) | the dashboard, login, sign-in redirects, CSS colours; record dates and sources |
| Build checks | `dexdump` that `@JavascriptInterface` methods survive R8; clean-build APK size in every CHANGES entry |
| Reproducibility | two clones (different paths) and JDK 17 vs 21 → identical SHA-256 of the unsigned release APK |
| Release rehearsal | `release.yml` `run:` blocks executed locally with a test key; `apksigcopier compare` passes |
| Adversarial re-read | "what if two things happen at once?", "what if the account is removed while…?" on every diff |
| Phone | owner only, from the CHANGES.md checklists |

## 9. Documentation structure

Same set as NextDNS Manager+, rewritten for AdGuard (copy the structure, not the NextDNS facts):

| File | Purpose | Updated when |
|---|---|---|
| `handoff.md` | state, where things are, rules, open tasks, the ready prompt | owner asks |
| `CLAUDE.md` | working rules, commands, hard constraints (with the sign-in exception), verified facts, TODO | rules/facts/status change |
| `PROJECT.md` | the map: decisions, reuse, architecture, file map, features, cache, how to change things, versions, tests, limits | files/features change |
| `CHANGES.md` | every change for a beginner + phone-test list + size | every commit that changes something |
| `REVIEW.md` | review findings and their status | after reviews |
| `UPSTREAM.md` | port list with NextDNS (§7.5) | a shared fix is made |
| `README.md` | public page (badges from `tools/readme-badges`) | features change |
| `BUILDING.md`, `RELEASE.md`, `fdroid.md`, `re-sign.md` | build, release/sign, F-Droid, re-signing | facts change |

## 10. GitHub workflows

Copied from NextDNS and kept as they are, except names:
- `ci.yml`: job `android` (`test lint assembleDebug assembleRelease`, APK artifact, size) on `ubuntu-latest`; job
  `pages` on **`ubuntu-24.04`** (Playwright 1.56.1 doesn't support 26.04) with the 3× retry of the Chromium install
  (Ubuntu's mirror fails sometimes).
- `icon.yml`: regenerate/check the icon from `artwork/icon.svg` (24.04, retry).
- `release.yml`: tag `v<versionName>` → checks (tag = versionName, changelog ≤ 500 chars, `app.repoUrl` = this
  repository) → unsigned build → **apksigner 34 with `--v1-signing-enabled false`** → `apksigcopier compare` → GitHub
  release; manual run = dry run. Never pass `-PrepoUrl` in a release build.

## 11. F-Droid compliance

- Free dependencies only; no tracking; `dependenciesInfo` off; reproducible build (§8).
- **Own identity**: new name, applicationId, icon (F-Droid requires it for derived apps).
- `License: GPL-3.0-only`; `AntiFeatures: NonFreeNet` (depends on the proprietary AdGuard DNS service).
- Google/Apple/Facebook sign-in: done in the website inside the app, no SDK → no Google Play Services; but the
  user-agent disguise for Google's page must be described honestly in the README/description.
- Store texts in `fastlane/metadata/android/en-US/`; `Binaries:` + `AllowedAPKSigningKeys:` route as in NextDNS
  `fdroid.md`; `UpdateCheckMode: Tags`.
- Trademark: "AdGuard" is AdGuard's trademark → owner decision on the name (§13).

## 12. Traps already paid for

From NextDNS `CLAUDE.md` §9 (read it in full): Maven Central answers the sandbox with HTTP 429 (the hook's mirror;
`google()` first in `settings.gradle`); lint `RequiresFeature` wants a positive `if (isFeatureSupported)` in the same
method; `setVisibility(GONE)` doesn't hide a page, `onPause()` does; `ProfileStore.deleteProfile` throws for a profile
used in this run; profile deletion is asynchronous and may delete data stored meanwhile (→ `WipeGate`); a cover view
doesn't hide content from TalkBack/keyboard (→ `LockScreen.covers`); dialogs need `FLAG_SECURE` each, pop-ups inherit
it; `HttpCache.setQuotaBytes` takes a fixed number; `-PrepoUrl` changes the APK; apksigner 35 or v1 signing breaks
F-Droid's check; keytool rejects `+` in a certificate name; Playwright vs Ubuntu 26.04; measure APK size only on a
clean build.

## 13. Owner decisions

1. App name ("AdGuard DNS Manager+" vs a neutral name: trademark) and applicationId (e.g.
   `org.rafee_m.adguarddnsmanager`; frozen after the first release).
2. Repository name and owner account (the About links read `app.repoUrl`).
3. Which sign-in providers to support (all three, or Google + Apple only).
4. Keep or drop the Ping window (depends on research).
5. Whether AdGuard's own features (e.g. multiple DNS servers per account) need anything native, or the dashboard is enough.
