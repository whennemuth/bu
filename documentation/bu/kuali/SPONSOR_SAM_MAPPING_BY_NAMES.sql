--select count(*) from SPONSOR_SAM_MAPPING_BY_NAMES order by sponsor_code;
--drop table SPONSOR_SAM_MAPPING_BY_NAMES;
--create table SPONSOR_SAM_MAPPING_BY_NAMES as 
insert into SPONSOR_SAM_MAPPING_BY_NAMES 
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
    with dups as (
        select s.sponsor_name, count(s.sponsor_code) as repeats
        from sponsor s, sponsor_sam_srn u
        where 
            upper(rtrim(ltrim(regexp_replace(s.SPONSOR_NAME, '( ){2,}', ' ' )))) = upper(rtrim(ltrim(regexp_replace(u.LEGALBUSINESSNAME, '( ){2,}', ' ' )))) and
            s.DUN_AND_BRADSTREET_NUMBER is null
        group by s.sponsor_name
        having count(s.sponsor_code) > 1
    )
    select s.sponsor_code, s.sponsor_name, u.*
    from sponsor s, sponsor_sam_srn u
    where 
        upper(rtrim(ltrim(regexp_replace(s.SPONSOR_NAME, '( ){2,}', ' ' )))) = upper(rtrim(ltrim(regexp_replace(u.LEGALBUSINESSNAME, '( ){2,}', ' ' )))) and
        s.DUN_AND_BRADSTREET_NUMBER is null
        and s.SPONSOR_NAME in (select sponsor_name from dups)
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
);