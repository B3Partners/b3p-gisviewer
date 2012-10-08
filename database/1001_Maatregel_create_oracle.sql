
    create table maatregel (
        id varchar2(255 char) not null,
        omschrijving varchar2(255 char),
        primary key (id)
    );

    create table maatregel_acties (
        id number(19,0) not null,
        aanwijzing varchar2(255 char),
        deficode varchar2(255 char),
        eenheid varchar2(255 char),
        regelnr number(10,0),
        release varchar2(255 char),
        swc number(10,0),
        tekst clob,
        type varchar2(255 char),
        volgnr number(10,0),
        vrij varchar2(255 char),
        wc number(10,0),
        primary key (id)
    );

    create table maatregel_custom_input (
        id number(19,0) not null,
        index number(10,0),
        value varchar2(255 char),
        maatregel varchar2(255 char),
        primary key (id)
    );

    create table maatregel_eigenschap (
        id number(19,0) not null,
        deficode varchar2(255 char),
        hoeveelheid number(10,0),
        maatregel number(19,0),
        primary key (id)
    );

    create table maatregel_eigenschap_custom_input (
        id number(19,0) not null,
        index number(10,0),
        value varchar2(255 char),
        maatregel_eigenschap number(19,0),
        primary key (id)
    );

    create table maatregel_gepland (
        id number(19,0) not null,
        bron_id number(10,0),
        feature_id varchar2(255 char),
        hoeveelheid number(10,0),
        object_type varchar2(255 char),
        maatregel varchar2(255 char),
        primary key (id)
    );

    create table vlak_maatregel (
        id number(19,0) not null,
        vlak_type varchar2(255 char),
        maatregel varchar2(255 char),
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

    create sequence maatregel_custom_input_id_seq;

    create sequence maatregel_eigenschap_custom_input_id_seq;

    create sequence maatregel_eigenschap_id_seq;

    create sequence maatregel_gepland_id_seq;

    create sequence raw_crow_id_seq;

    create sequence vlak_maatregel_id_seq;
