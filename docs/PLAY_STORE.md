# Google Play Store preparation checklist

1. **Signing** — Android Studio → Build → Generate Signed App Bundle/APK → create keystore (keep `.jks` out of Git).
2. **Version** — `app/build.gradle.kts` uses `versionCode = 2`, `versionName = "2.0.0"`.
3. **Icon** — Replace `app/src/main/res/drawable/ic_launcher_foreground.xml` with your final emerald iMali icon (or add adaptive mipmap assets).
4. **Screenshots** — Capture dashboard, add transaction, budgets/reports, stokvel, settings (phone resolution).
5. **Play Console** — Create app listing, upload AAB, complete content rating and privacy policy (mention MongoDB Atlas + Firebase).
6. **Evidence for PoE** — Screenshot of Play Console draft listing or signed APK build output in `docs/screenshots/`.
