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
	the_geom geometry,
	kenmerk character varying(255),
	CONSTRAINT meldingen_pkey PRIMARY KEY (id),
	CONSTRAINT enforce_dims_the_geom CHECK (ndims(the_geom) = 2),
	CONSTRAINT enforce_geotype_the_geom CHECK (geometrytype(the_geom) = 'POINT'::text OR the_geom IS NULL),
	CONSTRAINT enforce_srid_the_geom CHECK (srid(the_geom) = 28992)
)
WITHOUT OIDS;
ALTER TABLE meldingen OWNER TO demo;