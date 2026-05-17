(function () {
    'use strict';

    var root = document.querySelector('[data-admin]');
    if (!root) {
        return;
    }

    function clearDeleteOpenState() {
        root.querySelectorAll('.admin-table__row.is-delete-open').forEach(function (row) {
            row.classList.remove('is-delete-open');
        });
        root.querySelectorAll('.admin-delete-row.is-open').forEach(function (row) {
            row.classList.remove('is-open');
            row.setAttribute('aria-hidden', 'true');
        });
        root.querySelectorAll('[data-admin-delete-toggle]').forEach(function (toggle) {
            toggle.setAttribute('aria-expanded', 'false');
        });
    }

    function openDeletePanel(itemRow, deleteRow, toggleBtn) {
        clearDeleteOpenState();
        itemRow.classList.add('is-delete-open');
        deleteRow.classList.add('is-open');
        deleteRow.removeAttribute('aria-hidden');
        if (toggleBtn) {
            toggleBtn.setAttribute('aria-expanded', 'true');
        }
    }

    root.querySelectorAll('[data-admin-delete-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var itemRow = btn.closest('[data-admin-item-row]');
            if (!itemRow) {
                return;
            }
            var deleteRow = itemRow.nextElementSibling;
            if (!deleteRow || !deleteRow.classList.contains('admin-delete-row')) {
                return;
            }
            if (deleteRow.classList.contains('is-open')) {
                clearDeleteOpenState();
                return;
            }
            openDeletePanel(itemRow, deleteRow, btn);
        });
    });

    root.querySelectorAll('[data-admin-delete-cancel]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            clearDeleteOpenState();
        });
    });

    var dialog = document.getElementById('admin-member-dialog');
    var backdrop = document.getElementById('admin-member-dialog-backdrop');
    var form = document.getElementById('admin-member-form');
    var stateInput = document.getElementById('admin-member-state');
    var stateButtons = dialog
        ? dialog.querySelectorAll('[data-admin-member-state]')
        : [];
    var supportsShowModal = dialog && typeof dialog.showModal === 'function';

    function setSelectedState(value) {
        if (!stateInput) {
            return;
        }
        stateInput.value = value || '';
        stateButtons.forEach(function (btn) {
            var selected = btn.getAttribute('data-state-value') === value;
            btn.classList.toggle('is-selected', selected);
            btn.setAttribute('aria-pressed', selected ? 'true' : 'false');
        });
    }

    function closeMemberDialog() {
        if (!dialog) {
            return;
        }
        if (supportsShowModal && dialog.open) {
            dialog.close();
        } else {
            dialog.removeAttribute('open');
            dialog.classList.remove('is-fallback-open');
        }
        if (backdrop) {
            backdrop.hidden = true;
        }
    }

    function openMemberDialog(dataset) {
        if (!dialog || !form) {
            return;
        }
        var memberId = dataset.memberId;
        if (!memberId) {
            return;
        }
        form.setAttribute('action', '/admin/members/' + memberId + '/update-status');

        var codeEl = document.getElementById('admin-member-code');
        var nameEl = document.getElementById('admin-member-name');
        var nickEl = document.getElementById('admin-member-nickname');
        var emailEl = document.getElementById('admin-member-email');
        var joinedEl = document.getElementById('admin-member-joined');
        var keywordInput = document.getElementById('admin-member-keyword');

        if (codeEl) {
            codeEl.textContent = dataset.memberCode || '';
        }
        if (nameEl) {
            nameEl.textContent = dataset.memberName || '';
        }
        if (nickEl) {
            nickEl.textContent = dataset.memberNickname || '';
        }
        if (emailEl) {
            emailEl.textContent = dataset.memberEmail || '';
        }
        if (joinedEl) {
            joinedEl.textContent = dataset.memberJoined || '—';
        }
        if (keywordInput) {
            keywordInput.value = dataset.keyword || '';
        }

        setSelectedState(dataset.memberState || '활동');

        if (supportsShowModal) {
            if (!dialog.open) {
                dialog.showModal();
            }
        } else {
            dialog.setAttribute('open', '');
            dialog.classList.add('is-fallback-open');
        }
        if (backdrop) {
            backdrop.hidden = false;
        }
    }

    root.querySelectorAll('[data-admin-member-edit]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            openMemberDialog(btn.dataset);
        });
    });

    stateButtons.forEach(function (btn) {
        btn.addEventListener('click', function () {
            setSelectedState(btn.getAttribute('data-state-value'));
        });
    });

    if (dialog) {
        dialog.querySelectorAll('[data-admin-member-close]').forEach(function (btn) {
            btn.addEventListener('click', function (event) {
                event.preventDefault();
                closeMemberDialog();
            });
        });

        dialog.addEventListener('cancel', function (event) {
            event.preventDefault();
            closeMemberDialog();
        });

        dialog.addEventListener('close', function () {
            if (backdrop) {
                backdrop.hidden = true;
            }
        });

        dialog.addEventListener('click', function (event) {
            if (event.target === dialog) {
                closeMemberDialog();
            }
        });
    }

    if (backdrop) {
        backdrop.addEventListener('click', closeMemberDialog);
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            clearDeleteOpenState();
            if (dialog && (dialog.open || dialog.classList.contains('is-fallback-open'))) {
                closeMemberDialog();
            }
        }
    });
})();
