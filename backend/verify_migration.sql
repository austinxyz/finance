-- Verify Family Migration

-- 1. Check families table
SELECT '=== Families Table ===' as info;
SELECT * FROM families;

-- 2. Check users table with family relationship
SELECT '' as separator;
SELECT '=== Users with Family ===' as info;
SELECT id, username, family_id, age, annual_income, risk_tolerance FROM users;

-- 3. Verify join between users and families
SELECT '' as separator;
SELECT '=== User-Family Join ===' as info;
SELECT
    u.id as user_id,
    u.username,
    f.id as family_id,
    f.family_name,
    f.annual_expenses,
    f.emergency_fund_months
FROM users u
INNER JOIN families f ON u.family_id = f.id;
