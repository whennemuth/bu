-- Title : QA Tools #6 - Pending Proposals
-- Desc  : Report of all institutional proposal documents in a pending state for a specified period of time
-- By    : Bennett Gavrish 6/6/2013
-- Change: D.Haywood  11/08/2013  added calculated date for prior year
--         D.Haywood  09/02/2014  changed cut-off date for report from 12 months to 18 months

SELECT
  LATEST_PROPOSAL.DOCUMENT_NUMBER                      AS "DOC NUMBER",
  LATEST_PROPOSAL.PROPOSAL_NUMBER                      AS "PROPOSAL NUMBER",
  LATEST_PROPOSAL.SEQUENCE_NUMBER                      AS "VERSION",
  PROPOSAL_TYPE.DESCRIPTION                            AS "PROPOSAL TYPE",
  KCOEUS.PROPOSAL_STATUS.DESCRIPTION                   AS "STATUS DESCRIPTION",
  LATEST_PROPOSAL.SPONSOR_CODE                         AS "SPONSOR ID",
  SPONSOR.SPONSOR_NAME                                 AS "SPONSOR NAME",
  ACTIVITY_TYPE.DESCRIPTION                            AS "ACTIVITY TYPE",
  LATEST_PROPOSAL.LEAD_UNIT_NUMBER                     AS "LEAD UNIT",
  UNIT.UNIT_NAME                                       AS "LEAD UNIT NAME",
  LATEST_PROPOSAL.DEADLINE_DATE                        AS "DEADLINE DATE",
  KREW_DOC_HDR_T.DOC_HDR_STAT_CD                       AS "DOC STATUS",
  KRIM_PRNCPL_T.PRNCPL_NM                              AS "INITIATOR",
  PROPOSAL_PERSONS.FULL_NAME                           AS "PRINCIPAL INVESTIGATOR",
  LATEST_PROPOSAL.CREATE_TIMESTAMP                     AS "CREATED ON"

FROM SAPBWKCRM.LATEST_PROPOSAL
  LEFT JOIN KCOEUS.PROPOSAL_TYPE ON LATEST_PROPOSAL.PROPOSAL_TYPE_CODE = PROPOSAL_TYPE.PROPOSAL_TYPE_CODE
  LEFT JOIN KCOEUS.PROPOSAL_STATUS ON LATEST_PROPOSAL.STATUS_CODE = PROPOSAL_STATUS.PROPOSAL_STATUS_CODE
  LEFT JOIN KCOEUS.SPONSOR ON LATEST_PROPOSAL.SPONSOR_CODE = SPONSOR.SPONSOR_CODE
  LEFT JOIN KCOEUS.ACTIVITY_TYPE ON LATEST_PROPOSAL.ACTIVITY_TYPE_CODE = ACTIVITY_TYPE.ACTIVITY_TYPE_CODE
  LEFT JOIN KCOEUS.UNIT ON LATEST_PROPOSAL.LEAD_UNIT_NUMBER = UNIT.UNIT_NUMBER
  LEFT JOIN kcoeus.KREW_DOC_HDR_T ON LATEST_PROPOSAL.DOCUMENT_NUMBER = KREW_DOC_HDR_T.DOC_HDR_ID
  LEFT JOIN kcoeus.KRIM_PRNCPL_T ON KREW_DOC_HDR_T.INITR_PRNCPL_ID = KRIM_PRNCPL_T.PRNCPL_ID
  LEFT JOIN KCOEUS.PROPOSAL_PERSONS ON LATEST_PROPOSAL.PROPOSAL_ID = PROPOSAL_PERSONS.PROPOSAL_ID

WHERE (LATEST_PROPOSAL.STATUS_CODE = '1' OR LATEST_PROPOSAL.STATUS_CODE = '6')
  AND PROPOSAL_PERSONS.CONTACT_ROLE_CODE = 'PI'
  AND KREW_DOC_HDR_T.DOC_HDR_STAT_CD = 'F'

-- Calculate first day of current month for prior 18 months in YYYYMMDD format.
  AND LATEST_PROPOSAL.CREATE_TIMESTAMP < TO_CHAR(ADD_MONTHS(LAST_DAY(SYSDATE),-19)+1,'YYYYMMDD')

ORDER BY LATEST_PROPOSAL.PROPOSAL_NUMBER
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;