# Creates api/.env with a generated JWT_SECRET. You still paste MONGO_URI from Atlas.
$apiDir = Join-Path (Split-Path $PSScriptRoot -Parent) "api"
$envFile = Join-Path $apiDir ".env"
if (Test-Path $envFile) {
    Write-Host "api/.env already exists - not overwriting." -ForegroundColor Yellow
    exit 0
}
$jwt = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 48 | ForEach-Object { [char]$_ })
$lines = @(
    'MONGO_URI=mongodb+srv://imali_user:PASTE_PASSWORD@cluster0.xxxxx.mongodb.net/imali?retryWrites=true&w=majority'
    "JWT_SECRET=$jwt"
    'PORT=4000'
    'GOOGLE_CLIENT_ID=PASTE_WEB_CLIENT_ID.apps.googleusercontent.com'
)
$lines | Set-Content $envFile -Encoding UTF8
Write-Host "Created api/.env with generated JWT_SECRET." -ForegroundColor Green
Write-Host "Edit MONGO_URI after Atlas setup (see docs/ATLAS_RENDER_SETUP.md)."
