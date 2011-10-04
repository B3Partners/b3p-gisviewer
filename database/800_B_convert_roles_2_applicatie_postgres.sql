-- Zorg dat je eerst de benodigde tabellen van kaartenbalie in de gisviewer database plaatst.

-- nieuwe applicaties aanmaken obv configuraties
INSERT INTO APPLICATIE (
	NAAM,
	CODE,
	GEBRUIKERS_CODE,
	PARENT,
	DATUM_GEBRUIKT,
	READ_ONLY,
	USER_COPY,
	VERSIE,
	DEFAULT_APP
)
SELECT SETTING, SETTING, '', 0, now(),true,false,1,false
FROM CONFIGURATIE c GROUP BY SETTING;

-- Insert gebruikers_codes in applicatie.
update APPLICATIE a set GEBRUIKERS_CODE =
(SELECT PERSONALURL FROM USERS u, USERS_ROLES ur, ROLES r
where u.ID = ur.USERS AND r.ID = ur.ROLE AND r.ROLE = a.CODE limit 1);

-- User kaartlagen vullen o.b.v. rechten main orgs
INSERT INTO USER_KAARTLAAG (
	CODE,
	THEMAID,
	DEFAULT_ON
)
SELECT a.CODE, t.ID , t.VISIBLE FROM APPLICATIE a, THEMAS t, ORGANIZATION_LAYERS ol, USERS u, LAYER l, SERVICE_PROVIDER s
WHERE u.MAIN_ORGANIZATION = ol.ORGANIZATION 
AND ol.LAYER = l.ID 
AND t.WMS_LAYERS_REAL = s.ABBR || '_' || l.NAME 
AND u.PERSONALURL = a.GEBRUIKERS_CODE;

-- User kaartlagen vullen o.b.v. rechten organizations uit users_orgs
INSERT INTO USER_KAARTLAAG (
	CODE,
	THEMAID,
	DEFAULT_ON
)
SELECT a.CODE, t.ID , t.VISIBLE FROM APPLICATIE a, THEMAS t, ORGANIZATION_LAYERS ol, USERS u, LAYER l, SERVICE_PROVIDER s, USERS_ORGS us
WHERE u.id = us.users 
AND us.ORGANIZATION  = ol.ORGANIZATION 
AND ol.LAYER = l.ID 
AND t.WMS_LAYERS_REAL = s.ABBR || '_' || l.NAME 
AND u.PERSONALURL = a.GEBRUIKERS_CODE
AND u.MAIN_ORGANIZATION != us.ORGANIZATION;

-- hulptabellen droppen
drop table users;
drop table users_roles;
drop table roles;
drop table organization_layers;
drop table layer;
drop table service_provider;
drop table users_orgs;
