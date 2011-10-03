SET default_with_oids = false;

CREATE TABLE vergunningen (
    id integer NOT NULL,
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
    CONSTRAINT enforce_dims_the_geom CHECK ((ndims(the_geom) = 2)),
    CONSTRAINT enforce_geotype_the_geom CHECK (((geometrytype(the_geom) = 'POINT'::text) OR (the_geom IS NULL))),
    CONSTRAINT enforce_srid_the_geom CHECK ((srid(the_geom) = 28992))
);

CREATE SEQUENCE vergunningen_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

ALTER SEQUENCE vergunningen_id_seq OWNED BY vergunningen.id;

SELECT pg_catalog.setval('vergunningen_id_seq', 1, false);

ALTER TABLE vergunningen ALTER COLUMN id SET DEFAULT nextval('vergunningen_id_seq'::regclass);

INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (1, 7112, 'Aanvraag ontvankelijk', 'Bouwen en Wonen', 'bouwvergunning', 'B3Partners BV', 'zonnebaan', 12, '3542 EC', '2010-03-16 00:00:00', '01010000204071000000000000F00800410000000074041C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (2, 8763, 'Aanvraag ontvankelijk', 'Bouwen en Wonen', 'bouwvergunning', 'Brandmeester BV', 'zonnebaan', 76, '3542 ED', '2010-04-17 00:00:00', '01010000204071000000000000A8050041000000007C021C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (3, 4563, 'Aanvraag ontvankelijk', 'Bouwen en Wonen', 'sloopvergunning', 'Kadimo BV', 'zonnebaan', 15, '3542 JH', '2010-05-16 00:00:00', '01010000204071000000000000F0FAFF400000000040051C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (4, 8395, 'Aanvraag ontvankelijk', 'Bouwen en Wonen', 'sloopvergunning', 'Jylpersma BV', 'zonnebaan', 2, '3542 OI', '2010-06-12 00:00:00', '010100002040710000000000003002004100000000C4001C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (6, 3569, 'Aanvraag ontvankelijk', 'Bouwen en Wonen', 'bouwvergunning', 'Porter BV', 'Positronweg', 20, '3542 AA', '2010-08-19 00:00:00', '01010000204071000000000000280D004100000000F8021C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (7, 3451, 'Aanvraag ontvankelijk', 'Bouwen en Wonen', 'sloopvergunning', 'Overbaal BV', 'zonnebaan', 3, '3542 ET', '2010-09-16 00:00:00', '01010000204071000000000000800A004100000000FC061C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (10, 5694, 'Aanvraag ontvankelijk', 'Bouwen en Wonen', 'bouwvergunning', 'B3Partners BV', 'zonnebaan', 12, '3542 ER', '2010-12-06 00:00:00', '01010000204071000000000000F00800410000000074041C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (12, 87631, 'Aanvraag ontvankelijk', 'Milieu en Afval', 'Milieu: Revisievergunning artikel 8.4 WM', 'Brandmeester BV', 'zonnebaan', 76, '3542 QS', '2010-04-17 00:00:00', '01010000204071000000000000A8050041000000007C021C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (13, 45631, 'Aanvraag ontvankelijk', 'Milieu en Afval', 'Milieu: Revisievergunning artikel 8.4 WM', 'Kadimo BV', 'zonnebaan', 15, '3542 KH', '2010-05-16 00:00:00', '01010000204071000000000000F0FAFF400000000040051C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (15, 45781, 'Aanvraag ontvankelijk', 'Milieu en Afval', 'Milieu: Revisievergunning artikel 8.4 WM', 'Bliener BV', 'zonnebaan', 45, '3542 NG', '2010-07-16 00:00:00', '0101000020407100000000000018090041000000009C001C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (17, 28921, 'Aanvraag ontvankelijk', 'Milieu en Afval', 'Milieu: Revisievergunning artikel 8.4 WM', 'Overbaal BV', 'zonnebaan', 3, '3542 EB', '2010-09-16 00:00:00', '01010000204071000000000000800A004100000000FC061C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (18, 66671, 'Aanvraag ontvankelijk', 'Milieu en Afval', 'Milieu: Revisievergunning artikel 8.4 WM', 'Waterfront BV', 'zonnebaan', 67, '3542 VC', '2010-10-20 00:00:00', '01010000204071000000000000E0FFFF4000000000EC071C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (19, 24671, 'Aanvraag ontvankelijk', 'Milieu en Afval', 'Milieu: Revisievergunning artikel 8.4 WM', 'Royston BV', 'Niels Bohrweg', 55, '3542 VX', '2010-11-16 00:00:00', '010100002040710000000000006803004100000000500B1C41');
INSERT INTO vergunningen (id, nummer, status, soort, onderwerp, naam_organisatie, straatnaam, huisnummer, postcode, aanvraag_datum, the_geom) VALUES (11, 71121, 'Aanvraag ontvankelijk', 'Bouwen en Wonen', 'sloopvergunning', 'B3Partners BV', 'zonnebaan', 12, '3542 WR', '2010-03-16 00:00:00', '01010000204071000000000000F00800410000000074041C41');

ALTER TABLE ONLY vergunningen
    ADD CONSTRAINT vergunningen_pkey PRIMARY KEY (id);
