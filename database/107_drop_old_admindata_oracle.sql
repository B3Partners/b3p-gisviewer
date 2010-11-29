alter table themas drop 
(connectie, admin_tabel_opmerkingen, admin_tabel, admin_pk, admin_pk_complex,
admin_spatial_ref, admin_query, spatial_tabel_opmerkingen, spatial_tabel, spatial_pk,
spatial_pk_complex, spatial_admin_ref);

alter table thema_data drop (thema);

delete from thema_data where gegevensbron IS NULL;