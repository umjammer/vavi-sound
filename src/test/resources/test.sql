CREATE DATABASE itsmo;

GRANT ALL PRIVILEGES ON itsmo.*
TO itsmo@localhost
IDENTIFIED BY 'itsmo' WITH GRANT OPTION;

CREATE TABLE user (
  unique_id INTEGER NOT NULL AUTO_INCREMENT,
  client_id INTEGER NOT NULL,
  active INTEGER NULL DEFAULT 0,
  in_active_reason TINYINT UNSIGNED NULL,
  user_name VARCHAR(20) NOT NULL,
  passwd VARCHAR(20) NOT NULL,
  user_agent VARCHAR(60) NOT NULL,
  last_accessed_date DATETIME NOT NULL,
  share_event_list_updated TINYINT UNSIGNED NULL,
  schedule_category_version INTEGER ZEROFILL NOT NULL,
  office_url VARCHAR(255) NOT NULL,
  office_user_id VARCHAR(255) NOT NULL,
  office_password VARCHAR(255) NOT NULL,
  PRIMARY KEY(unique_id)
);

CREATE TABLE anniversary (
  anniversary_id INTEGER NOT NULL AUTO_INCREMENT,
  unique_id INTEGER NOT NULL,
  name VARCHAR(20) NULL,
  date INTEGER NULL,
  PRIMARY KEY(anniversary_id),
  INDEX anniversary_0_FKIndex1(unique_id)
);

CREATE TABLE phonegroup (
 phoneGroupId  INTEGER NOT NULL AUTO_INCREMENT,
 name VARCHAR(20) NOT NULL,
 PRIMARY KEY(phoneGroupId)
);

CREATE TABLE phonedevice (
 phoneDeviceId INTEGER NOT NULL AUTO_INCREMENT,
 name VARCHAR(20) NOT NULL,
 width INTEGER NULL DEFAULT 0,
 height INTEGER NULL DEFAULT 0,
 PRIMARY KEY(phoneDeviceId)
);

CREATE TABLE phonegroup (
 phoneGroupId  INTEGER NOT NULL AUTO_INCREMENT,
 name VARCHAR(20) NOT NULL,
 PRIMARY KEY(phoneGroupId)
);

CREATE TABLE group_device (
 phoneDeviceId INTEGER NOT NULL,
 phoneGroupId  INTEGER NOT NULL,
);

