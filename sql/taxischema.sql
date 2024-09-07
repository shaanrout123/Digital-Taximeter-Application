
USE taxi_db;

CREATE TABLE taxi_meter_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    distance DOUBLE NOT NULL,
    fare DOUBLE NOT NULL,
    tax DOUBLE NOT NULL,
    total_fare DOUBLE NOT NULL,
    tax_percent DOUBLE NOT NULL,
    journey_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

