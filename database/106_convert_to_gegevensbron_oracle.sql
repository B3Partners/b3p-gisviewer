insert into gegevensbron(id, naam, bron, admin_tabel, admin_pk, admin_query, admin_tabel_opmerkingen)
select gegevensbron_id_seq.nextval, naam, connectie, admin_tabel, admin_pk, admin_query, admin_tabel_opmerkingen from themas
where admin_tabel IS NOT NULL and admin_pk IS NOT NULL;

update themas t set gegevensbron = (select id from gegevensbron g where g.naam = t.naam)
where t.admin_tabel IS NOT NULL and t.admin_pk IS NOT NULL;

update thema_data td set gegevensbron = (select gegevensbron from themas t where td.thema = t.id);

