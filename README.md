# CashEye

[![CI](https://github.com/sharingan2077/CashEye/actions/workflows/ci.yml/badge.svg)](https://github.com/sharingan2077/CashEye/actions/workflows/ci.yml)
[![Latest release](https://img.shields.io/github/v/release/sharingan2077/CashEye)](https://github.com/sharingan2077/CashEye/releases/latest)
[![License](https://img.shields.io/github/license/sharingan2077/CashEye)](LICENSE)

Android-приложение для учёта личных финансов: счетов, доходов, расходов и аналитики.

**[Скачать последнюю версию](https://github.com/sharingan2077/CashEye/releases/latest)** ·
**[Сообщить об ошибке](https://github.com/sharingan2077/CashEye/issues/new/choose)** ·
**[Предложить улучшение](https://github.com/sharingan2077/CashEye/issues/new/choose)**

## Возможности

- Просмотр, создание, редактирование и удаление счетов, доходов и расходов.
- Аналитика операций с выбором периода, типа операции, категорий и счёта.
- Pull-to-refresh и понятные состояния загрузки, пустого списка и ошибки.
- Local-first режим: Room — источник данных для интерфейса. Ранее загруженные счета, категории и
  операции остаются доступны без сети.
- Durable outbox для локальных изменений. Изменения сначала сохраняются на устройстве, а затем
  синхронизируются с сервером через WorkManager при появлении сети.
- Курсы валют из Frankfurter с локальным кешем актуальных и исторических значений.
- Настройки:
  - отчётная валюта;
  - просмотр и локальный поиск статей;
  - светлая, тёмная и системная темы;
  - системный, русский, английский, немецкий, французский и испанский языки;
  - четырёхзначный PIN-код и биометрическая разблокировка, когда она поддерживается устройством.

PIN не хранится в открытом виде: сохраняется только salted verifier в
`EncryptedSharedPreferences`. Настройки темы и языка хранятся в DataStore.

## Архитектура

Проект использует multi-module Clean Architecture с MVVM и MVI-style presentation.

```text
app
├── core:model
├── core:designsystem
├── core:common
├── domain:finance
├── domain:settings
├── data:finance
├── data:settings
├── feature:expenses
├── feature:income
├── feature:accounts
├── feature:analytics
├── feature:settings
└── feature:splash
```

- `:app` собирает зависимости, корневую навигацию и Android-интеграции.
- `:feature:*` содержит Compose-экраны, ViewModel, UI state, intents и одноразовые effects.
- `:domain:*` содержит бизнес-контракты и use cases без Android-зависимостей.
- `:data:*` реализует контракты: Room, Retrofit, WorkManager, DataStore, шифрованное хранилище и
  мапперы.
- `:core:*` содержит переиспользуемые модели, дизайн-систему и общие утилиты.

Зависимости направлены внутрь: `app -> feature/data`, `feature -> domain/core`,
`data -> domain/core`.

## Стек

- Kotlin, Coroutines и Flow.
- Jetpack Compose и Material 3.
- Navigation 3 для корневой навигации.
- Metro для dependency injection и создания ViewModel.
- Room для локальной базы данных, Retrofit для API и WorkManager для фоновой синхронизации.
- DataStore и AndroidX Security Crypto для настроек и защищённого хранения PIN verifier.
- Vico для графиков и Lottie для splash-анимации.
- Kotlin Serialization и KSP для сериализации и генерации кода.
- JUnit, Compose UI Test, Detekt, ktlint и Android Lint для проверки качества.

## Локальная конфигурация

Для запросов к CashEye API создайте файл `local/api_key.txt` и поместите в него API-ключ без
кавычек. Каталог `local/` исключён из Git, поэтому ключ не попадёт в репозиторий.

Курсы валют загружаются из Frankfurter и отдельный ключ для них не нужен.

## Сборка и установка

Требования: JDK 21, Android SDK и устройство или эмулятор с Android 8.0 (API 26) и выше.

### Debug

#### Windows PowerShell

```powershell
.\gradlew assembleDebug
.\gradlew installDebug
```

#### macOS / Linux

```bash
chmod +x gradlew # требуется только при первом запуске, если файл не исполняемый
./gradlew assembleDebug
./gradlew installDebug
```

APK находится в `app/build/outputs/apk/debug/app-debug.apk`. Debug-версия имеет отдельный package
name `com.yandex.school.casheye.debug`, поэтому её можно установить рядом с release-версией.

### Release

#### Windows PowerShell

```powershell
.\gradlew assembleRelease
.\gradlew installRelease
```

#### macOS / Linux

```bash
./gradlew assembleRelease
./gradlew installRelease
```

APK находится в `app/build/outputs/apk/release/app-release.apk`. Для release включён R8: он удаляет
неиспользуемые код и ресурсы и выполняет обфускацию.

Для публичных релизов нужен отдельный keystore. GitHub Actions получает его и пароли только из
GitHub Secrets, собирает подписанный APK и публикует вместе с SHA-256. Подробная инструкция для
настройки первого релиза — в [docs/RELEASING.md](docs/RELEASING.md).

При смене ключа подписи Android не устанавливает новую версию поверх уже установленной версии с
другим ключом: старое приложение потребуется удалить один раз.

## Проверка качества

В проекте есть unit- и instrumented-тесты, включая UI-тесты настройки темы и ввода PIN-кода.
Полезные команды:

#### Windows PowerShell

```powershell
.\gradlew testDebugUnitTest
.\gradlew connectedDebugAndroidTest
.\gradlew detekt
.\gradlew ktlintCheck
.\gradlew lintDebug
```

#### macOS / Linux

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew detekt
./gradlew ktlintCheck
./gradlew lintDebug
```

## Участие и безопасность

Пожалуйста, прочитайте [CONTRIBUTING.md](CONTRIBUTING.md) перед первым pull request. Обычные
ошибки и предложения принимаются через Issues; уязвимости не публикуйте в открытом issue — порядок
сообщения описан в [SECURITY.md](SECURITY.md).

## Лицензия

Проект распространяется по [MIT License](LICENSE).
