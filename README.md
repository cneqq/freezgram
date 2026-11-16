<div align="center">

<img src="assets/freezgram-icon-black.png" width="128" alt="Freezgram">

# Freezgram

Telegram-клиент для Android с интерфейсом Freezgram, настройками оформления,
Liquid Glass и поддержкой Python-плагинов.

[Канал](https://t.me/freezgram) · [Исходники](https://github.com/cneqq/freezgram/tree/no-plugins) · [Релизы и APK](https://github.com/cneqq/freezgram/releases/latest) · [Сборки](https://github.com/cneqq/freezgram/actions/workflows/personal-apk.yml)

</div>

## О проекте

**Freezgram** — независимый Android-клиент на основе [exteraless](https://github.com/exteraless/exteraless)
и [NagramX](https://github.com/risin42/NagramX). Проект развивает собственное оформление
и настройки. Freezgram не является официальным приложением Telegram.

## Возможности

- Собственные название, иконки и оформление Freezgram.
- Настройки Liquid Glass для навигации, шапки чата и панели ввода.
- Настраиваемая iOS-панель навигации и панель ввода.
- Python-плагины с экраном установки, запрашиваемыми разрешениями и журналом событий.
- Настройки оформления, чатов, AyuMoments и функции exteraless/NagramX.
- Русский язык при первом запуске и переводы интерфейса.

Плагины могут выполнять код внутри приложения. Перед установкой проверяйте источник,
запрошенные разрешения и действия плагина. Модель разрешений ограничивает доступ к
поддерживаемым API, но сама по себе не делает неизвестный плагин безопасным.

Настройки Liquid Glass зависят от возможностей Android и устройства; если размытие
или преломление недоступно, интерфейс использует запасную подложку.

## Скачать APK

Нажмите [Релизы](https://github.com/cneqq/freezgram/releases/latest) и скачайте сборку
для устройства:

- **ARM64** — для большинства современных телефонов.
- **x86_64** — для устройств на Intel/AMD.
- **Универсальная** — содержит обе архитектуры и занимает больше места.

Каждый push в рабочую ветку `no-plugins` собирает и публикует все три APK.
При ручном запуске workflow можно выбрать один вариант или `all-three`.
Сборки предназначены для **Android 7.0+**. Имя пакета — **`com.freezgram.app`**;
обновление установленного приложения требует того же ключа подписи и более высокого
кода версии.

## Собрать самостоятельно

Клонируйте проект вместе с подмодулями:

```bash
git clone --branch no-plugins --recursive --shallow-submodules https://github.com/cneqq/freezgram.git
cd freezgram
```

Для Android-сборки нужны Java 21, Android SDK 37.0, Build Tools 36.0.0,
NDK 27.2.12479018 и CMake 3.31.6. Python runtime плагинов использует Chaquopy/Python 3.11;
для полной локальной сборки также нужны Python 3.11 и bash.

Подробности настройки API, подписи и GitHub Actions: [инструкция по личной сборке](docs/PERSONAL_BUILD.md).

## Ссылки

- Канал Freezgram: [@freezgram](https://t.me/freezgram).
- Репозиторий: [cneqq/freezgram](https://github.com/cneqq/freezgram).
- Исходный проект: [exteraless](https://github.com/exteraless/exteraless).
- База проекта: [NagramX](https://github.com/risin42/NagramX).

Благодарим авторов Telegram, exteraless, NagramX и других проектов,
чей код и материалы используются в клиенте. Авторство унаследованного кода
и материалов сохраняется за их авторами.

## Лицензия

Исходный код распространяется на условиях [GNU GPL v3](LICENSE).
Сведения об отдельных сторонних компонентах находятся в их каталогах.
