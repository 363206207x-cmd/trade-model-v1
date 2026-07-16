WITH contract_records(record) AS (
    SELECT format('SCHEMA|%s|owner=%s', n.nspname, pg_get_userbyid(n.nspowner))
    FROM pg_namespace n
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'

    UNION ALL

    SELECT format(
        'RELATION|%s.%s|kind=%s|owner=%s|persistence=%s|partition=%s|partkey=%s|bound=%s|rls=%s|force_rls=%s|view=%s',
        n.nspname, c.relname, c.relkind, pg_get_userbyid(c.relowner),
        c.relpersistence, c.relispartition,
        coalesce(pg_get_partkeydef(c.oid), ''),
        coalesce(pg_get_expr(c.relpartbound, c.oid, true), ''),
        c.relrowsecurity, c.relforcerowsecurity,
        CASE WHEN c.relkind IN ('v', 'm') THEN pg_get_viewdef(c.oid, true) ELSE '' END
    )
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
      AND c.relkind IN ('r', 'p', 'v', 'm', 'f')

    UNION ALL

    SELECT format(
        'COLUMN|%s.%s|position=%s|name=%s|type=%s|not_null=%s|default=%s|identity=%s|generated=%s|collation=%s',
        n.nspname, c.relname, a.attnum, a.attname,
        format_type(a.atttypid, a.atttypmod), a.attnotnull,
        coalesce(pg_get_expr(ad.adbin, ad.adrelid, true), ''),
        a.attidentity, a.attgenerated,
        CASE WHEN a.attcollation = 0 THEN '' ELSE coalesce(coll.collname, '') END
    )
    FROM pg_attribute a
    JOIN pg_class c ON c.oid = a.attrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    LEFT JOIN pg_attrdef ad ON ad.adrelid = a.attrelid AND ad.adnum = a.attnum
    LEFT JOIN pg_collation coll ON coll.oid = a.attcollation
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
      AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
      AND a.attnum > 0 AND NOT a.attisdropped

    UNION ALL

    SELECT format(
        'CONSTRAINT|%s.%s|name=%s|type=%s|deferrable=%s|deferred=%s|validated=%s|no_inherit=%s|definition=%s',
        n.nspname, rel.relname, con.conname, con.contype,
        con.condeferrable, con.condeferred, con.convalidated, con.connoinherit,
        pg_get_constraintdef(con.oid, true)
    )
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_namespace n ON n.oid = rel.relnamespace
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'

    UNION ALL

    SELECT format(
        'INDEX|%s.%s|table=%s|unique=%s|primary=%s|valid=%s|ready=%s|definition=%s',
        n.nspname, index_rel.relname, table_rel.relname,
        idx.indisunique, idx.indisprimary, idx.indisvalid, idx.indisready,
        pg_get_indexdef(index_rel.oid)
    )
    FROM pg_index idx
    JOIN pg_class index_rel ON index_rel.oid = idx.indexrelid
    JOIN pg_class table_rel ON table_rel.oid = idx.indrelid
    JOIN pg_namespace n ON n.oid = table_rel.relnamespace
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'

    UNION ALL

    SELECT format(
        'SEQUENCE|%s.%s|owner=%s|type=%s|start=%s|min=%s|max=%s|increment=%s|cycle=%s|cache=%s|dependency=%s.%s.%s|dependency_type=%s',
        n.nspname, seq_rel.relname, pg_get_userbyid(seq_rel.relowner),
        format_type(seq.seqtypid, NULL), seq.seqstart, seq.seqmin, seq.seqmax,
        seq.seqincrement, seq.seqcycle, seq.seqcache,
        coalesce(owner_ns.nspname, ''), coalesce(owner_rel.relname, ''),
        coalesce(owner_attr.attname, ''), coalesce(dep.deptype::text, '')
    )
    FROM pg_class seq_rel
    JOIN pg_namespace n ON n.oid = seq_rel.relnamespace
    JOIN pg_sequence seq ON seq.seqrelid = seq_rel.oid
    LEFT JOIN pg_depend dep ON dep.classid = 'pg_class'::regclass
        AND dep.objid = seq_rel.oid AND dep.deptype IN ('a', 'i')
    LEFT JOIN pg_class owner_rel ON owner_rel.oid = dep.refobjid
    LEFT JOIN pg_namespace owner_ns ON owner_ns.oid = owner_rel.relnamespace
    LEFT JOIN pg_attribute owner_attr ON owner_attr.attrelid = dep.refobjid
        AND owner_attr.attnum = dep.refobjsubid
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
      AND seq_rel.relkind = 'S'

    UNION ALL

    SELECT format(
        'POLICY|%s.%s|name=%s|permissive=%s|command=%s|roles=%s|qual=%s|with_check=%s',
        n.nspname, rel.relname, pol.polname, pol.polpermissive, pol.polcmd,
        coalesce((
            SELECT string_agg(coalesce(role_name.rolname, 'PUBLIC'), ',' ORDER BY coalesce(role_name.rolname, 'PUBLIC'))
            FROM unnest(pol.polroles) role_oid
            LEFT JOIN pg_roles role_name ON role_name.oid = role_oid
        ), ''),
        coalesce(pg_get_expr(pol.polqual, pol.polrelid, true), ''),
        coalesce(pg_get_expr(pol.polwithcheck, pol.polrelid, true), '')
    )
    FROM pg_policy pol
    JOIN pg_class rel ON rel.oid = pol.polrelid
    JOIN pg_namespace n ON n.oid = rel.relnamespace
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'

    UNION ALL

    SELECT format(
        'TRIGGER|%s.%s|name=%s|enabled=%s|definition=%s',
        n.nspname, rel.relname, trigger.tgname, trigger.tgenabled,
        pg_get_triggerdef(trigger.oid, true)
    )
    FROM pg_trigger trigger
    JOIN pg_class rel ON rel.oid = trigger.tgrelid
    JOIN pg_namespace n ON n.oid = rel.relnamespace
    WHERE NOT trigger.tgisinternal
      AND n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'

    UNION ALL

    SELECT format(
        'ROUTINE|%s.%s(%s)|kind=%s|language=%s|owner=%s|definition=%s',
        n.nspname, proc.proname, pg_get_function_identity_arguments(proc.oid),
        proc.prokind, language.lanname, pg_get_userbyid(proc.proowner),
        pg_get_functiondef(proc.oid)
    )
    FROM pg_proc proc
    JOIN pg_namespace n ON n.oid = proc.pronamespace
    JOIN pg_language language ON language.oid = proc.prolang
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'

    UNION ALL

    SELECT format(
        'TYPE|%s.%s|kind=%s|category=%s|not_null=%s|base_type=%s|default=%s|enum=%s',
        n.nspname, type.typname, type.typtype, type.typcategory, type.typnotnull,
        CASE WHEN type.typbasetype = 0 THEN '' ELSE format_type(type.typbasetype, type.typtypmod) END,
        coalesce(type.typdefault, ''),
        coalesce((
            SELECT string_agg(enum.enumlabel, ',' ORDER BY enum.enumsortorder)
            FROM pg_enum enum WHERE enum.enumtypid = type.oid
        ), '')
    )
    FROM pg_type type
    JOIN pg_namespace n ON n.oid = type.typnamespace
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
      AND type.typtype IN ('d', 'e')

    UNION ALL

    SELECT format(
        'FOREIGN_TABLE|%s.%s|server=%s|options=%s',
        n.nspname, rel.relname, server.srvname,
        coalesce(array_to_string(foreign_table.ftoptions, ','), '')
    )
    FROM pg_foreign_table foreign_table
    JOIN pg_class rel ON rel.oid = foreign_table.ftrelid
    JOIN pg_namespace n ON n.oid = rel.relnamespace
    JOIN pg_foreign_server server ON server.oid = foreign_table.ftserver
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'

    UNION ALL

    SELECT format(
        'FDW|%s|handler=%s|validator=%s|options=%s',
        fdw.fdwname, fdw.fdwhandler::regproc, fdw.fdwvalidator::regproc,
        coalesce(array_to_string(fdw.fdwoptions, ','), '')
    )
    FROM pg_foreign_data_wrapper fdw

    UNION ALL

    SELECT format(
        'FOREIGN_SERVER|%s|fdw=%s|type=%s|version=%s|options=%s',
        server.srvname, fdw.fdwname, coalesce(server.srvtype, ''),
        coalesce(server.srvversion, ''), coalesce(array_to_string(server.srvoptions, ','), '')
    )
    FROM pg_foreign_server server
    JOIN pg_foreign_data_wrapper fdw ON fdw.oid = server.srvfdw

    UNION ALL

    SELECT format(
        'EXTENSION|%s|version=%s|schema=%s',
        extension.extname, extension.extversion, n.nspname
    )
    FROM pg_extension extension
    JOIN pg_namespace n ON n.oid = extension.extnamespace
)
SELECT md5(coalesce(string_agg(record, E'\n' ORDER BY record), ''))
FROM contract_records;
