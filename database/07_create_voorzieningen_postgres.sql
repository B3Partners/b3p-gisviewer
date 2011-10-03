SET default_with_oids = false;

CREATE TABLE voorzieningen (
    id integer NOT NULL,
    soort character varying(255),
    straatnaam character varying(255),
    huisnummer integer,
    postcode character varying(15),
    the_geom geometry,
    CONSTRAINT enforce_dims_the_geom CHECK ((ndims(the_geom) = 2)),
    CONSTRAINT enforce_geotype_the_geom CHECK (((geometrytype(the_geom) = 'POINT'::text) OR (the_geom IS NULL))),
    CONSTRAINT enforce_srid_the_geom CHECK ((srid(the_geom) = 28992))
);

CREATE SEQUENCE voorzieningen_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

ALTER SEQUENCE voorzieningen_id_seq OWNED BY voorzieningen.id;

SELECT pg_catalog.setval('voorzieningen_id_seq', 1, false);

ALTER TABLE voorzieningen ALTER COLUMN id SET DEFAULT nextval('voorzieningen_id_seq'::regclass);

INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (20, 'Zwembad', 'zonnebaan', 12, '3542 EC', '01010000204071000000000000F00800410000000074041C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (6, 'Hotspot', 'Positronweg', 20, '3542 EC', '01010000204071000000000000280D004100000000F8021C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (21, 'Zwembad', 'sterrebaan', 1, '3542 AA', '01010000204071000000000000F00C00410000000048061C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (22, 'Hotspot', 'sterrebaan', 2, '3542 AB', '01010000204071000000000000780E0041000000007C051C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (23, 'Prullenbak', 'sterrebaan', 5, '3542 AC', '01010000204071000000000000300D004100000000A4081C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (24, 'Zwembad', 'floraweg', 10, '3543 AA', '010100002040710000000000002003004100000000FC051C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (25, 'Hotspot', 'floraweg', 20, '3543 AB', '01010000204071000000000000180900410000000050081C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (26, 'Prullenbak', 'floraweg', 23, '3543 AC', '01010000204071000000000000E0FAFF400000000098071C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (27, 'Zwembad', 'savannahweg', 30, '3542 KL', '01010000204071000000000000600600410000000030011C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (28, 'Hotspot', 'savannahweg', 32, '3542 KR', '0101000020407100000000000060FFFF400000000078041C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (29, 'Prullenbak', 'savannahweg', 37, '3542 KE', '01010000204071000000000000F00900410000000064FF1B41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (1, 'Prullenbak', 'zonnebaan', 12, '3542 AA', '01010000204071000000000000F00800410000000074041C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (2, 'Zwembad', 'zonnebaan', 76, '3542 AB', '01010000204071000000000000A8050041000000007C021C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (3, 'Zwembad', 'zonnebaan', 15, '3542 AC', '01010000204071000000000000F0FAFF400000000040051C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (4, 'Zwembad', 'zonnebaan', 2, '3542 AD', '010100002040710000000000003002004100000000C4001C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (5, 'Zwembad', 'zonnebaan', 45, '3542 AE', '0101000020407100000000000018090041000000009C001C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (7, 'Hotspot', 'zonnebaan', 3, '3542 JH', '01010000204071000000000000800A004100000000FC061C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (8, 'Zwembad', 'zonnebaan', 67, '3542 ED', '01010000204071000000000000E0FFFF4000000000EC071C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (9, 'Hotspot', 'Niels Bohrweg', 55, '3542 LI', '010100002040710000000000006803004100000000500B1C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (10, 'Zwembad', 'zonnebaan', 12, '3542 LO', '01010000204071000000000000F00800410000000074041C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (11, 'Prullenbak', 'zonnebaan', 12, '3542 IJ', '01010000204071000000000000F00800410000000074041C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (12, 'Prullenbak', 'zonnebaan', 76, '3542 TY', '01010000204071000000000000A8050041000000007C021C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (13, 'Prullenbak', 'zonnebaan', 15, '3542 XE', '01010000204071000000000000F0FAFF400000000040051C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (14, 'Prullenbak', 'zonnebaan', 2, '3542 ER', '010100002040710000000000003002004100000000C4001C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (15, 'Prullenbak', 'zonnebaan', 45, '3542 IT', '0101000020407100000000000018090041000000009C001C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (16, 'Hotspot', 'Positronweg', 20, '3542 MN', '01010000204071000000000000280D004100000000F8021C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (17, 'Hotspot', 'zonnebaan', 3, '3542 LK', '01010000204071000000000000800A004100000000FC061C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (18, 'Prullenbak', 'zonnebaan', 67, '3542 PO', '01010000204071000000000000E0FFFF4000000000EC071C41');
INSERT INTO voorzieningen (id, soort, straatnaam, huisnummer, postcode, the_geom) VALUES (19, 'Hotspot', 'Niels Bohrweg', 55, '3542 UH', '010100002040710000000000006803004100000000500B1C41');

ALTER TABLE ONLY voorzieningen
    ADD CONSTRAINT voorzieningen_pkey PRIMARY KEY (id);