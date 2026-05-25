(function () {
  'use strict';

  function updateGalleryCount(root, count) {
    const countEl = root.querySelector('[data-gallery-count]');
    const max = parseInt(root.dataset.maxImages || '0', 10);
    const template = root.dataset.galleryCountTemplate || '';
    if (!countEl || !max) {
      return;
    }
    countEl.textContent = template
      .replace('{0}', String(count))
      .replace('{1}', String(max));
  }

  function bindInlineDraftGallery(root) {
    const input = root.querySelector('[data-gallery-file-input]');
    const previewList = root.querySelector('[data-gallery-local-preview]');
    const emptyState = root.querySelector('[data-gallery-empty]');
    const uploadLabel = input ? input.closest('label') : null;
    if (!input || !previewList) {
      return;
    }

    const maxImages = parseInt(root.dataset.maxImages || '3', 10);
    const coverBadge = root.dataset.galleryCoverBadge || '';
    const badgeClass = root.dataset.galleryBadgeClass || 'badge badge-primary badge-sm font-bold';
    const removeLabel = root.dataset.galleryRemoveLabel || 'Remove image';
    const moveLeftLabel = root.dataset.galleryMoveLeftLabel || 'Move left';
    const moveRightLabel = root.dataset.galleryMoveRightLabel || 'Move right';

    const entries = [];
    const objectUrls = new Map();
    let nextId = 0;

    function revokeEntryUrl(entryId) {
      const url = objectUrls.get(entryId);
      if (url) {
        URL.revokeObjectURL(url);
        objectUrls.delete(entryId);
      }
    }

    function revokeAllUrls() {
      objectUrls.forEach(function (url) {
        URL.revokeObjectURL(url);
      });
      objectUrls.clear();
    }

    function syncInputFromEntries() {
      if (typeof DataTransfer === 'undefined') {
        return;
      }
      const transfer = new DataTransfer();
      entries.forEach(function (entry) {
        transfer.items.add(entry.file);
      });
      input.files = transfer.files;
    }

    function syncEntriesFromDom() {
      const ordered = [];
      previewList.querySelectorAll('[data-gallery-item]').forEach(function (item) {
        const id = parseInt(item.getAttribute('data-gallery-key'), 10);
        const entry = entries.find(function (candidate) {
          return candidate.id === id;
        });
        if (entry) {
          ordered.push(entry);
        }
      });
      entries.length = 0;
      entries.push.apply(entries, ordered);
    }

    function createIconButton(className, icon, label, extraAttrs) {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = className;
      button.setAttribute('aria-label', label);
      if (extraAttrs) {
        Object.keys(extraAttrs).forEach(function (key) {
          button.setAttribute(key, extraAttrs[key]);
        });
      }
      const iconEl = document.createElement('span');
      iconEl.className = 'material-symbols-outlined text-base';
      iconEl.textContent = icon;
      button.appendChild(iconEl);
      return button;
    }

    function render() {
      revokeAllUrls();
      previewList.innerHTML = '';

      entries.forEach(function (entry, index) {
        if (typeof URL === 'undefined' || typeof URL.createObjectURL !== 'function') {
          return;
        }
        const objectUrl = URL.createObjectURL(entry.file);
        objectUrls.set(entry.id, objectUrl);

        const item = document.createElement('li');
        item.className =
          'relative rounded-xl overflow-hidden border border-outline-variant/30 bg-base-200 group';
        item.setAttribute('data-gallery-item', '');
        item.setAttribute('data-gallery-key', String(entry.id));

        const img = document.createElement('img');
        img.src = objectUrl;
        img.alt = '';
        img.className = 'w-full aspect-square object-cover block';
        item.appendChild(img);

        if (index === 0 && coverBadge) {
          const badge = document.createElement('span');
          badge.className = 'absolute top-2 left-2 ' + badgeClass;
          badge.textContent = coverBadge;
          item.appendChild(badge);
        }

        const controls = document.createElement('div');
        controls.className =
          'absolute inset-x-0 bottom-0 p-2 flex items-center justify-between gap-2 bg-gradient-to-t from-black/60 to-transparent opacity-100 pointer-events-auto transition-opacity duration-150 [@media(hover:hover)]:opacity-0 [@media(hover:hover)]:pointer-events-none [@media(hover:hover)]:group-hover:opacity-100 [@media(hover:hover)]:group-hover:pointer-events-auto [@media(hover:hover)]:group-focus-within:opacity-100 [@media(hover:hover)]:group-focus-within:pointer-events-auto';

        const moveGroup = document.createElement('div');
        moveGroup.className = 'flex gap-1';

        const moveLeft = createIconButton(
          'btn btn-xs btn-circle btn-ghost text-white',
          'chevron_left',
          moveLeftLabel,
          { 'data-gallery-move': 'left' },
        );
        if (index === 0) {
          moveLeft.disabled = true;
        }

        const moveRight = createIconButton(
          'btn btn-xs btn-circle btn-ghost text-white',
          'chevron_right',
          moveRightLabel,
          { 'data-gallery-move': 'right' },
        );
        if (index === entries.length - 1) {
          moveRight.disabled = true;
        }

        moveGroup.appendChild(moveLeft);
        moveGroup.appendChild(moveRight);

        const removeButton = createIconButton(
          'btn btn-xs btn-circle btn-error',
          'close',
          removeLabel,
          { 'data-gallery-local-remove': String(entry.id) },
        );

        controls.appendChild(moveGroup);
        controls.appendChild(removeButton);
        item.appendChild(controls);
        previewList.appendChild(item);
      });

      const hasFiles = entries.length > 0;
      previewList.classList.toggle('hidden', !hasFiles);
      if (emptyState) {
        emptyState.classList.toggle('hidden', hasFiles);
      }
      if (uploadLabel) {
        uploadLabel.classList.toggle('hidden', entries.length >= maxImages);
      }
      updateGalleryCount(root, entries.length);
      bindInlineDraftInteractions();
    }

    function removeEntry(entryId) {
      const index = entries.findIndex(function (entry) {
        return entry.id === entryId;
      });
      if (index === -1) {
        return;
      }
      revokeEntryUrl(entryId);
      entries.splice(index, 1);
      render();
    }

    function bindInlineDraftInteractions() {
      previewList.querySelectorAll('[data-gallery-move]').forEach(function (button) {
        button.addEventListener('click', function (event) {
          event.preventDefault();
          const item = button.closest('[data-gallery-item]');
          if (!item) {
            return;
          }
          const direction = button.getAttribute('data-gallery-move');
          const sibling = direction === 'left' ? item.previousElementSibling : item.nextElementSibling;
          if (!sibling) {
            return;
          }
          if (direction === 'left') {
            previewList.insertBefore(item, sibling);
          } else {
            previewList.insertBefore(sibling, item);
          }
          syncEntriesFromDom();
          render();
        });
      });

      previewList.querySelectorAll('[data-gallery-local-remove]').forEach(function (button) {
        button.addEventListener('click', function (event) {
          event.preventDefault();
          const entryId = parseInt(button.getAttribute('data-gallery-local-remove'), 10);
          removeEntry(entryId);
        });
      });

      let draggedItem = null;
      previewList.querySelectorAll('[data-gallery-item]').forEach(function (item) {
        item.setAttribute('draggable', 'true');
        item.addEventListener('dragstart', function (event) {
          draggedItem = item;
          if (event.dataTransfer) {
            event.dataTransfer.effectAllowed = 'move';
            event.dataTransfer.setData('text/plain', item.getAttribute('data-gallery-key') || '');
          }
          item.classList.add('opacity-50');
        });
        item.addEventListener('dragend', function () {
          item.classList.remove('opacity-50');
          draggedItem = null;
        });
        item.addEventListener('dragover', function (event) {
          event.preventDefault();
          if (event.dataTransfer) {
            event.dataTransfer.dropEffect = 'move';
          }
        });
        item.addEventListener('drop', function (event) {
          event.preventDefault();
          if (!draggedItem || draggedItem === item) {
            return;
          }
          const items = Array.prototype.slice.call(
            previewList.querySelectorAll('[data-gallery-item]'),
          );
          const draggedIndex = items.indexOf(draggedItem);
          const targetIndex = items.indexOf(item);
          if (draggedIndex < targetIndex) {
            previewList.insertBefore(draggedItem, item.nextElementSibling);
          } else {
            previewList.insertBefore(draggedItem, item);
          }
          syncEntriesFromDom();
          render();
        });
      });
    }

    input.addEventListener('change', function () {
      const picked = Array.prototype.slice.call(input.files || []);
      input.value = '';
      let remaining = maxImages - entries.length;
      picked.forEach(function (file) {
        if (remaining <= 0) {
          return;
        }
        if (!file || !file.type || file.type.indexOf('image/') !== 0) {
          return;
        }
        entries.push({ id: nextId++, file: file });
        remaining -= 1;
      });
      render();
    });

    const form = root.closest('form');
    if (form) {
      form.addEventListener(
        'submit',
        function () {
          syncInputFromEntries();
        },
        true,
      );
    }

    window.addEventListener('beforeunload', revokeAllUrls);
  }

  function init() {
    document.querySelectorAll('[data-image-gallery]').forEach(bindInlineDraftGallery);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
