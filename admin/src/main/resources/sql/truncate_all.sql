-- Disable foreign key constraints
SET FOREIGN_KEY_CHECKS = 0;
-- Find tables to drop
SET GROUP_CONCAT_MAX_LEN=32768;
SET @tables = NULL;
SELECT GROUP_CONCAT(table_name) INTO @tables
  FROM information_schema.tables
  WHERE table_schema = (SELECT DATABASE());
SELECT IFNULL(@tables,'dummy') INTO @tables;
-- Drop tables
SET @tables = CONCAT('DROP TABLE IF EXISTS ', @tables);
PREPARE stmt FROM @tables;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- Reenable foreign key constraints
SET FOREIGN_KEY_CHECKS = 1;


--
-- Sample code for loading a temp table
--
-- Concept file.
DROP TABLE IF EXISTS tmp1;
CREATE TABLE tmp1 (
    id NUMERIC(18) UNSIGNED NOT NULL PRIMARY KEY
) CHARACTER SET utf8;

-- % mysqlimport --local -uotf -ppwd mappingservicedb tmp1.txt



