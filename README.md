# Питомец Финни

Прототип Android-приложения по финансовой грамотности для детей 7–11 лет
(хакатон Департамента финансов Москвы). Ребёнок заботится о феньке Финни: получает игровые
монеты, планирует неделю, покупает нужное и желаемое, копит на мечту и решает задания.
Реальных денег, рекламы, аккаунтов и сбора данных нет.

Подробная документация: [`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md) (архитектура, данные,
формулы, матрица требований, карта контента, тест-кейсы). Черновик карточки RuStore:
[`docs/RUSTORE.md`](docs/RUSTORE.md). Игровая логика подробно: [`GAMEPLAY.md`](GAMEPLAY.md),
визуальный стиль: [`DESIGN.md`](DESIGN.md).

## Версия

| | |
|---|---|
| Package name | `ru.finny.petgame` |
| Версия (versionName) | 1.0 |
| Номер сборки (versionCode) | 1 |
| Android | 8.0+ (minSdk 26), targetSdk 37, только портретная ориентация |

## Стек и окружение

- Kotlin 2.2.10, Jetpack Compose (BOM 2026.02.01, Material 3), Room 2.8.5 (SQLite), KSP.
- Android Gradle Plugin 9.4.0, Gradle 9.6.0 (wrapper в репозитории), JDK 17+.
- Android Studio актуальной версии с Android SDK Platform 37.
- Тесты: JUnit 4, Robolectric 4.17, kotlinx-coroutines-test.

Сервера, облака и ИИ в приложении нет. Всё работает без интернета.

## Быстрый запуск (debug)

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Или откройте проект в Android Studio и
запустите конфигурацию `app`.

## Сборка подписанного release APK

Ключ подписи в репозитории не хранится. Один раз создайте ключ вне репозитория:

```bash
keytool -genkeypair -v -keystore ~/finny-release.jks -alias finny \
  -keyalg RSA -keysize 2048 -validity 10000
```

Затем укажите его одним из двух способов.

1. Файл `keystore.properties` в корне проекта (он в `.gitignore`):

   ```properties
   storeFile=/Users/<вы>/finny-release.jks
   storePassword=<пароль хранилища>
   keyAlias=finny
   keyPassword=<пароль ключа>
   ```

2. Или переменные окружения: `FINNY_KEYSTORE_FILE`, `FINNY_KEYSTORE_PASSWORD`,
   `FINNY_KEY_ALIAS`, `FINNY_KEY_PASSWORD`.

Сборка и проверка подписи:

```bash
./gradlew assembleRelease
$ANDROID_HOME/build-tools/<версия>/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Готовый файл `app/build/outputs/apk/release/app-release.apk` ставится на телефон без среды
разработки (`adb install app-release.apk` или просто открыть файл на устройстве). AAB для
RuStore: `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.

Без ключа `assembleRelease` соберёт неподписанный APK, который не установится.

## Тесты

```bash
./gradlew testDebugUnitTest
```

109 автотестов: план бюджета, покупки, копилка и снятие, срок цели, итоги недели, рост питомца,
загрузка контента и сценарии `GameRepository` на Room (Robolectric).

## Демо для экспертов

1. Пройдите знакомство, создайте профиль и питомца.
2. В разделе **Для взрослого** решите пример и включите **Демо-режим**: все задания всех
   5 недель открыты сразу, без ожидания дней.
3. Пройдите цикл: план → задание → покупки → копилка → «Завершить неделю». Следующая неделя
   начинается сразу, так можно пройти все 5 недель подряд.
4. Для проверки покупок и мечт в демо-режиме есть кнопка **Начислить монеты**. Начисление
   подписано в журнале как «Демо-монеты».
5. **Сбросить тестовый профиль** (раздел «Для взрослого», с подтверждением) удаляет все данные и
   возвращает игру к первому запуску.

Если есть два устройства, сценарий можно пройти на обоих: данные хранятся только локально.

## Разрешения и данные

| Разрешение | Зачем |
|---|---|
| `android.permission.VIBRATE` | Лёгкая вибрация в такт листопаду при запуске и переходах. Выключается в настройках («Вибрация»). Опасным разрешением не является, диалог у пользователя не запрашивается. |

Интернета, камеры, микрофона, геолокации, контактов и Bluetooth приложение не просит.

Данные профиля (игровое имя, внешний вид питомца, монеты, покупки, план, копилка, задания)
хранятся только на устройстве: база Room `finny_pet.db` и настройки `finny_settings`
(звук, тема, вибрация, анимации). Облачная копия и перенос на другое устройство отключены
(`allowBackup="false"`, `backup_rules.xml`, `data_extraction_rules.xml`).

Удаление: раздел «Для взрослого» → «Сбросить тестовый профиль» → подтверждение. Также все данные
удаляются вместе с приложением или через «Настройки Android → Приложения → Питомец Финни →
Очистить данные».

## Структура репозитория

- `app/src/main/java/ru/finny/petgame/economy/` — расчёты бюджета, покупок, копилки, итогов недели и
  роста питомца (без Android-зависимостей).
- `app/src/main/java/ru/finny/petgame/content/` и `app/src/main/assets/content/*.json` — учебный
  контент: задания, товары, цели, недели.
- `app/src/main/java/ru/finny/petgame/data/` — Room и `GameRepository`.
- `app/src/main/java/ru/finny/petgame/ui/` — экраны и компоненты Compose.
- `app/src/test/` — автотесты.
- `tools/` — скрипты, которыми сгенерированы музыка и звуки.
- `docs/` — документация и материалы для RuStore.

## Лицензии и происхождение материалов

| Материал | Происхождение и права |
|---|---|
| Шрифт Nunito (`res/font/nunito.ttf`) | SIL Open Font License 1.1, текст лицензии в `res/raw/nunito_ofl.txt` |
| Звуки заданий (`res/raw/sfx_*.wav`) | Синтезированы командой без сэмплов: `python3 tools/sounds/task_sounds.py app/src/main/res/raw` |
| Фоновая музыка (`res/raw/music_forest.ogg`) | Синтезирована командой скриптом `tools/music/forest_theme.py` |
| Фото заставки (`drawable-nodpi/splash_day/evening/night.jpg`) | Предоставлены командой |
| Спрайт Финни (`drawable-nodpi/pet_fennec*.png`), иконка приложения | Изображение предоставлено командой, фон вырезан |
| Фоны леса (`scene_forest_*.jpg`) и предметы двора (`scene_*.png`) | Сгенерированы для проекта средствами генерации изображений |
| Аксессуары и одежда Финни | Нарисованы кодом в `ui/components/PetSprite.kt` |
| Библиотеки AndroidX, Jetpack Compose, Room, Kotlin | Apache License 2.0 |
| JUnit 4 | Eclipse Public License 1.0 (только тесты) |
| Robolectric | MIT (только тесты) |
