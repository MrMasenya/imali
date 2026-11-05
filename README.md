# iMali — OPSC6312 Portfolio of Evidence

[![Android Build and Unit Tests](https://github.com/MrMasenya/imali/actions/workflows/android-build-test.yml/badge.svg)](https://github.com/MrMasenya/imali/actions/workflows/android-build-test.yml)

**iMali** (isiZulu: *money*) is an Android personal finance app for South African users: offline-first expense capture, budgets, reports, stokvel groups, gamification, and a self-hosted REST API on **Render** with **MongoDB Atlas**.

| PoE phase | Status |
|-----------|--------|
| Part 1 — Research & design | See your submitted PDFs / Qwen draft |
| Part 2 — Prototype | Auth, settings, API, transactions, budgets, CI |
| Part 3 — Final PoE | SSO, offline sync, FCM, 3 languages, blob storage, stokvel, gamification |

**Demo videos**

| File | Purpose |
|------|---------|
| [`docs/demo/imali-poe-narrated-walkthrough.mp4`](docs/demo/imali-poe-narrated-walkthrough.mp4) | Generated narrated overview (56s) — **supplementary** |
| **YouTube (required for rubric)** | Record on your **physical phone** using [`docs/VIDEO_SCRIPT.md`](docs/VIDEO_SCRIPT.md) + [`docs/demo/PHONE_RECORDING.md`](docs/demo/PHONE_RECORDING.md), then paste the unlisted link here |

**Installable build:** [`apk/imali-debug.apk`](apk/imali-debug.apk) (also in [`docs/imali-debug.apk`](docs/imali-debug.apk)).

---

## Purpose and design

- **Offline-first** — Room stores transactions immediately; WorkManager syncs when online (load-shedding / data cost aware).
- **Security** — bcrypt password hashing on API; JWT in EncryptedSharedPreferences on device.
- **Innovation** — Stokvel mode, daily streaks/badges, trilingual UI (EN / zu / af).
- **Third-party libraries** — Retrofit ([Square](https://square.github.io/retrofit/)), Room & WorkManager ([Android Developers](https://developer.android.com)), Firebase Cloud Messaging ([Google](https://firebase.google.com/docs/cloud-messaging)).

See `RELEASE_NOTES.md` for changes since the Part 2 prototype.

---

## Repository layout

```
imali/
├── api/                    # Node.js Express API (deploy root on Render)
├── app/                    # Android (Kotlin + Jetpack Compose)
├── .github/workflows/      # GitHub Actions CI
├── docs/                   # Video script, Play Store checklist
├── AI_USAGE.md
└── RELEASE_NOTES.md
```

**Do not** put `api/` inside `app/` — Render expects the API at repo `api/` or a separate service root.

---

## Setup

**Atlas + Render not set up yet?** Follow **[docs/ATLAS_RENDER_SETUP.md](docs/ATLAS_RENDER_SETUP.md)** step by step (written for [github.com/MrMasenya/imali](https://github.com/MrMasenya/imali)).

Quick env file: `powershell -File scripts/init-env.ps1` then edit `MONGO_URI` in `api/.env`.

### 1. MongoDB Atlas

1. Free M0 cluster → Database Access user `imali_user` → Network Access `0.0.0.0/0`.
2. Copy connection string into `api/.env` as `MONGO_URI`.

### 2. API (local then Render)

```bash
cd api
cp .env.example .env   # fill MONGO_URI, JWT_SECRET, GOOGLE_CLIENT_ID
npm install
npm start
```

Test: `POST /api/auth/register` → `POST /api/auth/login` → `POST /api/transactions` with `Authorization: Bearer <token>`.

Deploy on Render: Web Service, root directory `api`, start command `npm start`, same env vars. Optional: `FIREBASE_SERVICE_ACCOUNT` JSON for push notifications.

### 3. Android Studio

1. Open the `imali` folder (Koala+ recommended). Use **JDK 17** (Gradle/AGP may fail on JDK 21+ in some environments).
2. Replace placeholders:
   - `app/google-services.json` — from Firebase console (package `za.ac.imali`).
   - `app/src/main/res/values/strings.xml` → `google_web_client_id` (OAuth Web client ID).
   - `app/.../ApiClient.kt` → `BASE_URL` = your Render URL with trailing `/`.
3. Sync Gradle → Run on a **physical phone** (required for final PoE video). Rebuild: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat assembleDebug`
4. Regenerate supplementary narrated video: `powershell -File scripts/build-demo-video.ps1`

### 4. GitHub

```bash
git init
git add -A
git commit -m "feat: iMali final PoE - Android app, API, CI"
git remote add origin https://github.com/MrMasenya/imali.git
git push -u origin main
```

Tag final submission: `git tag final-poe && git push origin final-poe`

---

## Testing

- **Unit tests:** `./gradlew testDebugUnitTest` (Validators, Finance helpers).
- **CI:** `.github/workflows/android-build-test.yml` builds debug APK and runs tests on push.

---

## API summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/auth/register` | Register (bcrypt) |
| POST | `/api/auth/login` | Login → JWT |
| POST | `/api/auth/sso` | Google ID token |
| GET/POST | `/api/transactions` | List / create (batch sync supported) |
| GET/POST | `/api/budgets` | Budgets |
| GET/POST | `/api/stokvels` | Stokvel groups |
| POST | `/api/stokvels/:id/contribute` | Contribution |
| POST | `/api/blobs` | Receipt blob storage (multipart) |
| PUT | `/api/users/me` | Settings |
| PUT | `/api/users/fcm-token` | FCM device token |

---

## References

- OPSC6312 PoE brief (2026).
- Android Developers — Room, WorkManager, Credential Manager.
- Render & MongoDB Atlas documentation.
- Part 1 research report (Wallet, Money Manager, Spendee).

---

## AI usage

See `AI_USAGE.md` (max 500 words for submission).
