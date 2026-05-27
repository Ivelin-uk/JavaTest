# Система за тестове (MVC: Spring Boot + React)

## Структура
- `backend/` - Spring Boot REST API (само endpoints, без templates/views)
- `frondend/` - React (Vite) frontend

## Реализирано в момента
- Регистрация: `STUDENT` / `TEACHER`
- Вход (login с username или email)
- `GET /api/auth/me`
- Админ панел (ако влезеш като `ADMIN`)
- Управление на потребители:
  - създаване
  - изтриване
  - смяна на роля
  - смяна на парола
  - активиране/деактивиране

## База данни (MAMP/MySQL)
`users` таблицата се създава автоматично при старт на backend, ако липсва.

По подразбиране backend е настроен за MAMP:
- host: `127.0.0.1`
- port: `3306`
- database: `testdb`
- user: `root`
- password: *(празна)*

## Стартиране

### 1) Backend (Terminal tab 1)
```bash
cd "/Users/ivelindilqnovmihaylov/Desktop/Дипломна работа/Project/backend"
mvn spring-boot:run
```

Ако получиш `Access denied for user 'root'`:
```bash
cd "/Users/ivelindilqnovmihaylov/Desktop/Дипломна работа/Project/backend"
DB_URL="jdbc:mysql://127.0.0.1:3306/testdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
DB_USERNAME="root" \
DB_PASSWORD="" \
mvn spring-boot:run
```

### 2) Frontend (Terminal tab 2)
```bash
cd "/Users/ivelindilqnovmihaylov/Desktop/Дипломна работа/Project/frondend"
npm install
npm run dev
```

## URLs
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8081`

## Първи вход
Ако таблицата `users` е празна, backend създава автоматично admin потребител:
- username: `admin`
- password: `admin12345`
