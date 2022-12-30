--create table company(id varchar(255),Name varchar(255));
http://localhost:8080/h2-console
    URL: jdbc:h2:file:./demo

create table configuration(id varchar(255), DB_URL varchar(255), land varchar(255), password varchar(255), umgebung varchar(255), user_name varchar(255) );

insert into  configuration(id, DB_URL, land, password, umgebung, user_name)
values ('72b11cec-0080-496a-a3a9-92494007befb','meine Url','HH','Geheim', 'Prod', 'Michi');

insert into  configuration(id, DB_URL, land, password, umgebung, user_name)
values ('72b11cec-0080-496a-a3a9-9249400722fb','meine zweite Url','HH','Geheim', 'QS', 'ekp');

