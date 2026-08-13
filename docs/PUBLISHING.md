# Publishing to Google Play — step by step

Everything on the **code side is done** (release build, R8, signing hook,
branded icon, splash, privacy policy, store copy, tester instructions). This is
the checklist for the parts only you can do — account, keys, and the Play
Console. Work through it top to bottom.

> Play Console's UI wording moves around occasionally; section **names** below
> are the stable landmarks even if menus shift.

---

## 0. Prerequisites
- [ ] A **Google account** for the developer identity.
- [ ] **Google Play Console** developer account — one-time **$25** at
  <https://play.google.com/console>. Identity/D-U-N-S verification can take 1–3
  days, so start this first.

---

## 1. Create your upload key (do once, keep forever)
The upload key signs every build you upload. **If you lose it and its passwords,
you can't update the app** — store them in a password manager and back up the
`.jks` file somewhere safe.

```bash
cd /path/to/NetworthCalculator
keytool -genkey -v -keystore upload-keystore.jks -alias upload \
        -keyalg RSA -keysize 2048 -validity 10000
```
It asks for a keystore password, your name/org (any values are fine), and a key
password (you can reuse the keystore password).

Then create the gitignored properties file the build reads:
```bash
cp keystore.properties.template keystore.properties
```
Edit `keystore.properties` with your real values:
```
storeFile=upload-keystore.jks
storePassword=<your store password>
keyAlias=upload
keyPassword=<your key password>
```

---

## 2. Build the signed App Bundle
Play requires an **`.aab`** (App Bundle), not an APK.
```bash
./gradlew :app:bundleRelease
```
Output: **`app/build/outputs/bundle/release/app-release.aab`** — this is what you upload.

> Sanity check the release build once on a device (install via
> `./gradlew :app:installRelease` or bundletool) — especially **Google sign-in +
> sync**, since those depend on the release signature.

---

## 3. Create the app in Play Console
Play Console → **Create app**:
- **App name:** `Net Worth Calculator`
- **Default language:** English (India) or English (US)
- **App or game:** App · **Free or paid:** Free
- Accept the developer program & US export declarations.

---

## 4. App content & policy declarations
Play Console → **Policy → App content** (complete every item; the app must have
zero "action required" here before it can go live):

- [ ] **Privacy policy** → paste:
  `https://shadabshaikh0.github.io/Networth-Calculator-app/privacy-policy.html`
- [ ] **App access** → "All functionality is available without special access."
  (Google sign-in is optional; the app works fully offline, so no test login is
  required for review.)
- [ ] **Ads** → No, the app has no ads.
- [ ] **Content rating** → fill the questionnaire (Finance, no objectionable
  content) → you'll get an "Everyone" rating.
- [ ] **Target audience & content** → 18+ (or 13+); **not** designed for children.
- [ ] **Data safety** → declare:
  - Collected/shared by developer: **None** (no backend, no analytics).
  - Data handled on-device / in the user's own Google Drive: **Financial info**
    (assets, liabilities, net worth). Not shared. Optional. User can delete it.
  - Encrypted in transit: Yes (HTTPS to Google APIs). User can request deletion:
    Yes (in-app + delete the Drive sheet).
- [ ] **Financial features** → declare it's a **personal finance / net-worth
  tracker**; it is **not** a lending, banking, crypto, or regulated financial
  product.
- [ ] **Government apps / News** → No.

---

## 5. Main store listing
Play Console → **Grow → Store presence → Main store listing**. Copy from
[`STORE_LISTING.md`](STORE_LISTING.md):
- [ ] **App name** (≤30), **Short description** (≤80), **Full description** (≤4000)
- [ ] **App icon** — 512×512 PNG (see §9)
- [ ] **Feature graphic** — 1024×500 PNG (see §9)
- [ ] **Phone screenshots** — 2 to 8, from [`../screenshots`](../screenshots)
- [ ] **Category:** Finance · **Contact email:** `sshadabshaikh7703@gmail.com`

---

## 6. First upload + Play App Signing
Play Console → **Test and release → Testing → Internal testing → Create new
release**:
- [ ] Keep **Play App Signing** enabled (default) — Google holds the real app
  signing key; you keep uploading with your upload key.
- [ ] **Upload** `app-release.aab`.
- [ ] Add **release notes** (e.g. "First release — track your net worth,
  offline-first, with optional Google Sheets sync.").
- [ ] Save / review.

---

## 7. ⚠️ Wire up Google sign-in for the released build (critical)
The Play-signed build uses a **different signature** than your local debug/upload
key, so you must register its SHA-1 or sign-in fails with `DEVELOPER_ERROR`.

1. Play Console → **Test and release → Setup → App integrity → App signing** →
   copy **both** SHA-1 fingerprints:
   - **App signing key certificate** SHA-1
   - **Upload key certificate** SHA-1
2. Google Cloud Console → **APIs & Services → Credentials** → open your **Android
   OAuth client** (package `com.shadabshaikh.networth`) → **add both** SHA-1
   fingerprints → Save.

> You can keep your existing debug SHA-1 there too — multiple fingerprints are
> allowed on one Android OAuth client.

---

## 8. Testing → Production
- [ ] **Internal testing** (up to 100 testers, available in minutes) — add your
  own email, install from the opt-in link, and smoke-test the released build
  end-to-end (add items, sign in, confirm the Drive sheet + sync).
- [ ] **Closed testing** — add testers (email list or a Google Group) and share
  the opt-in link plus [`TESTERS.md`](TESTERS.md).
- [ ] **New personal accounts:** you must run a **closed test with ~12+ testers
  for 14 continuous days** before you can request production. Check the exact
  current requirement shown in your Console.
- [ ] After that window, **Apply for production access** (Console prompts you).
- [ ] **Production → Create release** → upload/promote the `.aab` → roll out.
  Review typically takes a few days (finance apps can take longer).

---

## 9. The two graphics you still need
Binary images can't be generated from code — make these once:
- **App icon (512×512 PNG):** reuse the in-app icon look — a gold `#D5B475`
  square with a dark `₹`. In Android Studio: right-click `res` → **New → Image
  Asset → Launcher Icons**, or export the current `ic_launcher` at 512².
- **Feature graphic (1024×500 PNG):** a simple gold banner with the `₹` mark and
  "Net Worth Calculator" (Canva/Figma, ~5 min).

---

## 10. Later — updates & full public launch
- **Updates:** bump `versionCode` (and `versionName`) in `app/build.gradle.kts`,
  run `./gradlew :app:bundleRelease`, upload the new `.aab`.
- **OAuth verification:** the `drive.file` scope is "sensitive." Test users work
  without verification, but to remove the "Google hasn't verified this app"
  screen and go beyond 100 users, submit the app for **OAuth verification** in
  Google Cloud (app info, scope justification, a demo video, and the privacy
  policy URL).

---

### Quick reference
| Item | Value |
|---|---|
| Package | `com.shadabshaikh.networth` |
| Privacy policy URL | `https://shadabshaikh0.github.io/Networth-Calculator-app/privacy-policy.html` |
| Build the bundle | `./gradlew :app:bundleRelease` |
| Bundle path | `app/build/outputs/bundle/release/app-release.aab` |
| Contact email | `sshadabshaikh7703@gmail.com` |
| OAuth scope | `drive.file` (+ profile/email for the account chip) |
