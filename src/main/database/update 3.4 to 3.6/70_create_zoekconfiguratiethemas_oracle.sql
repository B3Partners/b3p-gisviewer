
    create table zoekconfiguratie_themas (
        id number(10,0) not null,
        zoekconfiguratie number(10,0),
        thema number(10,0),
        primary key (id)
    );
	
    create sequence zoekconfiguratie_themas_id_seq;

alter table zoekconfiguratie_themas 
        add constraint FK4537D7C47EE6CA2B 
        foreign key (zoekconfiguratie) 
        references zoekconfiguratie;

    alter table zoekconfiguratie_themas 
        add constraint FK4537D7C4B89C5283 
        foreign key (thema) 
        references themas;
