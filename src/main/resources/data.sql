-- Fix NULL version columns for @Version (optimistic locking) support
UPDATE branch_inventory SET version = 0 WHERE version IS NULL;
