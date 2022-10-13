-- Parm value is month offset. Use 0 for current month.
@new_ips_list.sql 0;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;