--https://www.regular-expressions.info/posixbrackets.html
--https://www.oracletutorial.com/oracle-string-functions/
--https://www.oracletutorial.com/oracle-string-functions/oracle-regexp_replace/
--select 
--    REGEXP_SUBSTR( s.sponsor_name, '[[:punct:]]+' ) as junk1, 
--    REGEXP_COUNT(s.sponsor_name, '[^a-z_A-Z0-9\x20\x9]', 1, 'i') as junk2,
--    UPPER(REGEXP_REPLACE(s.sponsor_name, '[^a-z_A-Z0-9\x20\x9]')) as junk3,
--    s.sponsor_name 
--from sponsor s;

--select * from SPONSOR_SAM_MAPPING_PUNC order by sponsor_code;
--drop table SPONSOR_SAM_MAPPING_PUNC;
--create table SPONSOR_SAM_MAPPING_PUNC as 
insert into SPONSOR_SAM_MAPPING_PUNC
(
    sponsor_code,
    sponsor_name,
    samRegistered,
    ueiSAM,
    ueiDUNS,
    cageCode,
    legalBusinessName,
    registrationStatus,
    evsSource,
    ueiStatus,
    ueiExpirationDate,
    ueiCreationDate,
    publicDisplayFlag,
    dnbOpenData,
    addressLine1,
    addressLine2,
    city,
    stateOrProvinceCode,
    zipCode,
    zipCodePlus4,
    countryCode
)
select * from (
--    with dups as (
--        select s.sponsor_name, count(s.sponsor_code) as repeats
--        from sponsor s, sponsor_sam_srn u
--        where 
--            UPPER(REGEXP_REPLACE(s.sponsor_name, '[^a-z_A-Z0-9\x20\x9]')) = UPPER(REGEXP_REPLACE(u.LEGALBUSINESSNAME, '[^a-z_A-Z0-9\x20\x9]'))
--            s.DUN_AND_BRADSTREET_NUMBER is null
--        group by s.sponsor_name
--        having count(s.sponsor_code) > 1
--    )
    select s.sponsor_code, s.sponsor_name, u.*
    from sponsor s, sponsor_sam_srn u
    where 
        UPPER(REGEXP_REPLACE(s.sponsor_name, '[^a-z_A-Z0-9\x20\x9]')) = UPPER(REGEXP_REPLACE(u.LEGALBUSINESSNAME, '[^a-z_A-Z0-9\x20\x9]'))
        and s.DUN_AND_BRADSTREET_NUMBER is null
--        and s.SPONSOR_NAME in (select sponsor_name from dups)
        and not exists (
            select * from SPONSOR_SAM_MAPPING s2
            where s2.UEISAM = u.UEISAM
        )
        and not exists (
            select * from SPONSOR_SAM_MAPPING_BY_NAME s3
            where s3.UEISAM = u.UEISAM
        )
        and not exists (
            select * from SPONSOR_SAM_MAPPING_BY_NAMES s4
            where s4.UEISAM = u.UEISAM
        )
        and not exists (
            select * from SPONSOR_SAM_MAPPING_PUNC s5
            where s5.UEISAM = u.UEISAM
        )
);