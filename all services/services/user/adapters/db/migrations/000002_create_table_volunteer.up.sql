CREATE TABLE volunteer (
	id SERIAL PRIMARY KEY,        
	email VARCHAR(255) NOT NULL,   
	password VARCHAR(255) NOT NULL,
	token VARCHAR(255)
);