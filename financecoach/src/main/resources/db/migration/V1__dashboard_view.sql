-- ============================================================
-- Dashboard Özet VIEW'u
-- Bu VIEW, DashboardSummary entity'si tarafından okunur.
-- Yeni ortama deploy'da bu SQL çalıştırılmalıdır.
--
-- Hesaplama mantığı:
-- - total_income/expense: Tüm zamanların toplamı
-- - current_month_expense_total: Bu aydaki toplam gider
-- - potential_monthly_savings: P4/P5/P6 öncelikli harcamaların %30'u
-- - savings_goal_progress_pct: (gelir - bu ay gider) / hedef * 100
-- - smart_spending_score: 100 - (optimize edilebilir / toplam * 100)
-- ============================================================

CREATE OR REPLACE VIEW v_user_dashboard_summary AS
WITH transaction_totals AS (
    SELECT
        t.user_id,
        COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'INCOME'), 0) AS total_income,
        COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'EXPENSE'), 0) AS total_expense
    FROM transactions t
    GROUP BY t.user_id
),
current_month_data AS (
    SELECT
        t.user_id,
        COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'EXPENSE'), 0) AS current_month_total,
        COALESCE(SUM(t.amount) FILTER (
            WHERE t.type = 'EXPENSE'
              AND (t.description LIKE '%P4%' OR t.description LIKE '%P5%' OR t.description LIKE '%P6%')
        ), 0) AS current_month_optimizable
    FROM transactions t
    WHERE DATE_TRUNC('month', t.transaction_date::timestamp) = DATE_TRUNC('month', NOW())
    GROUP BY t.user_id
)
SELECT
    u.id                    AS user_id,
    u.full_name,
    u.email,
    u.monthly_income        AS declared_monthly_income,
    u.monthly_savings_goal,

    COALESCE(tt.total_income, 0)                                    AS total_income,
    COALESCE(tt.total_expense, 0)                                   AS total_expense,
    COALESCE(tt.total_income, 0) - COALESCE(tt.total_expense, 0)    AS net_balance,

    COALESCE(cmd.current_month_total, 0)                            AS current_month_expense_total,
    ROUND(COALESCE(cmd.current_month_optimizable, 0) * 0.30, 2)    AS potential_monthly_savings,

    -- Tasarruf hedefi ilerleme yüzdesi (0-100 arasında clamp)
    CASE
        WHEN u.monthly_savings_goal > 0 THEN
            LEAST(GREATEST(
                ROUND(((u.monthly_income - COALESCE(cmd.current_month_total, 0)) / u.monthly_savings_goal) * 100, 1),
            0), 100)
        ELSE 0
    END AS savings_goal_progress_pct,

    -- Akıllı harcama skoru (100 = mükemmel, 0 = kötü)
    CASE
        WHEN COALESCE(cmd.current_month_total, 0) > 0 THEN
            ROUND(100 - (COALESCE(cmd.current_month_optimizable, 0) / cmd.current_month_total * 100), 1)
        ELSE 100
    END AS smart_spending_score

FROM users u
LEFT JOIN transaction_totals tt  ON tt.user_id = u.id
LEFT JOIN current_month_data cmd ON cmd.user_id = u.id;