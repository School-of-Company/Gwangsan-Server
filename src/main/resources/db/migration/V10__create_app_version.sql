CREATE TABLE IF NOT EXISTS tbl_app_version (
    platform VARCHAR(7) NOT NULL PRIMARY KEY,
    latest_version VARCHAR(32) NOT NULL,
    minimum_version VARCHAR(32) NOT NULL
);
