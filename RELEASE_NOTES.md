# iMali Release Notes (Prototype → Final PoE)

## Version 2.0.0 — Final PoE

### Added since Part 2 prototype

- **Google Single Sign-On (SSO)** — Credential Manager + server-side ID token verification (`POST /api/auth/sso`).
- **Offline mode with sync** — Room database stores transactions immediately; WorkManager uploads pending rows when the network returns.
- **Firebase Cloud Messaging** — device token registration; server sends budget (≥80%) and stokvel contribution alerts when Firebase Admin is configured.
- **Multi-language UI** — English, isiZulu (`values-zu`), and Afrikaans (`values-af`); switch in Settings (runtime locale recreate).
- **Blob storage** — receipt/file upload to MongoDB via `POST /api/blobs` (multer + binary schema).
- **Stokvel mode (User-defined 4)** — create groups, record contributions, view totals per member.
- **Gamification (User-defined 5)** — daily capture streaks and `week_streak` badge on the API.
- **Reports** — Compose bar chart of spending by category; colour-coded budget progress (green/amber/red).
- **Play Store readiness** — version 2.0.0, app icon asset, signing instructions in `docs/PLAY_STORE.md`.

### Unchanged from Part 2 core

- Email/password registration and login (bcrypt on server, JWT in EncryptedSharedPreferences).
- Settings synced to API (name, currency, theme, notifications, language).
- REST API on Render + MongoDB Atlas for auth, transactions, and budgets.
- GitHub Actions build + unit tests (`ValidatorsTest`, `FinanceTest`).

### Demo video

Add your unlisted YouTube link in `README.md` after recording (see `docs/VIDEO_SCRIPT.md`).
