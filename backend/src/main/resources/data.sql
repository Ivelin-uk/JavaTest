INSERT INTO users (username)
SELECT 'ivan'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'ivan');

INSERT INTO users (username)
SELECT 'maria'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'maria');
