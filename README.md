<h1> Разработка Системы Управления Банковскими Картами</h1>

<h2>Описание задачи</h2>
  <p>Разработать backend-приложение на Java (Spring Boot) для управления банковскими картами:</p>
  <ul>
    <li>Система должна обеспечивать:</li>
     <li>Создание, управление и просмотр данных о банковских картах/li>
     <li>Выполнение операций между картами пользователя</li>
  </ul>

<h2>Карта должна содержать следующие атрибуты:</h2>
  <ul>
    <li>Номер карты (зашифрован, отображается маской: <code>**** **** **** 1234</code>)</li>
    <li>Владелец</li>
    <li>Срок действия</li>
    <li>Статус: ACTIVE, BLOCKED, EXPIRED</li>
    <li>Баланс</li>
  </ul>

<h2>Функциональные требования:</h2>

<h3> Аутентификация и авторизация</h3>
  <ul>
    <li>Spring Security + JWT</li>
    <li>Роли: ADMIN и USER</li>
  </ul>

<h3>Возможности программы:</h3>
<strong>Администратор:</strong>
  <ul>
    <li>Создаёт, блокирует, активирует, удаляет карты</li>
    <li>Управляет пользователями</li>
    <li>Видит все карты</li>
  </ul>

<strong>Пользователь:</strong>
  <ul>
    <li>Просматривает свои карты (поиск + пагинация)</li>
    <li>Запрашивает блокировку карты</li>
    <li>Делает переводы между своими картами</li>
    <li>Смотрит баланс</li>
  </ul>

<h3>Возможности API:</h3>
  <ul>
    <li>CRUD для карт</li>
    <li>Переводы между своими картами</li>
    <li>Фильтрация и постраничная выдача</li>
    <li>Валидация и сообщения об ошибках</li>
  </ul>

<h3>Безопасность:</h3>
  <ul>
    <li>Шифрование данных</li>
    <li>Ролевой доступ</li>
    <li>Маскирование номеров карт</li>
  </ul>

<h3>Работа с БД:</h3>
  <ul>
    <li>MySQL</li>
    <li>Миграции через Liquibase</li>
  </ul>

<h3>Документация:</h3>
  <ul>
    <li>Swagger UI</li>
    <li>README.md с инструкцией запуска</li>
  </ul>

<h3>Быстрый старт (локально)</h3>
<ul>
<li>Поднимите MySQL:

   docker run --name mysql-bank -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=bankcards -p 3306:3306 -d mysql:8</li>
<li>Запустите приложение:
   mvn spring-boot:run</li>
<li>Swagger UI: http://localhost:8080/swagger-ui/index.html</li>
</ul>

<h3>Переменные конфигурации</h3>
<ul>
<li>См. src/main/resources/application.yml:</li>
<li>* spring.datasource.* — строка подключения к MySQL.</li>
<li>* app.security.jwt-secret — секрет для HS256 (смените в проде).</li>
<li>* app.crypto.aes-key-base64 — Base64ключ AES (хранить в секретах).</li>
</ul>

<h3>Создание администратора (dev)</h3>
<ul>
<li>insert into users(username,password) values('admin','$2a$10$KqLyn6mGfaQxwzq6tKQ7JOW2j6n7D9zv0wJ0cS9qgC7wC3zq1pZt2'); -- пароль: admin123</li>
<li>insert into user_roles(user_id,role) values(1,'ADMIN'),(1,'USER');</li>
</ul>

<h3>Докеризация</h3>
<ul>
<li>Сборка и запуск:</li>
<li>docker compose up --build</li>
<li>Приложение: http://localhost:8080, БД: порт 3306 (пользователь root/root, БД bankcards).</li>
</ul>

<h3>Типичный сценарий</h3>
<ul>
<li>POST /api/auth/register ? получить JWT.</li>
<li>POST /api/auth/login ? получить JWT.</li>
<li>ADMIN: POST /api/cards — создать карту пользователю.</li>
<li>USER: GET /api/cards — увидеть свои карты.</li>
<li>USER: POST /api/transfers — перевод между своими картами.</li>
</ul>

<h3>Тесты</h3>
<ul>
<li>mvn -q -Dtest=*ServiceTest test</li>
</ul>

<h3>Безопасность</h3>
<ul>
<li>JWT через заголовок Authorization: Bearer "token".</li>
<li>Номера карт шифруются (AESGCM), наружу — только маска.</li>
<li>Доступ по ролям и проверка владения ресурсами в сервисах/контроллерах.</li>
</ul>

<h3>Быстрый старт:</h3>
  <ul>
    <li>Docker Compose для dev-среды</li>
    <li>Liquibase миграции</li>
    <li>Юнит-тесты ключевой бизнес-логики</li>
  </ul>

<h2>Технологии:</h2>
  <p>
    Java 21, Spring Boot, Spring Security, Spring Data JPA, MySQL, Liquibase, Docker, JWT, Swagger (OpenAPI)
  </p>