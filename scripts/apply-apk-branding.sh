#!/usr/bin/env bash
set -euo pipefail

BRANDING_URL="${1:-}"
ROOT="${2:-.}"

if [ -z "$BRANDING_URL" ]; then
  echo "Usage: apply-apk-branding.sh <branding_api_url> [android_root]"
  exit 1
fi

python3 "$ROOT/scripts/apply_apk_branding.py" "$ROOT" "$BRANDING_URL"
