-- THIS IS AN EXAMPLE SCRIPT!
-- PLEASE, FOR THE LOVE OF GOD, DO NOT DO SOMETHING LIKE THIS IS PRODUCTION!
-- Create groups
CREATE
  ROLE read_only;

CREATE
  ROLE read_write;

-- Grant access to existing tables
GRANT USAGE ON
SCHEMA public TO read_only;

GRANT USAGE ON
SCHEMA public TO read_write;

-- Grant privileges to existing tables
GRANT SELECT
  ON
  ALL TABLES IN SCHEMA public TO read_only;

GRANT SELECT
  ,
  INSERT
    ,
    UPDATE
      ,
      DELETE
        ,
        TRUNCATE ON
        ALL TABLES IN SCHEMA public TO read_write;

-- Grant privileges to future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT
  ON
  TABLES TO read_only;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT
  ,
  INSERT
    ,
    UPDATE
      ,
      DELETE
        ,
        TRUNCATE ON
        TABLES TO read_write;

-- Create users with passwords
CREATE
  USER test_ro_user WITH PASSWORD 'test_ro_password';

CREATE
  USER test_rw_user WITH PASSWORD 'test_rw_password';

-- Grant previous roles to users
GRANT read_only TO test_ro_user;

GRANT read_write TO test_rw_user;