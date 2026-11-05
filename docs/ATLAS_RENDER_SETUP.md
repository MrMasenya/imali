# MongoDB Atlas + Render setup (MrMasenya/imali)

Follow in order. Keep passwords in `api/.env` only — never commit that file.

---

## Part A — MongoDB Atlas (~15 min)

1. Open [https://www.mongodb.com/cloud/atlas/register](https://www.mongodb.com/cloud/atlas/register) and sign in.
2. **Create** → **Deployment** → **M0 FREE** → choose a region close to you → Create.
3. **Security → Database Access** → Add user:
   - Username: `imali_user`
   - Password: generate strong password → **copy and save** (e.g. in Notepad).
   - Built-in role: **Atlas admin** (or **Read and write to any database** on M0).
4. **Security → Network Access** → **Add IP Address** → **Allow Access from Anywhere** (`0.0.0.0/0`) → Confirm.  
   (Required so Render and your phone can reach the cluster.)
5. **Deployment → Database** → **Connect** → **Drivers** → copy the connection string.  
   Example: `mongodb+srv://imali_user:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority`
6. Edit the string:
   - Replace `<password>` with your real password (URL-encode special chars like `@` → `%40`).
   - Add database name before `?`: `...mongodb.net/imali?retryWrites=...`

**Your `MONGO_URI` for `.env` and Render:**

```env
MONGO_URI=mongodb+srv://imali_user:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/imali?retryWrites=true&w=majority
```

---

## Part B — Test API locally (~10 min)

In PowerShell:

```powershell
cd C:\Users\Student\imali\api
Copy-Item .env.example .env
# Edit .env: paste MONGO_URI, set JWT_SECRET to a long random string (32+ chars)
notepad .env
npm install
npm start
```

You should see: `iMali API listening on port 4000`

Test in another terminal (replace password/email as needed):

```powershell
# Register
Invoke-RestMethod -Method Post -Uri http://localhost:4000/api/auth/register -ContentType "application/json" -Body '{"name":"Makwena","email":"test@example.com","password":"secret123"}'

# Login (copy token from response)
Invoke-RestMethod -Method Post -Uri http://localhost:4000/api/auth/login -ContentType "application/json" -Body '{"email":"test@example.com","password":"secret123"}'
```

Check Atlas → **Browse Collections** → database `imali` → collection `users` should appear.

---

## Part C — Push code to GitHub

```powershell
cd C:\Users\Student\imali
git config user.email "YOUR_EMAIL"
git config user.name "MrMasenya"
git remote add origin https://github.com/MrMasenya/imali.git
git add -A
git commit -m "feat: iMali final PoE - Android app, API, CI, docs"
git branch -M main
git push -u origin main
```

If the remote repo already has a README, use `git pull origin main --rebase` first or force only if you intend to replace it.

---

## Part D — Render (~15 min)

1. [https://dashboard.render.com](https://dashboard.render.com) → Sign up **with GitHub**.
2. **New +** → **Web Service** → connect **MrMasenya/imali**.
3. Settings:
   | Field | Value |
   |-------|--------|
   | Name | `imali-api` |
   | Root Directory | `api` |
   | Runtime | Node |
   | Build Command | `npm install` |
   | Start Command | `npm start` |
   | Instance type | Free |
4. **Environment** → Add variables:
   - `MONGO_URI` = same as local `.env`
   - `JWT_SECRET` = same as local (or new long secret)
   - `PORT` = `4000` (Render often sets `PORT` automatically — keep `npm start` using `process.env.PORT`)
5. **Create Web Service** → wait until **Live** (free tier may sleep after idle).
6. Copy URL, e.g. `https://imali-api.onrender.com`

Test:

```powershell
Invoke-RestMethod https://imali-api.onrender.com/
```

---

## Part E — Point the Android app at Render

Edit `app/src/main/java/za/ac/imali/data/ApiClient.kt`:

```kotlin
const val BASE_URL = "https://imali-api.onrender.com/"
```

(trailing slash required)

Sync Gradle → run app on phone → register → confirm data in Atlas.

---

## Part F — Google SSO (after API works)

1. [Firebase Console](https://console.firebase.google.com) → Add project → Add Android app `za.ac.imali`.
2. Download `google-services.json` → replace `app/google-services.json`.
3. Authentication → Sign-in method → **Google** → enable.
4. Project settings → copy **Web client ID** → paste into:
   - `app/src/main/res/values/strings.xml` → `google_web_client_id`
   - Render env → `GOOGLE_CLIENT_ID` (same Web client ID)

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `DB connection failed` on Render | Wrong password in URI, IP not `0.0.0.0/0`, or typo in database name |
| App `Unable to resolve host` | Wrong `BASE_URL`, or Render service sleeping (wake with browser request) |
| 503 on `/api/auth/sso` | Set `GOOGLE_CLIENT_ID` on Render |
| First request slow | Normal on Render free tier (cold start ~30–60s) |

When Atlas and Render are live, tick items in `POE_SUBMISSION_CHECKLIST.md`.
