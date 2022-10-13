-- Title : QA Tools #2 - Saved Awards
-- Desc  : Report of all awards in a saved state
-- By    : Bennett Gavrish, 6/6/2013


SELECT AWARD.DOCUMENT_NUMBER AS "DOCUMENT NUMBER",
       AWARD.AWARD_NUMBER AS "AWARD NUMBER",
       AWARD.SEQUENCE_NUMBER AS VERSION,
       AWARD_TRANSACTION_TYPE.DESCRIPTION AS "TRANSACTION TYPE",
       ACTIVITY_TYPE.DESCRIPTION AS "ACTIVITY TYPE",
       AWARD_STATUS.DESCRIPTION AS "AWARD STATUS",
       AWARD.LEAD_UNIT_NUMBER AS "LEAD UNIT CODE",
       UNIT.UNIT_NAME AS "LEAD UNIT",
       AWARD.TITLE,
       AWARD.SPONSOR_CODE AS "SPONSOR CODE",
       SPONSOR.SPONSOR_NAME AS "SPONSOR NAME",
       AWARD.ACCOUNT_NUMBER AS "ACCOUNT NUMBER",
       AWARD_PERSONS.FULL_NAME AS "PI NAME",
       KREW_DOC_HDR_T.CRTE_DT AS "CREATED ON",
       ACTIVITY_TYPE.UPDATE_TIMESTAMP AS "LAST UPDATED DATE",
       KRIM_PRNCPL_T.PRNCPL_NM AS INITIATOR,
       KREW_DOC_HDR_T.DOC_HDR_STAT_CD
  FROM    (   (   (   (   (   (   (   (   KCOEUS.AWARD_PERSONS AWARD_PERSONS
                                       INNER JOIN
                                          KCOEUS.AWARD AWARD
                                       ON (AWARD_PERSONS.AWARD_ID =
                                              AWARD.AWARD_ID))
                                   INNER JOIN
                                      KCOEUS.UNIT UNIT
                                   ON (AWARD.LEAD_UNIT_NUMBER =
                                          UNIT.UNIT_NUMBER))
                               INNER JOIN
                                  KCOEUS.AWARD_STATUS AWARD_STATUS
                               ON (AWARD_STATUS.STATUS_CODE =
                                      AWARD.STATUS_CODE))
                           INNER JOIN
                              KCOEUS.ACTIVITY_TYPE ACTIVITY_TYPE
                           ON (ACTIVITY_TYPE.ACTIVITY_TYPE_CODE =
                                  AWARD.ACTIVITY_TYPE_CODE))
                       INNER JOIN
                          KCOEUS.SPONSOR SPONSOR
                       ON (SPONSOR.SPONSOR_CODE = AWARD.SPONSOR_CODE))
                   INNER JOIN
                      kcoeus.KREW_DOC_HDR_T KREW_DOC_HDR_T
                   ON (KREW_DOC_HDR_T.DOC_HDR_ID = AWARD.DOCUMENT_NUMBER))
               INNER JOIN
                  kcoeus.KREW_DOC_TYP_T KREW_DOC_TYP_T
               ON (KREW_DOC_TYP_T.DOC_TYP_ID = KREW_DOC_HDR_T.DOC_TYP_ID))
           INNER JOIN
              kcoeus.KRIM_PRNCPL_T KRIM_PRNCPL_T
           ON (KRIM_PRNCPL_T.PRNCPL_ID = KREW_DOC_HDR_T.INITR_PRNCPL_ID))
       LEFT OUTER JOIN
          KCOEUS.AWARD_TRANSACTION_TYPE AWARD_TRANSACTION_TYPE
       ON (AWARD_TRANSACTION_TYPE.AWARD_TRANSACTION_TYPE_CODE =
              AWARD.TRANSACTION_TYPE_CODE)
 WHERE (  (  (  (    AWARD_PERSONS.CONTACT_ROLE_CODE = 'PI'
             AND KREW_DOC_TYP_T.DOC_TYP_NM = 'AwardDocument')
        AND KREW_DOC_HDR_T.DOC_HDR_STAT_CD = 'S')
       AND AWARD.STATUS_CODE <> '9')
     AND AWARD_STATUS.DESCRIPTION != 'Cancelled')

ORDER BY "AWARD NUMBER" ASC
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;