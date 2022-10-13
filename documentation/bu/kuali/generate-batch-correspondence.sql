-- Throw out all protcols except those that have the highest sequence
with p as (
    select 
        p1.PROTOCOL_ID,
        p1.PROTOCOL_NUMBER,
        max(p1.SEQUENCE_NUMBER) as maxseq
    from
        protocol p1
    where
        p1.active = 'Y' and
        p1.expiration_date is not null and
        p1.protocol_status_code in('200', '201', '202')
    group by
        p1.PROTOCOL_ID,
        p1.PROTOCOL_NUMBER
),
-- Throw out all protocol submissions except those for protocol sequences having the highest submission number
s as (
    select 
        s1.protocol_id,
        s1.protocol_number,
        s1.sequence_number,
        max(s1.submission_number) as maxsub
    from
        protocol_submission s1
    where
        s1.committee_id = 'CRC_IRB' and
        s1.submission_status_code in('203')
    group by
        s1.protocol_id,
        s1.protocol_number,
        s1.sequence_number
)
--Get all expiration dates for all qualifying protcols with submissions on them
select distinct 
    p2.protocol_number,
    p2.sequence_number,
    s.maxsub,
    p2.expiration_date
from p, s, protocol p2
where
    p.protocol_id = p2.protocol_id and
    p.protocol_number = p2.protocol_number and
    p.maxseq = p2.sequence_number and
    p.protocol_id = s.protocol_id and
    p.protocol_number = s.protocol_number and
    p.maxseq = s.sequence_number and
    p2.expiration_date >= trunc(sysdate)
order by
    p2.expiration_date desc;

select distinct
    p.PROTOCOL_ID,
    p.PROTOCOL_NUMBER,
    p.SEQUENCE_NUMBER,
    p.PROTOCOL_TYPE_CODE,
    p.PROTOCOL_STATUS_CODE,
    p.EXPIRATION_DATE
from protocol p
where 
    p.active = 'Y' and 
    p.expiration_date is not null and
    p.protocol_status_code in('200', '201', '202') and
    exists (
        select null 
        from (
            select ps.protcol_id, ps.protocol_number, ps.sequence_number, ps.committee_id
        ) s 
        where 
            p.protocol_id = s.protocol_id and
            p.protocol_number = s.protocol_number and
            p.sequence_number = s.sequence_number and
            s.committee_id = 'CRC_IRB' and
            s.submission_status_code in('203')
    )
order by expiration_date desc;



--QueryByCriteria from 
--	class org.kuali.kra.irb.Protocol  
--where [
--	protocolSubmissions.committeeId = CRC_IRB, 
--	expirationDate >= 2022-08-01, 
--	expirationDate <= 2022-09-01, [
--		protocolStatusCode IN [200, 201, 202]
--	], [
--		submissionStatusCode IN [203]
--	], 
--	sequenceNumber = ReportQuery from 
--		class org.kuali.kra.irb.Protocol max(sequence_number)  
--		where [
--			protocolNumber = parentQuery.protocolNumber
--		], 
--		protocolSubmissions.submissionNumber = ReportQuery from 
--		class org.kuali.kra.irb.actions.submit.ProtocolSubmission max(submission_number)  
--		where [
--			protocolNumber = parentQuery.protocolNumber
--		]
--	]

select distinct p.protocol_number, p.sequence_number, s.submission_number, p.protocol_status_code, s.submission_status_code, p.expiration_date, s.committee_id, p.approval_date
from protocol p, protocol_submission s
where
--    p.protocol_number = s.protocol_number and
--    p.sequence_number = s.sequence_number and 
    p.protocol_id = s.protocol_id and
    s.committee_id = 'CRC_IRB' and
    p.expiration_date >= to_date('2022-08-01','YYYY-MM-DD') and
    p.expiration_date <= to_date('2022-10-28','YYYY-MM-DD') and
    p.protocol_status_code in('200', '201', '202') and
    s.submission_status_code in('203') and
    s.sequence_number = (
        select s2.sequence_number   
        from protocol p2, protocol_submission s2
        where 
            p2.active = 'Y' and
            p2.expiration_date is not null and
            p2.protocol_id = s2.protocol_id and
            p2.sequence_number = s2.sequence_number and
            s2.protocol_number = p.protocol_number and
            s2.submission_number = (
                select max(submission_number) from protocol_submission
                where protocol_number = s2.protocol_number
            )
    )
order by 
    p.protocol_number, p.sequence_number, s.submission_number
    
    
    
SELECT DISTINCT a0.protocol_number, a0.sequence_number, a1.submission_number, a0.protocol_status_code, a1.submission_status_code, a0.expiration_date, a1.committee_id, a0.approval_date
FROM protocol a0, protocol_submission a1
WHERE
    a0.protocol_id = a1.protocol_id and
    a1.committee_id = 'CRC_IRB' and
    a0.expiration_date >= to_date('2022-08-01','YYYY-MM-DD') and
    a0.expiration_date <= to_date('2022-10-28','YYYY-MM-DD') and
    a0.protocol_status_code IN ( '200', '201', '202' ) and
    a1.submission_status_code IN ( '203' ) and
    a0.sequence_number = (
            SELECT
                MAX(sequence_number)
            FROM
                protocol b0
            WHERE
                b0.protocol_number = a0.protocol_number
        ) and
    a1.submission_number = (
        SELECT
            MAX(submission_number)
        FROM
            protocol_submission b0
        WHERE
            b0.protocol_number = a0.protocol_number 
            and b0.committee_id = 'CRC_IRB'
            and b0.committee_id = a1.committee_id
    );
    
    
--select * from protocol_submission where protocol_number = '1602003811' order by sequence_number, submission_number and commitee_id = 'CRC_IRB'