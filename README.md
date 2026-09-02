# Android APK — School Manager

WebView سبک برای باز کردن سامانه.

## تنظیم نام و لوگو (از پنل سامانه)

**تنظیمات سامانه → اپ اندروید**

- نام اپ روی گوشی
- لوگوی اپ (PNG/JPG)
- آدرس سامانه (Start URL)
- Application ID و نسخه

هر زمان تغییر دادید → APK را **دوباره build** کنید.

API برندینگ (عمومی):

```
https://YOUR-SITE/.../api/pwa/apk-branding
```

---

## ساخت APK — GitHub Actions

1. محتوای `android-app/` را در repo قرار دهید (در root یا پوشه `android-app/`).
2. در سامانه: **تنظیمات سامانه → اپ اندروید** → ذخیره.
3. GitHub → **Actions** → **Build Android APK** → **Run workflow**
4. فیلد **branding_url** = همان API بالا
5. از **Artifacts** فایل `app-release.apk` را دانلود کنید.
6. روی سرور: `public/downloads/school-manager.apk`

---

## کمتر شناخته شدن به‌عنوان بدافزار

| مورد | توضیح |
|------|--------|
| **Release + امضا** | workflow APK امضا‌شده (`assembleRelease`) می‌سازد |
| **keystore ثابت** | اولین build keystore می‌سازد و cache می‌کند |
| **HTTPS** | Start URL با `https://` ترجیحاً |
| **حداقل مجوز** | فقط `INTERNET` |
| **بدون cleartext سراسری** | HTTP فقط برای دامنه سامانه (در build) |

Secrets اختیاری: `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_PASSWORD`

---

## حجم کم (~۱–۲ مگ)

- بدون AppCompat / Kotlin
- R8 minify + shrinkResources
- فقط ARM (64 + 32 bit)
