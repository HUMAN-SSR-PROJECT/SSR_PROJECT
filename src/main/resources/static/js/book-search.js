(function () {
    var SEOUL_CODE = 11;
    var DISTRICTS = {};
    var SCROLL_STEP = 220;

    function loadDistrictCatalog() {
        var dataEl = document.getElementById('region-districts-data');
        if (!dataEl || !dataEl.textContent.trim()) {
            return;
        }
        try {
            DISTRICTS = JSON.parse(dataEl.textContent);
        } catch (e) {
            console.error('지역 코드 데이터를 읽지 못했습니다.', e);
            DISTRICTS = {};
        }
    }

    function initDragScroll(tabs) {
        if (!tabs || tabs.dataset.dragReady === 'true') {
            return;
        }

        var pointerActive = false;
        var isDragScroll = false;
        var suppressClick = false;
        var startX = 0;
        var startScrollLeft = 0;
        var DRAG_THRESHOLD = 8;

        function beginPointer(clientX) {
            pointerActive = true;
            isDragScroll = false;
            suppressClick = false;
            startX = clientX;
            startScrollLeft = tabs.scrollLeft;
        }

        function movePointer(clientX) {
            if (!pointerActive) {
                return;
            }
            var delta = clientX - startX;
            if (!isDragScroll && Math.abs(delta) > DRAG_THRESHOLD) {
                isDragScroll = true;
                suppressClick = true;
                tabs.classList.add('is-dragging');
            }
            if (isDragScroll) {
                tabs.scrollLeft = startScrollLeft - delta;
            }
        }

        function endPointer() {
            if (!pointerActive) {
                return;
            }
            pointerActive = false;
            isDragScroll = false;
            tabs.classList.remove('is-dragging');
        }

        tabs.addEventListener('mousedown', function (e) {
            if (e.button !== 0) {
                return;
            }
            beginPointer(e.pageX);
        });

        window.addEventListener('mousemove', function (e) {
            if (!pointerActive) {
                return;
            }
            if (isDragScroll) {
                e.preventDefault();
            }
            movePointer(e.pageX);
        });

        window.addEventListener('mouseup', endPointer);

        tabs.addEventListener('touchstart', function (e) {
            if (!e.touches[0]) {
                return;
            }
            beginPointer(e.touches[0].pageX);
        }, { passive: true });

        tabs.addEventListener('touchmove', function (e) {
            if (!pointerActive || !e.touches[0]) {
                return;
            }
            movePointer(e.touches[0].pageX);
        }, { passive: true });

        tabs.addEventListener('touchend', endPointer);
        tabs.addEventListener('touchcancel', endPointer);

        tabs.addEventListener('click', function (e) {
            if (suppressClick) {
                e.preventDefault();
                e.stopImmediatePropagation();
                suppressClick = false;
            }
        }, true);

        tabs.dataset.dragReady = 'true';
    }

    function initFilterScroll(scrollWrap) {
        if (!scrollWrap || scrollWrap.dataset.scrollReady === 'true') {
            return;
        }
        var tabs = scrollWrap.querySelector('.book-search__filter-tabs');
        var prev = scrollWrap.querySelector('[data-scroll-prev]');
        var next = scrollWrap.querySelector('[data-scroll-next]');
        if (!tabs || !prev || !next) {
            return;
        }

        function updateNav() {
            var maxScroll = tabs.scrollWidth - tabs.clientWidth;
            prev.disabled = tabs.scrollLeft <= 1;
            next.disabled = maxScroll <= 1 || tabs.scrollLeft >= maxScroll - 1;
        }

        prev.addEventListener('click', function () {
            tabs.scrollBy({ left: -SCROLL_STEP, behavior: 'smooth' });
        });
        next.addEventListener('click', function () {
            tabs.scrollBy({ left: SCROLL_STEP, behavior: 'smooth' });
        });
        tabs.addEventListener('scroll', updateNav);
        window.addEventListener('resize', updateNav);
        initDragScroll(tabs);
        scrollWrap.dataset.scrollReady = 'true';
        scrollWrap.__updateFilterNav = updateNav;
        updateNav();
    }

    function refreshFilterScroll(scrollWrap) {
        if (scrollWrap && typeof scrollWrap.__updateFilterNav === 'function') {
            scrollWrap.__updateFilterNav();
        }
    }

    function initBookSearch() {
        loadDistrictCatalog();

        var form = document.querySelector('[data-book-search-form]');
        if (!form) {
            return;
        }

        var cityInput = document.getElementById('search-city');
        var districtInput = document.getElementById('search-district');
        var keywordInput = document.getElementById('search-keyword');
        var clearButton = form.querySelector('[data-search-clear]');
        var cityTabs = document.querySelector('[data-city-tabs]');
        var districtTabs = document.querySelector('[data-district-tabs]');
        var districtScroll = districtTabs ? districtTabs.closest('[data-filter-scroll]') : null;
        var filterClear = document.querySelector('[data-filter-clear]');

        document.querySelectorAll('[data-filter-scroll]').forEach(initFilterScroll);

        function setActiveTab(container, code, attr) {
            if (!container) {
                return;
            }
            container.querySelectorAll('.book-search__filter-tab').forEach(function (tab) {
                var tabCode = tab.getAttribute(attr);
                tab.classList.toggle('is-active', tabCode === String(code));
            });
        }

        function renderDistrictTabs(cityCode, selectedDistrict) {
            if (!districtTabs) {
                return;
            }
            var list = DISTRICTS[String(cityCode)] || [{ code: 0, name: '전체' }];
            districtTabs.innerHTML = '';
            list.forEach(function (district) {
                var button = document.createElement('button');
                button.type = 'button';
                button.className = 'book-search__filter-tab';
                if (district.code === selectedDistrict) {
                    button.classList.add('is-active');
                }
                button.textContent = district.name;
                button.setAttribute('data-district-code', district.code);
                button.addEventListener('click', function () {
                    districtInput.value = district.code;
                    setActiveTab(districtTabs, district.code, 'data-district-code');
                    if (keywordInput && keywordInput.value.trim()) {
                        form.submit();
                    }
                });
                districtTabs.appendChild(button);
            });
            districtTabs.scrollLeft = 0;
            refreshFilterScroll(districtScroll);
        }

        function updateClearButton() {
            if (!clearButton || !keywordInput) {
                return;
            }
            clearButton.classList.toggle('is-visible', keywordInput.value.trim().length > 0);
        }

        if (cityTabs) {
            cityTabs.querySelectorAll('[data-city-code]').forEach(function (button) {
                button.addEventListener('click', function () {
                    var cityCode = parseInt(button.getAttribute('data-city-code'), 10);
                    cityInput.value = cityCode;
                    districtInput.value = 0;
                    setActiveTab(cityTabs, cityCode, 'data-city-code');
                    renderDistrictTabs(cityCode, 0);
                    if (keywordInput && keywordInput.value.trim()) {
                        form.submit();
                    }
                });
            });
        }

        if (filterClear) {
            filterClear.addEventListener('click', function () {
                cityInput.value = SEOUL_CODE;
                districtInput.value = 0;
                setActiveTab(cityTabs, SEOUL_CODE, 'data-city-code');
                renderDistrictTabs(SEOUL_CODE, 0);
            });
        }

        if (clearButton && keywordInput) {
            clearButton.addEventListener('click', function () {
                keywordInput.value = '';
                keywordInput.focus();
                updateClearButton();
            });
            keywordInput.addEventListener('input', updateClearButton);
            updateClearButton();
        }

        document.querySelectorAll('[data-suggest-keyword]').forEach(function (button) {
            button.addEventListener('click', function () {
                if (!keywordInput) {
                    return;
                }
                keywordInput.value = button.getAttribute('data-suggest-keyword') || '';
                updateClearButton();
                form.submit();
            });
        });

        var initialCity = parseInt(cityInput.value, 10) || SEOUL_CODE;
        var initialDistrict = parseInt(districtInput.value, 10) || 0;
        renderDistrictTabs(initialCity, initialDistrict);
    }

    document.addEventListener('DOMContentLoaded', initBookSearch);
})();
