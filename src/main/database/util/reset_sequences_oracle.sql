
select max(id) from bron;
select max(id) from clusters;
select max(id) from configuratie;
select max(id) from data_typen;
select max(id) from resultaatveld;
select max(id) from thema_data;
select max(id) from themas;
select max(id) from waarde_typen;
select max(id) from zoekconfiguratie;
select max(id) from zoekveld;

drop sequence bron_id_seq;
drop sequence clusters_id_seq;
drop sequence configuratie_id_seq;
drop sequence data_typen_id_seq;
drop sequence resultaatveld_id_seq;
drop sequence thema_data_id_seq;
drop sequence themas_id_seq;
drop sequence waarde_typen_id_seq;
drop sequence zoekconfiguratie_id_seq;
drop sequence zoekveld_id_seq;

create sequence bron_id_seq INCREMENT BY 1 START WITH 6;
create sequence clusters_id_seq INCREMENT BY 1 START WITH 74;
create sequence configuratie_id_seq INCREMENT BY 1 START WITH 25;
create sequence data_typen_id_seq INCREMENT BY 1 START WITH 5;
create sequence resultaatveld_id_seq INCREMENT BY 1 START WITH 11;
create sequence thema_data_id_seq INCREMENT BY 1 START WITH 3068;
create sequence themas_id_seq INCREMENT BY 1 START WITH 349;
create sequence waarde_typen_id_seq INCREMENT BY 1 START WITH 7;
create sequence zoekconfiguratie_id_seq INCREMENT BY 1 START WITH 4;
create sequence zoekveld_id_seq INCREMENT BY 1 START WITH 4;
