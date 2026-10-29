(() => {
  const toastEl = document.getElementById('appToast');
  const toastBody = document.getElementById('appToastBody');
  const toast = toastEl ? new bootstrap.Toast(toastEl, { delay: 2600 }) : null;

  function showToast(message) {
    if (!toast || !toastBody) return;
    toastBody.textContent = message;
    toast.show();
  }

  function showScreen(name) {
    document.querySelectorAll('.screen').forEach((screen) => {
      screen.classList.toggle('active', screen.id === `screen-${name}`);
    });
    document.querySelectorAll('#screenNav [data-screen]').forEach((link) => {
      link.classList.toggle('active', link.dataset.screen === name);
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  document.querySelectorAll('[data-screen]').forEach((element) => {
    element.addEventListener('click', (event) => {
      event.preventDefault();
      showScreen(element.dataset.screen);
    });
  });

  const themeToggle = document.getElementById('themeToggle');
  themeToggle?.addEventListener('click', () => {
    const root = document.documentElement;
    const dark = root.getAttribute('data-bs-theme') === 'dark';
    root.setAttribute('data-bs-theme', dark ? 'light' : 'dark');
    themeToggle.innerHTML = dark
      ? '<i class="bi bi-moon-stars"></i>'
      : '<i class="bi bi-sun"></i>';
  });

  document.querySelectorAll('[data-theme-set]').forEach((swatch) => {
    swatch.addEventListener('click', () => {
      const theme = swatch.dataset.themeSet;
      document.documentElement.classList.toggle('theme-teal', theme === 'teal');
      document.documentElement.setAttribute('data-bs-theme', theme === 'dark' ? 'dark' : 'light');
      if (themeToggle) {
        themeToggle.innerHTML = theme === 'dark'
          ? '<i class="bi bi-sun"></i>'
          : '<i class="bi bi-moon-stars"></i>';
      }
      showToast(theme === 'teal' ? 'Tema Océano (demo).' : `Tema ${theme === 'dark' ? 'oscuro' : 'claro'} (demo).`);
    });
  });

  document.querySelectorAll('form[data-demo-form]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      event.preventDefault();
      if (!form.reportValidity()) return;
      const modal = form.closest('.modal');
      if (modal) {
        bootstrap.Modal.getOrCreateInstance(modal).hide();
      }
      form.reset();
      showToast(form.dataset.demoMessage || 'Guardado (demo).');
    });
  });

  document.querySelectorAll('[data-demo-action]').forEach((button) => {
    button.addEventListener('click', () => showToast(button.dataset.demoAction || 'Acción (demo).'));
  });

  const refreshButtons = [document.getElementById('refreshRates'), document.getElementById('refreshRates2')].filter(Boolean);
  refreshButtons.forEach((button) => {
    button.addEventListener('click', () => {
      const original = button.innerHTML;
      button.disabled = true;
      button.innerHTML = '<span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Actualizando';
      window.setTimeout(() => {
        button.disabled = false;
        button.innerHTML = original;
        showToast('Referencias actualizadas: BCV, paralelo y USDT (demo).');
      }, 900);
    });
  });

  ['baseCurrency', 'secondaryCurrency'].forEach((id) => {
    document.getElementById(id)?.addEventListener('change', (event) => {
      showToast(`Divisa actualizada a ${event.target.value.split(' · ')[0]} (demo).`);
    });
  });
})();
