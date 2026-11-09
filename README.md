<div align="center">

<img src="assets/freezgram-icon-black.png" width="128" alt="Freezgram">

# Freezgram

Telegram-клиент для Android с интерфейсом Freezgram, гибкими настройками
оформления и сборками для ARM64, x86_64 и обеих архитектур сразу.

[Канал](https://t.me/freezgram) · [Исходники](https://github.com/cneqq/freezgram/tree/no-plugins) · [Релизы и APK](https://github.com/cneqq/freezgram/releases/latest) · [Сборки](https://github.com/cneqq/freezgram/actions/workflows/personal-apk.yml)

</div>

## О проекте

**Freezgram** — независимый Android-форк [exteraless](https://github.com/exteraless/exteraless),
который основан на [NagramX](https://github.com/risin42/NagramX). Проект развивает
собственное оформление и настройки клиента. Freezgram не является официальным
приложением Telegram.

Разработка Freezgram ведётся в ветке **`no-plugins`**. В ней удалены движок
плагинов, Python runtime, установка плагинов и связанные пункты интерфейса.
Ветка `main` содержит более раннюю редакцию; для актуального приложения
выбирайте `no-plugins`.

## Возможности

- Собственные название, иконки и оформление Freezgram.
- iOS-панель навигации с опциональным Liquid Glass.
- Общие настройки Liquid Glass с предпросмотром: размытие, сила преломления
  и прозрачность для навигации, шапки чата и панели ввода.
- Ширина и отступ снизу у навигации, видимость кнопки «Контакты» и
  регулировка положения iOS-панели ввода над нижним краем.
- Локальные значки сообщества Freezgram для основателя и официального канала.
- Русский язык при первом запуске и переводы интерфейса.
- Настройки оформления и функции, унаследованные от exteraless и NagramX.
- Плагины, встроенный механизм обновлений разработчика и установка APK из приложения удалены.

Преломление Liquid Glass требует Android 13+, включённого размытия и отключённого
энергосбережения. В остальных случаях используется более плотная подложка.
Настройки панели находятся в **Настройки Freezgram → Оформление → Стиль FreezGram → Панель навигации**.

Проект развивается: возможны ошибки и различия в отрисовке на разных устройствах.

## Скачать APK

1. Нажмите [Релизы](https://github.com/cneqq/freezgram/releases/latest) и скачайте APK для устройства.
2. Для большинства телефонов подходит **ARM64**; для устройств на Intel — **x86_64**.
   Универсальный APK содержит обе архитектуры и занимает больше места.
3. Установите скачанный файл `.apk`.

Если свежего релиза ещё нет, сборку можно запустить в
[GitHub Actions → Personal release APK](https://github.com/cneqq/freezgram/actions/workflows/personal-apk.yml),
выбрав **both**. Этот вариант собирает три APK: ARM64, x86_64 и универсальный.

Сборки предназначены для **Android 7.0+**.
Имя пакета — **`com.freezgram.app`**. Обновление поверх установленного Freezgram
требует того же пакета, того же ключа подписи и более высокого кода версии.

## Собрать самостоятельно

Компьютер не обязателен: сборка может выполняться в GitHub Actions.
Подробная инструкция — [личная ARM64-сборка](docs/PERSONAL_BUILD.md).
Для актуального Freezgram используйте только ветку `no-plugins`.

Для локальной работы клонируйте проект с подмодулями:

```bash
git clone --branch no-plugins --recursive --shallow-submodules https://github.com/cneqq/freezgram.git
cd freezgram
```

Текущему CI нужны Java 21, Android SDK 37.0, Build Tools 36.0.0,
NDK 27.2.12479018 и CMake 3.31.6. Полный порядок настройки API и подписи
описан в инструкции по сборке.

Workflow **Personal release APK** использует Secrets `API_ID`, `API_HASH`,
`RELEASE_KEYSTORE` и `KEYSTORE_PASS`; дополнительные параметры ключа описаны
в инструкции. API-параметры получают на [my.telegram.org](https://my.telegram.org).
Ключ подписи и пароли должны храниться вне исходников и публичных артефактов.

## Связь и исходный проект

- Канал Freezgram: [@freezgram](https://t.me/freezgram).
- Репозиторий Freezgram: [cneqq/freezgram](https://github.com/cneqq/freezgram).
- Исходный проект: [exteraless/exteraless](https://github.com/exteraless/exteraless).
- База exteraless: [NagramX](https://github.com/risin42/NagramX).

Благодарим авторов Telegram, exteraless, NagramX и других проектов,
чей код и материалы используются в клиенте. Авторство унаследованного кода
и материалов сохраняется за их авторами.

## Лицензия

Исходный код распространяется на условиях [GNU GPL v3](LICENSE).
Сведения об отдельных сторонних компонентах находятся в их каталогах.
