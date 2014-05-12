-- DROP TABLE mutaties;

CREATE TABLE mutaties (
  	fid serial NOT NULL,
  	the_geom geometry,
	type character varying(255),
	status character varying(255),
	naam_melder character varying(255),
	sld_code character varying(255),
  	CONSTRAINT mutaties_pk PRIMARY KEY (fid),
  	CONSTRAINT enforce_dims_the_geom CHECK (st_ndims(the_geom) = 2),
  	CONSTRAINT enforce_geotype_the_geom CHECK (geometrytype(the_geom) = 'POLYGON'::text OR the_geom IS NULL),
  	CONSTRAINT enforce_srid_the_geom CHECK (st_srid(the_geom) = 28992)
);

ALTER TABLE mutaties OWNER TO gouda;

-- DROP INDEX spatial_mutaties_the_geom;

CREATE INDEX spatial_mutaties_the_geom ON mutaties USING gist (the_geom);
