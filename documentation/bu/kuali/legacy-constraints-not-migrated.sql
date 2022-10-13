-- Show all legacy constraints that are not reflected as migration constraints
with constraints as (
    SELECT
        l.*
    FROM
        (
            SELECT
                *
            FROM
                all_constraints
            WHERE
                owner = 'KCOEUS'
                AND   table_name NOT LIKE '$BIN_%'
                AND   constraint_name NOT LIKE 'SYS_%'
        ) m,
        (
            SELECT
                OWNER,
                CONSTRAINT_NAME,
                CONSTRAINT_TYPE,
                TABLE_NAME,
                SEARCH_CONDITION_VC,
                R_OWNER,
                R_CONSTRAINT_NAME,
                DELETE_RULE,
                STATUS,
                DEFERRABLE,
                DEFERRED,
                VALIDATED,
                GENERATED,
                BAD,
                RELY,
                LAST_CHANGE,
                INDEX_OWNER,
                INDEX_NAME,
                INVALID,
                VIEW_RELATED,
                ORIGIN_CON_ID
            FROM
                all_constraints@stg_legacy
            WHERE
                owner = 'KCOEUS'
                AND   table_name NOT LIKE '$BIN_%'
                AND   constraint_name NOT LIKE 'SYS_%'
        ) l
    WHERE
        1 = 1
        AND   l.table_name = m.table_name (+)
        AND   l.constraint_name = m.constraint_name (+)
        AND   l.constraint_type = m.constraint_type (+)
        AND   m.table_name IS NULL
        AND   EXISTS (
            SELECT
                NULL
            FROM
                all_constraints m2
            WHERE
                m2.table_name = l.table_name
        )
    ORDER BY
        m.table_name
)
select * 
from constraints c 
--where 
--    c.constraint_type = 'R' 
--    and r_constraint_name like 'SYS_%'
;

--select * from all_constraints where table_name = 'COMM_SCHEDULE_MINUTES';    

--DROP DATABASE LINK STG_LEGACY;
--CREATE DATABASE LINK STG_LEGACY 
--    CONNECT TO KCOEUS IDENTIFIED BY "Cq9H#ZNp#8EtFwu5"
--    USING '(DESCRIPTION=
--                (ADDRESS=(PROTOCOL=TCP)(HOST=buaws-kuali-db-stage001.bu.edu)(PORT=1521))
--                (CONNECT_DATA=(SERVICE_NAME=KUALI))
--            )';
