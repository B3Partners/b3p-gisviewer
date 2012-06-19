    create table zoekconfiguratie_themas (
        id  serial not null,
        zoekconfiguratie int4,
        thema int4,
        primary key (id)
    );

    alter table zoekconfiguratie_themas 
        add constraint FK4537D7C47EE6CA2B 
        foreign key (zoekconfiguratie) 
        references zoekconfiguratie;

    alter table zoekconfiguratie_themas 
        add constraint FK4537D7C4B89C5283 
        foreign key (thema) 
        references themas;
