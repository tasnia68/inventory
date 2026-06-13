CREATE OR REPLACE FUNCTION prevent_posted_journal_entry_update()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'POSTED' THEN
        IF NEW.status = 'REVERSED'
           AND NEW.entry_number = OLD.entry_number
           AND NEW.journal_id = OLD.journal_id
           AND NEW.financial_event_id IS NOT DISTINCT FROM OLD.financial_event_id
           AND NEW.reversal_of_entry_id IS NOT DISTINCT FROM OLD.reversal_of_entry_id
           AND NEW.entry_date = OLD.entry_date
           AND NEW.source_document_type = OLD.source_document_type
           AND NEW.source_document_id = OLD.source_document_id
           AND NEW.source_document_number IS NOT DISTINCT FROM OLD.source_document_number
           AND NEW.memo IS NOT DISTINCT FROM OLD.memo
           AND NEW.currency = OLD.currency
           AND NEW.total_debits = OLD.total_debits
           AND NEW.total_credits = OLD.total_credits
           AND NEW.posted_at IS NOT DISTINCT FROM OLD.posted_at
           AND NEW.tenant_id = OLD.tenant_id THEN
            RETURN NEW;
        END IF;

        RAISE EXCEPTION 'Posted journal entries are immutable; create a reversal entry instead';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION prevent_posted_journal_entry_line_change()
RETURNS TRIGGER AS $$
DECLARE
    parent_status VARCHAR(32);
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT status INTO parent_status FROM journal_entries WHERE id = OLD.journal_entry_id;
    ELSE
        SELECT status INTO parent_status FROM journal_entries WHERE id = NEW.journal_entry_id;
    END IF;

    IF parent_status = 'POSTED' THEN
        RAISE EXCEPTION 'Posted journal entry lines are immutable; create a reversal entry instead';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_posted_journal_entry_update
BEFORE UPDATE ON journal_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_posted_journal_entry_update();

CREATE TRIGGER trg_prevent_posted_journal_entry_line_update
BEFORE UPDATE OR DELETE ON journal_entry_lines
FOR EACH ROW
EXECUTE FUNCTION prevent_posted_journal_entry_line_change();
