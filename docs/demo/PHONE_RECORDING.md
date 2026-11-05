# Required: physical phone demo (rubric)

The narrated slideshow in `imali-poe-narrated-walkthrough.mp4` is **supplementary**. Lecturers expect:

1. **Real device** screen recording (not emulator-only).
2. **Your voice** explaining each feature.
3. **MongoDB Atlas** and **Render** visible in the recording.

## Quick record steps (Android)

1. Enable Developer options → USB debugging; install the debug APK from Android Studio.
2. Settings → search **Screen recorder** (Samsung/Xiaomi/Pixel) or use **AZ Recorder** / **Mobizen**.
3. Follow `../VIDEO_SCRIPT.md` scene order; speak clearly in landscape.
4. Upload to YouTube **Unlisted**; paste URL into root `README.md`.

## After recording

```bash
git add README.md
git commit -m "docs: add demonstration video link"
git tag final-poe
git push origin main --tags
```
