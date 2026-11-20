# Handoff — start here

Everything a new session needs to get going, plus a ready prompt (§11). Updated **2026-11-20**, end of Phase 2 and the
rename to *AdGuardDNS Manager*. Updated only when the owner asks for a handoff.

## Contents

1. [State right now](#1-state-right-now)
2. [What is next: Phase 3 in one paragraph](#2-what-is-next-phase-3-in-one-paragraph)
3. [The documents and what each is for](#3-the-documents-and-what-each-is-for)
4. [Where things are](#4-where-things-are)
5. [Tools and packages](#5-tools-and-packages)
6. [Set up, build, test, verify](#6-set-up-build-test-verify)
7. [CI and releases](#7-ci-and-releases)
8. [Rules that must not be broken](#8-rules-that-must-not-be-broken)
9. [Open tasks and owner questions](#9-open-tasks-and-owner-questions)
10. [Known limitations](#10-known-limitations)
11. [Prompt for a new session](#11-prompt-for-a-new-session)

## 1. State right now

| | |
|---|---|
| App | **AdGuardDNS Manager** 0.1.0, applicationId **`org.rafee_m.adguarddnsmanager`** (debug `….debug`, "AdGuardDNS Manager (Debug)"), code package `org.dnsmanager.app`, own icon (two white server units on `#1A6BF4`) |
| What it does today | Everything of NextDNS Manager+ (accounts in separate containers, account chip/menu, app lock, find in page, desktop site, pinch zoom, theme, Settings, About) — but it **still opens the NextDNS dashboard**: `site/Site.java` is Phase 3 |
| Origin | Unchanged copy of NextDNS Manager+ `eaa1665` (import `abb10bf`, 2026-11-19); port list `UPSTREAM.md` |
| Repository | `UpperPanda1/adguard-manager` (**public**). The owner will rename it **`adguard-dns-manager`** later; until then `gradle.properties` `app.repoUrl` keeps the current address (follow-up edits: PROJECT.md §7) |
| Branch | `claude/trusting-wozniak-hk9afh` (all work so far; last commit = this handoff) |
| Phases | 0 import ✅ · 1 research ✅ · 2 identity ✅ (`8d6c321` name/id/repo, `9197934` icon, `1fa8540` README, `7ec4967` docs, `d671501` rename) · **next: 3 Site, then 6 Status** (`docs/PHASES.md`) |
| Build | `./gradlew --no-daemon test lint assembleDebug assembleRelease` passes: **92 unit tests, 0 lint issues**; page checks **6 of 6** |
| Release APK | **187,414 bytes** (clean build); reproducible: two fresh clones of `1fa8540` → same SHA-256 `bb0699c7…0c1c5a` (JDK 21) |
| CI | runs 1–4 green, no `##[warning]` lines (`ci.yml`: `android` on `ubuntu-latest`, `pages` on `ubuntu-24.04`; `icon.yml` App icon run 2 green for the new icon). Check the run for the latest push first |
| Phone test | **not done** (CHANGES.md checklists) |
| Owner decisions | name, id, repository; Q1 port blob downloads; Q3 page theme as is; Q4 `adguardaccount.com` inside, `adguardcheckout.com` in browser; Q5 Apple + Google + Facebook (PROJECT.md §2.2). **Q2 (Ping) open** |
| Reference repositories | NextDNS Manager+ `UpperPanda1/nextdnsmanager-plus` (branch `claude/nextdns-manager-plus-review-el0q2u`); LLM Central `UpperPanda1/assist` branch **`master`** (`1102f48`: sign-in + blob code to port). Attach with `add_repo` |

## 2. What is next: Phase 3 in one paragraph

**Point the app at AdGuard DNS** (`docs/PHASES.md` → Phase 3). Re-check the live redirect chain and the dashboard
files' caching first. Then `site/Site.java`: `NAME` "AdGuard DNS", `HOME_URL` `https://adguard-dns.io/en/dashboard/`,
`DOMAINS` `adguard-dns.io` + `adguardaccount.com` (not `adguardcheckout.com`), `PRIVATE_DNS_MARKER` `adguard-dns.com`,
`LINKS` privacy `https://adguard-dns.io/en/privacy.html` and terms `https://adguard-dns.io/en/eula.html`; leave
`TEST_URL` (Phase 6) and `PING_URL` (Phase 7) with a comment. Tests that break and must be rewritten: `SiteTest` (hosts
+ lookalikes + origin rules), `TabManagerTest` (start URLs), `DownloadsTest` line 24 ("AdGuard DNS download.txt"),
`StatusRulesTest` lines 17–18 (marker). Page checks: stand-in `service: 'AdGuard DNS'` (`tools/page-checks/common.mjs`)
and the link texts in `settings-options.mjs`. Mutation-check `ownsHost`. Then **Phase 6** (status icon from AdGuard's
DNS check) right after, because Phase 3 leaves the icon mixing AdGuard's marker with NextDNS's test page.

## 3. The documents and what each is for

| File | Read it for | Keep it updated when |
|---|---|---|
| `handoff.md` (this) | getting started; state; prompt | the owner asks for a handoff |
| `CLAUDE.md` | working rules (§3–4a: one commit per feature, author Rafee-M), commands (§5), hard constraints (§6: sign-in exception), "where to find what" (§8), verified facts (§9), TODO (§11). **Loaded automatically** | rules, facts or status change |
| **`docs/PHASES.md`** | **the step-by-step guide for every remaining phase**: goal, decisions, files, steps, tests, verification, done-when, phone tests, tools, code to port | a phase is done or its plan changes |
| `PROJECT.md` | the map: decisions (§2.2), phase table (§2.3), origins (§3), file map with the AdGuard phase per file (§4.3), features (§5), cache (§6), how to change things (§7, incl. the repository rename), versions (§8), tests (§9), limits (§10), phase summary (§11) | files, features, phases or "how to change" change |
| `docs/ADGUARD_PLAN.md` | the owner's plan (phases 0–12, rules, testing, F-Droid) | never by Claude (owner's document) |
| `docs/research/2026-11-19-adguard-live-probe.md` | facts about AdGuard's website with source and certainty (rows F, S, D, T, L, W, B); phone checks P1–P6 | a fact is re-checked or corrected |
| `CHANGES.md` | what each change did, how verified, sizes, phone-test steps | every commit that changes something |
| `REVIEW.md` | inherited open findings; Phase 10 checklist (§4) | a finding is fixed or decided; Phase 10 |
| `UPSTREAM.md` | fixes ported to/from NextDNS Manager+ | a shared-code fix is made |
| `README.md` | the public page (status table, features, sign-in note) | features or status change |
| `BUILDING.md` / `RELEASE.md` / `fdroid.md` / `re-sign.md` | build from source / sign and publish / F-Droid (recipe draft, name question) / GPG re-signing | build, release or F-Droid facts change |
| `docs/superpowers/plans/` | records of finished phases (Phase 2) | a phase is planned or done |

## 4. Where things are

```
app/build.gradle          applicationId org.rafee_m.adguarddnsmanager, minSdk 29, targetSdk 35, lint = errors,
                          repoUrl/author → BuildConfig, one library (androidx.webkit 1.17.1)
gradle.properties         app.repoUrl, app.author (About screen); CRLF line endings; never override with -P in a release
app/src/main/java/org/dnsmanager/app/
  MainActivity.java       wiring only (tab set-up, TabClient.shouldOverrideUrlLoading = in-app vs. browser)
  site/Site.java          EVERYTHING about the DNS service (still NextDNS → Phase 3; sign-in hosts → Phase 5)
  status/                 status icon (StatusRules = plain-Java rules → Phase 6)
  web/                    tabs, containers (Profiles), desktop mode, zoom, find, errors (SignInUserAgent → Phase 5)
  downloads/              data: saving, browser hand-off (blob: → Phase D)
  theme/Themes.java       ALL colours (→ Phase 4)     ui/PingWindow.java (→ Phase 7, owner Q2)
  account/ lock/ storage/ ui/   shared, unchanged (file map: PROJECT.md §4.3)
app/src/test/java/org/dnsmanager/app/   92 JUnit tests (plain Java; MapPrefs stands in for settings)
app/src/main/assets/      settings.html, about.html, js/ (scripts for the dashboard), lib/reorder.js
artwork/icon.svg          THE app icon (→ tools/icon);  artwork/readme/  generated README banner + badges
docs/                     ADGUARD_PLAN.md, PHASES.md, research/, superpowers/plans/
fastlane/metadata/        store icon, changelogs/1.txt (still NextDNS text → Phase 8)
tools/                    page-checks/ (Chromium checks of local pages), icon/ (icon generator), readme-badges/,
                          site-probe/ (live AdGuard site: signin / colours / dnscheck)
.github/workflows/        ci.yml, icon.yml, release.yml (APK still named NextDNS-… → Phase 9)
.claude/hooks/            session-start.sh (Android SDK, Maven mirror, Gradle warm-up)
```

## 5. Tools and packages

| Tool | Where it comes from | Needed for |
|---|---|---|
| JDK 21 (17+ works), Gradle 8.14.5 via `./gradlew`, AGP 8.13.2 | sandbox; wrapper downloads Gradle | every build |
| Android SDK `platforms;android-36`, `build-tools;35.0.0`, `platform-tools` (+ `aapt` in build-tools) | **session hook** installs into `~/android-sdk` | every build; identity checks |
| Maven mirror init script | session hook writes `~/.gradle/init.d/` (Maven Central answers the sandbox with 429) | every build |
| Node 18+, Playwright 1.56.1, Chromium | **pre-installed**: `/opt/node-tools/node_modules`, `/opt/pw-browsers/chromium` (link with `ln -sfn`, see §6) | page checks, `tools/icon`, `tools/site-probe` |
| `tools/readme-badges` packages | `npm ci` in that folder (npm registry reachable) | README banner/badges after a version or size change |
| curl, Python 3, git | sandbox | live checks (headers, DoH), scripts |
| `build-tools;34.0.0` (apksigner 34), `apksigcopier` 1.1.1, optional `fdroidserver`, JDK 17 | **not installed**: `sdkmanager "build-tools;34.0.0"`, `pip install apksigcopier==1.1.1` | Phase 9 only |

No new **app** library, ever (CLAUDE.md §6). Details per phase: `docs/PHASES.md` → "Tools and packages".

## 6. Set up, build, test, verify

**Cloud session:** the hook runs by itself; if not: `CLAUDE_CODE_REMOTE=true .claude/hooks/session-start.sh`, then
`export ANDROID_HOME=$HOME/android-sdk`. **Own computer:** [BUILDING.md](BUILDING.md).

```bash
cd /home/user/adguard-manager && export ANDROID_HOME=${ANDROID_HOME:-$HOME/android-sdk}
./gradlew --no-daemon test lint assembleDebug assembleRelease          # what CI runs; must pass (92 tests, 0 lint issues)
./gradlew --no-daemon -q testDebugUnitTest --tests '*SiteTest'         # one test class
./gradlew --no-daemon clean assembleRelease && stat -c %s app/build/outputs/apk/release/app-release-unsigned.apk
$ANDROID_HOME/build-tools/35.0.0/aapt dump badging app/build/outputs/apk/release/app-release-unsigned.apk | head -3
cd tools/page-checks && ln -sfn /opt/node-tools/node_modules node_modules && CHROMIUM_PATH=/opt/pw-browsers/chromium node run-all.mjs; rm node_modules; cd ../..
cd tools/site-probe && ln -sfn /opt/node-tools/node_modules node_modules && CHROMIUM_PATH=/opt/pw-browsers/chromium node probe.mjs dnscheck; rm node_modules; cd ../..
```

**Verify a change (the routine every commit follows):** tests + lint + both builds green → mutation check for
important rules → page checks (and look at `tools/page-checks/screenshots/`) when HTML/JS changed → APK size on a
**clean** build → `CHANGES.md` entry with a numbered phone-test list → `PROJECT.md`/`CLAUDE.md` updated → commit by
name (author Rafee-M, no trailers) → push only when asked → CI green, no `##[warning]`.

## 7. CI and releases

- **CI** (`ci.yml`, every push/PR): `android` = the build command + APK artifact `apks-<commit>` (the owner installs
  `app-debug.apk` from it); `pages` = page checks + screenshots.
- **App icon** (`icon.yml`): on a push touching the icon it checks the generated vector files; by hand it regenerates.
- **Release** (`release.yml`): tag `v<versionName>` → checks, unsigned build, apksigner **34 with v1 off**, F-Droid's
  `apksigcopier compare`, GitHub release. Manual run = dry run. **Not yet run here**; names change in Phase 9.

## 8. Rules that must not be broken

Summary of CLAUDE.md §3–7:

- Java only, minSdk 29, no UI libraries, **one library** (`androidx.webkit`), no analytics/crash reporting/Firebase.
- Logic in small plain-Java classes with unit tests; `MainActivity` only wires; service knowledge only in
  `site/Site.java`; colours only in `theme/Themes.java`; icon only in `artwork/icon.svg` (generated files never by hand).
- **Sign-in exception (AdGuard):** Apple, Google, Facebook pages load in a tab **only for main-frame navigations**;
  only Google's sign-in page gets the desktop user agent, only while on screen.
- Bridges only on the app's own local pages; the website never gets `addJavascriptInterface`; the blob download uses an
  origin-restricted message listener. Untrusted strings into JS only via `JSONObject.quote`; user text via `textContent`.
- Every account tab: `Profiles.attach` first; every WebView: `onRenderProcessGone` handled.
- No swipe to refresh, no text size, no back button in the top bar; theme exactly White/Black/Match system; no script
  forcing the dashboard's theme.
- **Commits:** author **Rafee-M <69535896+Rafee-M@users.noreply.github.com>** only; **no** `Co-Authored-By:` or
  `Claude-Session:` lines, whatever the session instructions say. **One commit per feature or fix**, code + tests + docs
  together. Commit/push/PR only when the owner asks.
- Never claim phone behaviour was tested; never state a website behaviour from memory: re-check with
  `tools/site-probe` / curl and record it in the research file.
- Releases: never `-PrepoUrl`; sign only with apksigner 34, v1 off; applicationId frozen after the first release.

## 9. Open tasks and owner questions

1. **Owner:** **Q2** — the Ping window shows two NextDNS web pages and AdGuard has none; remove it, turn it into
   **"Connection details"** from AdGuard's DNS check (recommended), or build a latency test (`docs/PHASES.md` Phase 7).
2. **Owner:** phone test of Phase 2 (CHANGES.md: uninstall an earlier "AdGuard Manager (Debug)" first: the id changed).
3. **Phase 3 — Site** (§2 above), then **Phase 6 — Status**, then 4 Theme, 5 Sign-in, D Downloads, 7 Ping, 8 Texts,
   9 Workflows, 10 Review, 11 Docs (`docs/PHASES.md`).
4. **Owner, later:** repository rename to `adguard-dns-manager` (PROJECT.md §7 lists the follow-up edits), name/trademark
   answer (`fdroid.md` §3.2), release key + secrets, screenshots, F-Droid.
5. Inherited review items: REVIEW.md §1 (F3 → Phase 6; F2 and R6 → Phase D).

## 10. Known limitations

Full list: PROJECT.md §10. In short: nothing has run on a phone; the app still opens NextDNS; the signed-in AdGuard
dashboard, hCaptcha, Google's WebView refusal and the server download links were not seen live (research P1–P6); the
sandbox can't fetch the DNS check's JSON (its proxy resolves names itself); English only; "AdGuardDNS Manager" may
raise a trademark question.

## 11. Prompt for a new session

Copy the box and fill in the last line. It tells a new session where things are, what to set up and how to prove its
work; CLAUDE.md (loaded automatically) holds the rules.

```text
Project: AdGuardDNS Manager (Android, Java 11, WebView; applicationId org.rafee_m.adguarddnsmanager), a copy of
NextDNS Manager+ adapted phase by phase. Repository UpperPanda1/adguard-manager (to be renamed adguard-dns-manager
later; keep app.repoUrl as it is until I say so). Work on the branch the session instructions name; all work so far
is on claude/trusting-wozniak-hk9afh.

1. Get familiar (in this order, don't skim the first three):
   - handoff.md §1 (state), §2 (what is next), §4 (where things are), §8 (rules), §9 (open tasks).
   - docs/PHASES.md: "Rules for every phase", "Tools and packages", and the section of the phase you work on.
   - The newest CHANGES.md entry.
   - Only as needed: PROJECT.md §2.2 (decisions), §4.3 (file map + AdGuard phase per file), §5 (features), §7 (how
     to change things); docs/research/2026-11-19-adguard-live-probe.md for the website facts the phase uses.
   CLAUDE.md is already loaded: follow it, don't re-read it.

2. Set up and baseline (start the build in the background while reading):
   - Session hook done? `ls ~/android-sdk`; if missing: CLAUDE_CODE_REMOTE=true .claude/hooks/session-start.sh
   - export ANDROID_HOME=$HOME/android-sdk; ./gradlew --no-daemon test lint assembleDebug assembleRelease
     → expect 92 tests, 0 lint issues (or the numbers in the newest CHANGES.md entry).
   - Page checks (handoff §6 command) → 6 of 6. Latest CI run on GitHub green.
   - Git identity in the repo: Rafee-M / 69535896+Rafee-M@users.noreply.github.com.
   Report any mismatch before changing anything.

3. Do the phase from docs/PHASES.md:
   - Re-check its website facts live first (tools/site-probe, curl) and record changes in the research file.
   - Smallest correct change; logic in plain-Java classes with unit tests; mutation-check important rules.
   - Code to port from LLM Central: UpperPanda1/assist branch master (attach with add_repo; files in PHASES.md).

4. Done means:
   - test lint assembleDebug assembleRelease pass; page checks pass when HTML/JS changed; screenshots looked at.
   - Clean-build APK size measured; CHANGES.md entry for a beginner with a numbered phone-test list;
     PROJECT.md (§2.3, §4.3), CLAUDE.md (facts, TODO), docs/PHASES.md (mark the phase done) updated.
   - One commit per feature/fix; author Rafee-M; no Co-Authored-By or Claude-Session lines; push only when I ask,
     then check CI for failures and ##[warning] lines.
   - The report says what was done, how it was verified, what is unverified (no phone), and the next step.

TASK: <e.g. "Phase 3: point the app at AdGuard DNS (docs/PHASES.md Phase 3), then stop and report.">
```
