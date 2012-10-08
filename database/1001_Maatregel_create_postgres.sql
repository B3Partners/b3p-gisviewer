
    create table maatregel (
        id varchar(255) not null,
        omschrijving varchar(255),
        primary key (id)
    );

    create table maatregel_acties (
        id  bigserial not null,
        aanwijzing varchar(255),
        deficode varchar(255),
        eenheid varchar(255),
        regelnr int4,
        release varchar(255),
        swc int4,
        tekst text,
        type varchar(255),
        volgnr int4,
        vrij varchar(255),
        wc int4,
        primary key (id)
    );

    create table maatregel_custom_input (
        id  bigserial not null,
        index int4,
        value varchar(255),
        maatregel varchar(255),
        primary key (id)
    );

    create table maatregel_eigenschap (
        id  bigserial not null,
        deficode varchar(255),
        hoeveelheid int4,
        maatregel int8,
        primary key (id)
    );

    create table maatregel_eigenschap_custom_input (
        id  bigserial not null,
        index int4,
        value varchar(255),
        maatregel_eigenschap int8,
        primary key (id)
    );

    create table maatregel_gepland (
        id  bigserial not null,
        bron_id int4,
        feature_id varchar(255),
        hoeveelheid int4,
        object_type varchar(255),
        maatregel varchar(255),
        primary key (id)
    );

    create table vlak_maatregel (
        id  bigserial not null,
        vlak_type varchar(255),
        maatregel varchar(255),
        primary key (id)
    );

    alter table maatregel_custom_input 
        add constraint FK8AD3200747824F58 
        foreign key (maatregel) 
        references maatregel;

    alter table maatregel_eigenschap 
        add constraint FK93ABE6E61A053ED 
        foreign key (maatregel) 
        references maatregel_gepland;

    alter table maatregel_eigenschap_custom_input 
        add constraint FKD8369635F3317B25 
        foreign key (maatregel_eigenschap) 
        references maatregel_eigenschap;

    alter table maatregel_gepland 
        add constraint FKD26E8AD247824F58 
        foreign key (maatregel) 
        references maatregel;

    alter table vlak_maatregel 
        add constraint FKDDC2D7B547824F58 
        foreign key (maatregel) 
        references maatregel;
