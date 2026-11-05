# Final PoE submission checklist (100 marks)

Use this list before submitting the GitHub link + video on the portal.

## Cloud (you must create these — not automatable)

- [ ] MongoDB Atlas M0 cluster + `imali_user` + network `0.0.0.0/0`
- [ ] Render Web Service (`api/`, `npm start`, `MONGO_URI`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`)
- [ ] Firebase project → `google-services.json` + Web OAuth client ID in `strings.xml`
- [ ] Optional: `FIREBASE_SERVICE_ACCOUNT` on Render for server push

## App

- [ ] `ApiClient.BASE_URL` = your Render URL (trailing `/`)
- [ ] App runs on **physical phone** without crashes
- [ ] Register, login, invalid input handled
- [ ] Google SSO works
- [ ] Settings + language switch (EN / zu / af)
- [ ] Offline add → online sync
- [ ] Budget alert / FCM (or show token in logs)
- [ ] Stokvel + streak/badge visible

## GitHub

- [ ] No zip upload — repo link only
- [ ] README personalised (badge URL, video link, screenshots)
- [ ] `RELEASE_NOTES.md`, `AI_USAGE.md`
- [ ] GitHub Actions green on latest commit
- [ ] Tag: `final-poe`
- [ ] 12+ meaningful commits

## Video

- [ ] 4–6 min, voice-over, landscape
- [ ] Shows app on phone + Atlas + Render
- [ ] YouTube unlisted link in README
- [ ] Supplementary: `docs/demo/imali-poe-narrated-walkthrough.mp4` (optional)

## Play Store evidence

- [ ] Signed APK/AAB + listing screenshots (`docs/PLAY_STORE.md`)
