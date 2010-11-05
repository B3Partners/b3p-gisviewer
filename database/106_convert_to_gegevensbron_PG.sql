insert into gegevensbron(naam, bron, admin_tabel, admin_pk, admin_query, admin_tabel_opmerkingen)
select naam, connectie, admin_tabel, admin_pk, admin_query, admin_tabel_opmerkingen from themas
where admin_tabel IS NOT NULL and admin_pk IS NOT NULL;

update themas t set gegevensbron = (select id from gegevensbron g where g.naam = t.naam)
where t.admin_tabel IS NOT NULL and t.admin_pk IS NOT NULL;

update thema_data td set gegevensbron = (select gegevensbron from themas t where td.thema = t.id);

alter table themas drop connectie, drop admin_tabel_opmerkingen, drop admin_tabel, drop admin_pk, drop admin_pk_complex,
drop admin_spatial_ref, drop admin_query, drop spatial_tabel_opmerkingen, drop spatial_tabel, drop spatial_pk,
drop spatial_pk_complex, drop spatial_admin_ref;

alter table thema_data drop thema;

delete from thema_data where gegevensbron IS NULL;

