\set ON_ERROR_STOP on
-- Read-only catalog verification of the exact dedicated role, not GRANT/REVOKE repair.
DO $card_verify$
DECLARE
  r record;
  t record;
  writer oid;
  privilege text;
  allowed text[];
BEGIN
  SELECT * INTO r FROM pg_roles WHERE rolname='rine_asset_card_writer';
  IF NOT FOUND THEN RAISE EXCEPTION 'ASSET_CARD_ROLE_MISSING'; END IF;
  writer:=r.oid;
  IF NOT r.rolcanlogin OR r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolinherit
     OR r.rolreplication OR r.rolbypassrls OR r.rolconfig IS NOT NULL
     OR EXISTS(SELECT 1 FROM pg_auth_members WHERE member=writer OR roleid=writer)
  THEN RAISE EXCEPTION 'ASSET_CARD_ROLE_ATTRIBUTES_OR_MEMBERSHIP_INVALID'; END IF;
  IF EXISTS(SELECT 1 FROM pg_database WHERE datdba=writer)
     OR EXISTS(SELECT 1 FROM pg_namespace WHERE nspowner=writer)
     OR EXISTS(SELECT 1 FROM pg_class WHERE relowner=writer)
  THEN RAISE EXCEPTION 'ASSET_CARD_ROLE_OWNERSHIP_FORBIDDEN'; END IF;
  IF has_database_privilege(writer,current_database(),'CREATE') OR has_database_privilege(writer,current_database(),'TEMP')
     OR has_database_privilege(writer,current_database(),'CONNECT WITH GRANT OPTION')
     OR NOT has_database_privilege(writer,current_database(),'CONNECT')
     OR NOT has_schema_privilege(writer,'public','USAGE')
     OR EXISTS(SELECT 1 FROM pg_namespace WHERE has_schema_privilege(writer,oid,'CREATE') OR has_schema_privilege(writer,oid,'USAGE WITH GRANT OPTION'))
  THEN RAISE EXCEPTION 'ASSET_CARD_DATABASE_OR_SCHEMA_PRIVILEGE_INVALID'; END IF;
  -- Table ACLs alone cannot prove isolation when a callable SECURITY DEFINER
  -- routine can read or mutate other business data using the routine owner's rights.
  IF EXISTS(SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
       WHERE p.prosecdef AND n.nspname NOT IN ('pg_catalog','information_schema')
         AND has_function_privilege(writer,p.oid,'EXECUTE'))
  THEN RAISE EXCEPTION 'ASSET_CARD_SECURITY_DEFINER_ACCESS_FORBIDDEN'; END IF;
  IF EXISTS(SELECT 1 FROM (VALUES ('tm_asset_card_snapshot'),('tm_asset_card_spot_bar'),('tm_asset_card_feature_history')) names(name)
       WHERE NOT EXISTS(SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
           WHERE n.nspname='public' AND c.relname=names.name AND c.relkind='r'))
  THEN RAISE EXCEPTION 'ASSET_CARD_TABLE_IDENTITY_INVALID'; END IF;
  FOR t IN SELECT c.oid,c.relname,c.relkind,n.nspname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%'
      AND c.relkind IN ('r','p','v','m','f','S')
  LOOP
    IF t.relkind='S' THEN
      IF has_sequence_privilege(writer,t.oid,'USAGE,SELECT,UPDATE') THEN RAISE EXCEPTION 'ASSET_CARD_SEQUENCE_PRIVILEGE_FORBIDDEN'; END IF;
      CONTINUE;
    END IF;
    allowed:=ARRAY[]::text[];
    IF t.nspname='public' AND t.relname='tm_asset_card_snapshot' THEN allowed:=ARRAY['SELECT','INSERT','UPDATE'];
    ELSIF t.nspname='public' AND t.relname IN ('tm_asset_card_spot_bar','tm_asset_card_feature_history') THEN allowed:=ARRAY['SELECT','INSERT','DELETE']; END IF;
    FOREACH privilege IN ARRAY ARRAY['SELECT','INSERT','UPDATE','DELETE','TRUNCATE','REFERENCES','TRIGGER'] LOOP
      IF has_table_privilege(writer,t.oid,privilege||' WITH GRANT OPTION')
      THEN RAISE EXCEPTION 'ASSET_CARD_GRANT_OPTION_FORBIDDEN'; END IF;
      IF (privilege=ANY(allowed)) <> has_table_privilege(writer,t.oid,privilege)
      THEN RAISE EXCEPTION 'ASSET_CARD_TABLE_PRIVILEGE_MATRIX_MISMATCH'; END IF;
      IF NOT privilege=ANY(allowed) AND privilege IN ('SELECT','INSERT','UPDATE','REFERENCES')
        AND has_any_column_privilege(writer,t.oid,privilege)
      THEN RAISE EXCEPTION 'ASSET_CARD_COLUMN_PRIVILEGE_FORBIDDEN'; END IF;
      IF privilege IN ('SELECT','INSERT','UPDATE','REFERENCES')
        AND has_any_column_privilege(writer,t.oid,privilege||' WITH GRANT OPTION')
      THEN RAISE EXCEPTION 'ASSET_CARD_GRANT_OPTION_FORBIDDEN'; END IF;
    END LOOP;
  END LOOP;
END
$card_verify$;
SELECT 'ASSET_CARD_ROLE_VERIFY=PASS';
