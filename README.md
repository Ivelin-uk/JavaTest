# User Management System (MVC)

Пълен пример за проект с:
- `backend` (Spring Boot, MVC архитектура, REST API, MySQL)
- `frondend` (самостоятелен frontend с JavaScript MVC подход)

## Какво е реализирано

### Backend (`backend/`)
- `Model`: `User`
- `Repository`: `UserRepository` (JdbcTemplate, CRUD)
- `Service`: `UserService` + `UserServiceImpl` (бизнес логика, валидация)
- `Controller`:
  - `PageController` за server-side MVC страница (Thymeleaf)
  - `UserController` за REST API
- `GlobalExceptionHandler` за централизирани API грешки
- `schema.sql` и `data.sql` за инициализация на БД

### Frontend (`frondend/`)
- Отделни JavaScript класове:
  - `UserModel` (достъп до API)
  - `UserView` (DOM визуализация)
  - `UserController` (управление на събития/потоци)
- CRUD операции: списък, добавяне, редакция, изтриване

## Изисквания

- Java 17+
- Maven 3.9+
- MySQL 8+

## Конфигурация на базата данни

По подразбиране backend ползва:
- URL: `jdbc:mysql://localhost:3306/testdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- Username: `root`
- Password: празна

Може да ги промениш с:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## Стартиране на backend

```bash
cd backend
mvn spring-boot:run
```

След старт:
- MVC страница: `http://localhost:8081/`
- REST API: `http://localhost:8081/api/users`

## Стартиране на frontend

Отделният frontend е в `frondend/`.

Вариант 1 (препоръчително):
```bash
cd frondend
python3 -m http.server 5500
```
Отвори: `http://localhost:5500`

Вариант 2:
- отвори `frondend/index.html` директно в браузър

## REST API endpoints

- `GET /api/users` - всички потребители
- `GET /api/users/{id}` - потребител по ID
- `POST /api/users` - създаване
- `PUT /api/users/{id}` - редакция
- `DELETE /api/users/{id}` - изтриване

Пример за `POST`:

```json
{
  "username": "new_user"
}
```
