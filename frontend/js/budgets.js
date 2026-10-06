/**
 * Budget Management Controller
 */

let currentSelectedMonth = new Date().getMonth() + 1;
let currentSelectedYear = new Date().getFullYear();
let editingBudgetId = null;
let deletingBudgetId = null;

async function loadBudgets() {
  if (!auth.requireAuth()) return;

  const container = document.getElementById('budgetsCardsContainer');
  if (!container) return;

  container.innerHTML = `
    <div style="grid-column: 1/-1; text-align: center; padding: 3rem;">
      <span class="spinner" style="border-top-color: var(--primary); width: 28px; height: 28px;"></span>
    </div>
  `;

  try {
    const res = await api.get('/budgets', {
      month: currentSelectedMonth,
      year: currentSelectedYear
    });

    const budgets = res.data || [];

    if (budgets.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1/-1;" class="empty-state">
          <div class="empty-state-icon">🎯</div>
          <div class="empty-state-title">No budgets set for this month</div>
          <p class="empty-state-desc">Set an overall monthly spending limit or category-specific budgets to stay on track.</p>
          <button class="btn btn-primary" onclick="openAddBudgetModal()">+ Create First Budget</button>
        </div>
      `;
      return;
    }

    container.innerHTML = budgets.map(b => {
      const fillPercent = Math.min(100, b.percentageUsed);
      const progressClass = b.status === 'exceeded' ? 'progress-exceeded' : (b.status === 'warning' ? 'progress-warning' : 'progress-normal');

      let statusBadgeLabel = `${b.percentageUsed}% Used`;
      if (b.status === 'exceeded') {
        statusBadgeLabel = `⚠️ Exceeded (${b.percentageUsed}%)`;
      } else if (b.status === 'warning') {
        statusBadgeLabel = `⚡ Near Limit (${b.percentageUsed}%)`;
      }

      return `
        <div class="budget-card">
          <div>
            <div class="budget-card-header">
              <div>
                <h3 style="font-size: 1.1rem; font-weight: 700; color: var(--text-main);">
                  ${escapeHtml(b.category === 'All' ? 'Overall Monthly Budget' : b.category)}
                </h3>
                <span class="badge badge-${b.status}" style="margin-top: 0.35rem;">
                  ${statusBadgeLabel}
                </span>
              </div>
              <div style="display: flex; gap: 0.35rem;">
                <button class="btn btn-sm btn-secondary" onclick="openEditBudgetModal('${b._id}')" title="Edit">
                  ✏️
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="openDeleteBudgetModal('${b._id}')" title="Delete">
                  🗑️
                </button>
              </div>
            </div>

            <div style="margin: 1rem 0;">
              <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 0.4rem;">
                <span style="font-size: 1.35rem; font-weight: 700; color: var(--text-main);">
                  ${formatCurrency(b.spent)}
                </span>
                <span style="font-size: 0.85rem; color: var(--text-muted);">
                  of ${formatCurrency(b.amount)}
                </span>
              </div>
              <div class="progress-track">
                <div class="progress-fill ${progressClass}" style="width: ${fillPercent}%;"></div>
              </div>
            </div>
          </div>

          <div class="budget-card-stats">
            <div>
              <span style="color: var(--text-muted);">Remaining: </span>
              <strong style="color: ${b.remaining < 0 ? 'var(--danger)' : 'var(--text-main)'};">
                ${formatCurrency(b.remaining)}
              </strong>
            </div>
            <div>
              <span style="color: var(--text-muted);">Period: </span>
              <strong>${b.month}/${b.year}</strong>
            </div>
          </div>
        </div>
      `;
    }).join('');

  } catch (err) {
    showToast(err.message || 'Failed to load budgets.', 'error');
  }
}

// Modal Handlers
function openAddBudgetModal() {
  editingBudgetId = null;
  document.getElementById('budgetModalTitle').textContent = 'Create Budget';
  document.getElementById('budgetForm').reset();
  document.getElementById('budgetMonth').value = currentSelectedMonth;
  document.getElementById('budgetYear').value = currentSelectedYear;
  document.getElementById('budgetModalBackdrop').classList.add('active');
}

async function openEditBudgetModal(id) {
  try {
    const res = await api.get(`/budgets/${id}`);
    const b = res.data;
    editingBudgetId = id;

    document.getElementById('budgetModalTitle').textContent = 'Edit Budget';
    document.getElementById('budgetCategory').value = b.category;
    document.getElementById('budgetMonth').value = b.month;
    document.getElementById('budgetYear').value = b.year;
    document.getElementById('budgetAmount').value = b.amount;

    document.getElementById('budgetModalBackdrop').classList.add('active');
  } catch (err) {
    showToast(err.message || 'Failed to fetch budget details.', 'error');
  }
}

function closeBudgetModal() {
  document.getElementById('budgetModalBackdrop').classList.remove('active');
  editingBudgetId = null;
}

function openDeleteBudgetModal(id) {
  deletingBudgetId = id;
  document.getElementById('deleteBudgetModalBackdrop').classList.add('active');
}

function closeDeleteBudgetModal() {
  document.getElementById('deleteBudgetModalBackdrop').classList.remove('active');
  deletingBudgetId = null;
}

async function confirmDeleteBudget() {
  if (!deletingBudgetId) return;
  const btn = document.getElementById('btnConfirmDeleteBudget');
  btn.disabled = true;

  try {
    await api.delete(`/budgets/${deletingBudgetId}`);
    showToast('Budget deleted successfully.', 'success');
    closeDeleteBudgetModal();
    loadBudgets();
  } catch (err) {
    showToast(err.message || 'Failed to delete budget.', 'error');
  } finally {
    btn.disabled = false;
  }
}

async function handleBudgetFormSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById('btnSaveBudget');
  btn.disabled = true;

  const payload = {
    category: document.getElementById('budgetCategory').value,
    month: parseInt(document.getElementById('budgetMonth').value, 10),
    year: parseInt(document.getElementById('budgetYear').value, 10),
    amount: parseFloat(document.getElementById('budgetAmount').value)
  };

  try {
    if (editingBudgetId) {
      await api.put(`/budgets/${editingBudgetId}`, payload);
      showToast('Budget updated successfully.', 'success');
    } else {
      await api.post('/budgets', payload);
      showToast('Budget created successfully.', 'success');
    }

    closeBudgetModal();
    loadBudgets();
  } catch (err) {
    showToast(err.message || 'Error saving budget.', 'error');
  } finally {
    btn.disabled = false;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  if (!auth.requireAuth()) return;

  // Set default month & year in selectors
  const monthSelect = document.getElementById('selectPeriodMonth');
  const yearSelect = document.getElementById('selectPeriodYear');

  if (monthSelect) {
    monthSelect.value = currentSelectedMonth;
    monthSelect.addEventListener('change', (e) => {
      currentSelectedMonth = parseInt(e.target.value, 10);
      loadBudgets();
    });
  }

  if (yearSelect) {
    yearSelect.value = currentSelectedYear;
    yearSelect.addEventListener('change', (e) => {
      currentSelectedYear = parseInt(e.target.value, 10);
      loadBudgets();
    });
  }

  const budgetForm = document.getElementById('budgetForm');
  if (budgetForm) {
    budgetForm.addEventListener('submit', handleBudgetFormSubmit);
  }

  const btnConfirmDelete = document.getElementById('btnConfirmDeleteBudget');
  if (btnConfirmDelete) {
    btnConfirmDelete.addEventListener('click', confirmDeleteBudget);
  }

  loadBudgets();
});
