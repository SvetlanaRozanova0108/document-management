# Запуск сервиса и мониторинг по логам


## Общее описание

Сервис управления документами предоставляет API для создания, согласования и утверждения документов. В сервисе работают фоновые процессы (воркеры), которые автоматически обрабатывают документы.

## 🚀 Запуск сервиса

### 1. Предварительные требования

Перед запуском убедитесь, что у вас установлены:

    Java 17 или выше

    Maven 3.8+ (или используйте ./mvnw)

    Docker (для запуска PostgreSQL)

    PostgreSQL (если не используете Docker)

### 2. Запуск базы данных

Через Docker Compose:

    Запустите: docker-compose up -d

### 3. Настройка конфигурации

Файл src/main/resources/application.yml уже содержит все необходимые настройки.

### 4. Сборка проекта

    Очистка и сборка: ./mvnw clean package

### 5. Запуск сервиса

    Через Maven: ./mvnw spring-boot:run

## 📊 Мониторинг прогресса по логам

### 1. Логи запуска сервиса

    2026-03-13 10:00:00 - Starting DocumentServiceApplication using Java 17
    2026-03-13 10:00:01 - No active profile set, falling back to 1 default profile: "default"
    2026-03-13 10:00:02 - Initializing Liquibase...
    2026-03-13 10:00:03 - Successfully applied 3 changelogs to database
    2026-03-13 10:00:04 - Started DocumentServiceApplication in 4.5 seconds
    2026-03-13 10:00:04 - Tomcat started on port 8080

### 2. Логи создания документов через API

    2026-03-13 10:05:23 - Creating new document with title: Тестовый документ
    2026-03-13 10:05:23 - Generated document number: DOC-20260313-000001
    2026-03-13 10:05:23 - Document created successfully. ID: 1, Number: DOC-20260313-000001, Time: 15ms

### 3. Логи SUBMIT-worker

    SUBMIT-WORKER ЗАПУЩЕН в 2026-03-13 10:06:00.123

    Статистика на момент запуска:
    Всего документов в статусе DRAFT: 450

    [ПАЧКА #1] Обработка начата
    Документов в пачке: 100 (ID: 1..100)
    Прогресс: 100/450 (22.2%)
    Осталось обработать: 350

    [ПАЧКА #1] Завершена
    Успешно отправлено: 100, Ошибок: 0
    Время выполнения: 778 мс

    SUBMIT-WORKER ЗАВЕРШЕН. Обработано: 100 документов

### 4. Логи APPROVE-worker

    APPROVE-WORKER ЗАПУЩЕН в 2026-03-13 10:07:30.456

    Статистика на момент запуска:
    Всего документов в статусе SUBMITTED: 380

    [ПАЧКА #1] Обработка начата
    Документов в пачке: 100 (ID: 101..200)
    Прогресс: 100/380 (26.3%)

    [ПАЧКА #1] Завершена
    Успешно утверждено: 98
    Ошибок статуса: 1
    Ошибок регистрации: 1
    Номера регистрации: REG-20260313-001, REG-20260313-002, ...

### 5. Логи пакетных операций

    2026-03-13 10:08:15 - Starting batch submit for 5 documents
    2026-03-13 10:08:15 - Document 1 processed with status: SUCCESS
    2026-03-13 10:08:15 - Document 2 processed with status: CONFLICT
    2026-03-13 10:08:15 - Document 3 processed with status: SUCCESS
    2026-03-13 10:08:15 - Document 4 processed with status: SUCCESS
    2026-03-13 10:08:15 - Document 5 processed with status: NOT_FOUND
    2026-03-13 10:08:15 - Batch submit completed. Total: 5, Success: 3, Time: 45ms

### 6. Логи ошибок

    2026-03-13 10:09:00 - ERROR Unexpected error processing document ID: 999
    java.sql.SQLException: Connection refused
    at com.itq.document.service.DocumentService.submitDocuments(DocumentService.java:123)

    2026-03-13 10:09:01 - WARN Document 2: CONFLICT - Cannot submit document in status APPROVED

