CREATE TABLE person (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	PRIMARY KEY(id),
	first_name varchar(255) not null,
	last_name varchar(255) not null
);

insert into person (first_name, last_name) values ('Dave', 'Syer');