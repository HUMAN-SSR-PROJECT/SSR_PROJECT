(function () {
    var root = document.querySelector('[data-book-detail]');
    if (!root) {
        return;
    }

    var libraries = [];
    var librariesEl = document.getElementById('book-detail-libraries-data');
    if (librariesEl && librariesEl.textContent) {
        try {
            libraries = JSON.parse(librariesEl.textContent);
        } catch (e) {
            libraries = [];
        }
    }

    var reviewsEditMap = {};
    var reviewsEditEl = document.getElementById('book-review-edit-data');
    if (reviewsEditEl && reviewsEditEl.textContent) {
        try {
            var reviewItems = JSON.parse(reviewsEditEl.textContent);
            reviewItems.forEach(function (item) {
                if (item && item.reviewId != null) {
                    reviewsEditMap[String(item.reviewId)] = item;
                }
            });
        } catch (e) {
            reviewsEditMap = {};
        }
    }

    var kakaoKey = root.getAttribute('data-kakao-key') || '';
    var kakaoReady = false;
    var kakaoLoading = false;
    var mapInstances = {};

    /* Tabs */
    var tabs = root.querySelectorAll('[data-book-tab]');
    var panels = root.querySelectorAll('[data-book-panel]');

    function activateTab(name) {
        tabs.forEach(function (tab) {
            var active = tab.getAttribute('data-book-tab') === name;
            tab.classList.toggle('is-active', active);
            tab.setAttribute('aria-selected', active ? 'true' : 'false');
        });
        panels.forEach(function (panel) {
            panel.classList.toggle('is-active', panel.getAttribute('data-book-panel') === name);
        });
    }

    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            activateTab(tab.getAttribute('data-book-tab'));
        });
    });

    var focusTab = root.getAttribute('data-focus-tab');
    if (focusTab === 'libraries' || focusTab === 'reviews') {
        activateTab(focusTab);
    } else {
        activateTab('info');
    }

    /* Review modal */
    var dialog = document.getElementById('book-review-modal');
    var backdrop = document.getElementById('book-review-backdrop');
    var reviewForm = document.getElementById('book-review-form');
    var openBtn = root.querySelector('[data-review-open]');
    var ratingInput = document.getElementById('review-rating');
    var commentInput = document.getElementById('review-comment');
    var reviewIdInput = document.getElementById('review-id');
    var submitBtn = document.getElementById('review-submit');
    var modalTitle = document.getElementById('book-review-modal-title');
    var modalEyebrow = document.getElementById('book-review-modal-eyebrow');
    var counterEl = dialog ? dialog.querySelector('[data-review-counter]') : null;
    var starButtons = dialog ? dialog.querySelectorAll('[data-review-star]') : [];
    var insertAction = reviewForm ? reviewForm.getAttribute('data-action-insert') : '';
    var updateAction = reviewForm ? reviewForm.getAttribute('data-action-update') : '';
    var supportsShowModal = dialog && typeof dialog.showModal === 'function';
    var lastFocusedElement = null;

    function updateReviewForm() {
        if (!submitBtn || !ratingInput || !commentInput) {
            return;
        }
        var rating = Number(ratingInput.value);
        var len = commentInput.value.trim().length;
        if (counterEl) {
            counterEl.textContent = len + '자 / 최소 10자';
        }
        submitBtn.disabled = !(rating >= 1 && rating <= 5 && len >= 10);
    }

    function setRating(value) {
        if (!ratingInput) {
            return;
        }
        ratingInput.value = String(value);
        starButtons.forEach(function (btn) {
            var star = Number(btn.getAttribute('data-review-star'));
            btn.classList.toggle('is-active', star <= value);
        });
        updateReviewForm();
    }

    function showBackdrop() {
        if (!backdrop) {
            return;
        }
        backdrop.removeAttribute('hidden');
        backdrop.setAttribute('aria-hidden', 'false');
    }

    function hideBackdrop() {
        if (!backdrop) {
            return;
        }
        backdrop.setAttribute('hidden', '');
        backdrop.setAttribute('aria-hidden', 'true');
    }

    function closeReviewDialog() {
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
        if (lastFocusedElement && typeof lastFocusedElement.focus === 'function') {
            lastFocusedElement.focus();
        }
    }

    function openReviewDialog(mode, payload) {
        if (!dialog) {
            return;
        }
        lastFocusedElement = document.activeElement;

        var isEdit = mode === 'edit';
        if (reviewForm) {
            reviewForm.action = isEdit ? updateAction : insertAction;
        }
        if (reviewIdInput) {
            reviewIdInput.value = isEdit && payload ? String(payload.reviewId || '') : '';
        }
        if (modalEyebrow) {
            modalEyebrow.textContent = isEdit ? 'EDIT REVIEW' : 'WRITE A REVIEW';
        }
        if (modalTitle) {
            modalTitle.textContent = isEdit ? '리뷰 수정' : '리뷰 작성';
        }
        if (submitBtn) {
            submitBtn.textContent = isEdit ? '수정 완료' : '리뷰 등록';
        }

        if (isEdit && payload) {
            var editData = reviewsEditMap[String(payload.reviewId)] || {};
            setRating(Number(editData.rating || payload.rating) || 0);
            if (commentInput) {
                commentInput.value = editData.comment || '';
            }
        } else {
            setRating(0);
            if (commentInput) {
                commentInput.value = '';
            }
        }
        updateReviewForm();

        if (supportsShowModal) {
            dialog.showModal();
        } else {
            showBackdrop();
            dialog.setAttribute('open', '');
            dialog.classList.add('is-fallback-open');
            document.body.style.overflow = 'hidden';
        }

        var firstStar = dialog.querySelector('[data-review-star="1"]');
        if (firstStar && typeof firstStar.focus === 'function') {
            firstStar.focus();
        }
    }

    if (openBtn && dialog) {
        openBtn.addEventListener('click', function () {
            openReviewDialog('create');
        });
    }

    root.querySelectorAll('[data-review-edit]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            openReviewDialog('edit', {
                reviewId: btn.getAttribute('data-review-id')
            });
        });
    });

    if (dialog) {
        dialog.querySelectorAll('[data-review-close]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                closeReviewDialog();
            });
        });

        dialog.addEventListener('cancel', function (event) {
            event.preventDefault();
            closeReviewDialog();
        });

        dialog.addEventListener('close', function () {
            hideBackdrop();
            document.body.style.overflow = '';
        });

        dialog.addEventListener('click', function (event) {
            if (event.target === dialog) {
                closeReviewDialog();
            }
        });

        starButtons.forEach(function (btn) {
            btn.addEventListener('click', function () {
                setRating(Number(btn.getAttribute('data-review-star')));
            });
        });

        if (commentInput) {
            commentInput.addEventListener('input', updateReviewForm);
        }

        updateReviewForm();
    }

    if (backdrop) {
        backdrop.addEventListener('click', function () {
            closeReviewDialog();
        });
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && dialog && (dialog.open || dialog.classList.contains('is-fallback-open'))) {
            closeReviewDialog();
        }
    });

    /* Kakao map */
    function loadKakaoSdk(callback) {
        if (kakaoReady && window.kakao && window.kakao.maps) {
            callback();
            return;
        }
        if (kakaoLoading) {
            var wait = setInterval(function () {
                if (kakaoReady) {
                    clearInterval(wait);
                    callback();
                }
            }, 100);
            return;
        }
        if (!kakaoKey) {
            return;
        }
        kakaoLoading = true;
        var script = document.createElement('script');
        script.src = '//dapi.kakao.com/v2/maps/sdk.js?appkey=' + encodeURIComponent(kakaoKey) + '&autoload=false';
        script.onload = function () {
            window.kakao.maps.load(function () {
                kakaoReady = true;
                kakaoLoading = false;
                callback();
            });
        };
        script.onerror = function () {
            kakaoLoading = false;
        };
        document.head.appendChild(script);
    }

    function renderMap(index, panel) {
        var canvas = panel.querySelector('[data-map-canvas]');
        if (!canvas) {
            return;
        }
        var lib = libraries[index];
        if (!lib || lib.lat == null || lib.lon == null) {
            canvas.innerHTML = '<p style="padding:24px;text-align:center;color:#888880;font-size:13px;">지도 좌표 정보가 없습니다.</p>';
            return;
        }
        var center = new window.kakao.maps.LatLng(lib.lat, lib.lon);
        if (mapInstances[index]) {
            setTimeout(function () {
                mapInstances[index].relayout();
                mapInstances[index].setCenter(center);
            }, 200);
            return;
        }
        var map = new window.kakao.maps.Map(canvas, {
            center: center,
            level: 4
        });
        new window.kakao.maps.Marker({
            map: map,
            position: center
        });
        mapInstances[index] = map;
        setTimeout(function () {
            map.relayout();
            map.setCenter(center);
        }, 200);
    }

    function closeAllMapPanels() {
        root.querySelectorAll('[data-map-panel]').forEach(function (panel) {
            panel.classList.remove('is-open');
        });
        root.querySelectorAll('[data-map-toggle]').forEach(function (btn) {
            btn.setAttribute('aria-expanded', 'false');
        });
    }

    root.querySelectorAll('[data-map-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var indexAttr = btn.getAttribute('data-map-index');
            if (indexAttr === null || indexAttr === '') {
                return;
            }
            var index = Number(indexAttr);
            var panel = root.querySelector('[data-map-panel="' + indexAttr + '"]');
            if (!panel) {
                return;
            }
            var isOpen = panel.classList.contains('is-open');
            closeAllMapPanels();
            if (isOpen) {
                return;
            }

            var librariesPanel = root.querySelector('[data-book-panel="libraries"]');
            if (librariesPanel && !librariesPanel.classList.contains('is-active')) {
                activateTab('libraries');
            }

            panel.classList.add('is-open');
            btn.setAttribute('aria-expanded', 'true');

            var sideName = panel.querySelector('[data-map-name]');
            var sideAddr = panel.querySelector('[data-map-addr]');
            var kakaoLink = panel.querySelector('[data-map-kakao-link]');
            var lib = libraries[index];
            if (lib) {
                if (sideName) {
                    sideName.textContent = lib.name || '';
                }
                if (sideAddr) {
                    sideAddr.textContent = lib.addr || '';
                }
                if (kakaoLink && lib.lat != null && lib.lon != null) {
                    kakaoLink.href = 'https://map.kakao.com/link/map/'
                        + encodeURIComponent(lib.name || '도서관')
                        + ',' + lib.lat + ',' + lib.lon;
                }
            }

            loadKakaoSdk(function () {
                renderMap(index, panel);
            });
        });
    });
})();
