ALTER TABLE journal_entries
    ADD COLUMN reversal_of_entry_id UUID;

ALTER TABLE journal_entries
    ADD CONSTRAINT fk_journal_entries_reversal_of_entry
        FOREIGN KEY (reversal_of_entry_id) REFERENCES journal_entries (id);

CREATE INDEX idx_journal_entries_reversal_of_entry ON journal_entries (reversal_of_entry_id);
