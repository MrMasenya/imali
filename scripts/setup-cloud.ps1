# Interactive helper — run in PowerShell after creating Atlas / Render / Firebase accounts.
Write-Host "=== iMali cloud setup ===" -ForegroundColor Green
Write-Host "1. Create api/.env from api/.env.example"
Write-Host "2. Set MONGO_URI, JWT_SECRET, GOOGLE_CLIENT_ID"
Write-Host "3. Deploy api/ on Render (root directory: api, start: npm start)"
Write-Host "4. Download google-services.json into app/"
Write-Host "5. Set google_web_client_id in app/src/main/res/values/strings.xml"
Write-Host "6. Set ApiClient.BASE_URL to your Render URL"
Write-Host ""
if (-not (Test-Path "api\.env")) {
    Copy-Item "api\.env.example" "api\.env"
    Write-Host "Created api/.env — edit before npm start" -ForegroundColor Yellow
}
