(function () {
  const status = document.querySelector('.copy-status');

  function absoluteUrl(path) {
    return new URL(path, window.location.href).href;
  }

  async function copyPath(path) {
    const url = absoluteUrl(path);
    try {
      await navigator.clipboard.writeText(url);
      if (status) status.textContent = 'Copied: ' + url;
    } catch (error) {
      if (status) status.textContent = url;
    }
  }

  document.querySelectorAll('[data-copy]').forEach(function (button) {
    button.addEventListener('click', function () {
      copyPath(button.getAttribute('data-copy'));
    });
  });

  const lab = document.querySelector('.motion-lab');
  const readout = document.querySelector('.motion-readout');
  const speed = document.querySelector('[data-motion-speed]');
  if (!lab) return;

  document.querySelectorAll('[data-motion-mode]').forEach(function (button) {
    button.addEventListener('click', function () {
      const mode = button.getAttribute('data-motion-mode');
      lab.dataset.motion = mode;
      document.querySelectorAll('[data-motion-mode]').forEach(function (item) {
        item.classList.toggle('is-selected', item === button);
      });
      if (readout) readout.textContent = mode === 'spin' ? 'orbit / 8s' : mode === 'breathe' ? 'breathe / 2.8s' : 'still / paused';
    });
  });

  if (speed) {
    speed.addEventListener('input', function () {
      lab.style.setProperty('--speed', speed.value);
    });
  }
}());
