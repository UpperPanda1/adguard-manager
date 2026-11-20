# F-Droid submission: what it entails

Status of **AdGuardDNS Manager** (`org.rafee_m.adguarddnsmanager`, version 0.1.0 / code 1) against F-Droid's rules, and the
work that remains. Adapted 2026-11-20 from NextDNS Manager+'s `fdroid.md` (reviews of 2026-11-19), whose build and
release set-up this app shares. Facts about F-Droid come from its documentation pages (links in §8; read for NextDNS
Manager+ on 2026-11-19, not re-read); facts about this app come from the code and from commands that were run.
Anything not checked is marked **UNVERIFIED**.

**Short verdict:** technically a good candidate (one free dependency, no tracking, reproducible build shown for this
app). **Not ready to submit:** the AdGuard features themselves (Phases 3–D), store texts, screenshots, the release
workflow's names (Phase 9), a release key, a `v0.1.0` tag, an answer to the name question and a run of F-Droid's own
tools are missing.

## 0. The submission in one list (what "submitting" actually means)

| # | Step | Who / where | Done? |
|---|---|---|---|
| 1 | The app works with AdGuard: Phases 3–D (PROJECT.md §11) | this repo | no |
| 2 | Store texts and screenshots in `fastlane/metadata/android/en-US/` (§3.1) | this repo | icon only |
| 3 | Answer the name/trademark question (§3.2) | owner | name chosen, question open |
| 4 | Create a release key; GitHub secrets (§3.3, `RELEASE.md` §3) | owner | no |
| 5 | Release workflow: unsigned build → sign with `apksigner` 34, v1 off → `apksigcopier compare` → GitHub release; file names for this app (Phase 9) | this repo (`.github/workflows/release.yml`) | inherited; **names still NextDNS**; not yet run here |
| 6 | Tag `v0.1.0` on a clean commit; the workflow publishes the signed APK | owner | no |
| 7 | Fork `gitlab.com/fdroid/fdroiddata`, add `metadata/org.rafee_m.adguarddnsmanager.yml` (§3.5) | GitLab | no |
| 8 | Run `fdroid lint`, `rewritemeta`, `build`, `scanner` on it locally (§3.4) | own computer | no |
| 9 | Open the merge request; answer the reviewers (name, anti-feature, sign-in, screenshots) | GitLab | no |
| 10 | After merge: every release = `versionCode` + 1, changelog file, tag (§3.6); F-Droid's bot picks it up | this repo | — |

## 1. How F-Droid works, in 6 lines

1. You do not upload an APK. You send a **recipe** (a YAML file) to `gitlab.com/fdroid/fdroiddata` as a merge request.
2. F-Droid's server **builds the app from your public source** at the tag the recipe names, using only free tools.
3. Either F-Droid signs the APK with its own key, **or** (reproducible builds) it checks that its build equals the APK
   you signed and publishes **yours** (`Binaries:` + `AllowedAPKSigningKeys:`).
4. Store text and pictures are read from your repository (`fastlane/metadata/android/<locale>/`).
5. A new tag with a higher `versionCode` is found automatically (`UpdateCheckMode: Tags`).
6. Reviewers read the merge request by hand: names, licences, trademarks, anti-features.

## 2. Checklist: what is already right

| Requirement (F-Droid source) | This app | Evidence |
|---|---|---|
| FLOSS licence | GPL-3.0 (`LICENSE`); recipe field `License: GPL-3.0-only` | LICENSE present; descends from GPL-3.0 code (NextDNS Manager+, LLM Central), so `-only` is the safe label |
| Dependencies free, from a trusted Maven repo | one library: `androidx.webkit:webkit:1.17.1` (Apache-2.0), from Google's Maven | `app/build.gradle`, `settings.gradle` (`google()`, `mavenCentral()` only; `FAIL_ON_PROJECT_REPOS`) |
| No tracking SDKs ("strictly forbidden") | none: no Firebase/Sentry/analytics; WebView's own metrics switched off. Apple/Google/Facebook sign-in (Phase 5) runs on their **websites** inside the WebView: no SDK, no Google Play Services | `AndroidManifest.xml` has `WebView.MetricsOptOut`; 3 permissions |
| A derived app needs a **new application id** and "name, icon and string changes" | id `org.rafee_m.adguarddnsmanager` (≠ NextDNS Manager+'s `org.rafee_m.nextdnsmanagerplus`), name "AdGuardDNS Manager", own icon (`artwork/icon.svg`, 2026-11-20) | `aapt dump badging` of the built APKs (CHANGES.md 2026-11-20) |
| "A website wrapper should provide native features" | native top bar, several isolated accounts, app lock, status icon, find in page, downloads of files the page makes | Features in `PROJECT.md` §5 |
| Build from source with open tools | Gradle wrapper pinned by SHA-256, AGP 8.13.2, Gradle 8.14.5, SDK 36 | `gradle/wrapper/gradle-wrapper.properties` |
| Reproducible build | two fresh clones of commit `1fa8540` at different paths gave the **same** SHA-256 `bb0699c7…0c1c5a` (JDK 21) | §5 |
| `versionName`/`versionCode` readable | literal values in `app/build.gradle` (`0.1.0` / `1`) | the recipe's auto-update reads them from the tagged revision |
| No signing secrets in the tree | release signing only if `local.properties` has `signing.*` (git-ignored); the committed debug key is a debug key | `app/build.gradle` |
| Backup does not copy logins | `app_webview/` excluded in both backup rule files | `res/xml/*backup*.xml` |
| CI builds the release on every push | `ci.yml` job `android` passed for the import (`abb10bf`) | GitHub Actions |

## 3. What is still missing (in order)

### 3.1 Store texts and pictures (blocks the listing)

Only `images/icon.png` (512 × 512) exists, and `changelogs/1.txt` still describes NextDNS Manager+ (Phase 8). F-Droid
reads this layout:

```
fastlane/metadata/android/en-US/
├── title.txt               ≤ 50 characters          "AdGuardDNS Manager"   (see 3.2)
├── short_description.txt   ≤ 80 characters, mandatory
├── full_description.txt    ≤ 4000 characters, mandatory
├── changelogs/1.txt        file name = versionCode, ≤ 500 characters
└── images/
    ├── icon.png            done
    └── phoneScreenshots/1.png, 2.png …   (PNG or JPEG; none exist yet)
```

Put in the full description what F-Droid reviewers look for:
- it is an **unofficial** client for a proprietary service (AdGuard DNS) and needs an AdGuard account;
- it sends nothing anywhere except to AdGuard's addresses, plus the sign-in pages of Apple, Google or Facebook when the
  user chooses them; the one automatic request (AdGuard's DNS check, for the status icon, only while the app is on
  screen) carries no account data;
- **Google's sign-in page** is shown with a desktop user agent, and only that page, because Google refuses sign-in in
  embedded browsers (plan §11: describe it honestly);
- several accounts need Android System WebView 119+.

### 3.2 The name (trademark)

F-Droid's policy: apps "need to obtain necessary trademark and copyright permissions". **"AdGuard" is AdGuard's
trademark**, and AdGuard itself publishes apps named "AdGuard …", so **"AdGuardDNS Manager" can be read as an official
AdGuard app** — a stronger concern than "NextDNS Manager+" had. The owner chose the name on 2026-11-20. Ways to answer a
reviewer (or AdGuard), in order of effort:

1. Keep the name; make "unofficial, not affiliated with AdGuard" prominent in the store texts (About, README and the
   Settings disclaimer already say "not affiliated").
2. Ask AdGuard for permission to use the name.
3. A neutral name with the service in the description, e.g. "DNS Manager for AdGuard DNS".

The applicationId and the code package are not affected by this choice; **decide before the first public release**
(changing the display name later is possible, the id is not). Whether an app with a similar name is already listed on
F-Droid: **UNVERIFIED**.

### 3.3 Signing key and release workflow (blocks reproducible publishing)

Inherited from NextDNS Manager+: `.github/workflows/release.yml` and [`RELEASE.md`](RELEASE.md) (§3–4). What they do:

1. Create a release key once (`keytool -genkeypair … -keyalg RSA -keysize 4096 -validity 10000`); **a different key from
   NextDNS Manager+'s**; back it up; keep it **outside** the repository.
2. GitHub secrets: keystore (base64), keystore password, key alias, key password.
3. Workflow on a `v*` tag: `./gradlew assembleRelease` (unsigned), sign **with `apksigner` from build-tools 34.0.0 and
   `--v1-signing-enabled false`**, run `apksigcopier compare` (1.1.1), publish the APK on the GitHub release, print the
   signer's SHA-256 (needed in 3.5). **Phase 9** renames the asset to `AdGuardDNS-Manager-<version>.apk` (today
   `NextDNS-Manager-Plus-…`) and re-tests the scripts on this app's APK.
   - Tested on NextDNS Manager+'s APK (2026-11-19): 34 + v1 off → `apksigcopier compare` passes; 34 with default
     options → fails ("CHUNKED_SHA512 digest mismatch"); 35 + v1 off → fails.
4. **Do not pass `-PrepoUrl` in the release build.** F-Droid builds the tag without it, so the About links come from
   `gradle.properties`. A release built with any other value is a different APK and F-Droid's check fails.

### 3.4 Run F-Droid's own tools locally (finds surprises before a human does)

```bash
python3 -m venv ~/fdroid-venv && . ~/fdroid-venv/bin/activate
pip install fdroidserver
# in a clone of gitlab.com/fdroid/fdroiddata, with metadata/org.rafee_m.adguarddnsmanager.yml written (3.5):
fdroid readmeta
fdroid rewritemeta org.rafee_m.adguarddnsmanager
fdroid lint org.rafee_m.adguarddnsmanager
fdroid build -v -l org.rafee_m.adguarddnsmanager        # F-Droid's own build of the tagged source
fdroid scanner org.rafee_m.adguarddnsmanager            # looks for binaries / non-free or tracking libraries
```

Things the scanner/reviewer may touch in this tree (none verified for this app yet):
- `gradle/wrapper/gradle-wrapper.jar` (a committed binary). LLM Central reports that F-Droid's clean-up removes the
  signing block from `build.gradle` and the Gradle wrapper, and that its build of the cleaned tree still matched; this
  app's `build.gradle` has the same signing shape.
- `app/debug.keystore` (committed on purpose, password `android`, so debug builds from different computers install over
  each other). The release build never reads it. If F-Droid objects, remove it and let CI create the debug key.

### 3.5 The recipe (draft; **UNVERIFIED**: validate with `fdroid lint` and `fdroid build`)

File `metadata/org.rafee_m.adguarddnsmanager.yml` in your fork of `fdroiddata`:

```yaml
Categories:
  - Connectivity            # pick from F-Droid's current category list (UNVERIFIED that this name is on it)
License: GPL-3.0-only
AuthorName: Rafee-M
WebSite: https://github.com/UpperPanda1/adguard-manager
SourceCode: https://github.com/UpperPanda1/adguard-manager
IssueTracker: https://github.com/UpperPanda1/adguard-manager/issues

AntiFeatures:
  NonFreeNet:
    en-US: Only works with the proprietary AdGuard DNS service (an AdGuard account is required).

RepoType: git
Repo: https://github.com/UpperPanda1/adguard-manager.git

Binaries: https://github.com/UpperPanda1/adguard-manager/releases/download/v%v/AdGuardDNS-Manager-%v.apk
AllowedAPKSigningKeys: <sha256 of YOUR release certificate, lower case, no colons, from the release run's summary>

Builds:
  - versionName: 0.1.0
    versionCode: 1
    commit: v0.1.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.1.0
CurrentVersionCode: 1
```

Notes on the choices:
- **`NonFreeNet`** is F-Droid's label for apps that "promote or depend entirely on a proprietary network service". It is
  a label shown to users, not a rejection. Anti-features must be declared; an undeclared one gets a rejection.
- **Not** `Tracking`: the app reports nothing; the status request goes to the service the user already chose. The
  sign-in pages are the providers' own websites, opened only when the user picks them.
- **`Binaries` + `AllowedAPKSigningKeys`** = the reproducible-build route: F-Droid builds, compares with your signed
  APK (signature copying), and publishes yours. The `Binaries` URL pattern must match the release asset's name exactly
  (Phase 9 sets it to `AdGuardDNS-Manager-<version>.apk`).
- **The repository address appears five times** (`WebSite`, `SourceCode`, `IssueTracker`, `Repo`, `Binaries`): if the
  repository moves, change them in the recipe **and** `app.repoUrl` in `gradle.properties` in the same release.
- `subdir: app` because the Gradle module is `app`. The F-Droid build must not need `local.properties`; it does not.

### 3.6 Release routine (every version)

1. Raise `versionCode` by 1 and set `versionName` in `app/build.gradle` (literal numbers, no computing).
2. Write `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (≤ 500 characters).
3. `CHANGES.md` entry; CI green.
4. Check `app.repoUrl` in `gradle.properties` is the repository's current address (§4).
5. Tag `v<versionName>` on a **clean** commit and push it; the release workflow builds, signs, checks, publishes.
6. F-Droid's bot finds the tag (`UpdateCheckMode: Tags`) and proposes the update; usually no human step.

## 4. The repository address inside the APK

The About screen's GitHub links, the repository name and "Developed by" are **not** written in the page: they are read
when the app is built.

| What | Where | Rule |
|---|---|---|
| `app.repoUrl`, `app.author` | `gradle.properties` (Windows line endings) | the single source of truth: `https://github.com/UpperPanda1/adguard-manager`, `Rafee-M` |
| local override | `-PrepoUrl=https://github.com/<owner>/<repo>` | test builds only; **never** for a release (below) |
| validation | `app/build.gradle` | build **fails** unless the address is `https://github.com/<owner>/<repo>` (a trailing `/` or `.git` is removed); author: letters, digits, `_ . -` and spaces, 1–60 characters |
| use | `BuildConfig.REPO_URL` / `AUTHOR` → `AboutWindow.infoJson` → `assets/about.html` | the page re-checks the shape before using it for links, and inserts text with `textContent` |
| release check | `release.yml` | a real release stops if `app.repoUrl` isn't the repository it runs in |

**Why not `git remote get-url origin`:** AGP already embeds the commit hash (`META-INF/version-control-info.textproto`),
and a clone's remote differs between computers (HTTPS vs SSH, forks, local paths), which would make the same commit
build to different APKs.

**Why never `-PrepoUrl` for a release:** proven for NextDNS Manager+ (2026-11-19): the same commit built with another
address gave a different SHA-256. F-Droid builds without the flag, so the two would not match.

**When the repository moves:** edit `app.repoUrl` (one line), commit, tag. Other places that name the repository are
static text: `README.md`, `about.html` "Built on" links (NextDNS Manager+, LLM Central, NextDNS Manager), the recipe in
3.5. The check accepts **GitHub only**.

## 5. Reproducibility: what was proven and what was not

**This app, 2026-11-20** (JDK 21, Gradle 8.14.5, AGP 8.13.2, the Claude sandbox): commit **`1fa8540`** cloned fresh from
GitHub into two directories at different paths, `./gradlew assembleRelease` in each:

```
bb0699c71c709a65cd1d4dc909446247cb648f19897acda1e0689738710c1c5a   clone A  scratchpad/repro/a
bb0699c71c709a65cd1d4dc909446247cb648f19897acda1e0689738710c1c5a   clone B  scratchpad/repro/deeper/path/b
```

(`cmp`: identical.) The only VCS data in the APK is the commit hash, so **every new commit changes the APK**: F-Droid
must build exactly the tagged commit. A build from a **dirty** tree is not comparable.

**JDK 17 vs 21:** proven identical for NextDNS Manager+ (`fb2a583`, formerly `cc867de`, 2026-11-19). **Not repeated for this app**: only
JDK 21 is installed in the sandbox.

**Not proven yet:** a different computer/OS; F-Droid's own build server result (`fdroid build`); that the *signed* APK
of this app matches F-Droid's build under `apksigcopier compare` (Phase 9 rehearsal, then the first real run).

Recipe to repeat: `git clone <repo> a && git clone <repo> b && (cd a && ./gradlew --no-daemon assembleRelease) &&
(cd b && ./gradlew --no-daemon assembleRelease) && cmp a/app/build/outputs/apk/release/app-release-unsigned.apk
b/app/build/outputs/apk/release/app-release-unsigned.apk` (no output from `cmp` = identical). Each clone needs a
`local.properties` with `sdk.dir=…` (or `ANDROID_HOME` set).

## 6. CI facts that matter for a release workflow

- `ubuntu-latest` is **Ubuntu 26.04**. Playwright 1.56.1 refuses it, so the `pages` and `icon` jobs are pinned to
  `ubuntu-24.04`; `android` runs on `ubuntu-latest`. A release workflow that only runs Gradle and `apksigner` can use
  `ubuntu-latest`; anything with Playwright must stay on 24.04 until Playwright supports 26.04.
- CI run 1 of this repository (the import, 2026-11-19) passed both jobs with no `##[warning]` lines.

## 7. Risks, ranked

| # | Risk | Why it matters | Action |
|---|---|---|---|
| 1 | Name/trademark ("AdGuardDNS Manager" may look official) | reviewers or AdGuard can ask | §3.2 before the first release |
| 2 | Sign-in user agent on Google's page | reviewers may ask why the app changes its identity | honest description (3.1); only that page, only while shown (CLAUDE.md §6) |
| 3 | No phone test yet | F-Droid does not test, but a broken first release hurts users | owner's phone checklists in `CHANGES.md` |
| 4 | `-PrepoUrl` used in a release build | F-Droid's build would differ → not published | 3.3 step 4; the release check (§4) |
| 5 | Release workflow not yet run here; names still NextDNS | runner differences; `Binaries` mismatch | Phase 9 rehearsal, then a dry run with the test key |
| 6 | Another OS / F-Droid's build server / JDK 17 | not yet compared for this app | `fdroid build` in §3.4 |
| 7 | Committed binaries (wrapper jar, debug keystore) | scanner findings | run `fdroid scanner`; drop the debug keystore if asked |
| 8 | App only useful with an AdGuard account | label `NonFreeNet`; reviewers may ask for screenshots | screenshots + honest description |

## 8. Sources

- Inclusion Policy: https://f-droid.org/docs/Inclusion_Policy/
- Anti-Features: https://f-droid.org/docs/Anti-Features/
- Reproducible Builds: https://f-droid.org/docs/Reproducible_Builds/
- Build Metadata Reference: https://f-droid.org/docs/Build_Metadata_Reference/
- Descriptions, graphics, screenshots: https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/
  (read for NextDNS Manager+ on 2026-11-19; not re-read for this file)
- NextDNS Manager+'s `fdroid.md` and `RELEASE.md` at `eaa1665` (the tested signing table and reproducibility history)
