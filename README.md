# Smart Test - Интелигентна платформа за онлайн тестове

Проектът е реализиран по MVC принцип с отделен frontend и backend.

## Технологии
- Frontend: React + Vite
- Backend: Spring Boot + Spring Security + JDBC
- Database: MySQL (MAMP)

## Структура
- `backend/` - REST API (без templates)
- `frondend/` - UI приложение

## Реализирани функционалности (MVP)
- Регистрация и вход в системата (`STUDENT` / `TEACHER`, bootstrap `ADMIN`)
- Управление на потребители и роли (admin):
  - създаване/изтриване
  - смяна на роля
  - смяна на парола
  - активиране/деактивиране
- Номенклатури за роли и достъпи:
  - `roles`
  - `access_objects`
  - `role_access`
- Динамична role-based защита на контролерите:
  - `/api/users/**` -> `ADMIN_PANEL`
  - `/api/teacher/**` -> `TEACHER_PANEL`
  - `/api/teacher/reports/**` -> `REPORTS`
  - `/api/student/**` -> `STUDENT_PANEL`
- Админ конфигурация на права:
  - матрица `Role -> Access Object` (в UI)
  - активиране/деактивиране на роли (без `ADMIN`)
- Създаване и редакция на тестове (teacher/admin)
- Ръчно добавяне на въпроси и отговори
- Генериране на въпроси чрез AI endpoint (симулирана AI генерация)
- Задаване на тест към ученик
- Задаване на тест към група ученици
- Решаване на тест с визуализация на един въпрос на страница
- Автоматично оценяване и изчисляване на резултат
- Записване на резултати и история
- Справки и статистики за teacher/admin
- Контрол при смяна на tab/blur:
  - текущият въпрос се маркира като грешен
  - записва се нарушение
  - преминава към следващ въпрос
- Адаптивен интерфейс за desktop/mobile

## Database schema (основни таблици)
- `users`
- `roles`, `access_objects`, `role_access`
- `subjects`
- `tests`
- `questions`
- `question_options`
- `student_groups`
- `group_members`
- `test_assignments`
- `test_attempts`
- `attempt_answers`

## Стартиране

### 1) Увери се, че MySQL работи (MAMP)
- host: `127.0.0.1`
- port: `3306`
- db: `testdb`
- user: `root`
- password: празна

### 2) Backend (tab 1)
```bash
cd "/Users/ivelindilqnovmihaylov/Desktop/Дипломна работа/Project/backend"
DB_URL="jdbc:mysql://127.0.0.1:3306/testdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" DB_USERNAME="root" DB_PASSWORD="" mvn spring-boot:run
```

### 3) Frontend (tab 2)
```bash
cd "/Users/ivelindilqnovmihaylov/Desktop/Дипломна работа/Project/frondend"
npm install
npm run dev
```

## URLs
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8081`

## Ако backend не тръгва
- Ако пише `Port 8081 was already in use`:
```bash
lsof -ti :8081 | xargs kill -9
```
- Ако пише `Access denied for user 'root'@'localhost'`:
  - провери `DB_USERNAME` / `DB_PASSWORD`
  - използвай `127.0.0.1` вместо `localhost`

## Първи достъп
Ако таблицата `users` е празна, backend създава admin:
- username: `admin`
- password: `admin12345`

## Роли и потоци
- `ADMIN`: администраторски панел за потребители
- `TEACHER`: предмети, тестове, въпроси, групи, задания, справки
- `STUDENT`: зададени тестове, решаване, резултати
