
-- ── CREAR MULTA MANUALMENTE ──────────────────────────────────────────────
-- 1. Mira el id del loan que quieres asociar a la multa
SELECT id, due_date FROM loans WHERE user_id = (SELECT id FROM users WHERE email = 'multas@multas.com');

-- 2. Inserta la multa (sustituye loan_id por el id del loan)
INSERT INTO fines (loan_id, user_id, days_overdue, penalty_days, penalty_until, created_at)
VALUES (
    2,               --loan_id (ejemplo, sustituye por el id real)
    (SELECT id FROM users WHERE email = 'multas@multas.com'),
    10,
    27,
    '2026-04-10',   -- 7 días base + (10 días × 2) = 27 días de penalización
    NOW()
);

-- 3. Actualiza el penalty_until del usuario para que coincida
UPDATE users 
SET penalty_until = '2026-04-10' 
WHERE email = 'multas@multas.com';



-- Elimina las multas de prueba
DELETE FROM fines WHERE user_id = (SELECT id FROM users WHERE email = 'multas@multas.com');
-- Resetea la penalización
UPDATE users SET penalty_until = NULL WHERE email = 'multas@multas.com';



-- ── MODIFICAR MULTA ──────────────────────────────────────────────────────
UPDATE fines 
SET 
    days_overdue = 15,
    penalty_days = 37,           -- 7 + (15 × 2) = 37 días
    penalty_until = '2026-05-02'
WHERE id = 9;

-- Actualizar también penaltyUntil del usuario
UPDATE users 
SET penalty_until = '2026-05-09'
WHERE id = 9;


-- ── ELIMINAR MULTA ───────────────────────────────────────────────────────
-- Limpiar penalización del usuario primero
UPDATE users 
SET penalty_until = NULL
WHERE id = (SELECT user_id FROM fines WHERE id = 9);

-- Eliminar la multa
DELETE FROM fines WHERE id = 9;


-- ── HACER USUARIO ADMIN ──────────────────────────────────────────────────
UPDATE users 
SET role = 'ADMIN' 
WHERE email = 'anna@bookmania.com';

-- Verificar
SELECT id, name, email, role FROM users;