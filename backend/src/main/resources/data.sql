INSERT INTO mvc_users (username)
SELECT 'ivan'
WHERE NOT EXISTS (SELECT 1 FROM mvc_users WHERE username = 'ivan');

INSERT INTO mvc_users (username)
SELECT 'maria'
WHERE NOT EXISTS (SELECT 1 FROM mvc_users WHERE username = 'maria');
