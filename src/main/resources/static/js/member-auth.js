(function () {
    var BIRTH_PATTERN = /^\d{8}$/;
    var EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    function digitsOnly(value) {
        return (value || '').replace(/\D/g, '');
    }

    function parseBirth(raw) {
        var digits = digitsOnly(raw);
        if (!BIRTH_PATTERN.test(digits)) {
            return null;
        }
        var year = parseInt(digits.slice(0, 4), 10);
        var month = parseInt(digits.slice(4, 6), 10);
        var day = parseInt(digits.slice(6, 8), 10);
        var date = new Date(year, month - 1, day);
        if (
            date.getFullYear() !== year ||
            date.getMonth() !== month - 1 ||
            date.getDate() !== day
        ) {
            return null;
        }
        return (
            String(year).padStart(4, '0') +
            '-' +
            String(month).padStart(2, '0') +
            '-' +
            String(day).padStart(2, '0')
        );
    }

    function showFieldError(field, message) {
        field.classList.add('is-invalid');
        var existing = field.parentElement.querySelector('.auth__field-error');
        if (existing) {
            existing.textContent = message;
            return;
        }
        var error = document.createElement('p');
        error.className = 'auth__field-error';
        error.setAttribute('role', 'alert');
        error.textContent = message;
        field.parentElement.appendChild(error);
    }

    function clearFieldError(field) {
        field.classList.remove('is-invalid');
        var wrap = field.closest('.auth__field');
        if (!wrap) {
            return;
        }
        var error = wrap.querySelector('.auth__field-error');
        if (error) {
            error.remove();
        }
    }

    function validateEmailInput(input) {
        var value = input.value.trim();
        if (!value) {
            showFieldError(input, '이메일을 입력해 주세요.');
            return false;
        }
        if (!EMAIL_PATTERN.test(value)) {
            showFieldError(input, '올바른 이메일 형식이 아닙니다.');
            return false;
        }
        clearFieldError(input);
        return true;
    }

    function validatePasswordInput(input, options) {
        var value = input.value;
        if (!value) {
            showFieldError(input, '비밀번호를 입력해 주세요.');
            return false;
        }
        if (value.indexOf(' ') >= 0) {
            showFieldError(input, '비밀번호에 공백을 사용할 수 없습니다.');
            return false;
        }
        if (options && options.minLength && value.length < options.minLength) {
            showFieldError(input, '비밀번호는 8자 이상이어야 합니다.');
            return false;
        }
        clearFieldError(input);
        return true;
    }

    function validateNameInput(input) {
        var value = input.value.trim();
        if (!value) {
            showFieldError(input, '이름을 입력해 주세요.');
            return false;
        }
        if (value.length < 2) {
            showFieldError(input, '이름은 2자 이상 입력해 주세요.');
            return false;
        }
        clearFieldError(input);
        return true;
    }

    function validateNicknameInput(input) {
        var value = input.value.trim();
        if (!value) {
            showFieldError(input, '닉네임을 입력해 주세요.');
            return false;
        }
        if (value.length < 2 || value.length > 20) {
            showFieldError(input, '닉네임은 2~20자로 입력해 주세요.');
            return false;
        }
        clearFieldError(input);
        return true;
    }

    function validateBirthInput(input) {
        var value = input.value.trim();
        if (!value) {
            showFieldError(input, '생년월일을 입력해 주세요.');
            return false;
        }
        if (!parseBirth(value)) {
            showFieldError(input, '생년월일은 19970701 형식(8자리)으로 입력해 주세요.');
            return false;
        }
        clearFieldError(input);
        return true;
    }

    function bindBirthInput(input) {
        input.addEventListener('input', function () {
            input.value = digitsOnly(input.value).slice(0, 8);
        });
    }

    function prepareBirthHidden(form) {
        var birthInput = form.querySelector('[data-birth-input]');
        var birthHidden = form.querySelector('[data-birth-hidden]');
        if (!birthInput || !birthHidden) {
            return true;
        }
        var iso = parseBirth(birthInput.value.trim());
        if (!iso) {
            showFieldError(birthInput, '생년월일은 19970701 형식(8자리)으로 입력해 주세요.');
            return false;
        }
        birthHidden.value = iso;
        return true;
    }

    function initLoginForm() {
        var form = document.querySelector('[data-auth-login]');
        if (!form) {
            return;
        }
        form.addEventListener('submit', function (event) {
            var email = form.querySelector('input[name="email"]');
            var password = form.querySelector('input[name="password"]');
            var valid =
                validateEmailInput(email) &&
                validatePasswordInput(password, { minLength: 1 });
            if (!valid) {
                event.preventDefault();
            }
        });
    }

    function initJoinForm() {
        var form = document.querySelector('[data-auth-join]');
        if (!form) {
            return;
        }
        var birthInput = form.querySelector('[data-birth-input]');
        var password = form.querySelector('input[name="password"]');
        var confirm = form.querySelector('input[name="passwordConfirm"]');
        if (birthInput) {
            bindBirthInput(birthInput);
        }

        form.addEventListener('submit', function (event) {
            var email = form.querySelector('input[name="email"]');
            var name = form.querySelector('input[name="name"]');
            var nickname = form.querySelector('input[name="nickname"]');
            var valid =
                validateEmailInput(email) &&
                validateNameInput(name) &&
                validateNicknameInput(nickname) &&
                validateBirthInput(birthInput) &&
                validatePasswordInput(password, { minLength: 8 }) &&
                prepareBirthHidden(form);

            if (confirm) {
                if (!confirm.value) {
                    showFieldError(confirm, '비밀번호 확인을 입력해 주세요.');
                    valid = false;
                } else if (confirm.value !== password.value) {
                    showFieldError(confirm, '비밀번호가 일치하지 않습니다.');
                    valid = false;
                } else {
                    clearFieldError(confirm);
                }
            }

            if (form.dataset.emailVerified !== 'true') {
                event.preventDefault();
                showFieldError(email, '이메일 중복 확인을 해 주세요.');
                valid = false;
            }

            if (!valid) {
                event.preventDefault();
            }
        });

        var checkForm = document.querySelector('[data-auth-email-check]');
        if (checkForm) {
            checkForm.addEventListener('submit', function (event) {
                var email = checkForm.querySelector('input[name="email"]');
                if (!validateEmailInput(email)) {
                    event.preventDefault();
                }
            });
        }
    }

    function initFindAccount() {
        var root = document.querySelector('[data-find-account]');
        if (!root) {
            return;
        }

        var tabs = root.querySelectorAll('[data-find-tab]');
        var panels = root.querySelectorAll('[data-find-panel]');

        tabs.forEach(function (tab) {
            tab.addEventListener('click', function () {
                var target = tab.getAttribute('data-find-tab');
                tabs.forEach(function (item) {
                    var active = item === tab;
                    item.classList.toggle('is-active', active);
                    item.setAttribute('aria-selected', active ? 'true' : 'false');
                });
                panels.forEach(function (panel) {
                    var isMatch = panel.getAttribute('data-find-panel') === target;
                    panel.hidden = !isMatch;
                });
            });
        });

        root.querySelectorAll('[data-find-form]').forEach(function (form) {
            var birthInput = form.querySelector('[data-birth-input]');
            if (birthInput) {
                bindBirthInput(birthInput);
            }
            form.addEventListener('submit', function (event) {
                var valid = true;
                if (form.getAttribute('data-find-form') === 'id') {
                    valid =
                        validateNameInput(form.querySelector('input[name="name"]')) &&
                        validateBirthInput(birthInput) &&
                        prepareBirthHidden(form);
                } else {
                    valid =
                        validateEmailInput(form.querySelector('input[name="email"]')) &&
                        validateNameInput(form.querySelector('input[name="name"]')) &&
                        validateBirthInput(birthInput) &&
                        prepareBirthHidden(form);
                }
                if (!valid) {
                    event.preventDefault();
                }
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initLoginForm();
        initJoinForm();
        initFindAccount();
    });
})();
