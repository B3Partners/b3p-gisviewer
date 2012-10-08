ALTER table maatregel_custom_input rename to maatregel_eigenschap_custom_input;

create table maatregel_custom_input (
        id  bigserial not null,
        index int4,
        value varchar(255),
        maatregel int8,
        primary key (id)
);

alter table maatregel_custom_input 
        add constraint FK8AD320071A053ED 
        foreign key (maatregel) 
        references maatregel_gepland;
