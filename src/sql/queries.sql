-- ── CREAR MULTA MANUALMENTE ──────────────────────────────────────────────
INSERT INTO fines (created_at, days_overdue, penalty_days, penalty_until, loan_id, user_id)
VALUES (
    NOW(),
    10,                          -- días de retraso
    27,                          -- 7 + (10 × 2) = 27 días de penalización
    CURRENT_DATE + INTERVAL '27 days',  -- fecha hasta la que está bloqueado
    1,                           -- id del préstamo
    1                            -- id del usuario
);

-- Actualizar penaltyUntil del usuario
UPDATE users 
SET penalty_until = CURRENT_DATE + INTERVAL '27 days'
WHERE id = 1;


-- ── MODIFICAR MULTA ──────────────────────────────────────────────────────
UPDATE fines 
SET 
    days_overdue = 15,
    penalty_days = 37,           -- 7 + (15 × 2) = 37 días
    penalty_until = CURRENT_DATE + INTERVAL '37 days'
WHERE id = 1;

-- Actualizar también penaltyUntil del usuario
UPDATE users 
SET penalty_until = CURRENT_DATE + INTERVAL '7 days'
WHERE id = 1;


-- ── ELIMINAR MULTA ───────────────────────────────────────────────────────
-- Limpiar penalización del usuario primero
UPDATE users 
SET penalty_until = NULL
WHERE id = (SELECT user_id FROM fines WHERE id = 1);

-- Eliminar la multa
DELETE FROM fines WHERE id = 1;


-- ── HACER USUARIO ADMIN ──────────────────────────────────────────────────
UPDATE users 
SET role = 'ADMIN' 
WHERE email = 'anna@bookmania.com';

-- Verificar
SELECT id, name, email, role FROM users;