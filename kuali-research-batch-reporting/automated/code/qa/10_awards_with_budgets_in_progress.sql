-- Title : QA Tools #10 - Awards with Budgets in Progress
-- Desc  : List of all Awards with any version associated with a Budget "in progress".
-- By    : D.Haywood 01/29/2014
-- Change:

SELECT AWD.AWARD_NUMBER             as "Award Number",
       AWD.SEQUENCE_NUMBER          as "Award Seq Nbr",
       AWD.DOCUMENT_NUMBER          as "Award Doc Nbr",
       AWD.TITLE                    as "Award Title",
       AWD.STATUS_CODE              as "Award Status Cd",
       ASTAT.DESCRIPTION            as "Award Status Desc",
       BUD.BUDGET_ID                as "Budget ID",
       BUD.VERSION_NUMBER           as "Budget Version Nbr",
       ABX.AWARD_BUDGET_STATUS_CODE as "Budget Status Cd",
       ABS.DESCRIPTION              as "Budget Status Desc",
       BUD.DOCUMENT_NUMBER        	as "Budget Doc Nbr",
       ABX.UPDATE_TIMESTAMP         as "Budget Update Time Stamp",
       ABX.UPDATE_USER              as "Budget Update User",
       DOC.CRTE_DT                  as "Document Create Dt",
       PER.PRNCPL_NM                as "Document Initiator"



FROM KCOEUS.AWARD_BUDGET_EXT          ABX

INNER JOIN KCOEUS.AWARD_BUDGET_STATUS ABS
   ON ABX.AWARD_BUDGET_STATUS_CODE = ABS.AWARD_BUDGET_STATUS_CODE
INNER JOIN KCOEUS.BUDGET              BUD
   ON ABX.BUDGET_ID = BUD.BUDGET_ID
INNER JOIN KCOEUS.BUDGET_DOCUMENT     ZBD
   ON BUD.DOCUMENT_NUMBER = ZBD.DOCUMENT_NUMBER


/* Join Awards to Budget and include Award even if no budget exists
   for a given Award. */
RIGHT JOIN KCOEUS.AWARD AWD
   ON ZBD.PARENT_DOCUMENT_KEY = AWD.DOCUMENT_NUMBER

LEFT JOIN KCOEUS.AWARD_STATUS ASTAT
       ON AWD.STATUS_CODE = ASTAT.STATUS_CODE


/* Include date stamp and user from Rice Document.
   This provides actual date and user who initiated a budget. */
LEFT JOIN kcoeus.KREW_DOC_HDR_T  DOC
  ON BUD.DOCUMENT_NUMBER = DOC.DOC_HDR_ID
LEFT JOIN kcoeus.KRIM_PRNCPL_T PER
  ON DOC.INITR_PRNCPL_ID = PER.PRNCPL_ID


/* List of awards where a budget in progress exists on any version.
   Joining with Awards by Award Nbr produces list of all versions of
   those awards. */
INNER JOIN
	(SELECT PAWD.AWARD_NUMBER
	 FROM KCOEUS.AWARD_BUDGET_EXT          ABX
	 INNER JOIN KCOEUS.BUDGET              BUDX
		ON ABX.BUDGET_ID = BUDX.BUDGET_ID
	 INNER JOIN KCOEUS.BUDGET_DOCUMENT     BDX
		ON BUDX.DOCUMENT_NUMBER = BDX.DOCUMENT_NUMBER
	 INNER JOIN KCOEUS.AWARD PAWD
		ON BDX.PARENT_DOCUMENT_KEY = PAWD.DOCUMENT_NUMBER
	 WHERE ABX.AWARD_BUDGET_STATUS_CODE = '1'
	 GROUP BY PAWD.AWARD_NUMBER
	 ORDER BY PAWD.AWARD_NUMBER) P
  ON P.AWARD_NUMBER = AWD.AWARD_NUMBER

WHERE TO_CHAR(ABX.UPDATE_TIMESTAMP,'YYYYMMDD') < TO_CHAR(SYSDATE,'YYYYMMDD')

ORDER BY AWD.AWARD_NUMBER,
         AWD.SEQUENCE_NUMBER,
         AWD.DOCUMENT_NUMBER,
         BUD.BUDGET_ID
;
         
-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;