(function () {
    var root = document.querySelector('[data-mylib-readbook]');
    if (!root) {
        return;
    }

    /* Tabs */
    var tabs = root.querySelectorAll('[data-mylib-tab]');
    var panels = root.querySelectorAll('[data-mylib-panel]');
    var initial = root.getAttribute('data-initial-tab') || 'reading';

    function activateTab(name) {
        tabs.forEach(function (tab) {
            var active = tab.getAttribute('data-mylib-tab') === name;
            tab.classList.toggle('is-active', active);
            tab.setAttribute('aria-selected', active ? 'true' : 'false');
            var badge = tab.querySelector('.mylib-readbook__tab-badge');
            if (badge) {
                badge.classList.toggle('is-active', active);
            }
        });
        panels.forEach(function (panel) {
            panel.classList.toggle('is-active', panel.getAttribute('data-mylib-panel') === name);
        });
        if (window.history && window.history.replaceState) {
            var url = new URL(window.location.href);
            if (name === 'reading') {
                url.searchParams.delete('tab');
            } else {
                url.searchParams.set('tab', name);
            }
            window.history.replaceState(null, '', url.pathname + url.search);
        }
    }

    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            activateTab(tab.getAttribute('data-mylib-tab'));
        });
    });

    var params = new URLSearchParams(window.location.search);
    var tabParam = params.get('tab');
    if (tabParam === 'soon' || tabParam === 'finished' || tabParam === 'reading') {
        initial = tabParam;
    }
    activateTab(initial);

    var dialogBackdrop = document.getElementById('mylib-dialog-backdrop');

    /* 완독 모달 */
    var dialog = document.getElementById('mylib-finish-modal');
    var backdrop = dialogBackdrop;
    var form = document.getElementById('mylib-finish-form');
    var bookIdInput = document.getElementById('mylib-finish-book-id');
    var ratingInput = document.getElementById('mylib-finish-rating');
    var endDatetimeInput = document.getElementById('mylib-finish-end-datetime');
    var endDateInput = document.getElementById('mylib-finish-end-date');
    var memoInput = document.getElementById('mylib-finish-memo');
    var submitBtn = document.getElementById('mylib-finish-submit');
    var returnTabInput = document.getElementById('mylib-finish-return-tab');
    var starsValue = document.getElementById('mylib-finish-stars-value');
    var coverImg = document.getElementById('mylib-finish-cover-img');
    var coverFallback = document.getElementById('mylib-finish-cover-fallback');
    var titleEl = document.getElementById('mylib-finish-book-title');
    var writerEl = document.getElementById('mylib-finish-book-writer');
    var genreEl = document.getElementById('mylib-finish-book-genre');
    var starButtons = dialog ? dialog.querySelectorAll('[data-finish-star]') : [];
    var modalTitleEl = document.getElementById('mylib-finish-modal-title');
    var supportsShowModal = dialog && typeof dialog.showModal === 'function';
    var lastFocused = null;
    var selectedRating = 0;

    function todayIsoDate() {
        var d = new Date();
        var m = String(d.getMonth() + 1).padStart(2, '0');
        var day = String(d.getDate()).padStart(2, '0');
        return d.getFullYear() + '-' + m + '-' + day;
    }

    function syncEndDatetime() {
        if (!endDateInput || !endDatetimeInput) {
            return;
        }
        var dateVal = endDateInput.value;
        endDatetimeInput.value = dateVal ? dateVal + 'T12:00:00' : '';
    }

    function setRating(value) {
        selectedRating = value;
        if (ratingInput) {
            ratingInput.value = String(value);
        }
        starButtons.forEach(function (btn) {
            var star = Number(btn.getAttribute('data-finish-star'));
            btn.classList.toggle('is-active', star <= value);
        });
        if (starsValue) {
            starsValue.textContent = value.toFixed(1) + ' / 5.0';
        }
        updateSubmitState();
    }

    function updateSubmitState() {
        if (!submitBtn) {
            return;
        }
        syncEndDatetime();
        var ok = selectedRating >= 1 && endDateInput && endDateInput.value;
        submitBtn.disabled = !ok;
    }

    function showBackdrop() {
        if (backdrop) {
            backdrop.hidden = false;
            backdrop.setAttribute('aria-hidden', 'false');
        }
    }

    function hideBackdrop() {
        if (backdrop) {
            backdrop.hidden = true;
            backdrop.setAttribute('aria-hidden', 'true');
        }
    }

    function closeFinishModal() {
        if (!dialog) {
            return;
        }
        if (supportsShowModal && dialog.open) {
            dialog.close();
        } else {
            dialog.removeAttribute('open');
            dialog.classList.remove('is-fallback-open');
        }
        hideBackdrop();
        document.body.style.overflow = '';
        if (lastFocused && typeof lastFocused.focus === 'function') {
            lastFocused.focus();
        }
    }

    function fillFinishModalBook(card) {
        var title = card.getAttribute('data-book-title') || '';
        var writer = card.getAttribute('data-book-writer') || '';
        var genre = card.getAttribute('data-book-genre') || '';
        var img = card.getAttribute('data-book-img') || '';

        if (titleEl) {
            titleEl.textContent = title;
        }
        if (writerEl) {
            writerEl.textContent = writer;
        }
        if (genreEl) {
            if (genre) {
                genreEl.textContent = genre;
                genreEl.hidden = false;
            } else {
                genreEl.hidden = true;
            }
        }
        if (coverImg && coverFallback) {
            if (img) {
                coverImg.src = img;
                coverImg.alt = title;
                coverImg.hidden = false;
                coverFallback.hidden = true;
            } else {
                coverImg.hidden = true;
                coverFallback.hidden = false;
            }
        }
    }

    function showFinishModal() {
        if (supportsShowModal) {
            dialog.showModal();
        } else {
            showBackdrop();
            dialog.setAttribute('open', '');
            dialog.classList.add('is-fallback-open');
            document.body.style.overflow = 'hidden';
        }
    }

    function openFinishModal(card, mode) {
        if (!dialog || !card) {
            return;
        }
        lastFocused = document.activeElement;

        var bookId = card.getAttribute('data-book-id') || '';
        var rating = Number(card.getAttribute('data-read-rating')) || 0;
        var isEdit = mode === 'edit';

        if (bookIdInput) {
            bookIdInput.value = bookId;
        }
        if (modalTitleEl) {
            modalTitleEl.textContent = isEdit ? '완독 기록 편집' : '완독 기록 이동';
        }
        if (returnTabInput) {
            returnTabInput.value = isEdit ? 'finished' : 'reading';
        }
        fillFinishModalBook(card);

        if (isEdit) {
            var end = card.getAttribute('data-read-end') || '';
            var memoEl = card.querySelector('.mylib-card__memo-src');
            var memo = memoEl ? memoEl.textContent : '';
            if (endDateInput) {
                endDateInput.value = end || todayIsoDate();
            }
            if (memoInput) {
                memoInput.value = memo;
            }
            setRating(rating >= 1 && rating <= 5 ? Math.round(rating) : 0);
        } else {
            if (memoInput) {
                memoInput.value = '';
            }
            if (endDateInput) {
                endDateInput.value = todayIsoDate();
            }
            setRating(rating >= 1 && rating <= 5 ? Math.round(rating) : 0);
        }

        showFinishModal();

        var firstStar = dialog.querySelector('[data-finish-star="1"]');
        if (firstStar && typeof firstStar.focus === 'function') {
            firstStar.focus();
        }
    }

    root.querySelectorAll('[data-mylib-open-finish]').forEach(function (btn) {
        btn.addEventListener('click', function (event) {
            event.preventDefault();
            event.stopPropagation();
            var card = btn.closest('.mylib-card--reading');
            openFinishModal(card, 'create');
        });
    });

    root.querySelectorAll('[data-mylib-open-finish-edit]').forEach(function (btn) {
        btn.addEventListener('click', function (event) {
            event.preventDefault();
            event.stopPropagation();
            var card = btn.closest('.mylib-card--finished');
            openFinishModal(card, 'edit');
        });
    });

    if (dialog) {
        dialog.querySelectorAll('[data-mylib-finish-close]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                closeFinishModal();
            });
        });

        dialog.addEventListener('cancel', function (event) {
            event.preventDefault();
            closeFinishModal();
        });

        dialog.addEventListener('close', function () {
            hideBackdrop();
            document.body.style.overflow = '';
        });

        dialog.addEventListener('click', function (event) {
            if (event.target === dialog) {
                closeFinishModal();
            }
        });
    }

    starButtons.forEach(function (btn) {
        btn.addEventListener('click', function () {
            setRating(Number(btn.getAttribute('data-finish-star')));
        });
    });

    if (endDateInput) {
        endDateInput.addEventListener('change', updateSubmitState);
        endDateInput.addEventListener('input', updateSubmitState);
    }

    if (form) {
        form.addEventListener('submit', function () {
            syncEndDatetime();
        });
    }

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }
        var readingDlg = document.getElementById('mylib-reading-modal');
        if (readingDlg && (readingDlg.open || readingDlg.classList.contains('is-fallback-open'))) {
            closeReadingModal();
            return;
        }
        if (dialog && (dialog.open || dialog.classList.contains('is-fallback-open'))) {
            closeFinishModal();
            return;
        }
    });

    /* 읽을 책 → 읽는 중 모달 */
    var readingDialog = document.getElementById('mylib-reading-modal');
    var readingForm = document.getElementById('mylib-reading-form');
    var readingDesc = document.getElementById('mylib-reading-modal-desc');
    var readingCoverImg = document.getElementById('mylib-reading-cover-img');
    var readingCoverFallback = document.getElementById('mylib-reading-cover-fallback');
    var readingTitleEl = document.getElementById('mylib-reading-book-title');
    var readingWriterEl = document.getElementById('mylib-reading-book-writer');
    var readingGenreEl = document.getElementById('mylib-reading-book-genre');
    var readingSupportsModal = readingDialog && typeof readingDialog.showModal === 'function';
    var readingLastFocused = null;

    function closeReadingModal() {
        if (!readingDialog) {
            return;
        }
        if (readingSupportsModal && readingDialog.open) {
            readingDialog.close();
        } else {
            readingDialog.removeAttribute('open');
            readingDialog.classList.remove('is-fallback-open');
        }
        if (backdrop) {
            backdrop.hidden = true;
            backdrop.setAttribute('aria-hidden', 'true');
        }
        document.body.style.overflow = '';
        if (readingLastFocused && typeof readingLastFocused.focus === 'function') {
            readingLastFocused.focus();
        }
    }

    function openReadingModal(card) {
        if (!readingDialog || !card || !readingForm) {
            return;
        }
        readingLastFocused = document.activeElement;

        var bookId = card.getAttribute('data-book-id') || '';
        var title = card.getAttribute('data-book-title') || '';
        var writer = card.getAttribute('data-book-writer') || '';
        var genre = card.getAttribute('data-book-genre') || '';
        var img = card.getAttribute('data-book-img') || '';
        var state = Number(card.getAttribute('data-reading-state')) || 1;

        if (readingForm.action.indexOf('/0/') !== -1) {
            readingForm.action = readingForm.action.replace('/0/', '/' + bookId + '/');
        } else {
            readingForm.action = readingForm.action.replace(/\/\d+\//, '/' + bookId + '/');
        }

        if (readingTitleEl) {
            readingTitleEl.textContent = title;
        }
        if (readingWriterEl) {
            readingWriterEl.textContent = writer;
        }
        if (readingGenreEl) {
            if (genre) {
                readingGenreEl.textContent = genre;
                readingGenreEl.hidden = false;
            } else {
                readingGenreEl.hidden = true;
            }
        }
        if (readingCoverImg && readingCoverFallback) {
            if (img) {
                readingCoverImg.src = img;
                readingCoverImg.alt = title;
                readingCoverImg.hidden = false;
                readingCoverFallback.hidden = true;
            } else {
                readingCoverImg.hidden = true;
                readingCoverFallback.hidden = false;
            }
        }
        if (readingDesc) {
            if (state === 2) {
                readingDesc.innerHTML =
                    '이미 <strong>읽는 중</strong>에 있습니다. <strong>읽을 책</strong>(즐겨찾기) 목록은 그대로 유지됩니다.';
            } else {
                readingDesc.innerHTML =
                    '이 책을 <strong>읽을 책</strong>에 두고 <strong>읽는 중</strong>에 추가합니다.';
            }
        }

        if (readingSupportsModal) {
            readingDialog.showModal();
        } else {
            if (backdrop) {
                backdrop.hidden = false;
                backdrop.setAttribute('aria-hidden', 'false');
            }
            readingDialog.setAttribute('open', '');
            readingDialog.classList.add('is-fallback-open');
            document.body.style.overflow = 'hidden';
        }

        var confirmBtn = readingDialog.querySelector('.mylib-reading-modal__confirm');
        if (confirmBtn && typeof confirmBtn.focus === 'function') {
            confirmBtn.focus();
        }
    }

    root.querySelectorAll('[data-mylib-open-reading]').forEach(function (btn) {
        btn.addEventListener('click', function (event) {
            event.preventDefault();
            event.stopPropagation();
            var card = btn.closest('.mylib-card--soon');
            openReadingModal(card);
        });
    });

    if (readingDialog) {
        readingDialog.querySelectorAll('[data-mylib-reading-close]').forEach(function (btn) {
            btn.addEventListener('click', closeReadingModal);
        });

        readingDialog.addEventListener('cancel', function (event) {
            event.preventDefault();
            closeReadingModal();
        });

        readingDialog.addEventListener('close', function () {
            if (backdrop) {
                backdrop.hidden = true;
                backdrop.setAttribute('aria-hidden', 'true');
            }
            document.body.style.overflow = '';
        });

        readingDialog.addEventListener('click', function (event) {
            if (event.target === readingDialog) {
                closeReadingModal();
            }
        });
    }

    if (backdrop) {
        backdrop.addEventListener('click', function () {
            if (readingDialog && (readingDialog.open || readingDialog.classList.contains('is-fallback-open'))) {
                closeReadingModal();
            } else if (dialog && (dialog.open || dialog.classList.contains('is-fallback-open'))) {
                closeFinishModal();
            }
        });
    }

    /* URL ?bookId= — 완독 탭에서 편집 모달 자동 오픈 */
    var autoBookId = params.get('bookId');
    if (autoBookId && tabParam === 'finished') {
        var autoCard = root.querySelector('.mylib-card--finished[data-book-id="' + autoBookId + '"]');
        if (autoCard) {
            openFinishModal(autoCard, 'edit');
        }
    }
})();
