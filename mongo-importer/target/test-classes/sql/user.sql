<!-- SELECT_USERS
SELECT 
  p.PRNCPL_ID AS "schoolId",
  p.PRNCPL_NM AS "username",
  p.ACTV_IND AS "active",
  n.FIRST_NM AS "firstName",
  n.LAST_NM AS "lastName",
  n.LAST_NM || ', ' || n.FIRST_NM AS "name",
  e.EMAIL_ADDR AS "email",
  nvl(admins.role,'user') AS "role" 
  ${END_SELECT}
FROM 
  KRIM_PRNCPL_T p,
  KRIM_ENTITY_NM_T n,
  KRIM_ENTITY_EMAIL_T e,
  (SELECT p.PRNCPL_ID, p.PRNCPL_NM AS userid, 'admin' AS role,
    FROM KRIM_PERM_T perm,
    KRIM_ROLE_PERM_T rp,
    KRIM_ROLE_T r,
    KRIM_ROLE_MBR_T m,
    KRIM_PRNCPL_T p,
    WHERE perm.NM = 'Modify Entity'
     AND perm.PERM_ID = rp.PERM_ID
     AND r.ROLE_ID = rp.ROLE_ID
     AND m.ROLE_ID = r.ROLE_ID
     AND m.MBR_TYP_CD = 'P'
     AND m.ACTV_TO_DT is null
     AND m.MBR_ID = p.PRNCPL_ID
   ) admins 
   ${END_FROM}
WHERE p.PRNCPL_ID = n.ENTITY_ID
  AND n.DFLT_IND = 'Y' -- get default name
  AND n.ACTV_IND = 'Y' -- active name
  AND p.PRNCPL_ID = e.ENTITY_ID
  AND e.DFLT_IND = 'Y' -- get default email
  AND e.ACTV_IND = 'Y' -- active email
  AND p.PRNCPL_ID = admins.PRNCPL_ID(+)
  ${END_WHERE}
-->

<!-- SELECT_ROLE_IDS
SELECT ROLE_ID 
FROM KRIM_ROLE_PERM_T 
WHERE PERM_ID in (
  SELECT PERM_ID
  FROM KRIM_PERM_T
  WHERE NM = 'Modify Entity'
    AND NMSPC_CD = 'KR-IDM'
  )
-->

<!-- SELECT_PERMISSION_ASSIGNEES
SELECT * 
FROM KRIM_ROLE_MBR_T 
WHERE (MBR_TYP_CD = 'P' OR MBR_TYP_CD = 'G')
  AND (ACTV_FRM_DT is null OR ACTV_FRM_DT <= TRUNC(SYSDATE))
  AND (ACTV_TO_DT is null OR ACTV_TO_DT >= TRUNC(SYSDATE))
  AND ROLE_ID in (
    ${SELECT_ROLE_IDS}
  )
-->

<!-- SELECT_GROUPS = <<-EOS
SELECT GRP_NM AS "name",
  RP_DESC AS "description",
  ACTV_IND AS "active" 
FROM KRIM_GRP_T
WHERE ACTV_IND = 'Y'
-->