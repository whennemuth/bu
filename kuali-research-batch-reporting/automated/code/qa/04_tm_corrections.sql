-- Title : QA Tools #4 - Time & Money Corrections
-- Desc  : Report of all time and money corrections
-- By    : B. Gavrish  06/06/2013
-- Change: D.Haywood   10/31/2013  added calculated date for prior month's records
--         D.Haywood   09/02/2014  added reference to PENDING_TRANSACTIONS_EXTENSION following KC 5.2 upgrade

SELECT
  TM.DOCUMENT_NUMBER                           AS "T+M DOC NUM",
  TM.AWARD_NUMBER                              AS "AWARD NUMBER",
  PND.UPDATE_TIMESTAMP                         AS "LAST UPDATED",
  DECODE (PND.SOURCE_AWARD_NUMBER,
          '000000-00000', 'External',
          PND.SOURCE_AWARD_NUMBER)             AS "SOURCE AWARD",
  DECODE (PND.DESTINATION_AWARD_NUMBER,
          '000000-00000', 'External',
          PND.DESTINATION_AWARD_NUMBER)        AS "DESTINATION AWARD",
  PND.OBLIGATED_AMOUNT                         AS "OBLIGATED AMOUNT",
  PND.ANTICIPATED_AMOUNT                       AS "ANTICIPATED AMOUNT",
  EXT.BUDGET_PERIOD                            AS "BUDGET PERIOD",
  AAT.COMMENTS                                 AS "COMMENTS",
  TM.UPDATE_USER                               AS "UPDATED BY"

FROM KCOEUS.PENDING_TRANSACTIONS PND

LEFT JOIN KCOEUS.PENDING_TRANSACTIONS_EXTENSION EXT
  ON EXT.TRANSACTION_ID = PND.TRANSACTION_ID

INNER JOIN KCOEUS.TIME_AND_MONEY_DOCUMENT TM
  ON  TM.DOCUMENT_NUMBER = PND.DOCUMENT_NUMBER
INNER JOIN KCOEUS.AWARD_AMOUNT_TRANSACTION AAT
  ON (AAT.AWARD_NUMBER   = TM.AWARD_NUMBER
  AND AAT.TRANSACTION_ID = TM.DOCUMENT_NUMBER)

WHERE AAT.TRANSACTION_TYPE_CODE = '11'
  AND TO_CHAR(PND.UPDATE_TIMESTAMP,'YYYYMMDD') BETWEEN TO_CHAR(ADD_MONTHS(LAST_DAY(SYSDATE),-2)+1,'YYYYMMDD') -- first day of prior month
                                                   AND TO_CHAR(ADD_MONTHS(LAST_DAY(SYSDATE),-1),'YYYYMMDD')   -- last day of prior month

ORDER BY TM.AWARD_NUMBER
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;