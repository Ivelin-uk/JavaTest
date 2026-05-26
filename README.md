# User Admin Panel

Проектът е разделен на:
- `backend/` - Spring Boot REST API (без templates/views)
- `frondend/` - всички изгледи (HTML/CSS/JS)

## Backend (само endpoints)

Backend работи с таблица `users` и поддържа:
- създаване на потребител
- изтриване на потребител
- смяна на парола
- смяна на роля

### API endpoints

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}/role`
- `PUT /api/users/{id}/password`
- `DELETE /api/users/{id}`

## Стартиране

### 1) Backend

```bash
cd backend
DB_URL="jdbc:mysql://127.0.0.1:3306/testdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
DB_USERNAME="root" \
DB_PASSWORD="" \
mvn spring-boot:run
```

Backend URL:
- `http://localhost:8081`

### 2) Frontend

```bash
cd frondend
python3 -m http.server 5500
```

Frontend URL:
- `http://localhost:5500`

## Бележки

- В backend **няма** `templates` и няма server-side view rendering.
- Полето `password_hash` се записва с BCrypt hash.
