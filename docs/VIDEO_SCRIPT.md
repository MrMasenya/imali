# Demonstration video script (4–6 minutes, physical phone, voice-over)

Record in landscape with clear audio. Show **MongoDB Atlas** collections and **Render** logs where noted.

1. **Intro** — App name iMali, purpose (offline-first budgeting for SA users).
2. **Register** — New user; show bcrypt-backed account in Atlas `users` collection.
3. **Login** — Email/password; invalid password handled without crash.
4. **Google SSO** — Continue with Google; show linked user in Atlas.
5. **Settings** — Change name, currency, dark theme, notifications; switch language (English → isiZulu → Afrikaans).
6. **Add transaction (online)** — Expense; show document in `transactions` collection.
7. **Offline mode** — Enable airplane mode; add transaction; show Room pending state; disable airplane mode; show sync in Render logs.
8. **Budgets** — Set Food budget; add expenses until ≥80%; show FCM notification (if Firebase configured).
9. **Reports** — Category chart and colour-coded budget bars.
10. **Stokvel** — Create group, add contribution; show `stokvels` in Atlas.
11. **Gamification** — Show streak and badge after daily capture.
12. **GitHub** — Briefly show repo, Actions green check, README + video link.

Upload to YouTube as **Unlisted** and paste the link into `README.md`.
