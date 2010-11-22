CREATE TABLE voorzieningen (
	id serial NOT NULL,
	soort character varying(255),  
	straatnaam character varying(255),
	huisnummer integer,
	postcode character varying(15), 
	the_geom geometry,
	CONSTRAINT vergunningen_pkey PRIMARY KEY (id),
	CONSTRAINT enforce_dims_the_geom CHECK (ndims(the_geom) = 2),
	CONSTRAINT enforce_geotype_the_geom CHECK (geometrytype(the_geom) = 'POINT'::text OR the_geom IS NULL),
	CONSTRAINT enforce_srid_the_geom CHECK (srid(the_geom) = 28992)
)
WITHOUT OIDS;
ALTER TABLE vergunningen OWNER TO postgres;