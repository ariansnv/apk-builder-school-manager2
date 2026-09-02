#!/usr/bin/env python3
"""Apply School Manager APK branding from api/pwa/apk-branding JSON."""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
from urllib.parse import urljoin
from xml.sax.saxutils import escape

try:
    from PIL import Image
except ImportError:
    Image = None  # type: ignore


def absolutize(url: str, base: str) -> str:
    url = (url or "").strip()
    if not url:
        return ""
    if url.startswith("http://") or url.startswith("https://"):
        return url
    return urljoin(base if base.endswith("/") else base + "/", url.lstrip("/"))


def fetch_json(url: str) -> dict:
    req = urllib.request.Request(
        url,
        headers={"Accept": "application/json", "User-Agent": "SchoolManagerApkBuild/1.0"},
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            if resp.status != 200:
                raise RuntimeError(f"HTTP {resp.status}")
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        raise RuntimeError(f"HTTP {exc.code} from branding API") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Cannot reach branding API: {exc.reason}") from exc


def main() -> int:
    if len(sys.argv) < 3:
        print("Usage: apply_apk_branding.py <android_root> <branding_api_url>", file=sys.stderr)
        return 1

    root = sys.argv[1]
    source = sys.argv[2].strip()

    if not (source.startswith("http://") or source.startswith("https://")):
        print(
            "ERROR: branding_url must be a full URL starting with https:// "
            f"(got: {source!r})",
            file=sys.stderr,
        )
        print("Example: https://my.school.ir/public/api/pwa/apk-branding", file=sys.stderr)
        return 1

    try:
        payload = fetch_json(source)
    except RuntimeError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    if payload.get("ok") is False:
        print("ERROR: branding API returned ok=false", file=sys.stderr)
        return 1

    app_name = (payload.get("app_name") or "School Manager").strip()
    start_url = (payload.get("start_url") or "https://example.com/").strip()
    if not start_url.endswith("/"):
        start_url += "/"
    logo_url = absolutize(
        (payload.get("logo_url") or payload.get("icon_url") or "").strip(),
        start_url,
    )
    app_id = (payload.get("application_id") or "com.schoolmanager.app").strip()
    version_name = (payload.get("version_name") or "1.0.0").strip()
    version_code = int(payload.get("version_code") or 1)
    branding_hash = (payload.get("branding_hash") or "dev").strip()
    theme = (payload.get("theme_color") or "#2563EB").strip().lstrip("#")

    if "example.com" in start_url:
        print(
            "ERROR: start_url still points to example.com — fix settings in admin panel first.",
            file=sys.stderr,
        )
        return 1

    if not start_url.lower().startswith("https://"):
        print("WARNING: start_url does not use HTTPS — Play Protect may warn more.", file=sys.stderr)

    strings_path = os.path.join(root, "app/src/main/res/values/strings.xml")
    with open(strings_path, "w", encoding="utf-8") as f:
        f.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n")
        f.write(f"    <string name=\"app_name\">{escape(app_name)}</string>\n")
        f.write(f"    <string name=\"app_start_url\">{escape(start_url)}</string>\n")
        f.write(f"    <string name=\"app_branding_hash\">{escape(branding_hash)}</string>\n")
        f.write("</resources>\n")

    colors_path = os.path.join(root, "app/src/main/res/values/ic_launcher_colors.xml")
    with open(colors_path, "w", encoding="utf-8") as f:
        f.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n")
        f.write(f"    <color name=\"ic_launcher_background\">#{theme}</color>\n")
        f.write("</resources>\n")

    host = re.sub(r"^https?://", "", start_url).split("/")[0].split(":")[0]
    xml_dir = os.path.join(root, "app/src/main/res/xml")
    os.makedirs(xml_dir, exist_ok=True)
    cleartext_block = ""
    if start_url.lower().startswith("http://"):
        cleartext_block = f"""
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">{escape(host)}</domain>
    </domain-config>"""
    ns_path = os.path.join(xml_dir, "network_security_config.xml")
    with open(ns_path, "w", encoding="utf-8") as f:
        f.write(
            f"""<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>{cleartext_block}
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
"""
        )

    gradle_path = os.path.join(root, "app/build.gradle")
    if os.path.isfile(gradle_path):
        text = open(gradle_path, encoding="utf-8").read()
        text = re.sub(r'applicationId\s+"[^"]+"', f'applicationId "{app_id}"', text, count=1)
        text = re.sub(r'versionCode\s+\d+', f'versionCode {version_code}', text, count=1)
        text = re.sub(r'versionName\s+"[^"]+"', f'versionName "{version_name}"', text, count=1)
        with open(gradle_path, "w", encoding="utf-8") as f:
            f.write(text)

    logo_applied = False
    if logo_url:
        if Image is None:
            print(
                "ERROR: Pillow is required to apply logo icons. Run: pip install Pillow",
                file=sys.stderr,
            )
            return 1
        try:
            import io

            logo_req = urllib.request.Request(
                logo_url,
                headers={"User-Agent": "SchoolManagerApkBuild/1.0"},
            )
            raw = urllib.request.urlopen(logo_req, timeout=30).read()
            img = Image.open(io.BytesIO(raw)).convert("RGBA")
            sizes = {
                "mipmap-mdpi": 48,
                "mipmap-hdpi": 72,
                "mipmap-xhdpi": 96,
                "mipmap-xxhdpi": 144,
                "mipmap-xxxhdpi": 192,
            }
            for folder, size in sizes.items():
                out_dir = os.path.join(root, "app/src/main/res", folder)
                os.makedirs(out_dir, exist_ok=True)
                resized = img.resize((size, size), Image.LANCZOS)
                resized.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")
                resized.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")
            fg_path = os.path.join(root, "app/src/main/res/drawable/ic_launcher_foreground.png")
            os.makedirs(os.path.dirname(fg_path), exist_ok=True)
            img.resize((108, 108), Image.LANCZOS).save(fg_path, "PNG")
            fg_xml = os.path.join(root, "app/src/main/res/drawable/ic_launcher_foreground.xml")
            if os.path.isfile(fg_xml):
                os.remove(fg_xml)
            anydpi = os.path.join(root, "app/src/main/res/mipmap-anydpi-v26")
            os.makedirs(anydpi, exist_ok=True)
            adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
            with open(os.path.join(anydpi, "ic_launcher.xml"), "w", encoding="utf-8") as f:
                f.write(adaptive)
            with open(os.path.join(anydpi, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
                f.write(adaptive)

            manifest_path = os.path.join(root, "app/src/main/AndroidManifest.xml")
            if os.path.isfile(manifest_path):
                text = open(manifest_path, encoding="utf-8").read()
                text = text.replace(
                    'android:icon="@drawable/ic_launcher_foreground"',
                    'android:icon="@mipmap/ic_launcher"',
                ).replace(
                    'android:roundIcon="@drawable/ic_launcher_foreground"',
                    'android:roundIcon="@mipmap/ic_launcher_round"',
                )
                with open(manifest_path, "w", encoding="utf-8") as f:
                    f.write(text)
            logo_applied = True
        except Exception as exc:
            print(f"ERROR: failed to download/apply logo from {logo_url!r}: {exc}", file=sys.stderr)
            return 1
    else:
        print("WARNING: branding API returned no logo_url — default icon will be used.", file=sys.stderr)

    print(f"Applied branding: {app_name} / v{version_code} ({branding_hash})")
    print(f"Start URL: {start_url}")
    print(f"Application ID: {app_id}")
    print(f"Logo applied: {'yes' if logo_applied else 'no'}")

    with open(strings_path, encoding="utf-8") as f:
        written = f.read()
    if "example.com" in written:
        print("ERROR: start_url still points to example.com after write", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
