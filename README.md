# Masterly - Beauty Master Service

Микросервисное веб-приложение для управления салоном красоты. Состоит из 2 микросервисов:
- **Core Service** - бэкенд API (Spring Boot, JPA, PostgreSQL, Flyway, JWT)
- **Web Service** - веб-интерфейс (Spring Boot, Thymeleaf, Feign Client)

## Технологии

- Spring Boot 3.2.4
- Spring Security / JWT
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Flyway (миграции)
- Thymeleaf (шаблонизатор)
- Feign Client (взаимодействие сервисов)
- Lombok / MapStruct

### Структура проекта

    masterly-config/          # Родительский POM и общие настройки
    masterly-core-service/    # Бэкенд API (порт 8081)
    masterly-web-service/     # Веб-интерфейс (порт 8082)

### Требования

- Java 17
- PostgreSQL 17
- Maven (или встроенный mvnw)
- Git

### Установка и запуск

#### 1. Клонируйте репозитории

    git clone https://github.com/i-dler1/masterly-config.git
    git clone https://github.com/i-dler1/masterly-core-service.git
    git clone https://github.com/i-dler1/masterly-web-service.git

#### 2. Запустите PostgreSQL

#### 3. Установите родительский POM
    
    cd masterly-config
    mvn clean install

#### 4. Запустите Core Service

    cd masterly-core-service
    mvn spring-boot:run

#### 5. Запустите Web Service (в новом окне терминала)

    cd masterly-web-service
    mvn spring-boot:run

### Автоматическая инициализация

При первом запуске Core Service автоматически создаёт:
Пользователя БД masterly_user
Базу данных masterly_db
Все таблицы через Flyway миграции
