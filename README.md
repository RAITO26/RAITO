<img width="1280" height="720" alt="image" src="https://github.com/user-attachments/assets/14df3ba6-8b1e-486b-8bdd-fbaac0e95f96" />



خیلی خوشحالم که خوشت اومد رفیق! این تغییرات جدید (پشتیبانی از اندروید ۷ تا ۱۷ و پردازنده عمومی/Universal) پروژه رو خیلی کامل‌تر و فنی‌تر نشون میده.

این موارد رو هم به بخش ویژگی‌ها اضافه کردم و یک جدول مشخصات فنی شیک براش ساختم تا کاربران در یک نگاه متوجه بشن برنامه‌ات روی چه سیستم‌هایی نصب میشه.

کل متن داخل کادر زیر رو کپی کن و جایگزین کل فایل `README.md` پروژه‌ات کن:

```markdown
<div align="center">

<!-- لوگو و عنوان پروژه -->
<h1>📱 RAITO </h1>
<p><strong>Notification Management & Android Device Control via Telegram Bot</strong></p>
<p><em>مدیریت نوتیفیکیشن‌ها و کنترل کنترل گوشی اندروید از طریق ربات تلگرام</em></p>

<!-- نشان‌های پویا و شیک گیت‌هاب -->
<p>
  <img src="https://img.shields.io/github/stars/RAITO26/RAITO?style=for-the-badge&logo=github&color=FFD700" alt="Stars" />
  <img src="https://img.shields.io/github/license/RAITO26/RAITO?style=for-the-badge&color=green" alt="License" />
  <img src="https://img.shields.io/github/languages/top/RAITO26/RAITO?style=for-the-badge&color=blue" alt="Languages" />
</p>

---

<h4>
  <a href="#-english-description">English Guide</a> •
  <a href="#-key-features">Features</a> •
  <a href="#-system-requirements">Requirements</a> •
  <a href="#-how-to-setup">Setup</a> •
  <a href="#-معرفی-به-زبان-فارسی">راهنمای فارسی</a> •
  <a href="#-ویژگی‌های-کلیدی">ویژگی‌ها</a> •
  <a href="#-مشخصات-فنی">مشخصات فنی</a> •
  <a href="#-نحوه-راه‌اندازی">راه‌اندازی</a>
</h4>

</div>

<br />

<!-- ========================================== -->
<!--             ENGLISH DESCRIPTION            -->
<!-- ========================================== -->

## 🇺🇸 English Description

### 📝 About RAITO
**RAITO** is an open-source Android application designed to manage, log, and backup your device notifications directly inside a Telegram bot. 

Imagine receiving an important bank SMS or an official notification, and accidentally deleting it without any way to recover it. **RAITO** solves this problem by instantly forwarding all your notifications and SMS to your private Telegram bot, ensuring you never lose important information.

### 🚀 Key Features
* 📱 **Device Status Monitor:** Easily check your device status in real-time.
* 📊 **Live Logs:** View live error logs and status updates on the fly.
* 📃 **SMS Management & Backup:** View all SMS history on your phone and take backups.
* 💡 **Call History Tracking:** Monitor missed and rejected calls with backup options.
* 📲 **App Manager:** View all installed apps through a neat panel inside the Telegram bot.
* 📁 **File Manager Panel:** Browse files, manage folders, and back up your device files directly to Telegram.
* 📑 **Notification Backup:** Export and backup all received notifications as a `.txt` file.

### ⚙️ System Requirements
| Requirement | Specification |
| :--- | :--- |
| **OS Platform** | Android (Specifically designed for Android Mobile Phones) |
| **Android Version** | Android 7.0 (Nougat) up to Android 17 (Fully Compatible) |
| **Architecture** | Universal (Supports ARM, ARM64, x86, etc.) |

### ⚙️ How to Setup

1. Open Telegram and search for [@BotFather](https://t.me/BotFather) to create a new bot. Copy the generated **HTTP API Token**:
   `8525496066:AAHfkEhChRG76u3GK7pwTYxBzCG2.....`
2. Open [@userinfobot](https://t.me/userinfobot) to get your Telegram **Numeric ID**:
   `10336.......`
3. Open the **RAITO** app on your phone, enter the **Token** and **Admin ID** in the designated fields, and tap **START**.
4. Start your Telegram bot to manage your device locally.

> ⚠️ **Google Play Protect Warning:**
> Since this application monitors device notifications, Google Play Protect might flag it. 
> To install it, click on **"More Details"** and select **"Install anyway"**. The app is 100% open-source, and you can inspect the code yourself.

---

<!-- ========================================== -->
<!--             PERSIAN DESCRIPTION            -->
<!-- ========================================== -->

<div dir="rtl">

## 🇮🇷 معرفی به زبان فارسی

### 📝 درباره پروژه RAITO
**رایتو (RAITO)** یک نرم‌افزار متن‌باز اندرویدی است که برای مدیریت، ذخیره و پشتیبان‌گیری از نوتیفیکیشن‌های دریافتی گوشی در ربات تلگرام طراحی شده است.

فرض کنید یک پیامک مهم بانکی یا اداری دریافت کرده‌اید و به اشتباه آن را پاک می‌کنید. رایتو این مشکل را حل می‌کند؛ این برنامه نه تنها پیامک‌ها، بلکه تمام نوتیفیکیشن‌های دریافتی گوشی شما را بلافاصله به ربات تلگرامی شخصی‌تان ارسال و ذخیره می‌کند تا هیچ اطلاعاتی را از دست ندهید.

### 🚀 ویژگی‌های کلیدی
* 📱 **بررسی وضعیت دستگاه:** مشاهده وضعیت لحظه‌ای گوشی در ربات.
* 📊 **مشاهده زنده لاگ‌ها:** نمایش گزارش‌های وضعیت، خطاها و لاگ‌های سیستم به صورت زنده.
* 📃 **مدیریت و بکاپ پیامک‌ها:** نمایش تمامی پیامک‌های موجود در گوشی و امکان فایل پشتیبان از آن‌ها.
* 💡 **گزارش تماس‌ها:** نمایش تماس‌های بی‌پاسخ و رد شده به همراه امکان بکاپ‌گیری.
* 📲 **مدیریت برنامه‌ها:** نمایش لیست تمامی اپلیکیشن‌های نصب شده روی گوشی به صورت پنل در ربات.
* 📁 **مدیریت فایل پیشرفته:** مشاهده فایل‌ها و پوشه‌های گوشی در تلگرام و امکان ذخیره و پشتیبان‌گیری مستقیم آن‌ها.
* 📑 **بکاپ نوتیفیکیشن‌ها:** خروجی گرفتن و بکاپ تمامی اعلان‌ها در قالب فایل متنی `TXT`.

### ⚙️ مشخصات فنی و نیازها
| مشخصه | توضیحات |
| :--- | :--- |
| **سیستم‌عامل** | اندروید (مخصوص و بهینه‌سازی شده برای گوشی‌های موبایل) |
| **نسخه اندروید** | پشتیبانی کامل از اندروید نسخه 7.0 تا اندروید 17 |
| **معماری پردازنده** | Universal (عمومی - سازگار با انواع پردازنده‌های ARM, ARM64, x86 و غیره) |

### ⚙️ نحوه راه‌اندازی

۱. ابتدا در تلگرام وارد ربات [@BotFather](https://t.me/BotFather) شده و یک ربات جدید بسازید تا به شما یک توکن بدهد:
   `8525496066:AAHfkEhChRG76u3GK7pwTYxBzCG2.....`
۲. برای پیدا کردن آیدی عددی خود، ربات [@userinfobot](https://t.me/userinfobot) را استارت کنید تا آیدی شما را نمایش دهد:
   `10336.......`
۳. اطلاعات دریافتی (توکن و آیدی عددی) را در برنامه قرار داده و دکمه **START** را بزنید و سپس ربات خود را در تلگرام استارت کنید.

> ⚠️ **هشدار سپر امنیتی گوگل (Google Play Protect):**
> به دلیل دسترسی‌های برنامه برای مدیریت نوتیفیکیشن‌ها، ممکن است گوگل‌پلی آن را به عنوان برنامه ناشناس شناسایی کند. برای نصب کافیست روی گزینه **More Details** بزنید و سپس **Install anyway** را انتخاب کنید. کد برنامه کاملاً باز است و می‌توانید خودتان آن را بررسی کنید.

</div>

<br />

---

<!-- ========================================== -->
<!--              GALLERY & SCREENS             -->
<!-- ========================================== -->

## 📸 Screenshots / تصاویر محیط برنامه

<details>
<summary><b>Click to view app screenshots / برای مشاهده تصاویر کلیک کنید</b></summary>
<br />
<div align="center">
  <table border="0">
    <tr>
      <td><img width="240" alt="photo_1" src="https://github.com/user-attachments/assets/5bf436d0-01e5-4ecf-b5e4-09bd9e65c260" /></td>
      <td><img width="240" alt="photo_2" src="https://github.com/user-attachments/assets/dee38dde-5cda-4d58-8cd6-ae94fa7ab80c" /></td>
      <td><img width="240" alt="photo_3" src="https://github.com/user-attachments/assets/0b4008fd-e577-4959-8168-7574156e1c96" /></td>
    </tr>
    <tr>
      <td><img width="240" alt="photo_4" src="https://github.com/user-attachments/assets/9a3335dc-0291-4557-8039-856c6af5f3db" /></td>
      <td><img width="240" alt="photo_5" src="https://github.com/user-attachments/assets/d5b75f39-7c74-45ec-8a9f-9dfd06296442" /></td>
      <td><img width="240" alt="photo_6" src="https://github.com/user-attachments/assets/ec6af364-dd25-4f7d-baf6-b660edc6bd40" /></td>
    </tr>
    <tr>
      <td><img width="240" alt="photo_7" src="https://github.com/user-attachments/assets/c067c7ab-9ff7-4181-a6b5-fc9ef8b81840" /></td>
      <td><img width="240" alt="photo_8" src="https://github.com/user-attachments/assets/96a00b81-aa57-4435-b558-480f60e5cc6a" /></td>
      <td><img width="240" alt="photo_9" src="https://github.com/user-attachments/assets/f67dff52-7b75-489b-83c4-3579561cda15" /></td>
    </tr>
     <tr>
      <td><img width="240" alt="photo_10" src="https://github.com/user-attachments/assets/b31a44b6-8b48-4516-ad51-cbc3b815fb70" /></td>
      <td><img width="240" alt="photo_11" src="https://github.com/user-attachments/assets/b0581dc4-109c-4848-bb77-0e156abe99eb" /></td>
      <td><img width="240" alt="Play Protect Error" src="https://github.com/user-attachments/assets/1423752c-e180-4f02-ac89-d153eed57620" /></td>
    </tr>
  </table>
</div>
</details>

<br />

---

<!-- ========================================== -->
<!--                  DISCLAIMER                -->
<!-- ========================================== -->

## ⚖️ Disclaimer / سلب مسئولیت

<div dir="rtl">
این برنامه صرفاً جهت استفاده شخصی و مانیتورینگ دستگاه خودتان توسعه یافته است. مسئولیت هرگونه استفاده نادرست یا سوءاستفاده از این ابزار تماماً بر عهده کاربر نهایی است و توسعه‌دهنده هیچ‌گونه مسئولیتی در قبال آن نمی‌پذیرد.
</div>


```

This application is developed solely for personal device monitoring and backup purposes.
The developer holds no responsibility for any misuse or malicious exploitation of this software.
Use it at your own risk.

```

---

<div align="center">

🌟 **If you find this project useful, please give it a Star! It helps me build more open-source tools.**  
🌟 *اگر این پروژه برای شما مفید بود، لطفاً با دادن ستاره (Star) به آن در گیت‌هاب از من حمایت کنید.*

<sub>Developed with ❤️ by <a href="https://github.com/RAITO26">RAITO26</a></sub>

</div>

```
---

<div align="center">
  <sub>Developed with ❤️ by <a href="https://github.com/RAITO26">RAITO26</a></sub>
</div>
