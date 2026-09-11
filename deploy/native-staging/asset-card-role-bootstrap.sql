\set ON_ERROR_STOP on
-- Definition only. Execution requires separately authorized administration of an exact target database.
-- Existing roles are verify-only. No old role/ACL/data is repaired or altered.
SELECT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='rine_asset_card_writer') AS card_role_exists \gset
\if :card_role_exists
  \ir asset-card-role-verify.sql
\else
BEGIN;
DO $card_bootstrap$
BEGIN
  -- PUBLIC grants cannot be denied to one role. Refuse, do not mutate historical PUBLIC ACLs.
  IF EXISTS(SELECT 1 FROM pg_database d, LATERAL aclexplode(COALESCE(d.datacl,acldefault('d',d.datdba))) a
       WHERE d.datname=current_database() AND a.grantee=0 AND (a.privilege_type IN ('CREATE','TEMPORARY') OR a.is_grantable))
     OR EXISTS(SELECT 1 FROM pg_namespace n, LATERAL aclexplode(COALESCE(n.nspacl,acldefault('n',n.nspowner))) a
       WHERE a.grantee=0 AND (a.privilege_type='CREATE' OR a.is_grantable))
     OR EXISTS(SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace,
       LATERAL aclexplode(COALESCE(c.relacl,acldefault(CASE WHEN c.relkind='S' THEN 'S'::"char" ELSE 'r'::"char" END,c.relowner))) a
       WHERE n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%'
       AND c.relkind IN ('r','p','v','m','f','S') AND a.grantee=0)
     OR EXISTS(SELECT 1 FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace,
       LATERAL aclexplode(a.attacl) permission WHERE permission.grantee=0
       AND n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%')
  THEN RAISE EXCEPTION 'PREEXISTING_PUBLIC_PRIVILEGE_CONFLICT'; END IF;
  -- A new NOINHERIT role still receives PUBLIC routine EXECUTE. Do not create it
  -- when a user-domain definer routine could delegate its owner's business access.
  IF EXISTS(SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace,
       LATERAL aclexplode(COALESCE(p.proacl,acldefault('f',p.proowner))) permission
       WHERE p.prosecdef AND n.nspname NOT IN ('pg_catalog','information_schema')
         AND permission.grantee=0 AND permission.privilege_type='EXECUTE')
  THEN RAISE EXCEPTION 'PREEXISTING_SECURITY_DEFINER_ACCESS'; END IF;
  IF EXISTS(SELECT 1 FROM (VALUES ('tm_asset_card_snapshot'),('tm_asset_card_spot_bar'),('tm_asset_card_feature_history')) names(name)
       WHERE NOT EXISTS(SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
           WHERE n.nspname='public' AND c.relname=names.name AND c.relkind='r'))
  THEN RAISE EXCEPTION 'ASSET_CARD_TABLE_IDENTITY_INVALID'; END IF;
  CREATE ROLE rine_asset_card_writer LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
  EXECUTE format('GRANT CONNECT ON DATABASE %I TO rine_asset_card_writer',current_database());
  GRANT USAGE ON SCHEMA public TO rine_asset_card_writer;
  GRANT SELECT,INSERT,UPDATE ON public.tm_asset_card_snapshot TO rine_asset_card_writer;
  GRANT SELECT,INSERT,DELETE ON public.tm_asset_card_spot_bar TO rine_asset_card_writer;
  GRANT SELECT,INSERT,DELETE ON public.tm_asset_card_feature_history TO rine_asset_card_writer;
END
$card_bootstrap$;
\ir asset-card-role-verify.sql
COMMIT;
\endif
