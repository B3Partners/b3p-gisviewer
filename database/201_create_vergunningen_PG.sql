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
	primary key (id)
);

SELECT AddGeometryColumn ('vergunningen','the_geom',28992,'GEOMETRY',2);