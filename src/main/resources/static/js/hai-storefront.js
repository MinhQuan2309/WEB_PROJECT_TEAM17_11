// Script xu ly Lazy Loading 4 block Trang chu, tinh gia dong trang Chi tiet SP va dem ky tu Binh luan
document.addEventListener('DOMContentLoaded', function () {

    // 1. Lazy loading cho 4 block 20 san pham tren trang chu User
    document.querySelectorAll('.js-lazy-load-block').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const type = btn.getAttribute('data-block-type');
            const nextPage = parseInt(btn.getAttribute('data-next-page') || '1', 10);
            const size = parseInt(btn.getAttribute('data-page-size') || '20', 10);
            const containerSelector = btn.getAttribute('data-target-container');
            const container = document.querySelector(containerSelector);
            if (!container) return;

            btn.disabled = true;
            const originalHtml = btn.innerHTML;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang tải...';

            fetch(`/products/api/block?type=${encodeURIComponent(type)}&page=${nextPage}&size=${size}`)
                .then(res => res.json())
                .then(data => {
                    if (data.items && data.items.length > 0) {
                        data.items.forEach(item => {
                            const col = document.createElement('div');
                            col.className = 'col';
                            col.innerHTML = `
                                <div class="card h-100 product-card shadow-sm border-0">
                                    <img class="card-img-top product-thumb" src="${item.imageUrl}" alt="${item.name}"/>
                                    <div class="card-body d-flex flex-column">
                                        <h6 class="card-title mb-1 text-truncate" title="${item.name}">${item.name}</h6>
                                        <div class="small text-muted mb-2">${item.shopName || ''}</div>
                                        <div class="mt-auto d-flex justify-content-between align-items-center">
                                            <span class="fw-bold text-danger">${item.formattedPrice}</span>
                                            <small class="text-muted">
                                                <i class="bi bi-star-fill text-warning"></i> ${item.ratingAvg}
                                                &middot; Đã bán ${item.soldCount}
                                            </small>
                                        </div>
                                    </div>
                                    <a class="stretched-link" href="/products/${item.id}" aria-label="${item.name}"></a>
                                </div>`;
                            container.appendChild(col);
                        });
                    }
                    if (data.hasNext) {
                        btn.setAttribute('data-next-page', String(data.page + 1));
                        btn.disabled = false;
                        btn.innerHTML = originalHtml;
                    } else {
                        btn.remove();
                    }
                })
                .catch(() => {
                    btn.disabled = false;
                    btn.innerHTML = originalHtml;
                });
        });
    });

    // 2. Cap nhat gia dong tren trang Chi tiet san pham (Size + Topping + So luong)
    const detailForm = document.getElementById('productOptionForm');
    if (detailForm) {
        const basePrice = parseFloat(detailForm.getAttribute('data-base-price') || '0');
        const unitPriceEl = document.getElementById('dynamicUnitPrice');
        const totalPriceEl = document.getElementById('dynamicTotalPrice');
        const qtyInput = document.getElementById('quantityInput');

        const formatVnd = (amount) => {
            return new Intl.NumberFormat('vi-VN').format(Math.round(amount)) + ' đ';
        };

        const recalculatePrice = () => {
            let extraVariant = 0;
            const checkedVariant = detailForm.querySelector('input[name="variantId"]:checked');
            if (checkedVariant) {
                extraVariant = parseFloat(checkedVariant.getAttribute('data-extra-price') || '0');
            }

            let extraToppings = 0;
            detailForm.querySelectorAll('input[name="toppingIds"]:checked').forEach(cb => {
                extraToppings += parseFloat(cb.getAttribute('data-topping-price') || '0');
            });

            const qty = Math.max(1, parseInt(qtyInput ? qtyInput.value : '1', 10) || 1);
            const unitPrice = basePrice + extraVariant + extraToppings;
            const total = unitPrice * qty;

            if (unitPriceEl) unitPriceEl.textContent = formatVnd(unitPrice);
            if (totalPriceEl) totalPriceEl.textContent = formatVnd(total);
        };

        detailForm.querySelectorAll('input[name="variantId"], input[name="toppingIds"]').forEach(el => {
            el.addEventListener('change', recalculatePrice);
        });
        if (qtyInput) {
            qtyInput.addEventListener('input', recalculatePrice);
            qtyInput.addEventListener('change', recalculatePrice);
        }
        recalculatePrice();
    }

    // 3. Bo dem ky tu binh luan (>= 50 ky tu)
    const reviewTextarea = document.getElementById('reviewContentInput');
    const reviewCounter = document.getElementById('reviewCharCounter');
    const reviewSubmitBtn = document.getElementById('reviewSubmitBtn');
    if (reviewTextarea && reviewCounter) {
        const updateCounter = () => {
            const len = reviewTextarea.value.trim().length;
            reviewCounter.textContent = `${len}/50 ký tự tối thiểu`;
            if (len >= 50) {
                reviewCounter.classList.remove('text-danger');
                reviewCounter.classList.add('text-success');
                if (reviewSubmitBtn) reviewSubmitBtn.disabled = false;
            } else {
                reviewCounter.classList.remove('text-success');
                reviewCounter.classList.add('text-danger');
            }
        };
        reviewTextarea.addEventListener('input', updateCounter);
        updateCounter();
    }
});
