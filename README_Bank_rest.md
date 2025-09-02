# Система управления банковскими картами — Bank REST

Java 21 + Spring Boot приложение для управления банковскими картами с JWT-аутентификацией, ролями `ADMIN/USER`, шифрованием номеров карт и документацией OpenAPI.

---

## Основные возможности

### Администратор

* Создание, активация, блокировка, удаление карт.
* Управление пользователями.
* Просмотр всех карт с фильтрами и пагинацией.

### Пользователь

* Просмотр **своих** карт (поиск по последним 4 цифрам, фильтр по статусу, пагинация).
* Запрос блокировки своей карты.
* Переводы между **своими** картами.
* Просмотр баланса.

---

## Атрибуты карты

* **Номер карты** — хранится **зашифрованным** (AES-GCM), наружу отдается маской `**** **** **** 1234`.
* **Владелец** — связь с пользователем.
* **Срок действия** — строка вида `MM/YY` (например, `12/29`).
* **Статус** — `ACTIVE`, `BLOCKED`, `EXPIRED`.
* **Баланс** — decimal.

---

## Технологии

* Java 21, Spring Boot, Spring Web, Spring Data JPA, Spring Security.
* JWT (jjwt).
* MySQL (или PostgreSQL), HikariCP.
* Liquibase (миграции).
* Swagger / OpenAPI (springdoc).
* Docker / Docker Compose.
* Unit-тесты (JUnit, Mockito).

---

## Структура проекта

```
src/main/java/com/example/bankcards/
  config/            # Конфигурации
    OpenApiConfig.java
    SecurityConfig.java
  controller/        # REST-контроллеры
    AuthController.java
    CardController.java
    TransferController.java
  dto/               # DTO
    auth/ (AuthResponse, LoginRequest, RegisterRequest)
    card/ (CardCreateRequest, CardResponse)
    error/ (ApiError)
    transfer/ (TransferRequest)
  entity/            # JPA-сущности
    Card.java
    CardStatus.java
    Role.java
    Transfer.java
    User.java
  exception/         # Исключения и глобальный обработчик
    BadRequestException.java
    ForbiddenException.java
    GlobalExceptionHandler.java
    NotFoundException.java
  repository/        # Spring Data JPA
    CardRepository.java
    TransferRepository.java
    UserRepository.java
  security/          # Безопасность
    BankUserDetailsService.java
    JwtAuthFilter.java
    JwtService.java
  service/           # Бизнес-логика
    CardService.java
    TransferService.java
    UserService.java
  util/              # Утилиты
    CryptoUtil.java
    DateUtil.java
    MaskingUtil.java

src/main/resources/
  application.yml
  db/migration/
    001-users.yaml
    002-cards.yaml
    003-transfers.yaml
    004-seed-dev.yaml
  db.changelog-master.yaml
  static/docs/
    openapi.yaml
```

> Миграции: `src/main/resources/db/migration`, мастер-чейнджлог: `db.changelog-master.yaml`.

---

## Безопасность

* **JWT** в заголовке:
  `Authorization: Bearer <jwt>`
* **Роли**: `ADMIN`, `USER`.
  В коде проверка через `@PreAuthorize("hasRole('ADMIN')")` (Spring ожидает `ROLE_ADMIN`; маппинг из `ADMIN` в `ROLE_ADMIN` выполняет `BankUserDetailsService` при построении `GrantedAuthority`).
* **Шифрование** номеров карт: AES-GCM (ключ в Base64 в `app.crypto.aes-key-base64`). Формат хранения: `Base64(iv):Base64(ciphertext)`.

---

## Конфигурация (пример `application.yml`)

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bankcards?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: GOlden
  jpa:
    hibernate:
      ddl-auto: validate #схему ведёт Liquibase, не Hibernate.
    properties:
      hibernate:
        format_sql: true
    open-in-view: false #предотвращаем N+1 ленивых загрузок на уровне web

  liquibase:
    change-log: classpath:db/migration/db.changelog-master.yaml
    contexts: dev

#dev-заглушки, в проде хранить в секретах
app:
  security:
    jwt-secret: "changeme-please-32-bytes-minimum-secret-key"
    jwt-exp-min: 120
  crypto:
    aes-key-base64: "3Ju/4BZDL+OaMu5wn4SkMqZ/85nZ72EVwuYoVu8kQsg="

server:
  port: 8080
logging:
  level:
    org.springframework.security: INFO




### Как сгенерировать ключи

* **AES ключ (16/24/32 байта) → Base64**:

    * Java: `Base64.getEncoder().encodeToString(SecureRandom(32 bytes))`
    * OpenSSL: `openssl rand -base64 32`
* **JWT secret**: любая случайная строка (например, 32+ байта Base64).

Добавьте переменные окружения:

```
APP_AES_KEY_BASE64=...  # например, M2Yd... (Base64)
APP_JWT_SECRET=...      # например, yW9... (Base64/строка)
```

---

## Запуск

### Локально (Maven)

```bash
# JDK 21, Maven 3.9+
mvn clean package
java -jar target/Bank_REST-*.jar
```

### Docker Compose (dev)

`docker-compose.yml` (пример):

```yaml
version: "3.8"
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: bankcards
      MYSQL_ROOT_PASSWORD: root
      MYSQL_USER: app
      MYSQL_PASSWORD: app
    ports:
      - "3306:3306"
    command: --default-authentication-plugin=mysql_native_password
  app:
    image: bank-rest:latest
    build: .
    depends_on: [db]
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/bankcards?useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: app
      SPRING_DATASOURCE_PASSWORD: app
      APP_AES_KEY_BASE64: <your_base64_aes_key>
      APP_JWT_SECRET: <your_jwt_secret>
    ports:
      - "8080:8080"
```

---

## Документация API

* Swagger UI: `http://localhost:8080/swagger-ui/index.html`
* OpenAPI JSON: `/v3/api-docs`
* Исходник OpenAPI: `src/main/resources/static/docs/openapi.yaml`

### Аутентификация

1. `POST /api/auth/register` — регистрация
2. `POST /api/auth/login` → **возвращает `token`**
3. Нажмите **Authorize** в Swagger UI → вставьте **только** `Bearer <token>` (без JSON).

### Карты

* `GET /api/cards` — список карт.
  Параметры:

    * `status` — `ACTIVE|BLOCKED|EXPIRED` (опц.)
    * `last4` — строка (опц.)
    * `page` — int (по умолчанию 0)
    * `size` — int (по умолчанию 10)
      **USER** видит только свои; **ADMIN** — все.

* `POST /api/cards` — **ADMIN**. Создать карту.

  ```json
  {
    "number": "1111222233334444",
    "expiry": "12/29",
    "initialBalance": 100.00,
    "ownerUsername": "user"
  }
  ```

* `GET /api/cards/{id}` — получить карту (ADMIN — любую; USER — только свою).

* `PATCH /api/cards/{id}/block` — блокировать:

    * USER может **только свою** (перед этим сервис проверяет владение).
    * ADMIN — любую.

* `PATCH /api/cards/{id}/activate` — **ADMIN**.

* `DELETE /api/cards/{id}` — **ADMIN**.

### Переводы

* `POST /api/transfers` — перевод между **своими** картами:

  ```json
  { "fromCardId": 100, "toCardId": 101, "amount": 50.00 }
  ```

### Валидация и ошибки

Глобальный обработчик возвращает единый формат:

```json
{
  "message": "text",
  "path": "/api/..",
  "timestamp": "..."
}
```

* 400 — валидация / бизнес-ошибка (BadRequest).
* 401 — неавторизован.
* 403 — доступ запрещен (нет роли/не владелец).
* 404 — не найдено.
* 500 — внутренняя ошибка.

---

## Хранение номера карты (шифрование)

`CryptoUtil` использует `AES/GCM/NoPadding`:

* IV = 12 байт (96 бит), случайный на каждое шифрование.
* Тег аутентичности = 128 бит.
* Формат: `Base64(iv):Base64(ciphertext)`.

> Важно: **ключ** в `app.crypto.aes-key-base64` должен быть 16/24/32 байта (AES-128/192/256) в Base64. Неправильная длина приведет к ошибке `Encrypt failed`.

---

## Работа с ленивой загрузкой (LAZY) и `owner`

Чтобы исключить `LazyInitializationException` при маппинге `Card → CardResponse`, репозиторий `CardRepository` помечен `@EntityGraph(attributePaths = "owner")` для методов выборки (включая `findAll`, `findById`, фильтры). Это гарантирует, что `card.getOwner().getUsername()` доступен вне транзакции.

---

## Роли и префикс `ROLE_`

* В БД роли хранятся как `ADMIN`/`USER`.
* `BankUserDetailsService` добавляет префикс `ROLE_` при построении `GrantedAuthority`.
* В аннотациях используем `@PreAuthorize("hasRole('ADMIN')")` — это проверит наличие `ROLE_ADMIN`.

> Регистр в `hasRole('ADMIN')` — используйте верхний (`ADMIN`) для однозначности.

---

## Миграции (Liquibase)

* Мастер-файл: `db.changelog-master.yaml`, подключает `001-004`.
* **Seed-данные** для dev — `004-seed-dev.yaml` (проверьте логины/роли/BCrypt-хеши).

> Если правили файл миграции, а Liquibase ругается на checksum:
>
> * либо откатить изменение,
> * либо обновить checksum через `liquibase clearCheckSums` (или удалить запись из `databasechangelog` в dev),
> * либо создать **новую** миграцию вместо правки старой.

---

## Примеры cURL

```bash
# Регистрация
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'

# Логин
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq -r .token)

# Получить карты (ADMIN видит все)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/cards?page=0&size=10&status=ACTIVE"

# Создать карту (ADMIN)
curl -X POST http://localhost:8080/api/cards \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"number":"1111222233334444","expiry":"12/29","initialBalance":100,"ownerUsername":"user"}'

# Блокировка карты
curl -X PATCH http://localhost:8080/api/cards/100/block \
  -H "Authorization: Bearer $TOKEN"
```

---

## Тестирование

* Юнит-тесты сервисов (CardService/TransferService/UserService):

    * валидация бизнес-правил (владение картой для USER, баланс/остаток, статусы).
    * шифрование/маскирование.
    * генерация JWT.
* Интеграционные тесты контроллеров (MockMvc) для ключевых сценариев.

---

## Частые проблемы и их решения

* **401 bad credentials** — неверный логин/пароль или BCrypt в БД не совпадает с `PasswordEncoder`. Проверьте `SecurityConfig.passwordEncoder()` и сиды.
* **403 при запросах** — роль не та. Убедитесь, что токен у пользователя с ролью `ADMIN` для админ-эндпоинтов. В Swagger **вставляйте только** `Bearer <token>`.
* **`Encrypt failed`** — неверная длина AES-ключа. Сгенерируйте корректный ключ и положите в `APP_AES_KEY_BASE64`.
* **`LazyInitializationException` на `owner`** — репозиторий карт должен использовать `@EntityGraph(attributePaths = "owner")`. Уже учтено.
* **Liquibase checksum error** — не правьте существующие миграции; добавляйте новые или сбрасывайте checksum в dev.

---

## Лицензия

Внутренний учебный проект.
