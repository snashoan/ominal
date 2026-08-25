(function () {
  const gallery = document.querySelector('.gallery');
  if (!gallery) return;
  document.querySelectorAll('button[data-motion]').forEach(function (button) {
    button.addEventListener('click', function () {
      gallery.dataset.motion = button.getAttribute('data-motion');
      document.querySelectorAll('button[data-motion]').forEach(function (item) {
        item.classList.toggle('is-selected', item === button);
      });
    });
  });
}());
