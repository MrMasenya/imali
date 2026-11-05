# iMali installable APK

| File | Description |
|------|-------------|
| `imali-debug.apk` | Debug build for PoE testing on a physical device |

Install: copy to your phone → open file → allow unknown sources if prompted.

**Before use:** set your Render API URL in the app build (`ApiClient.BASE_URL`) or install after configuring Atlas + Render (see `docs/ATLAS_RENDER_SETUP.md`).

Rebuild locally:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
copy app\build\outputs\apk\debug\app-debug.apk apk\imali-debug.apk
```
