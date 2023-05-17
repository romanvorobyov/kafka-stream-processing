CREATE SEQUENCE IF NOT EXISTS history_seq
  START WITH 1
  INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS history
(
    id   BIGINT DEFAULT nextval('history_seq') PRIMARY KEY,
    account_id BIGINT NOT NULL,
    diff BIGINT NOT NULL,
    "value" BIGINT NOT NULL,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_HISTORY_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES account (id)
);