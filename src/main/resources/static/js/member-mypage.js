(function () {
    var root = document.querySelector('[data-mypage]');
    if (!root || !root.classList.contains('mypage--edit')) {
        return;
    }

    var fileInput = root.querySelector('[data-mypage-avatar-input]');
    var imgEl = root.querySelector('[data-mypage-avatar-img]');
    var initialEl = root.querySelector('[data-mypage-avatar-initial]');

    if (!fileInput) {
        return;
    }

    fileInput.addEventListener('change', function () {
        var file = fileInput.files && fileInput.files[0];
        if (!file || !file.type.startsWith('image/')) {
            return;
        }

        var reader = new FileReader();
        reader.onload = function () {
            if (imgEl) {
                imgEl.src = reader.result;
                imgEl.hidden = false;
            } else {
                var newImg = document.createElement('img');
                newImg.className = 'mypage__avatar-img';
                newImg.setAttribute('data-mypage-avatar-img', '');
                newImg.alt = '프로필 미리보기';
                newImg.src = reader.result;
                var label = fileInput.closest('.mypage__avatar--editable');
                if (label) {
                    label.insertBefore(newImg, label.firstChild);
                    imgEl = newImg;
                }
            }
            if (initialEl) {
                initialEl.hidden = true;
            }
        };
        reader.readAsDataURL(file);
    });
})();
