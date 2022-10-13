-- Use current active Award and then connect to  Award_Comment to get Invoice Instructions (i.e. Comment Type Code = 1)

select award.award_number                    as "Award Number",
       award.award_id                        as "Award ID",
       status.description                    as "Award Status",
       award.account_type_code               as "Account Type Cd",
       award.title                           as "Title",
       award.lead_unit_number                as "Lead Unit Number",
       unit.unit_name                        as "Lead Unit Name",
       dbms_lob.substr(com.comments, 4000,1) as "Comments"

from kcoeus.award         AWARD

-- Select 'active' award version, which may not be latest seq, by looking in SAP BW KCRM view of Award Maxseq.
-- To avoid duplicate actives, only the highest award ID value for a given award number is selected. 

RIGHT JOIN
    (select AWD.award_number,
            max(AWD.award_id) as MAX_AWARD_ID
     from kcoeus.award AWD
     RIGHT JOIN sapbwkcrm.awards_maxseq MX
             ON MX.award_id = AWD.award_id
     GROUP BY AWD.AWARD_NUMBER
    ) maxseq
  ON MAXSEQ.MAX_AWARD_ID = AWARD.AWARD_ID


LEFT JOIN KCOEUS.AWARD_STATUS STATUS
       ON STATUS.STATUS_CODE = AWARD.STATUS_CODE

LEFT JOIN KCOEUS.UNIT UNIT
       ON unit.unit_number = award.lead_unit_number

LEFT JOIN kcoeus.award_comment COM
       ON COM.AWARD_ID = award.award_id

WHERE SUBSTR(award.award_number,8,5) = '00001'  -- parent
  AND com.comment_type_code = '1'

ORDER BY award.AWARD_NUMBER;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;