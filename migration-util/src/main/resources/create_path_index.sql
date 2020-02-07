drop table if exists PathIndex;

create table PathIndex (
    path varchar primary key,
    uuid varchar,
    file_type tinyint
);

create index path_index on PathIndex(path);
create index uuid_index on PathIndex(uuid);