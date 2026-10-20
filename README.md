<div align="center">

# 🦎 DNS Switcher

**One-tap Private DNS switcher for Android — popup list + Quick Settings tile. No VPN. No ads. No tracking.**

[![Release](https://img.shields.io/github/v/release/golaso024/dns-switcher?color=3DDC84&logo=github)](../../releases/latest)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](#-installation)
[![License](https://img.shields.io/badge/license-MIT-3DDC84)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)

**[English](#-english)** · **[Русский](#-русский)**

</div>

---

## 🇬🇧 English

> [!IMPORTANT]
> **One ADB command is required after install.**
> Android protects the Private DNS setting behind a signature-level permission, so a regular app cannot change it on its own. Grant it once from any computer with ADB:
>
> ```bash
> adb shell pm grant com.salamander.dnsswitch android.permission.WRITE_SECURE_SETTINGS
> ```
>
> Without this the app will install and open, but won't be able to switch DNS. The setting survives reboots and app updates — you only do this once.

### Why

Android has a built-in **Private DNS** (DNS-over-TLS) feature, but it's buried in *Settings → Network → Private DNS*, and switching between providers takes several taps. This app turns it into a one-tap popup or a Quick Settings tile.

**The app has no `INTERNET` permission at all.** It never touches the network — it only flips a system setting. Android itself handles all DNS traffic.

### Features

- 🪟 **Popup picker** — tap the tile or the app icon, a small card appears over whatever you're doing
- 📋 **Built-in providers**: Google · Cloudflare · Quad9 · Comss.one · AdGuard · Malw.link · AstraCat · XBoxDNS
- ➕ **Custom servers** — add any DoT hostname (e.g. your self-hosted `dns.example.com`); stored locally on device
- 🧩 **Quick Settings tile** — shows the currently active DNS right in the shade
- 🔔 **Persistent notification** with the active DNS and a one-tap **Disable** button
- 🪶 **~1 MB APK** — R8 + resource shrinking, no heavyweight dependencies

<!-- 📸 Screenshots — drop your images into docs/screenshots/ and uncomment:
<p align="center">
  <img src="docs/screenshots/popup.png" width="280" alt="DNS picker popup" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/tile.png" width="280" alt="Quick settings tile" />
</p>
-->

### Architecture

The whole app is two components and zero services running in background:

```
┌─────────────────────────────┐      ┌──────────────────────────────┐
│   DnsSelectorActivity       │ ←────   DnsTileService (QS tile)   │
│   transparent Compose popup │      │   opens the popup on tap     │
│                             │      └──────────────────────────────┘
│  reads/writes               │
│  Settings.Global:           │      ┌──────────────────────────────┐
│  · private_dns_mode         │ ───→ │  Persistent notification     │
│  · private_dns_specifier    │      │  active DNS + Disable action │
└─────────────────────────────┘      └──────────────────────────────┘
              │
              ▼
   SharedPreferences — user's custom server list (local only)
```

| Component | Role |
|---|---|
| `DnsSelectorActivity` | Translucent Compose card (excluded from Recents). Reads the current DNS, applies the chosen one, reacts to the notification's *Disable* action via the `DISABLE_DNS` intent extra, then dismisses itself. |
| `DnsTileService` | Standard `TileService`. Tap → opens the popup (via `PendingIntent` on Android 14+, as required). Label mirrors the active DNS host. |
| System settings | The actual switch: `private_dns_mode` = `hostname`/`off`, `private_dns_specifier` = provider host. This is exactly what the hidden system UI edits. |
| SharedPreferences | Custom user servers (`dns_switcher_prefs`). Nothing ever leaves the device. |

Everything UI-related is Jetpack Compose (Material 3, dynamic color on Android 12+). Release builds ship with R8 full mode and log stripping.

### Installation

1. Grab the APK from [**Releases**](../../releases/latest) and install it
2. Run the ADB command from the yellow box above
3. Add the **Private DNS** tile to your Quick Settings shade — done

<details>
<summary>How to run ADB without installing a full IDE</summary>

- **Windows/macOS**: download [platform-tools](https://developer.android.com/tools/releases/platform-tools), unzip, open a terminal in that folder
- **Linux**: `sudo apt install adb`
- Enable *Developer options → USB debugging* on the phone, connect via USB, run the command

</details>

### Build from source

```bash
./gradlew :app:assembleRelease   # unsigned, sign with your keystore
./gradlew :app:assembleDebug     # debug, ready to install
```

### Tech stack

Kotlin 2.0 · Jetpack Compose + Material 3 · AGP 9 · minSdk 26 / targetSdk 36 · zero third-party dependencies

---

## 🇷🇺 Русский

> [!IMPORTANT]
> **После установки нужна одна ADB-команда.**
> Android закрывает настройку «Приватный DNS» разрешением уровня подписи — обычное приложение не может менять её само. Выдайте разрешение один раз с любого компьютера, где есть ADB:
>
> ```bash
> adb shell pm grant com.salamander.dnsswitch android.permission.WRITE_SECURE_SETTINGS
> ```
>
> Без этого приложение установится и откроется, но переключать DNS не сможет. Разрешение переживает перезагрузки и обновления приложения — команда выполняется один раз.

### Зачем

В Android есть встроенный **Приватный DNS** (DNS-over-TLS), но спрятан он в *Настройки → Сеть → Приватный DNS*, и переключение между провайдерами — это несколько тапов. Это приложение превращает его в всплывающее окно в один тап или плитку в шторке.

**У приложения вообще нет разрешения `INTERNET`.** Оно не трогает сеть — только переключает системную настройку. Всем DNS-трафиком занимается сам Android.

### Возможности

- 🪟 **Всплывающее меню** — тап по плитке или иконке, и карточка появляется поверх всего
- 📋 **Встроенные провайдеры**: Google · Cloudflare · Quad9 · Comss.one · AdGuard · Malw.link · AstraCat · XBoxDNS
- ➕ **Свои серверы** — добавьте любой DoT-хост (например, свой `dns.example.com`); хранится только на устройстве
- 🧩 **Плитка в шторке** — показывает активный DNS прямо в быстрых настройках
- 🔔 **Постоянное уведомление** с текущим DNS и кнопкой «Выключить» в один тап
- 🪶 **APK ~1 МБ** — R8 + сжатие ресурсов, без тяжёлых зависимостей

<!-- 📸 Скриншоты — закиньте картинки в docs/screenshots/ и раскомментируйте:
<p align="center">
  <img src="docs/screenshots/popup.png" width="280" alt="Попап выбора DNS" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/tile.png" width="280" alt="Плитка в шторке" />
</p>
-->

### Архитектура

Всё приложение — два компонента и ноль фоновых сервисов:

```
┌─────────────────────────────┐      ┌──────────────────────────────┐
│   DnsSelectorActivity       │ ←────   DnsTileService (QS-плитка)  │
│   прозрачный Compose-попап  │      │   открывает попап по тапу    │
│                             │      └──────────────────────────────┘
│  читает/пишет               │
│  Settings.Global:           │      ┌──────────────────────────────┐
│  · private_dns_mode         │ ───→ │  Постоянное уведомление      │
│  · private_dns_specifier    │      │  активный DNS + «Выключить»  │
└─────────────────────────────┘      └──────────────────────────────┘
              │
              ▼
   SharedPreferences — список своих серверов (только локально)
```

| Компонент | Роль |
|---|---|
| `DnsSelectorActivity` | Полупрозрачная Compose-карточка (исключена из «Недавних»). Читает текущий DNS, применяет выбранный, обрабатывает кнопку «Выключить» из уведомления через intent-extra `DISABLE_DNS` и закрывается. |
| `DnsTileService` | Стандартный `TileService`. Тап → открыть попап (через `PendingIntent` на Android 14+, как требует система). В подписи плитки — активный DNS-хост. |
| Системные настройки | Сам переключатель: `private_dns_mode` = `hostname`/`off`, `private_dns_specifier` = хост провайдера. Ровно то, что редактирует скрытый системный интерфейс. |
| SharedPreferences | Пользовательские серверы (`dns_switcher_prefs`). Ничего не покидает устройство. |

Весь UI — Jetpack Compose (Material 3, динамический цвет на Android 12+). Релизные сборки идут с R8 full mode и вырезанным логированием.

### Установка

1. Скачайте APK из [**Releases**](../../releases/latest) и установите
2. Выполните ADB-команду из жёлтого блока выше
3. Добавьте плитку **Private DNS** в шторку — готово

<details>
<summary>Как запустить ADB, не ставя целую Android Studio</summary>

- **Windows/macOS**: скачайте [platform-tools](https://developer.android.com/tools/releases/platform-tools), распакуйте, откройте терминал в этой папке
- **Linux**: `sudo apt install adb`
- На телефоне включите *Для разработчиков → Отладка по USB*, подключите кабель, выполните команду

</details>

### Сборка из исходников

```bash
./gradlew :app:assembleRelease   # без подписи — подпишите своим ключом
./gradlew :app:assembleDebug     # debug, готов к установке
```

### Стек

Kotlin 2.0 · Jetpack Compose + Material 3 · AGP 9 · minSdk 26 / targetSdk 36 · ноль сторонних зависимостей
