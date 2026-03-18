# Анализ поисковых запросов и индексов

## Общее описание

В этом документе представлен анализ производительности поисковых запросов к таблице documents с использованием EXPLAIN ANALYZE, а также рекомендации по оптимизации с помощью индексов.

## Пример поискового запроса

    SELECT *
    FROM documents
    WHERE status = 'DRAFT'
    AND author ILIKE '%Иванов%'
    AND created_at BETWEEN '2026-01-01' AND '2026-03-13';

    Что делает запрос:
    Ищет документы в статусе DRAFT
    Автор содержит подстроку "Иванов"
    Документы созданы в период с 1 января 2026 по 13 марта 2026

## EXPLAIN ANALYZE

### Запрос с индексами

    EXPLAIN (ANALYZE, BUFFERS)
    SELECT *
    FROM documents
    WHERE status = 'DRAFT'
    AND author ILIKE '%Иванов%'
    AND created_at BETWEEN '2026-01-01' AND '2026-03-13';

    Результат:

    Seq Scan on documents  (cost=0.00..4133.00 rows=1 width=99) (actual time=72.337..72.338 rows=0 loops=1)
    Filter: (((author)::text ~~* '%Иванов%'::text) AND (created_at >= '2026-01-01 00:00:00'::timestamp without time zone) AND (created_at <= '2026-03-13 00:00:00'::timestamp without time zone) AND ((status)::text = 'DRAFT'::text))
    Rows Removed by Filter: 100000
    Buffers: shared hit=2133
    Planning:
    Buffers: shared hit=5
    Planning Time: 0.099 ms
    Execution Time: 72.350 ms

## Индексы для оптимизации

### 1. Составной индекс для частых комбинаций фильтров

    Самый эффективный индекс:
    CREATE INDEX IF NOT EXISTS idx_documents_status_created_author
    ON documents(status, created_at, author);

    Статус (status) - высокая селективность (мало уникальных значений)
    Дата создания (created_at) - хорошая селективность для диапазонов
    Автор (author) - для финальной фильтрации

### 2. Индекс для поиска по статусу и дате

    CREATE INDEX IF NOT EXISTS idx_documents_status_created
    ON documents(status, created_at);

### 3. Индекс для поиска по статусу

    CREATE INDEX IF NOT EXISTS idx_documents_status
    ON documents(status);

### 4. Индекс для поиска по автору

    CREATE EXTENSION IF NOT EXISTS pg_trgm;

    CREATE INDEX IF NOT EXISTS idx_documents_author_trgm
    ON documents USING gin (author gin_trgm_ops);

### 5. Индекс для поиска по дате

    CREATE INDEX IF NOT EXISTS idx_documents_created_at
    ON documents(created_at);

## Полный набор индексов из Liquibase:

    <changeSet id="create-indexes" author="system">
        <!-- Индекс для поиска по статусу -->
        <createIndex indexName="idx_documents_status" tableName="documents">
            <column name="status"/>
        </createIndex>
    
        <!-- Индекс для поиска по дате создания -->
        <createIndex indexName="idx_documents_created_at" tableName="documents">
            <column name="created_at"/>
        </createIndex>
        
        <!-- Индекс для автора (для точного поиска) -->
        <createIndex indexName="idx_documents_author" tableName="documents">
            <column name="author"/>
        </createIndex>

        <!-- Индекс для поиска по статусу и дате -->
        <createIndex indexName="idx_documents_status_created" tableName="documents">
            <column name="status"/>
            <column name="created_at"/>
        </createIndex>

        <!-- Составной индекс для частых комбинаций фильтров -->
        <createIndex indexName="idx_documents_status_created_author" tableName="documents">
            <column name="status"/>
            <column name="created_at"/>
            <column name="author"/>
        </createIndex>
    </changeSet>

### Проверка EXPLAIN после добавления индексов:

    EXPLAIN (ANALYZE, BUFFERS)
    SELECT *
    FROM documents
    WHERE status = 'DRAFT'
    AND author ILIKE '%Иванов%'
    AND created_at BETWEEN '2026-01-01' AND '2026-03-13';

    Результат:
    
    Index Scan using idx_documents_created_at on documents  (cost=0.29..8.32 rows=1 width=99) (actual time=0.004..0.005 rows=0 loops=1)
    Index Cond: ((created_at >= '2026-01-01 00:00:00'::timestamp without time zone) AND (created_at <= '2026-03-13 00:00:00'::timestamp without time zone))
    Filter: (((author)::text ~~* '%Иванов%'::text) AND ((status)::text = 'DRAFT'::text))
    Buffers: shared hit=2
    Planning:
    Buffers: shared hit=43 read=5
    Planning Time: 0.573 ms
    Execution Time: 0.019 ms

## 🎯 Выводы

    1. Без индексов поиск выполняется 72 мс
    2. С индексами время выполнения менее 5 мс

