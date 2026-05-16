(function () {
    function initProfileMenu() {
        var profile = document.querySelector('[data-profile]');
        if (!profile) {
            return;
        }

        var trigger = profile.querySelector('[data-profile-toggle]');
        var menu = profile.querySelector('[data-profile-menu]');
        if (!trigger || !menu) {
            return;
        }

        function closeMenu() {
            profile.classList.remove('is-open');
            trigger.setAttribute('aria-expanded', 'false');
            menu.hidden = true;
        }

        function openMenu() {
            profile.classList.add('is-open');
            trigger.setAttribute('aria-expanded', 'true');
            menu.hidden = false;
        }

        function toggleMenu() {
            if (menu.hidden) {
                openMenu();
            } else {
                closeMenu();
            }
        }

        trigger.addEventListener('click', function (event) {
            event.stopPropagation();
            toggleMenu();
        });

        document.addEventListener('click', function (event) {
            if (!profile.contains(event.target)) {
                closeMenu();
            }
        });

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                closeMenu();
            }
        });
    }

    function initHeroSearch() {
        var form = document.querySelector('[data-hero-search]');
        if (!form) {
            return;
        }

        form.addEventListener('submit', function (event) {
            var input = form.querySelector('input[name="keyword"]');
            if (input && !input.value.trim()) {
                event.preventDefault();
                window.location.href = form.dataset.emptyAction || '/book/search';
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initProfileMenu();
        initHeroSearch();
    });
})();
