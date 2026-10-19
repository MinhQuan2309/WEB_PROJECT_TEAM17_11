// JavaScript riêng của module vendor (Thiện). Không sửa app.js dùng chung.
//
// 1. Thêm / bỏ dòng size trong form sản phẩm, đánh số lại name="variants[i]..." để Spring bind được.
// 2. Xem trước ảnh ngay khi chọn file.

(function () {
    'use strict';

    // ===== 1. Dòng size động =====
    var table = document.getElementById('variantTable');
    var template = document.getElementById('variantRowTemplate');

    if (table && template) {
        var tbody = table.querySelector('tbody');

        function rows() {
            return Array.prototype.slice.call(tbody.querySelectorAll('tr'));
        }

        // Đánh số lại toàn bộ dòng để name luôn là variants[0], variants[1]...
        function renumber() {
            rows().forEach(function (tr, index) {
                tr.querySelectorAll('input[name]').forEach(function (input) {
                    input.name = input.name.replace(/variants\[\d+]/, 'variants[' + index + ']');
                    if (input.id) {
                        input.id = input.id.replace(/variants\d+/, 'variants' + index);
                    }
                });
            });
        }

        var addButton = document.getElementById('addVariant');
        if (addButton) {
            addButton.addEventListener('click', function () {
                var html = template.innerHTML.replace(/__INDEX__/g, String(rows().length));
                tbody.insertAdjacentHTML('beforeend', html);
            });
        }

        tbody.addEventListener('click', function (event) {
            var button = event.target.closest('.remove-variant');
            if (!button) {
                return;
            }
            var current = rows();
            if (current.length <= 1) {
                // Giữ lại ít nhất một dòng trống để form vẫn gửi được.
                current[0].querySelectorAll('input').forEach(function (input) {
                    if (input.type !== 'hidden') {
                        input.value = '';
                    }
                });
                return;
            }
            button.closest('tr').remove();
            renumber();
        });
    }

    // ===== 2. Xem trước ảnh =====
    document.querySelectorAll('input[type=file][data-preview-target]').forEach(function (input) {
        input.addEventListener('change', function () {
            var target = document.querySelector(input.getAttribute('data-preview-target'));
            if (!target || !input.files || input.files.length === 0) {
                return;
            }
            target.src = URL.createObjectURL(input.files[0]);
            target.classList.remove('d-none');
        });
    });
})();