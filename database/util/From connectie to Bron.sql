INSERT INTO bron(
	naam, url, gebruikersnaam, wachtwoord)
SELECT naam,connectie_url,gebruikersnaam,wachtwoord
FROM connecties;

ALTER TABLE themas DROP CONSTRAINT fkcbdb434e2921d2a3;

UPDATE themas t set
connectie= (select id from bron b where(
		b.naam = (select c.naam from connecties c where c.id=t.connectie)
		and b.url = (select c.connectie_url from connecties c where c.id=t.connectie)
		and b.wachtwoord = (select c.wachtwoord from connecties c where c.id=t.connectie)
		and b.gebruikersnaam = (select c.gebruikersnaam from connecties c where c.id=t.connectie)
		)
	);
ALTER TABLE themas ADD CONSTRAINT themas_bron FOREIGN KEY (connectie) REFERENCES bron (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

drop table connecties;