/**
 * Expense Management Controller
 */

const state = {
  page: 1,
  limit: 10,
  search: '',
  category: '',
  paymentMethod: '',
  startDate: '',
  endDate: '',
  minAmount: '',
  maxAmount: '',
  sort: 'newest'
};

let editingExpenseId = null;
let deletingExpenseId = null;
let searchDebounceTimer = null;

async function loadExpenses() {
  if (!auth.requireAuth()) return;

  const tableBody = document.getElementById('expensesTableBody');
  const paginationInfo = document.getElementById('paginationInfo');
  const btnPrev = document.getElementById('btnPrevPage');
  const btnNext = document.getElementById('btnNextPage');

  if (!tableBody) return;

  tableBody.innerHTML = `
    <tr>
      <td colspan="7" style="text-align: center; padding: 2rem;">
        <span class="spinner" style="border-top-color: var(--primary); width: 24px; height: 24px;"></span>
      </td>
    </tr>
  `;

  try {
    const res = await api.get('/expenses', state);
    const expenses = res.data || [];
    const pagination = res.pagination || { currentPage: 1, totalPages: 1, totalItems: 0 };

    if (expenses.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="7" class="empty-state">
            <div class="empty-state-icon">💸</div>
            <div class="empty-state-title">No expenses found</div>
            <p class="empty-state-desc">Try adjusting your filters or search query, or record a new expense.</p>
          </td>
        </tr>
      `;
    } else {
      tableBody.innerHTML = expenses.map(exp => {
        const dateStr = new Date(exp.date).toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric'
        });

        return `
          <tr>
            <td style="color: var(--text-muted); font-size: 0.85rem;">${dateStr}</td>
            <td>
              <span class="badge badge-${escapeHtml(exp.category)}">${escapeHtml(exp.category)}</span>
            </td>
            <td>
              <div style="font-weight: 500;">${escapeHtml(exp.description)}</div>
              ${exp.notes ? `<div style="font-size: 0.775rem; color: var(--text-muted);">${escapeHtml(exp.notes)}</div>` : ''}
            </td>
            <td style="font-size: 0.85rem; color: var(--secondary);">${escapeHtml(exp.paymentMethod)}</td>
            <td style="font-weight: 600; text-align: right; color: var(--text-main);">
              ${formatCurrency(exp.amount)}
            </td>
            <td style="text-align: right;">
              <button class="btn btn-sm btn-secondary" onclick="openEditModal('${exp._id}')" title="Edit">
                ✏️
              </button>
              <button class="btn btn-sm btn-outline-danger" onclick="openDeleteModal('${exp._id}')" title="Delete" style="margin-left: 0.35rem;">
                🗑️
              </button>
            </td>
          </tr>
        `;
      }).join('');
    }

    // Update pagination controls
    if (paginationInfo) {
      const startItem = pagination.totalItems === 0 ? 0 : (pagination.currentPage - 1) * state.limit + 1;
      const endItem = Math.min(pagination.totalItems, pagination.currentPage * state.limit);
      paginationInfo.textContent = `Showing ${startItem}-${endItem} of ${pagination.totalItems} entries`;
    }

    if (btnPrev) btnPrev.disabled = pagination.currentPage <= 1;
    if (btnNext) btnNext.disabled = pagination.currentPage >= pagination.totalPages;

  } catch (err) {
    showToast(err.message || 'Failed to load expenses.', 'error');
  }
}

// Modal Handlers
function openAddModal() {
  editingExpenseId = null;
  document.getElementById('expenseModalTitle').textContent = 'Add New Expense';
  document.getElementById('expenseForm').reset();
  document.getElementById('expenseDate').value = new Date().toISOString().split('T')[0];
  document.getElementById('expenseModalBackdrop').classList.add('active');
}

async function openEditModal(id) {
  try {
    const res = await api.get(`/expenses/${id}`);
    const exp = res.data;
    editingExpenseId = id;

    document.getElementById('expenseModalTitle').textContent = 'Edit Expense';
    document.getElementById('expenseAmount').value = exp.amount;
    document.getElementById('expenseCategory').value = exp.category;
    document.getElementById('expenseDescription').value = exp.description;
    document.getElementById('expensePaymentMethod').value = exp.paymentMethod;
    document.getElementById('expenseDate').value = new Date(exp.date).toISOString().split('T')[0];
    document.getElementById('expenseNotes').value = exp.notes || '';

    document.getElementById('expenseModalBackdrop').classList.add('active');
  } catch (err) {
    showToast(err.message || 'Failed to fetch expense details.', 'error');
  }
}

function closeExpenseModal() {
  document.getElementById('expenseModalBackdrop').classList.remove('active');
  editingExpenseId = null;
}

function openDeleteModal(id) {
  deletingExpenseId = id;
  document.getElementById('deleteModalBackdrop').classList.add('active');
}

function closeDeleteModal() {
  document.getElementById('deleteModalBackdrop').classList.remove('active');
  deletingExpenseId = null;
}

async function confirmDeleteExpense() {
  if (!deletingExpenseId) return;
  const btn = document.getElementById('btnConfirmDelete');
  btn.disabled = true;

  try {
    await api.delete(`/expenses/${deletingExpenseId}`);
    showToast('Expense deleted successfully.', 'success');
    closeDeleteModal();
    loadExpenses();
  } catch (err) {
    showToast(err.message || 'Failed to delete expense.', 'error');
  } finally {
    btn.disabled = false;
  }
}

async function handleExpenseFormSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById('btnSaveExpense');
  btn.disabled = true;

  const payload = {
    amount: parseFloat(document.getElementById('expenseAmount').value),
    category: document.getElementById('expenseCategory').value,
    description: document.getElementById('expenseDescription').value.trim(),
    paymentMethod: document.getElementById('expensePaymentMethod').value,
    date: document.getElementById('expenseDate').value,
    notes: document.getElementById('expenseNotes').value.trim()
  };

  try {
    if (editingExpenseId) {
      await api.put(`/expenses/${editingExpenseId}`, payload);
      showToast('Expense updated successfully.', 'success');
    } else {
      await api.post('/expenses', payload);
      showToast('Expense created successfully.', 'success');
    }

    closeExpenseModal();
    loadExpenses();
  } catch (err) {
    showToast(err.message || 'Error saving expense.', 'error');
  } finally {
    btn.disabled = false;
  }
}

async function exportExpensesCsv() {
  try {
    showToast('Preparing CSV export...', 'info', 2000);
    await api.download('/expenses/export', `expenses_${new Date().toISOString().split('T')[0]}.csv`);
    showToast('CSV downloaded successfully.', 'success');
  } catch (err) {
    showToast(err.message || 'Failed to export CSV.', 'error');
  }
}

// Event Listeners initialization
document.addEventListener('DOMContentLoaded', () => {
  if (!auth.requireAuth()) return;

  loadExpenses();

  // Search input with debounce
  const searchInput = document.getElementById('filterSearch');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      clearTimeout(searchDebounceTimer);
      searchDebounceTimer = setTimeout(() => {
        state.search = e.target.value.trim();
        state.page = 1;
        loadExpenses();
      }, 350);
    });
  }

  // Filter dropdowns
  const categoryFilter = document.getElementById('filterCategory');
  if (categoryFilter) {
    categoryFilter.addEventListener('change', (e) => {
      state.category = e.target.value;
      state.page = 1;
      loadExpenses();
    });
  }

  const methodFilter = document.getElementById('filterPaymentMethod');
  if (methodFilter) {
    methodFilter.addEventListener('change', (e) => {
      state.paymentMethod = e.target.value;
      state.page = 1;
      loadExpenses();
    });
  }

  const sortFilter = document.getElementById('filterSort');
  if (sortFilter) {
    sortFilter.addEventListener('change', (e) => {
      state.sort = e.target.value;
      state.page = 1;
      loadExpenses();
    });
  }

  // Date filters
  const startInput = document.getElementById('filterStartDate');
  if (startInput) {
    startInput.addEventListener('change', (e) => {
      state.startDate = e.target.value;
      state.page = 1;
      loadExpenses();
    });
  }

  const endInput = document.getElementById('filterEndDate');
  if (endInput) {
    endInput.addEventListener('change', (e) => {
      state.endDate = e.target.value;
      state.page = 1;
      loadExpenses();
    });
  }

  // Amount filters
  const minAmt = document.getElementById('filterMinAmount');
  if (minAmt) {
    minAmt.addEventListener('change', (e) => {
      state.minAmount = e.target.value;
      state.page = 1;
      loadExpenses();
    });
  }

  const maxAmt = document.getElementById('filterMaxAmount');
  if (maxAmt) {
    maxAmt.addEventListener('change', (e) => {
      state.maxAmount = e.target.value;
      state.page = 1;
      loadExpenses();
    });
  }

  // Reset Filters
  const btnReset = document.getElementById('btnResetFilters');
  if (btnReset) {
    btnReset.addEventListener('click', () => {
      state.page = 1;
      state.search = '';
      state.category = '';
      state.paymentMethod = '';
      state.startDate = '';
      state.endDate = '';
      state.minAmount = '';
      state.maxAmount = '';
      state.sort = 'newest';

      document.querySelectorAll('.filter-bar input, .filter-bar select').forEach(input => {
        input.value = '';
      });
      document.getElementById('filterSort').value = 'newest';
      loadExpenses();
    });
  }

  // Pagination Next / Prev
  const btnPrev = document.getElementById('btnPrevPage');
  if (btnPrev) {
    btnPrev.addEventListener('click', () => {
      if (state.page > 1) {
        state.page--;
        loadExpenses();
      }
    });
  }

  const btnNext = document.getElementById('btnNextPage');
  if (btnNext) {
    btnNext.addEventListener('click', () => {
      state.page++;
      loadExpenses();
    });
  }

  // Expense Modal Form
  const expenseForm = document.getElementById('expenseForm');
  if (expenseForm) {
    expenseForm.addEventListener('submit', handleExpenseFormSubmit);
  }

  // Delete Confirmation
  const btnDeleteConfirm = document.getElementById('btnConfirmDelete');
  if (btnDeleteConfirm) {
    btnDeleteConfirm.addEventListener('click', confirmDeleteExpense);
  }

  // CSV Export Button
  const btnExport = document.getElementById('btnExportCsv');
  if (btnExport) {
    btnExport.addEventListener('click', exportExpensesCsv);
  }
});
