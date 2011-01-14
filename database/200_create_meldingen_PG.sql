CREATE TABLE meldingen (
	id serial NOT NULL,
	naam_zender character varying(255),
	adres_zender character varying(255),
	email_zender character varying(255),
	melding_type character varying(255),
	melding_tekst text,
	melding_status character varying(255),
	melding_commentaar text,
	naam_ontvanger character varying(255),
	datum_ontvangst timestamp without time zone,
	datum_afhandeling timestamp without time zone,
	primary key (id)
);

SELECT AddGeometryColumn ('the_geom',28992,'GEOMETRY',2);