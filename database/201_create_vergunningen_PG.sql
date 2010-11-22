CREATE TABLE vergunningen (
	id serial NOT NULL,
	nummer integer,
	status character varying(255),
	soort character varying(255),  
	onderwerp character varying(255),
	naam_organisatie character varying(255),
	straatnaam character varying(255),
	huisnummer integer,
	postcode character varying(15), 
	aanvraag_datum timestamp without time zone,
	the_geom geometry,
	CONSTRAINT vergunningen_pkey PRIMARY KEY (id),
	CONSTRAINT enforce_dims_the_geom CHECK (ndims(the_geom) = 2),
	CONSTRAINT enforce_geotype_the_geom CHECK (geometrytype(the_geom) = 'POINT'::text OR the_geom IS NULL),
	CONSTRAINT enforce_srid_the_geom CHECK (srid(the_geom) = 28992)
)
WITHOUT OIDS;
ALTER TABLE vergunningen OWNER TO postgres;