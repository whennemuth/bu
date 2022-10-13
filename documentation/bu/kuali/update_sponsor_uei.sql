--CREATE TABLE SPONSOR_BKUP_220331 AS (SELECT * FROM sponsor);
--commit;

select count(*) as BEFORE_organization_uei_count from organization where uei is not null;

select count(*) as BEFORE_sponsor_uei_count from sponsor where uei is not null;

update sponsor s set uei = (
    select distinct ueisam 
    from sponsor_sam_mapping m 
    where s.dun_and_bradstreet_number = m.ueiduns
)
where exists (
    select 1  
    from sponsor_sam_mapping m 
    where s.dun_and_bradstreet_number = m.ueiduns
);

select count(*) as AFTER_sponsor_uei_count from sponsor where uei is not null;

select count(*) as AFTER_organization_uei_count from organization where uei is not null;

--rollback;
--commit;


