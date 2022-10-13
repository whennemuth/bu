/* Find / Scan for a text value anywhere in Database */
SET SERVEROUTPUT ON SIZE 100000

DECLARE
    match_count   INTEGER;
BEGIN
    FOR t IN (
        SELECT
            owner,
            table_name,
            column_name
        FROM
            all_tab_columns
        WHERE
            owner = 'KCOEUS'
            AND   data_type LIKE '%CHAR%'
    ) LOOP
        EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM '
        || t.owner
        || '.'
        || t.table_name
        || ' WHERE '
        || t.column_name
        || ' = :1' INTO
            match_count
            USING 'U04728856';

        IF
            match_count > 0
        THEN
            dbms_output.put_line(t.table_name
            || ' '
            || t.column_name
            || ' '
            || match_count);

        END IF;

    END LOOP;
END;
/
