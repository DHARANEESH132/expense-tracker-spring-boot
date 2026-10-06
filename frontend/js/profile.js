/**
 * User Profile Controller
 */

async function loadProfile() {
  if (!auth.requireAuth()) return;

  try {
    const res = await api.get('/auth/me');
    const user = res.data.user;

    // Update form inputs and labels
    const inputName = document.getElementById('profileName');
    const inputEmail = document.getElementById('profileEmail');
    const displayCreated = document.getElementById('profileCreatedAt');
    const displayHeaderName = document.getElementById('profileCardName');
    const displayHeaderEmail = document.getElementById('profileCardEmail');

    if (inputName) inputName.value = user.name || '';
    if (inputEmail) inputEmail.value = user.email || '';
    if (displayHeaderName) displayHeaderName.textContent = user.name || 'User';
    if (displayHeaderEmail) displayHeaderEmail.textContent = user.email || '';

    if (displayCreated && user.createdAt) {
      const dateStr = new Date(user.createdAt).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
      displayCreated.textContent = `Member since ${dateStr}`;
    }

    // Keep localStorage updated
    auth.setSession(auth.getToken(), user);
    auth.populateUserUI();

  } catch (err) {
    showToast(err.message || 'Failed to load profile details.', 'error');
  }
}

async function handleUpdateProfile(e) {
  e.preventDefault();
  const btn = document.getElementById('btnSaveProfile');
  btn.disabled = true;

  const newName = document.getElementById('profileName').value.trim();

  try {
    const res = await api.put('/auth/profile', { name: newName });
    showToast('Profile updated successfully!', 'success');

    const updatedUser = res.data.user;
    auth.setSession(auth.getToken(), updatedUser);
    auth.populateUserUI();

    document.getElementById('profileCardName').textContent = updatedUser.name;
  } catch (err) {
    showToast(err.message || 'Failed to update profile.', 'error');
  } finally {
    btn.disabled = false;
  }
}

async function handleChangePassword(e) {
  e.preventDefault();
  const btn = document.getElementById('btnChangePassword');
  btn.disabled = true;

  const currentPassword = document.getElementById('currentPassword').value;
  const newPassword = document.getElementById('newPassword').value;
  const confirmPassword = document.getElementById('confirmPassword').value;

  if (newPassword !== confirmPassword) {
    showToast('New passwords do not match.', 'error');
    btn.disabled = false;
    return;
  }

  if (newPassword.length < 6) {
    showToast('New password must be at least 6 characters long.', 'error');
    btn.disabled = false;
    return;
  }

  try {
    await api.put('/auth/change-password', {
      currentPassword,
      newPassword
    });

    showToast('Password changed successfully.', 'success');
    document.getElementById('passwordForm').reset();
  } catch (err) {
    showToast(err.message || 'Failed to change password.', 'error');
  } finally {
    btn.disabled = false;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  if (!auth.requireAuth()) return;

  loadProfile();

  const profileForm = document.getElementById('profileForm');
  if (profileForm) {
    profileForm.addEventListener('submit', handleUpdateProfile);
  }

  const passwordForm = document.getElementById('passwordForm');
  if (passwordForm) {
    passwordForm.addEventListener('submit', handleChangePassword);
  }
});
