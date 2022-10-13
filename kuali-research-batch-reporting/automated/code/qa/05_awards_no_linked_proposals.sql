-- Title : QA Tools #5 - Awards with no Linked Proposals
-- Desc  : Report of all non-closed, non-canceled awards without an associated proposal record.
-- By    : Mohammed Kousheh & Dean Haywood  05/16/13 (revised 06/05/13)
-- Change: D.Haywood  10/31/13  added calculated date for prior month's records

SELECT
  a1.award_number          AS "Award Number",
  a1.sequence_number       AS "Version",
  a1.title                 AS "Title",
  awt.description          AS "Award Type Desc",
  stat.description         AS "Status Desc",
  a1.sponsor_code          AS "Sponsor Code",
  spon.sponsor_name        AS "Sponsor Name",
  a1.lead_unit_number      AS "Lead Unit Nbr",
  unit.unit_name           AS "Lead Unit Name",
  Person.full_name         AS "PI Name",
  a1.award_effective_date  AS "Project Start Date",
  a4.final_expiration_date AS "Project End Date",
  doc.crte_dt              AS "Award Creation Date",
  tran.description         AS "Transaction Type Desc"

/* Find awards with proposals and join them with *all* awards. Any version of an award
   may be linked to a proposal, but once it is linked then *all* versions of that award
   are considered to be linked. This is why the right join is made back on the award table
   using award number to associate all versions with the linked proposal version.
   Later we'll filter this list to just include awards w/o proposals. */
FROM kcoeus.award a2
INNER JOIN kcoeus.award_funding_proposals afp
        ON afp.award_id = a2.award_id
RIGHT JOIN kcoeus.award a1
        ON a2.award_number = a1.award_number

/* Select 'active' award version, which may not be latest seq, by looking in SAP BW KCRM view of Award Maxseq. */
RIGHT JOIN sapbwkcrm.awards_maxseq maxseq
        ON maxseq.award_id = a1.award_id

/* Join other tables for required descriptions, etc. */
LEFT JOIN kcoeus.award_type              awt
       ON a1.award_type_code = awt.award_type_code
LEFT JOIN kcoeus.award_status            stat
       ON a1.status_code = stat.status_code
LEFT JOIN kcoeus.sponsor                 spon
       ON a1.sponsor_code = spon.sponsor_code
LEFT JOIN kcoeus.unit                    unit
       ON a1.lead_unit_number = unit.unit_number
LEFT JOIN kcoeus.award_transaction_type  tran
       ON a1.transaction_type_code = award_transaction_type_code
LEFT JOIN kcoeus.krew_doc_hdr_t doc
       on a1.document_number = doc.doc_hdr_id

/* Get PIs for each award. There may be multiple PIs which
   will cause the same award to be listed for each PI */
left join kcoeus.award_persons person
       ON (Person.award_id = a1.award_id and Person.contact_role_code ='PI')

/* Get project end date from award_amount_info. There can be multiple records for for a given award ID.
   Get latest one which is record with greatest info ID key value in set.
   Join selected record to itself based on info ID to get associated end date. */
LEFT JOIN KCOEUS.AWARD_AMOUNT_INFO a4
                           INNER JOIN (SELECT AWARD_ID, MAX(AWARD_AMOUNT_INFO_ID) AS MAXAMOUNTID
                                        FROM KCOEUS.AWARD_AMOUNT_INFO
                                        GROUP BY AWARD_ID) MAXAMOUNTINFO
                                  ON (a4.AWARD_ID = MAXAMOUNTINFO.AWARD_ID AND a4.AWARD_AMOUNT_INFO_ID = MAXAMOUNTINFO.MAXAMOUNTID)
           ON maxseq.AWARD_ID = a4.AWARD_ID

/* Apply filter to include only awards not linked to proposal and apply additional filters.*/
WHERE a2.award_number IS NULL                                -- awards unlinked to a proposal
  AND substr (a1.award_number, 8, 5)  = '00001'              -- only primary awards
  AND TO_CHAR(doc.crte_dt,'YYYYMMDD') BETWEEN TO_CHAR(ADD_MONTHS(LAST_DAY(SYSDATE),-2)+1,'YYYYMMDD') -- first day of prior month
                                          AND TO_CHAR(ADD_MONTHS(LAST_DAY(SYSDATE),-1),'YYYYMMDD')   -- last day of prior month
  AND substr (a1.award_number, 1, 1) != '1'                  -- exclude converted award records (awards beginning with '1')
  AND a1.award_type_code             != 7                    -- exclude Industry Funded Clinical Trials
  AND (a1.status_code NOT IN (6,8,9,10))                     -- exclude PAFO/OSP closing, closed, canceled, or do-not-use status
  AND (tran.description = 'New')                   			 -- include only new transactions

ORDER BY a1.award_number, a1.sequence_number
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;