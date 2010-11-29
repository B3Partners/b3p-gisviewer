create table gegevensbron (
        id serial not null,
        naam varchar(255),
        bron int4,
		admin_tabel varchar(255),
        admin_pk varchar(255),
		admin_query text,
		parent int4,
		admin_fk varchar(255),
		admin_tabel_opmerkingen text,
        primary key (id)
    );
	
alter table gegevensbron 
        add constraint FK_PARENT 
        foreign key (parent) 
        references gegevensbron;
--scripts foutieve fk te herstellen     
--alter table gegevensbron drop constraint fk_parent;
--ALTER TABLE gegevensbron ADD CONSTRAINT fk_parent FOREIGN KEY (parent) REFERENCES gegevensbron (id);        
		
ALTER TABLE thema_data ADD COLUMN gegevensbron int4;

alter table thema_data 
        add constraint FK_GEGEVENSBRON 
        foreign key (gegevensbron)
        references gegevensbron;

ALTER TABLE themas ADD COLUMN gegevensbron int4;

alter table themas
        add constraint FK_THEMAS_GEGEVENSBRON 
        foreign key (gegevensbron)
        references gegevensbron;