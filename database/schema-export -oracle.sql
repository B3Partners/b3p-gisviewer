
    create table Clusters (
        id number(10,0) not null,
        naam varchar2(255 char),
        omschrijving varchar2(255 char),
        belangnr number(10,0) not null,
        metadatalink varchar2(255 char),
        default_cluster number(1,0) not null,
        hide_legend number(1,0) not null,
        hide_tree number(1,0) not null,
        background_cluster number(1,0) not null,
        extra_level number(1,0) not null,
        callable number(1,0) not null,
        default_visible number(1,0) not null,
        parent number(10,0),
        primary key (id)
    );

    create table Configuratie (
        id number(10,0) not null,
        property varchar2(255 char),
        propval varchar2(255 char),
        setting varchar2(255 char),
        type varchar2(255 char),
        primary key (id)
    );

    create table DataTypen (
        id number(10,0) not null,
        naam varchar2(255 char),
        primary key (id)
    );

    create table ThemaData (
        id number(10,0) not null,
        label varchar2(255 char),
        eenheid varchar2(255 char),
        omschrijving varchar2(255 char),
        basisregel number(1,0) not null,
        voorbeelden varchar2(255 char),
        kolombreedte number(10,0) not null,
        thema number(10,0),
        waardeType number(10,0),
        dataType number(10,0),
        commando varchar2(255 char),
        kolomnaam varchar2(255 char),
        dataorder number(10,0),
        primary key (id)
    );

    create table Themas (
        id number(10,0) not null,
        code varchar2(255 char),
        naam varchar2(255 char),
        metadata_link varchar2(255 char),
        connectie number(10,0),
        belangnr number(10,0) not null,
        "CLUSTER" number(10,0) not null,
        opmerkingen clob,
        analyse_thema number(1,0) not null,
        locatie_thema number(1,0) not null,
        visible number(1,0) not null,
        admin_tabel_opmerkingen varchar2(255 char),
        admin_tabel varchar2(255 char),
        admin_pk varchar2(255 char),
        admin_pk_complex number(1,0) not null,
        admin_spatial_ref varchar2(255 char),
        admin_query clob,
        spatial_tabel_opmerkingen clob,
        spatial_tabel varchar2(255 char),
        spatial_pk varchar2(255 char),
        spatial_pk_complex number(1,0) not null,
        spatial_admin_ref varchar2(255 char),
        wms_url varchar2(255 char),
        wms_layers varchar2(255 char),
        wms_layers_real varchar2(255 char),
        wms_legendlayer varchar2(255 char),
        wms_legendlayer_real varchar2(255 char),
        wms_querylayers varchar2(255 char),
        wms_querylayers_real varchar2(255 char),
        update_frequentie_in_dagen number(10,0),
        view_geomtype varchar2(255 char),
        organizationcodekey varchar2(255 char),
        maptipstring varchar2(255 char),
        sldattribuut varchar2(255 char),
        layoutadmindata varchar2(255 char),
        primary key (id)
    );

    create table WaardeTypen (
        id number(10,0) not null,
        naam varchar2(255 char),
        primary key (id)
    );

    create table bron (
        id number(10,0) not null,
        naam varchar2(255 char),
        url varchar2(255 char),
        gebruikersnaam varchar2(255 char),
        wachtwoord varchar2(255 char),
        volgorde number(10,0),
        primary key (id)
    );

    create table resultaatveld (
        id number(10,0) not null,
        naam varchar2(255 char),
        label varchar2(255 char),
        volgorde number(10,0),
        attribuutnaam varchar2(255 char),
        typen number(10,0),
        zoekconfiguratie number(10,0),
        primary key (id)
    );

    create table zoekconfiguratie (
        id number(10,0) not null,
        naam varchar2(255 char),
        featuretype varchar2(255 char),
        parentbron number(10,0),
        parentzoekconfiguratie number(10,0),
        primary key (id)
    );

    create table zoekveld (
        id number(10,0) not null,
        naam varchar2(255 char),
        label varchar2(255 char),
        attribuutnaam varchar2(255 char),
        typen number(10,0),
        volgorde number(10,0),
        zoekconfiguratie number(10,0),
        primary key (id)
    );

    alter table Clusters 
        add constraint FK4F4191D9213F33D3 
        foreign key (parent) 
        references Clusters;

    alter table ThemaData 
        add constraint FK783B8EEF68E2FD5E 
        foreign key (dataType) 
        references DataTypen;

    alter table ThemaData 
        add constraint FK783B8EEFAB554B9E 
        foreign key (waardeType) 
        references WaardeTypen;

    alter table ThemaData 
        add constraint FK783B8EEFB89C5283 
        foreign key (thema) 
        references Themas;

    alter table Themas 
        add constraint FK95402F6E908F3D23 
        foreign key ("CLUSTER") 
        references Clusters;

    alter table Themas 
        add constraint FK95402F6E9CEEC42E 
        foreign key (connectie) 
        references bron;

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

    create sequence bron_id_seq;

    create sequence clusters_id_seq;

    create sequence configuratie_id_seq;

    create sequence connecties_id_seq;

    create sequence data_typen_id_seq;

    create sequence resultaatveld_id_seq;

    create sequence thema_data_id_seq;

    create sequence themas_id_seq;

    create sequence waarde_typen_id_seq;

    create sequence zoekconfiguratie_id_seq;

    create sequence zoekveld_id_seq;
