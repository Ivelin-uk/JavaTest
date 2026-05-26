# Проект: Spring Boot + MySQL (Потребители)

Това е уеб приложение със Spring Boot, което чете потребители от MySQL база данни и ги показва:
- в уеб страница
- през REST API

## Структура на проекта

- `backend/` - Spring Boot приложение
- `frondend/` - в момента е празна папка

## Технологии

- Java 17+
- Spring Boot 3.2.6
- Maven
- MySQL
- Thymeleaf

## Изисквания

Преди стартиране е необходимо да имаш инсталирани:
- Java 17 или по-нова
- Maven 3.9+
- MySQL (локално)

## Конфигурация на базата данни

Приложението по подразбиране използва:
- URL: `jdbc:mysql://localhost:3306/testdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- Потребител: `root`
- Парола: празна

Създай база и таблица:

```sql
CREATE DATABASE IF NOT EXISTS testdb;
USE testdb;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255) NOT NULL
);
```

Примерни данни (по желание):

```sql
INSERT INTO users (username) VALUES ('Ivan'), ('Maria'), ('Georgi');
```

## Стартиране на проекта

От главната директория на проекта:

```bash
cd backend
mvn spring-boot:run
```

## Достъп до приложението

- Уеб страница: `http://localhost:8081/`
- API endpoint: `http://localhost:8081/api/users/names`

## Промяна на DB настройките (по желание)

Можеш да подадеш променливи на средата:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Пример:

```bash
DB_URL="jdbc:mysql://localhost:3306/testdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
DB_USERNAME="root" \
DB_PASSWORD="" \
mvn spring-boot:run
```
