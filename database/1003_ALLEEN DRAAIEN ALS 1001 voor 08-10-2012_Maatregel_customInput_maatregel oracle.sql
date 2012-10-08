ALTER table maatregel_custom_input rename to maatregel_eigenschap_custom_input;

create table maatregel_custom_input (
        id number(19,0) not null,
        index number(10,0),
        value varchar2(255 char),
        maatregel number(19,0),
        primary key (id)
);

alter table maatregel_custom_input 
        add constraint FK8AD320071A053ED 
        foreign key (maatregel) 
        references maatregel_gepland;
        
create sequence maatregel_eigenschap_custom_input_id_seq;
        