# اپ اندروید — School Manager

WebView سبک که سامانه مدرسه را با نام و لوگوی اختصاصی شما باز می‌کند.

---

## فهرست

1. [پیش‌نیازها](#۱-پیش‌نیازها)
2. [تنظیمات در پنل سامانه](#۲-تنظیمات-در-پنل-سامانه)
3. [بررسی API قبل از build](#۳-بررسی-api-قبل-از-build)
4. [ساخت APK با GitHub Actions (پیشنهادی)](#۴-ساخت-apk-با-github-actions-پیشنهادی)
5. [آپلود APK روی سرور](#۵-آپلود-apk-روی-سرور)
6. [نصب روی گوشی](#۶-نصب-روی-گوشی)
7. [ساخت محلی روی Windows](#۷-ساخت-محلی-روی-windows)
8. [به‌روزرسانی نام / لوگو / آدرس](#۸-به‌روزرسانی-نام--لوگو--آدرس)
9. [عیب‌یابی](#۹-عیب‌یابی)
10. [Play Protect و امنیت](#۱۰-play-protect-و-امنیت)
11. [ساختار فنی پروژه](#۱۱-ساختار-فنی-پروژه)

---

## ۱. پیش‌نیازها

| مورد | توضیح |
|------|--------|
| سامانه نصب‌شده | installer اجرا شده و سایت با HTTPS در دسترس است |
| repo در GitHub | پوشه `android-app/` داخل همان repo باشد |
| تنظیمات اپ | در پنل ادمین ذخیره شده باشد |
| APK روی سرور | بعد از build در `public/downloads/school-manager.apk` |

---

## ۲. تنظیمات در پنل سامانه

مسیر: **تنظیمات سامانه → اپ موبایل (وب‌اپ و APK)**

### فیلدهای مهم (بخش «اپ اندروید»)

| فیلد | مثال برای مجتمع امام هادی | توضیح |
|------|---------------------------|--------|
| **نام اپ روی گوشی** | `مجتمع امام هادی(ع)` | نامی که زیر آیکون روی Home Screen دیده می‌شود |
| **آدرس سامانه (Start URL)** | `https://my.emamhadischool.ir/public/` | دقیقاً آدرس ورود سامانه — با `https://` و `/` در انتها |
| **Application ID** | `ir.emamhadischool.app` | شناسه یکتا — **بعد از انتشار عوض نکنید** |
| **لوگوی اپ** | PNG/JPG مربعی | پیشنهاد: ۵۱۲×۵۱۲ |
| **نسخه نمایشی** | `1.0.0` | فقط برای نمایش؛ کد نسخه خودکار مدیریت می‌شود |

### بعد از ذخیره

- **کد نسخه (version code)** خودکار +۱ می‌شود (با تغییر نام/لوگو).
- **امضای برندینگ (hash)** عوض می‌شود.
- در همان صفحه، **API برندینگ** و **پیش‌نمایش JSON** را ببینید.

### API برندینگ (کپی کنید — آدرس کامل HTTPS)

```
https://YOUR-DOMAIN/public/api/pwa/apk-branding
```

مثال واقعی:

```
https://my.emamhadischool.ir/public/api/pwa/apk-branding
```

> **اشتباه رایج:** گذاشتن `/public/api/...` بدون `https://domain` — build بدون برندینگ انجام می‌شود و اپ «مدرسه‌یار» + `example.com` باز می‌کند.

---

## ۳. بررسی API قبل از build

قبل از هر build، این URL را در مرورگر باز کنید. باید JSON شبیه زیر ببینید:

```json
{
  "ok": true,
  "app_name": "مجتمع امام هادی(ع)",
  "start_url": "https://my.emamhadischool.ir/public/",
  "logo_url": "https://my.emamhadischool.ir/public/uploads/...",
  "application_id": "ir.emamhadischool.app",
  "version_code": 2,
  "version_name": "1.0.0",
  "branding_hash": "09439d38a0784f34"
}
```

### چک‌لیست سریع

- [ ] `"ok": true`
- [ ] `"start_url"` با `https://` شروع شود و **example.com نباشد**
- [ ] `"logo_url"` با `https://` شروع شود (آدرس کامل، نه `/public/...`)
- [ ] `"app_name"` همان نام مدرسه باشد

### بررسی از خط فرمان (Linux / Git Bash)

```bash
cd android-app
chmod +x scripts/verify-branding-api.sh
./scripts/verify-branding-api.sh "https://my.emamhadischool.ir/public/api/pwa/apk-branding"
```

اگر خطا داد، **اول تنظیمات پنل را ذخیره کنید** و فایل‌های PHP جدید را روی سرور آپلود کنید.

---

## ۴. ساخت APK با GitHub Actions (پیشنهادی)

روش پیشنهادی — بدون نصب Android Studio روی ویندوز.

### مرحله‌به‌مرحله

1. **کد را push کنید**  
   مطمئن شوید پوشه `android-app/` در repo هست.

2. **تنظیمات پنل را ذخیره کنید**  
   (بخش ۲)

3. **GitHub → تب Actions**

4. **Workflow: `Build Android APK`**

5. **Run workflow** (دکمه سمت راست)

6. فیلد **`branding_url`** — آدرس کامل API را paste کنید:

   ```
   https://my.emamhadischool.ir/public/api/pwa/apk-branding
   ```

7. **Run workflow** را بزنید و صبر کنید (حدود ۳–۸ دقیقه).

8. بعد از موفقیت → **Artifacts** → **`school-manager-apk`** → دانلود `app-release.apk`

### GitHub چه کار می‌کند؟

1. API برندینگ را می‌خواند و اعتبارسنجی می‌کند
2. نام، آدرس، لوگو، Application ID و نسخه را در پروژه اعمال می‌کند
3. اگر هنوز `example.com` باشد → **build متوقف می‌شود**
4. APK امضا‌شده Release می‌سازد
5. فایل را به عنوان Artifact آپلود می‌کند

### Secrets اختیاری (امضای ثابت)

| Secret | توضیح |
|--------|--------|
| `ANDROID_KEYSTORE_PASSWORD` | رمز keystore |
| `ANDROID_KEY_PASSWORD` | رمز کلید |

بدون Secret، رمز پیش‌فرض `SchoolManager2026!` است. keystore اولین بار ساخته و cache می‌شود.

---

## ۵. آپلود APK روی سرور

فایل دانلود‌شده را با این نام روی سرور بگذارید:

```
public/downloads/school-manager.apk
```

### مسیر کامل (مثال cPanel)

```
/home/USER/public_html/my.emamhadischool.ir/public/downloads/school-manager.apk
```

### تست لینک دانلود

```
https://my.emamhadischool.ir/public/downloads/school-manager.apk
```

یا:

```
https://my.emamhadischool.ir/public/app/apk
```

در پنل **تنظیمات → اپ موبایل** باید «دانلود APK فعلی روی سرور» لینک بدهد.

---

## ۶. نصب روی گوشی

1. **اپ قدیمی را حذف کنید** (اگر نام «مدرسه‌یار» بود یا example.com باز می‌کرد)
2. APK جدید را دانلود و نصب کنید
3. اگر Play Protect هشدار داد → [بخش ۱۰](#۱۰-play-protect-و-امنیت)
4. اپ را باز کنید — باید مستقیم سایت مدرسه باز شود

### اگر پیام «نسخه جدید را نصب کنید» آمد

یعنی نسخه یا برندینگ روی سرور جدیدتر از APK نصب‌شده است:

1. APK تازه build کنید
2. روی سرور آپلود کنید
3. اپ قدیمی را حذف و APK جدید نصب کنید

---

## ۷. ساخت محلی روی Windows

اگر JDK 17 و Android SDK دارید:

### نصب پیش‌نیازها

```powershell
winget install Microsoft.OpenJDK.17
# Android Studio یا Android SDK Command-line Tools
```

### ساخت (با برندینگ از سرور)

```powershell
cd android-app
.\build-apk.ps1 -BrandingUrl "https://my.emamhadischool.ir/public/api/pwa/apk-branding"
```

خروجی:

- `android-app/app/build/outputs/apk/debug/app-debug.apk`
- کپی خودکار به `public/downloads/school-manager.apk`

### ساخت Release محلی (نیاز به keystore)

```powershell
.\build-apk.ps1 -BrandingUrl "https://..." -Release
```

> **توصیه:** برای Release امضا‌شده از GitHub Actions استفاده کنید.

---

## ۸. به‌روزرسانی نام / لوگو / آدرس

| تغییر | کار لازم |
|-------|----------|
| نام اپ | پنل → ذخیره → build جدید → نصب مجدد |
| لوگو | همان |
| Start URL | همان |
| Application ID | فقط **قبل از اولین انتشار** — بعداً عوض نکنید |

با هر تغییر نام/لوگو، **version code** در پنل +۱ می‌شود. APK جدید باید همان کد را داشته باشد.

برای اجبار به‌روزرسانی همه کاربران: تیک **«انتشار نسخه جدید»** را بزنید و ذخیره کنید.

---

## ۹. عیب‌یابی

### اپ «مدرسه‌یار» است و example.com باز می‌کند

**علت:** برندینگ در build اعمال نشده.

**راه‌حل:**

1. API را در مرورگر چک کنید (بخش ۳)
2. در GitHub فیلد `branding_url` باید **کامل** باشد:
   ```
   https://my.emamhadischool.ir/public/api/pwa/apk-branding
   ```
   ❌ `/public/api/pwa/apk-branding`  
   ❌ `my.emamhadischool.ir/public/...` (بدون https)
3. build را دوباره اجرا کنید
4. اپ قدیمی را **حذف** و APK جدید نصب کنید

### لوگو درست نیست / آیکون پیش‌فرض است

**علت:** `logo_url` در API نسبی بود (مثلاً `/public/uploads/...`) — GitHub نمی‌تواند دانلود کند.

**راه‌حل:** فایل `ApkBrandingService.php` به‌روز را آپلود کنید. `logo_url` باید با `https://` شروع شود.

### GitHub build با خطای branding_url متوقف شد

| خطا | راه‌حل |
|-----|--------|
| must start with https:// | آدرس کامل بگذارید |
| example.com | Start URL را در پنل تنظیم و ذخیره کنید |
| logo_url must be absolute | PHP جدید را آپلود کنید |
| HTTP 503 | حالت تعمیرات سراسری — موقتاً خاموش کنید |

### اپ باز نمی‌شود / صفحه سفید

- Start URL را چک کنید (باید همان آدرسی باشد که در مرورگر کار می‌کند)
- HTTPS باشد
- اینترنت گوشی فعال باشد

### پیام به‌روزرسانی مدام می‌آید

- APK روی سرور قدیمی است → build و آپلود جدید
- version code در APK با پنل یکی نیست → دوباره build با API تازه

### نصب APK روی گوشی خطا می‌دهد

- «Install unknown apps» برای مرورگر/File Manager فعال باشد
- اگر قبلاً با Application ID دیگر نصب کرده‌اید، اول حذف کنید

---

## ۱۰. Play Protect و امنیت

APK خارج از Google Play Store است. **Play Protect** ممکن است هشدار دهد — برای sideload عادی است.

### مراحل نصب با هشدار

1. **More details** / **جزئیات بیشتر**
2. **Install anyway** / **نصب در هر صورت**

### چرا هشدار می‌دهد؟

- اپ در Play Store نیست
- امضای developer ناشناس برای Google است
- WebView wrapper کم‌حجم است

### کاهش هشدار (انجام‌شده در پروژه)

| مورد | وضعیت |
|------|--------|
| APK امضا‌شده (Release) | ✅ |
| keystore ثابت (cache در GitHub) | ✅ |
| فقط مجوز INTERNET | ✅ |
| بدون cleartext (HTTPS) | ✅ |
| R8 minify | ✅ |
| حجم ~۱–۲ MB | ✅ |

---

## ۱۱. ساختار فنی پروژه

```
android-app/
├── app/
│   ├── build.gradle          ← applicationId, versionCode
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/.../MainActivity.java   ← WebView + چک به‌روزرسانی
│       └── res/values/strings.xml     ← app_name, app_start_url (در build پر می‌شود)
├── scripts/
│   ├── apply_apk_branding.py          ← خواندن API و اعمال برندینگ
│   ├── apply-apk-branding.sh
│   └── verify-branding-api.sh         ← اعتبارسنجی قبل از build
├── build-apk.ps1                      ← ساخت محلی Windows
└── README.md                          ← همین فایل
```

### جریان داده

```
پنل ادمین → api/pwa/apk-branding → GitHub Actions / build-apk.ps1
    → strings.xml + icons + build.gradle → APK → public/downloads/
    → کاربر نصب → WebView → start_url
    → api/pwa/app-update (چک نسخه)
```

### APIهای مرتبط

| API | کاربرد |
|-----|--------|
| `GET /api/pwa/apk-branding` | برندینگ برای build |
| `GET /api/pwa/app-update?version_code=&branding_hash=` | چک به‌روزرسانی در اپ |
| `GET /app/apk` | دانلود APK از سرور |

---

## خلاصه سریع (چک‌لیست)

```
☐ تنظیمات اپ در پنل ذخیره شد
☐ API در مرورگر JSON درست نشان می‌دهد (logo_url با https)
☐ GitHub workflow با branding_url کامل اجرا شد
☐ Artifact دانلود شد
☐ فایل در public/downloads/school-manager.apk آپلود شد
☐ اپ قدیمی از گوشی حذف شد
☐ APK جدید نصب شد — نام و آدرس درست است
```

---

## پشتیبانی

اگر بعد از این مراحل هنوز مشکل دارید:

1. JSON خروجی API را کپی کنید
2. لاگ GitHub Actions (مرحله Validate / Apply branding / Verify) را ببینید
3. محتوای `strings.xml` بعد از build را چک کنید (`example.com` نباید باشد)
