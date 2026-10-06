/**
 * Dashboard Controller and Chart Visualizations
 */

let categoryChartInstance = null;
let monthlyChartInstance = null;

async function loadDashboard() {
  if (!auth.requireAuth()) return;

  const now = new Date();
  const currentMonth = now.getMonth() + 1;
  const currentYear = now.getFullYear();

  // Current month date range for current month stats
  const startOfMonth = new Date(currentYear, now.getMonth(), 1).toISOString().split('T')[0];
  const endOfMonth = new Date(currentYear, now.getMonth() + 1, 0).toISOString().split('T')[0];

  try {
    // Fetch dashboard data concurrently
    const [
      allTimeSummaryRes,
      monthSummaryRes,
      categoryRes,
      monthlyRes,
      budgetsRes,
      recentExpensesRes
    ] = await Promise.all([
      api.get('/expenses/summary'),
      api.get('/expenses/summary', { startDate: startOfMonth, endDate: endOfMonth }),
      api.get('/expenses/category-summary'),
      api.get('/expenses/monthly-summary', { months: 6 }),
      api.get('/budgets', { month: currentMonth, year: currentYear }),
      api.get('/expenses', { limit: 5, sort: 'newest' })
    ]);

    // 1. Populate Metric Cards
    renderMetrics(allTimeSummaryRes.data, monthSummaryRes.data);

    // 2. Render Category Doughnut Chart
    renderCategoryChart(categoryRes.data);

    // 3. Render Monthly Spending Bar Chart
    renderMonthlyChart(monthlyRes.data);

    // 4. Render Budget Health Widget
    renderBudgetStatus(budgetsRes.data);

    // 5. Render Recent Transactions Table
    renderRecentTransactions(recentExpensesRes.data);

  } catch (err) {
    showToast(err.message || 'Failed to load dashboard metrics.', 'error');
  }
}

function renderMetrics(allTime, currentMonth) {
  const elTotal = document.getElementById('statTotalSpend');
  const elMonth = document.getElementById('statMonthSpend');
  const elAvg = document.getElementById('statAvgExpense');
  const elHigh = document.getElementById('statHighExpense');

  if (elTotal) elTotal.textContent = formatCurrency(allTime?.totalExpenses || 0);
  if (elMonth) elMonth.textContent = formatCurrency(currentMonth?.totalExpenses || 0);
  if (elAvg) elAvg.textContent = formatCurrency(allTime?.averageExpense || 0);
  if (elHigh) elHigh.textContent = formatCurrency(allTime?.highestExpense || 0);
}

function renderCategoryChart(data) {
  const canvas = document.getElementById('categoryChart');
  if (!canvas || typeof Chart === 'undefined') return;

  const categories = data?.categories || [];
  const emptyPlaceholder = document.getElementById('categoryChartEmpty');

  if (categories.length === 0) {
    canvas.style.display = 'none';
    if (emptyPlaceholder) emptyPlaceholder.style.display = 'flex';
    return;
  }

  canvas.style.display = 'block';
  if (emptyPlaceholder) emptyPlaceholder.style.display = 'none';

  const labels = categories.map(c => c.category);
  const values = categories.map(c => c.total);

  const palette = [
    '#4f46e5', '#06b6d4', '#10b981', '#f59e0b',
    '#ec4899', '#8b5cf6', '#3b82f6', '#f97316', '#64748b'
  ];

  if (categoryChartInstance) {
    categoryChartInstance.destroy();
  }

  categoryChartInstance = new Chart(canvas, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{
        data: values,
        backgroundColor: palette.slice(0, labels.length),
        borderWidth: 2,
        borderColor: '#ffffff'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right',
          labels: { boxWidth: 12, padding: 12, font: { size: 12 } }
        },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const val = ctx.raw || 0;
              const pct = categories[ctx.dataIndex]?.percentage || 0;
              return ` ${ctx.label}: ${formatCurrency(val)} (${pct}%)`;
            }
          }
        }
      },
      cutout: '68%'
    }
  });
}

function renderMonthlyChart(monthlyData) {
  const canvas = document.getElementById('monthlyChart');
  if (!canvas || typeof Chart === 'undefined') return;

  const items = monthlyData || [];
  const labels = items.map(m => m.label);
  const values = items.map(m => m.total);

  if (monthlyChartInstance) {
    monthlyChartInstance.destroy();
  }

  monthlyChartInstance = new Chart(canvas, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Spending',
        data: values,
        backgroundColor: '#4f46e5',
        borderRadius: 6,
        maxBarThickness: 40
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => ` Spending: ${formatCurrency(ctx.raw)}`
          }
        }
      },
      scales: {
        x: { grid: { display: false } },
        y: {
          beginAtZero: true,
          ticks: {
            callback: (val) => '₹' + val.toLocaleString('en-IN')
          },
          grid: { color: '#f1f5f9' }
        }
      }
    }
  });
}

function renderBudgetStatus(budgets) {
  const container = document.getElementById('dashboardBudgetList');
  if (!container) return;

  if (!budgets || budgets.length === 0) {
    container.innerHTML = `
      <div class="empty-state" style="padding: 1.5rem 0;">
        <p class="empty-state-desc">No budgets set for this month.</p>
        <a href="budgets.html" class="btn btn-sm btn-secondary">+ Set a Budget</a>
      </div>
    `;
    return;
  }

  container.innerHTML = budgets.map(b => {
    const fillPercent = Math.min(100, b.percentageUsed);
    const progressClass = b.status === 'exceeded' ? 'progress-exceeded' : (b.status === 'warning' ? 'progress-warning' : 'progress-normal');

    return `
      <div class="budget-item">
        <div class="budget-item-header">
          <div>
            <span class="budget-item-title">${escapeHtml(b.category === 'All' ? 'Overall Budget' : b.category)}</span>
            <span class="badge badge-${b.status}" style="margin-left: 0.5rem; font-size: 0.7rem;">
              ${b.percentageUsed}% used
            </span>
          </div>
          <div class="budget-item-amounts">
            <strong>${formatCurrency(b.spent)}</strong> of ${formatCurrency(b.amount)}
          </div>
        </div>
        <div class="progress-track">
          <div class="progress-fill ${progressClass}" style="width: ${fillPercent}%;"></div>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 0.775rem; color: var(--text-muted); margin-top: 0.35rem;">
          <span>Remaining: ${formatCurrency(b.remaining)}</span>
          ${b.status === 'exceeded' ? '<span style="color: var(--danger); font-weight: 600;">Exceeded</span>' : ''}
          ${b.status === 'warning' ? '<span style="color: var(--warning); font-weight: 600;">Approaching Limit</span>' : ''}
        </div>
      </div>
    `;
  }).join('');
}

function renderRecentTransactions(expenses) {
  const tbody = document.getElementById('recentTransactionsBody');
  if (!tbody) return;

  if (!expenses || expenses.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="4" class="empty-state" style="padding: 2rem 0;">
          <p class="empty-state-desc">No expenses recorded yet.</p>
          <a href="expenses.html" class="btn btn-sm btn-primary">+ Add First Expense</a>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = expenses.map(exp => {
    const formattedDate = new Date(exp.date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric'
    });

    return `
      <tr>
        <td style="color: var(--text-muted);">${formattedDate}</td>
        <td>
          <span class="badge badge-${escapeHtml(exp.category)}">${escapeHtml(exp.category)}</span>
        </td>
        <td style="font-weight: 500;">${escapeHtml(exp.description)}</td>
        <td style="text-align: right; font-weight: 600; color: var(--text-main);">
          ${formatCurrency(exp.amount)}
        </td>
      </tr>
    `;
  }).join('');
}

document.addEventListener('DOMContentLoaded', loadDashboard);
