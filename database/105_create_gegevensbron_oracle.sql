create table gegevensbron (
        id number(10,0) not null,
        naam varchar2(255 char),
        bron number(10,0),
		admin_tabel varchar2(255 char),
        admin_pk varchar2(255 char),
		admin_query clob,
		parent number(10,0),
		admin_fk varchar2(255 char),
		admin_tabel_opmerkingen clob,
        primary key (id)
    );
	
alter table gegevensbron 
        add constraint FK_PARENT 
        foreign key (parent) 
        references gegevensbron;
--scripts foutieve fk te herstellen     
--alter table gegevensbron drop constraint fk_parent;
--ALTER TABLE gegevensbron ADD CONSTRAINT fk_parent FOREIGN KEY (parent) REFERENCES gegevensbron (id);        
		
create sequence gegevensbron_id_seq;

ALTER TABLE thema_data ADD (gegevensbron NUMBER(10,0));

alter table thema_data 
        add constraint FK_GEGEVENSBRON 
        foreign key (gegevensbron)
        references gegevensbron;
		
ALTER TABLE themas ADD (gegevensbron NUMBER(10,0));

alter table themas
        add constraint FK_THEMAS_GEGEVENSBRON 
        foreign key (gegevensbron)
        references gegevensbron;