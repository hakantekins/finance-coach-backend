package com.financecoach.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/**
 * PostgreSQL üzerindeki 'v_user_dashboard_summary' view'una karşılık gelen entity.
 * @Immutable: Bu tabloya yazma işlemi yapılamaz, sadece okuma amaçlıdır.
 */
@Entity
@Immutable
@Table(name = "v_user_dashboard_summary")
@Getter
public class DashboardSummary {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    private String email;

    @Column(name = "declared_monthly_income")
    private BigDecimal declaredMonthlyIncome;

    @Column(name = "monthly_savings_goal")
    private BigDecimal monthlySavingsGoal;

    @Column(name = "total_income")
    private BigDecimal totalIncome;

    @Column(name = "total_expense")
    private BigDecimal totalExpense;

    @Column(name = "net_balance")
    private BigDecimal netBalance;

    @Column(name = "current_month_expense_total")
    private BigDecimal currentMonthExpenseTotal;

    @Column(name = "potential_monthly_savings")
    private BigDecimal potentialMonthlySavings;

    @Column(name = "savings_goal_progress_pct")
    private Double savingsGoalProgressPct;

    @Column(name = "smart_spending_score")
    private Double smartSpendingScore;
}