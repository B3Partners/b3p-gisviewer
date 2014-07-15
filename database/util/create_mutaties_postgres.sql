-- postGIS voorbeeld mutaties tabel
CREATE TABLE mutaties (
  	id serial NOT NULL,  	
	soort character varying(255),
	status character varying(255),
	naam_melder character varying(255),
	email_melder character varying(255),
	opmerking text,
	the_geom geometry,
  	CONSTRAINT mutaties_pk PRIMARY KEY (id),
  	CONSTRAINT enforce_dims_the_geom CHECK (st_ndims(the_geom) = 2),
  	CONSTRAINT enforce_geotype_the_geom CHECK (geometrytype(the_geom) = 'POLYGON'::text OR the_geom IS NULL),
  	CONSTRAINT enforce_srid_the_geom CHECK (st_srid(the_geom) = 28992)
);

-- Goedzetten owner van tabel
ALTER TABLE mutaties OWNER TO username;

-- Aanmaken spatial index
CREATE INDEX spatial_mutaties_the_geom ON mutaties USING gist (the_geom);
