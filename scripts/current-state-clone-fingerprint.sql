\set ON_ERROR_STOP on
\pset tuples_only on
\pset format unaligned
\pset fieldsep '|'

BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '120s';
SET LOCAL lock_timeout = '5s';

SELECT 'SCHEMA_TABLE_COUNT', COUNT(*)
FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE';

SELECT 'TM_TABLE_COUNT', COUNT(*)
FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name LIKE 'tm\_%' ESCAPE '\';

SELECT format(
    'SELECT %L, COUNT(*) FROM %I.%I;',
    'TABLE_ROW_COUNT|' || table_name,
    table_schema,
    table_name
)
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name LIKE 'tm\_%' ESCAPE '\'
ORDER BY table_name
\gexec

SELECT 'TABLE_COLUMN_COUNT|' || table_name, COUNT(*)
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name LIKE 'tm\_%' ESCAPE '\'
GROUP BY table_name
ORDER BY table_name;

SELECT 'INDEX_COUNT|' || tablename, COUNT(*)
FROM pg_indexes
WHERE schemaname = 'public' AND tablename LIKE 'tm\_%' ESCAPE '\'
GROUP BY tablename
ORDER BY tablename;

SELECT 'CONSTRAINT_COUNT|' || c.relname || '|' || con.contype, COUNT(*)
FROM pg_constraint con
JOIN pg_class c ON c.oid = con.conrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relname LIKE 'tm\_%' ESCAPE '\'
GROUP BY c.relname, con.contype
ORDER BY c.relname, con.contype;

SELECT 'SEQUENCE_STATE|' || sequencename,
       COALESCE(last_value::text, 'NULL')
FROM pg_sequences
WHERE schemaname = 'public'
ORDER BY sequencename;

SELECT 'FLYWAY_HISTORY|' || installed_rank || '|' || COALESCE(version, 'NULL') || '|'
       || description || '|' || success || '|' || COALESCE(checksum::text, 'NULL'),
       1
FROM flyway_schema_history
ORDER BY installed_rank;

COMMIT;
