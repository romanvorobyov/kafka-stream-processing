CREATE TABLE IF NOT EXISTS account
(
    id BIGINT NOT NULL PRIMARY KEY,
    "value" BIGINT NOT NULL,
    version SMALLINT NOT NULL DEFAULT 0,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);