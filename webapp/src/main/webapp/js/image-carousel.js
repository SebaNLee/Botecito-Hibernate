(function () {
  'use strict';

  function init() {
    document.querySelectorAll('[data-image-carousel]').forEach(function (root) {
      const main = root.querySelector('[data-carousel-main]');
      const thumbs = Array.prototype.slice.call(root.querySelectorAll('[data-carousel-thumb]'));
      const counter = root.querySelector('[data-carousel-index]');
      const prevBtn = root.querySelector('[data-carousel-prev]');
      const nextBtn = root.querySelector('[data-carousel-next]');
      if (!main || thumbs.length === 0) return;

      let active = 0;

      function setActive(idx) {
        if (idx < 0) idx = thumbs.length - 1;
        if (idx >= thumbs.length) idx = 0;
        active = idx;
        const url = thumbs[idx].getAttribute('data-carousel-url');
        if (url) main.setAttribute('src', url);
        thumbs.forEach(function (t, i) {
          if (i === idx) {
            t.classList.add('border-primary');
            t.classList.remove('border-transparent', 'hover:border-outline-variant');
          } else {
            t.classList.remove('border-primary');
            t.classList.add('border-transparent', 'hover:border-outline-variant');
          }
        });
        if (counter) counter.textContent = String(idx + 1);
      }

      thumbs.forEach(function (t, i) {
        t.addEventListener('click', function () { setActive(i); });
      });
      if (prevBtn) prevBtn.addEventListener('click', function () { setActive(active - 1); });
      if (nextBtn) nextBtn.addEventListener('click', function () { setActive(active + 1); });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
