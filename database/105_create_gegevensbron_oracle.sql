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
        references clusters;
		
create sequence gegevensbron_id_seq;