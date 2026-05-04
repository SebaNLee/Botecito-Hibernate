(function () {
  'use strict';

  const REOPEN_FLAG_KEY = 'paw.imageGallery.reopen';

  function isInsideModal(el) {
    return !!el.closest('[data-image-gallery-modal]');
  }

  function markReopen() {
    try {
      sessionStorage.setItem(REOPEN_FLAG_KEY, '1');
    } catch (e) {}
  }

  function shouldReopen() {
    try {
      if (sessionStorage.getItem(REOPEN_FLAG_KEY) === '1') {
        sessionStorage.removeItem(REOPEN_FLAG_KEY);
        return true;
      }
    } catch (e) {}
    return false;
  }

  function patchFormForModalReturn(form) {
    if (!form || !isInsideModal(form)) return;
    form.addEventListener('submit', markReopen);
  }

  function autoSubmitFileInput(input) {
    input.addEventListener('change', function () {
      if (input.files && input.files.length > 0) {
        const form = input.closest('form');
        if (form) {
          if (isInsideModal(form)) markReopen();
          form.submit();
        }
      }
    });
  }

  function bindModalOpeners() {
    document.querySelectorAll('[data-gallery-modal-open]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        const id = btn.getAttribute('data-gallery-modal-open');
        const dialog = document.getElementById(id);
        if (dialog && typeof dialog.showModal === 'function') {
          dialog.showModal();
        } else if (dialog) {
          dialog.setAttribute('open', '');
        }
      });
    });
  }

  function patchModalForms() {
    document.querySelectorAll('[data-image-gallery-modal] form').forEach(patchFormForModalReturn);
  }

  function reopenModalIfRequested() {
    if (!shouldReopen()) return;
    const dialog = document.querySelector('[data-image-gallery-modal]');
    if (dialog && typeof dialog.showModal === 'function') {
      dialog.showModal();
    }
  }

  function currentOrder(list) {
    const items = list.querySelectorAll('[data-gallery-item]');
    const keys = [];
    items.forEach(function (li) {
      keys.push(li.getAttribute('data-gallery-key'));
    });
    return keys;
  }

  function submitReorder(list, keys) {
    const reorderForm = list.parentElement.querySelector('[data-gallery-reorder-form]');
    if (!reorderForm) {
      return;
    }
    const orderInput = reorderForm.querySelector('[data-gallery-order-input]');
    if (!orderInput) {
      return;
    }
    orderInput.value = keys.join(',');
    if (isInsideModal(reorderForm)) markReopen();
    reorderForm.submit();
  }

  function bindMoveButtons(list) {
    list.querySelectorAll('[data-gallery-move]').forEach(function (button) {
      button.addEventListener('click', function (event) {
        event.preventDefault();
        const li = button.closest('[data-gallery-item]');
        if (!li) return;
        const direction = button.getAttribute('data-gallery-move');
        const sibling = direction === 'left' ? li.previousElementSibling : li.nextElementSibling;
        if (!sibling) return;
        if (direction === 'left') {
          list.insertBefore(li, sibling);
        } else {
          list.insertBefore(sibling, li);
        }
        submitReorder(list, currentOrder(list));
      });
    });
  }

  function bindDragAndDrop(list) {
    let draggedItem = null;
    list.querySelectorAll('[data-gallery-item]').forEach(function (li) {
      li.setAttribute('draggable', 'true');
      li.addEventListener('dragstart', function (event) {
        draggedItem = li;
        if (event.dataTransfer) {
          event.dataTransfer.effectAllowed = 'move';
          event.dataTransfer.setData('text/plain', li.getAttribute('data-gallery-key') || '');
        }
        li.classList.add('opacity-50');
      });
      li.addEventListener('dragend', function () {
        li.classList.remove('opacity-50');
        draggedItem = null;
      });
      li.addEventListener('dragover', function (event) {
        event.preventDefault();
        if (event.dataTransfer) {
          event.dataTransfer.dropEffect = 'move';
        }
      });
      li.addEventListener('drop', function (event) {
        event.preventDefault();
        if (!draggedItem || draggedItem === li) return;
        const items = Array.prototype.slice.call(list.querySelectorAll('[data-gallery-item]'));
        const draggedIndex = items.indexOf(draggedItem);
        const targetIndex = items.indexOf(li);
        if (draggedIndex < targetIndex) {
          list.insertBefore(draggedItem, li.nextElementSibling);
        } else {
          list.insertBefore(draggedItem, li);
        }
        submitReorder(list, currentOrder(list));
      });
    });
  }

  function init() {
    document.querySelectorAll('[data-image-gallery]').forEach(function (root) {
      root.querySelectorAll('[data-gallery-file-input]').forEach(autoSubmitFileInput);
      const list = root.querySelector('[data-gallery-sortable]');
      if (list) {
        bindMoveButtons(list);
        bindDragAndDrop(list);
      }
    });
    bindModalOpeners();
    patchModalForms();
    reopenModalIfRequested();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
