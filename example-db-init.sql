-- Use the appropriate user name and database
GRANT ALL PRIVILEGES ON *.* TO 'admin'@'%' IDENTIFIED BY 'example' WITH GRANT OPTION;
USE `mysql`;
UPDATE `user` SET `host` = '%' WHERE `host` = '%';
FLUSH PRIVILEGES;
