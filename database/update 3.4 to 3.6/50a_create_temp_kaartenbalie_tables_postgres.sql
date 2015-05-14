-- run eerst dblink.sql in de databases waar je gegevens uit wilt halen
-- met postgres kun je normaal geen connectie maken naar andere databases.
-- in Windows staat dit script in postgres installatiefolder /share/contrib/

-- vervang 'dbname=kaartenbalie34 password=XxXxXxxxxxxxxxxXxXxX' door juiste connectie string !

-- users
CREATE TABLE users
(
	id serial NOT NULL,
	main_organization integer NOT NULL,
	personalurl character varying(255),
	CONSTRAINT users_pkey PRIMARY KEY (id)
);
INSERT INTO users (
	id,
	main_organization,
	personalurl
)
( SELECT * FROM dblink('dbname=kaartenbalie34 password=XxXxXxxxxxxxxxxXxXxX','SELECT id, main_organization, personalurl FROM USERS u') 
AS t1(id integer, main_organization integer, personalurl varchar(255)) );

-- users_roles
CREATE TABLE users_roles
(
	users integer NOT NULL,
	role integer NOT NULL,
	CONSTRAINT users_roles_pkey PRIMARY KEY (users, role)
);
INSERT INTO users_roles (
	users,
	role
)
( SELECT * FROM dblink('dbname=kaartenbalie34 password=XxXxXxxxxxxxxxxXxXxX','SELECT users, role FROM users_roles ur') 
AS t1(users integer, role integer) );

-- roles
CREATE TABLE roles
(
	id integer NOT NULL,
	role varchar(255),
	CONSTRAINT roles_pkey PRIMARY KEY (id)
);
INSERT INTO roles (
	id,
	role
)
( SELECT * FROM dblink('dbname=kaartenbalie34 password=XxXxXxxxxxxxxxxXxXxX','SELECT id, role FROM roles r') 
AS t1(id integer, role varchar(255)) );

-- organization_layers
CREATE TABLE organization_layers
(
	organization integer NOT NULL,
	layer integer NOT NULL,
	CONSTRAINT organization_layers_pkey PRIMARY KEY (organization, layer)
);
INSERT INTO organization_layers (
	organization,
	layer
)
( SELECT * FROM dblink('dbname=kaartenbalie34 password=XxXxXxxxxxxxxxxXxXxX','SELECT organization, layer FROM organization_layers ol') 
AS t1(organization integer, layer integer) );

-- layer
CREATE TABLE layer
(
	id integer NOT NULL,
	name varchar(200),
	CONSTRAINT layer_pkey PRIMARY KEY (id)
);
INSERT INTO layer (
	id,
	name
)
( SELECT * FROM dblink('dbname=kaartenbalie34 password=XxXxXxxxxxxxxxxXxXxX','SELECT id, name FROM layer l') 
AS t1(id integer, name varchar(200)) );

-- service_provider
CREATE TABLE service_provider
(
	id integer NOT NULL,
	abbr varchar(60),
	CONSTRAINT service_provider_pkey PRIMARY KEY (id)
);
INSERT INTO service_provider (
	id,
	abbr
)
( SELECT * FROM dblink('dbname=kaartenbalie34 password=XxXxXxxxxxxxxxxXxXxX','SELECT id, abbr FROM service_provider sp') 
AS t1(id integer, abbr varchar(200)) );

-- users_orgs
CREATE TABLE users_orgs
(
	organization integer NOT NULL,
	users integer NOT NULL,
	CONSTRAINT users_orgs_pkey PRIMARY KEY (users, organization)
);
INSERT INTO users_orgs (
	organization,
	users
)
( SELECT * FROM dblink('dbname=kaartenbalie34 password=XxXxXxxxxxxxxxxXxXxX','SELECT organization, users FROM users_orgs uo') 
AS t1(organization integer, users integer) );
