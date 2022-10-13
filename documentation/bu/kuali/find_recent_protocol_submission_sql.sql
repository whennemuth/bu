select s.sql_text, b.position, b.value_string
from v$sql s, v$sql_bind_capture b
where 
    s.sql_id =  b.sql_id and
    s.child_number = b.child_number and
    s.parsing_schema_name = 'KCOEUS' and
    s.module = 'JDBC Thin Client' and
    s.sql_text like '%PROTOCOL_SUBMISSION%';
