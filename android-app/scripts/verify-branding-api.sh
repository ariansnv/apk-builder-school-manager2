#!/usr/bin/env bash
# Verify School Manager branding API before APK build.
set -euo pipefail

URL="${1:-}"
if [ -z "$URL" ]; then
  echo "Usage: verify-branding-api.sh <branding_api_url>"
  exit 1
fi

if [[ ! "$URL" =~ ^https:// ]]; then
  echo "::error::branding_url must start with https:// (got: $URL)"
  exit 1
fi

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

HTTP_CODE="$(curl -fsSL -o "$TMP" -w '%{http_code}' "$URL" || true)"
if [ "$HTTP_CODE" != "200" ]; then
  echo "::error::Branding API returned HTTP $HTTP_CODE"
  echo "URL: $URL"
  exit 1
fi

python3 - "$TMP" <<'PY'
import json, sys
path = sys.argv[1]
data = json.load(open(path, encoding="utf-8"))
if data.get("ok") is False:
    raise SystemExit("API returned ok=false")
name = (data.get("app_name") or "").strip()
start = (data.get("start_url") or "").strip()
if not name:
    raise SystemExit("app_name is empty")
if "example.com" in start:
    raise SystemExit("start_url still points to example.com — save mobile app settings first")
if not start.startswith("https://"):
    print("::warning::start_url is not HTTPS")
logo = (data.get("logo_url") or data.get("icon_url") or "").strip()
if logo and not logo.startswith("https://"):
    raise SystemExit(f"logo_url must be absolute HTTPS for GitHub build (got: {logo!r})")
print("Branding API OK")
print(f"  app_name     = {name}")
print(f"  start_url    = {start}")
print(f"  version_code = {data.get('version_code')}")
print(f"  app_id       = {data.get('application_id')}")
print(f"  logo_url     = {logo or '(none)'}")
PY
