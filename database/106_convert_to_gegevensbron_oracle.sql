insert into gegevensbron(id, naam, bron, admin_tabel, admin_pk, admin_query, admin_tabel_opmerkingen)
select gegevensbron_id_seq.nextval, naam, connectie, admin_tabel, admin_pk, admin_query, admin_tabel_opmerkingen from themas
where admin_tabel IS NOT NULL and admin_pk IS NOT NULL;

update themas t set gegevensbron = (select id from gegevensbron g where g.naam = t.naam)
where t.admin_tabel IS NOT NULL and t.admin_pk IS NOT NULL;

update thema_data td set gegevensbron = (select gegevensbron from themas t where td.thema = t.id);

--alter table themas drop 
--(connectie, admin_tabel_opmerkingen, admin_tabel, admin_pk, admin_pk_complex,
--admin_spatial_ref, admin_query, spatial_tabel_opmerkingen, spatial_tabel, spatial_pk,
--spatial_pk_complex, spatial_admin_ref);

--alter table thema_data drop (thema);

--delete from thema_data where gegevensbron IS NULL;

