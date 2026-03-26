ALTER TABLE sprint ADD account_id uuid NOT NULL;
ALTER TABLE sprint ADD CONSTRAINT sprint_account_fk FOREIGN KEY (account_id) REFERENCES account(account_id);
