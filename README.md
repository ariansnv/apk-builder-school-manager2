# Android APK — School Manager Web Wrapper



اپ WebView که همان سامانه را باز می‌کند.



## ساخت APK **بدون Android Studio**



سه روش — از ساده به پیشرفته:



---



### روش ۱ — GitHub Actions (پیشنهادی، بدون نصب چیزی روی ویندوز)

**مهم:** روی GitHub باید **خود پروژه اندروید** باشد، نه فقط فایل workflow.

#### اگر repo جداگانه ساختید (مثل `apk-builder-school-manager2`)

محتوای پوشه `android-app/` را **در ریشه همان repo** کپی کنید:

```
your-repo/
  .github/workflows/build-apk.yml
  gradlew
  gradlew.bat
  gradle/
  gradle.properties
  settings.gradle
  build.gradle
  app/
    build.gradle
    src/main/...
```

یعنی `gradlew` و `app/` مستقیم در root باشند — **نه** داخل `android-app/`.

#### اگر کل School-manager را push می‌کنید

ساختار `android-app/gradlew` و `android-app/app/` کافی است؛ workflow هر دو حالت را خودکار تشخیص می‌دهد.

---

1. قبل از build، آدرس سامانه را در `app/src/main/res/values/strings.xml` تنظیم کنید:



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

### خطای `working directory ... android-app ... No such file or directory`

پوشه `android-app` روی GitHub وجود ندارد. یا:

- **repo جدا:** همه فایل‌های داخل `android-app/` را در **root** repo بگذارید، یا
- **repo کامل:** کل پوشه `android-app/` را push کنید.

Workflow جدید قبل از build وجود `gradlew` را چک می‌کند.

### خطای `Could not find or load main class "-Xmx64m"`

1. فایل `gradlew` روی GitHub قدیمی/خراب است — نسخهٔ داخل School-manager را دوباره push کنید.
2. حتماً **`gradle/wrapper/gradle-wrapper.jar`** هم commit شود (بدون این فایل build نمی‌شود).
3. workflow جدید دیگر به `gradlew` shell وابسته نیست و مستقیم با Java اجرا می‌شود.

اگر `gradlew` را دستی اجرا می‌کنید:

```bash
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain assembleDebug
```

---

## نکات



- User-Agent شامل `SchoolManagerApp` است (ورود پایدار + PWA).

- برای Web Push ترجیحاً HTTPS استفاده کنید.

- Android Studio **لازم نیست** — فقط برای build به JDK + Android SDK (یا GitHub Actions) نیاز دارید.

