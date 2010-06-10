    create table resultaatveld (
        id serial not null,
        naam varchar(255),
        label varchar(255),
        volgorde int4,
        attribuutnaam varchar(255),
        type int4,
        zoekconfiguratie int4,
        primary key (id)
    );

    create table zoekconfiguratie (
        id serial not null,
        naam varchar(255),
        featuretype varchar(255),
        parentbron int4,
        parentzoekconfiguratie int4,
        primary key (id)
    );

    create table zoekveld (
        id serial not null,
        naam varchar(255),
        label varchar(255),
        attribuutnaam varchar(255),
        type int4,
        volgorde int4,
        zoekconfiguratie int4,
        primary key (id)
    );

    alter table resultaatveld 
        add constraint FK1DFFA7FE7EE6CA2B 
        foreign key (zoekconfiguratie) 
        references zoekconfiguratie;

    alter table zoekconfiguratie 
        add constraint FK88B2EC896D4F3ED5 
        foreign key (parentzoekconfiguratie) 
        references zoekconfiguratie;

    alter table zoekconfiguratie 
        add constraint FK88B2EC89B0E5BA81 
        foreign key (parentbron) 
        references bron;

    alter table zoekveld 
        add constraint FK239789827EE6CA2B 
        foreign key (zoekconfiguratie) 
        references zoekconfiguratie;
