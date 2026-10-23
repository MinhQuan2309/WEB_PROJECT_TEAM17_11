// Kết nối WebSocket/STOMP để nhận thông báo đơn hàng theo thời gian thực (tuần 4, Thiện).
//
// Cách dùng trong một trang Thymeleaf:
//   <div id="realtimeToasts" class="toast-container position-fixed top-0 end-0 p-3"></div>
//   <script th:inline="javascript">
//       window.utetraRealtime = { shopId: /*[[${shopId}]]*/ null };
//   </script>
//   <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
//   <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
//   <script th:src="@{/js/order-realtime.js}"></script>
//
// Kênh nhận:
//   /topic/shop/{shopId}/orders   -> mọi người trong shop (Vendor)
//   /user/queue/orders            -> riêng khách đặt đơn (và chủ shop)
//   /user/queue/deliveries        -> riêng shipper được phân công

(function () {
    'use strict';

    var config = window.utetraRealtime || {};

    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        return; // Trang không nạp thư viện STOMP thì bỏ qua, không làm hỏng trang.
    }

    var socket = new SockJS('/ws');
    var client = Stomp.over(socket);
    client.debug = null; // Tắt log từng khung tin cho đỡ rối console.

    function showToast(text) {
        var host = document.getElementById('realtimeToasts');
        if (!host) {
            return;
        }
        var el = document.createElement('div');
        el.className = 'toast show align-items-center text-bg-primary border-0 mb-2';
        el.setAttribute('role', 'alert');
        el.innerHTML = '<div class="d-flex"><div class="toast-body"></div>'
            + '<button type="button" class="btn-close btn-close-white me-2 m-auto"></button></div>';
        el.querySelector('.toast-body').textContent = text;
        el.querySelector('.btn-close').addEventListener('click', function () { el.remove(); });
        host.appendChild(el);
        window.setTimeout(function () { el.remove(); }, 8000);
    }

    function onMessage(frame) {
        var event;
        try {
            event = JSON.parse(frame.body);
        } catch (e) {
            return;
        }
        var text = event.newOrder
            ? 'Đơn mới ' + event.code + ' - ' + (event.total || 0).toLocaleString('vi-VN') + ' đ'
            : 'Đơn ' + event.code + ': ' + (event.statusLabel || event.status) + ' (tổng ' + (event.total || 0).toLocaleString('vi-VN') + ' đ)';
        showToast(text);

        // Trang nào có bảng đơn hàng thì tự nạp lại để thấy dữ liệu mới.
        var table = document.getElementById('vendorOrdersTable');
        if (table && config.reloadOnEvent !== false) {
            window.setTimeout(function () { window.location.reload(); }, 1500);
        }
        document.dispatchEvent(new CustomEvent('utetra:order-event', { detail: event }));
    }

    client.connect({}, function () {
        if (config.shopId) {
            client.subscribe('/topic/shop/' + config.shopId + '/orders', onMessage);
        }
        client.subscribe('/user/queue/orders', onMessage);
        if (config.shipper) {
            client.subscribe('/user/queue/deliveries', onMessage);
        }
    }, function () {
        // Mất kết nối: để lần sau người dùng tải lại trang là kết nối lại, không spam.
    });
})();