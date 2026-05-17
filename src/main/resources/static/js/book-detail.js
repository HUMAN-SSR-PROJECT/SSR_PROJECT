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

    var kakaoKey = (root.getAttribute('data-kakao-key') || '').trim();
    var kakaoReady = false;
    var kakaoLoading = false;
    var kakaoLoadFailed = false;
    var mapInstances = {};

    function isKakaoKeyConfigured() {
        return kakaoKey.length >= 20
            && kakaoKey.indexOf('$') === -1
            && kakaoKey.indexOf('KAKAO_MAP') === -1;
    }

    function mapLoadFailMessage() {
        if (!isKakaoKeyConfigured()) {
            return '카카오 JavaScript 키가 페이지에 전달되지 않았습니다. '
                + '.env의 KAKAO_MAP_JS_KEY 설정 후 ./gradlew bootRun으로 완전히 재시작하세요.';
        }
        return '카카오 지도 SDK를 불러오지 못했습니다. '
            + '① 개발자 콘솔의 JavaScript 키(REST 키 아님) ② Web 플랫폼 사이트 도메인에 '
            + 'http://localhost:8111 과 http://127.0.0.1:8111 등록 ③ 브라우저 주소가 등록한 도메인과 동일한지 확인하세요.';
    }

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
    function showMapMessage(canvas, message) {
        if (!canvas) {
            return;
        }
        canvas.innerHTML = '<p class="book-detail__map-fallback">' + message + '</p>';
    }

    function loadKakaoSdk(callback, onFail) {
        if (!isKakaoKeyConfigured()) {
            if (typeof onFail === 'function') {
                onFail();
            }
            return;
        }
        if (kakaoLoadFailed) {
            if (typeof onFail === 'function') {
                onFail();
            }
            return;
        }
        if (kakaoReady && window.kakao && window.kakao.maps) {
            callback();
            return;
        }
        if (kakaoLoading) {
            var wait = setInterval(function () {
                if (kakaoReady) {
                    clearInterval(wait);
                    callback();
                } else if (kakaoLoadFailed) {
                    clearInterval(wait);
                    if (typeof onFail === 'function') {
                        onFail();
                    }
                }
            }, 100);
            return;
        }
        function finishLoad() {
            if (!window.kakao || !window.kakao.maps) {
                kakaoLoading = false;
                kakaoLoadFailed = true;
                console.error('[book-detail] Kakao Maps SDK 없음 — 키·도메인 설정 확인');
                if (typeof onFail === 'function') {
                    onFail();
                }
                return;
            }
            window.kakao.maps.load(function () {
                kakaoReady = true;
                kakaoLoading = false;
                callback();
            });
        }

        var existing = document.querySelector('script[src*="dapi.kakao.com/v2/maps/sdk.js"]');
        if (existing) {
            if (window.kakao && window.kakao.maps) {
                finishLoad();
            } else {
                existing.addEventListener('load', finishLoad);
                existing.addEventListener('error', function () {
                    kakaoLoading = false;
                    kakaoLoadFailed = true;
                    console.error('[book-detail] Kakao SDK script 로드 실패(도메인 미등록 가능)');
                    if (typeof onFail === 'function') {
                        onFail();
                    }
                });
            }
            return;
        }

        kakaoLoading = true;
        var script = document.createElement('script');
        script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey='
            + encodeURIComponent(kakaoKey)
            + '&autoload=false';
        script.onload = finishLoad;
        script.onerror = function () {
            kakaoLoading = false;
            kakaoLoadFailed = true;
            console.error('[book-detail] Kakao SDK script 로드 실패(도메인 미등록 가능)');
            if (typeof onFail === 'function') {
                onFail();
            }
        };
        document.head.appendChild(script);
    }

    function renderMap(index, panel) {
        var canvas = panel.querySelector('[data-map-canvas]');
        if (!canvas) {
            return;
        }
        var lib = libraries[index];
        if (!lib) {
            showMapMessage(canvas, '도서관 정보를 불러올 수 없습니다.');
            return;
        }
        var lat = parseFloat(lib.lat);
        var lon = parseFloat(lib.lon);
        if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
            showMapMessage(canvas, '지도 좌표 정보가 없습니다.');
            return;
        }
        if (!window.kakao || !window.kakao.maps) {
            showMapMessage(canvas, '지도를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
            return;
        }
        var center = new window.kakao.maps.LatLng(lat, lon);
        if (mapInstances[index]) {
            setTimeout(function () {
                mapInstances[index].relayout();
                mapInstances[index].setCenter(center);
            }, 320);
            return;
        }
        canvas.innerHTML = '';
        try {
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
            }, 320);
        } catch (e) {
            console.error('[book-detail] Kakao Map 생성 실패', e);
            showMapMessage(canvas, mapLoadFailMessage());
        }
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
                if (kakaoLink) {
                    var lat = parseFloat(lib.lat);
                    var lon = parseFloat(lib.lon);
                    if (Number.isFinite(lat) && Number.isFinite(lon)) {
                        kakaoLink.href = 'https://map.kakao.com/link/map/'
                            + encodeURIComponent(lib.name || '도서관')
                            + ',' + lat + ',' + lon;
                        kakaoLink.hidden = false;
                    } else {
                        kakaoLink.hidden = true;
                    }
                }
            }

            var canvas = panel.querySelector('[data-map-canvas]');

            loadKakaoSdk(function () {
                renderMap(index, panel);
            }, function () {
                showMapMessage(canvas, mapLoadFailMessage());
            });
        });
    });
})();
