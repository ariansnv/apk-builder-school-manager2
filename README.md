# Android APK — School Manager Web Wrapper



اپ WebView که همان سامانه را باز می‌کند.



## ساخت APK **بدون Android Studio**



سه روش — از ساده به پیشرفته:



---



### روش ۱ — GitHub Actions (پیشنهادی، بدون نصب چیزی روی ویندوز)



1. پروژه را روی GitHub push کنید.

2. قبل از build، آدرس سامانه را در `app/src/main/res/values/strings.xml` تنظیم کنید:



```xml

<string name="app_start_url">https://your-domain.com/School-manager/public/</string>

```



3. در GitHub: **Actions** → **Build Android APK** → **Run workflow**

4. بعد از اتمام، از **Artifacts** فایل `school-manager-apk` را دانلود کنید.

5. (اختیاری) فایل را در سرور قرار دهید:



```

public/downloads/school-manager.apk

```



---



### روش ۲ — PWABuilder (اگر سایت HTTPS عمومی دارید)



1. بروید به [pwabuilder.com](https://www.pwabuilder.com/)

2. آدرس سامانه را وارد کنید (مثلاً `https://school.example.com/School-manager/public/`)

3. **Package for stores** → **Android** → APK را دانلود کنید.



> برای localhost / XAMPP محلی کار نمی‌کند — باید آدرس از اینترنت در دسترس باشد.



---



### روش ۳ — خط فرمان ویندوز (بدون Android Studio، فقط JDK + SDK)



```powershell

winget install Microsoft.OpenJDK.17

# Android SDK: یک‌بار Android Studio نصب کنید فقط برای SDK، یا command-line tools

cd android-app

# strings.xml را ویرایش کنید

.\build-apk.ps1

```



خروجی:



- `android-app/app/build/outputs/apk/debug/app-debug.apk`

- کپی خودکار به `public/downloads/school-manager.apk`



---



## تنظیم آدرس سامانه (مهم)



`app/src/main/res/values/strings.xml`:



| محیط | مثال |

|------|------|

| سرور واقعی | `https://school.example.com/School-manager/public/` |

| تست روی گوشی در شبکه محلی | `http://192.168.1.50/School-manager/public/` |

| شبیه‌ساز اندروید | `http://10.0.2.2/School-manager/public/` |



> روی گوشی واقعی `10.0.2.2` کار **نمی‌کند** — IP کامputer خود را بگذارید.



---



## دانلود از سامانه



بعد از قرار دادن APK روی سرور:



- `/app/apk`

- یا راهنمای نصب → دانلود APK



---



## عیب‌یابی GitHub Actions

### خطای `Could not find or load main class "-Xmx64m"`

فایل `android-app/gradlew` ناقص یا با خط‌پایان Windows (CRLF) push شده بود. نسخهٔ رسمی Gradle جایگزین شده. این فایل‌ها را commit و push کنید:

- `android-app/gradlew`
- `.gitattributes` (برای LF روی Linux)

در workflow از **`assembleDebug`** استفاده کنید، نه `build`:

```yaml
run: ./gradlew assembleDebug --no-daemon
working-directory: android-app
```

---

## نکات



- User-Agent شامل `SchoolManagerApp` است (ورود پایدار + PWA).

- برای Web Push ترجیحاً HTTPS استفاده کنید.

- Android Studio **لازم نیست** — فقط برای build به JDK + Android SDK (یا GitHub Actions) نیاز دارید.

