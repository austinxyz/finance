SELECT
    u.id,
    u.username,
    u.family_id,
    f.family_name
FROM users u
LEFT JOIN families f ON u.family_id = f.id;
