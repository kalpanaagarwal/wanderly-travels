-- ==========================================================
-- Wanderly Travels - Database Schema (reference / backup)
-- ==========================================================
-- You do NOT need to run this file by hand.
-- The Java application (see data/DBInitializer.java) creates
-- the database and every table automatically the first time
-- you run it, and fills the destinations/tour_packages tables
-- with starter data if they are empty.
--
-- This file is kept only as a reference, and as a backup in
-- case you ever want to recreate the database manually.
-- ==========================================================

CREATE DATABASE IF NOT EXISTS wanderly_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE wanderly_db;

-- ----------------------------------------------------------
-- users: one row per registered account.
-- username has a UNIQUE constraint so the database itself
-- will refuse a duplicate username even if the Java code
-- did not check first.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    full_name     VARCHAR(100),
    email         VARCHAR(100),
    phone         VARCHAR(20),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ----------------------------------------------------------
-- destinations: places tourists can read about (Home /
-- Destinations pages).
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS destinations (
    destination_id INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    location       VARCHAR(150),
    description    TEXT,
    tourist_info   TEXT,
    image_path     VARCHAR(255)
) ENGINE=InnoDB;

-- ----------------------------------------------------------
-- tour_packages: bookable packages, each linked to a
-- destination.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS tour_packages (
    package_id     INT AUTO_INCREMENT PRIMARY KEY,
    package_name   VARCHAR(150) NOT NULL,
    destination_id INT,
    duration       VARCHAR(50),
    price          DECIMAL(10,2) NOT NULL,
    description    TEXT,
    image_path     VARCHAR(255),
    CONSTRAINT fk_package_destination FOREIGN KEY (destination_id)
        REFERENCES destinations(destination_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ----------------------------------------------------------
-- bookings: one row per confirmed booking, tied to the
-- exact user who made it (user_id) and the package booked
-- (package_id). This is what keeps each user's bookings
-- separate from every other user's bookings.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
    booking_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    package_id     INT NOT NULL,
    travel_date    DATE NOT NULL,
    num_people     INT NOT NULL,
    total_amount   DECIMAL(10,2) NOT NULL,
    contact_phone  VARCHAR(20),
    contact_email  VARCHAR(100),
    status         VARCHAR(20) DEFAULT 'CONFIRMED',
    booking_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_package FOREIGN KEY (package_id)
        REFERENCES tour_packages(package_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------
-- contact_messages: messages submitted through the Contact
-- page.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS contact_messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100),
    email      VARCHAR(100),
    subject    VARCHAR(150),
    message    TEXT,
    sent_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
