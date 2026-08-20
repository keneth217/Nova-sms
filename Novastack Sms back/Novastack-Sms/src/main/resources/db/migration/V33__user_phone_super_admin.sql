ALTER TABLE users
    ADD COLUMN phone VARCHAR(30) NULL;

UPDATE users
SET phone = '254711766223'
WHERE role = 'SUPER_ADMIN'
  AND (phone IS NULL OR phone = '');
