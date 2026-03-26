/* ═══════════════════════════════════════════════════════════════
   InsureTrack Enterprise — app.js  v4.0  (fully tested & fixed)
   ═══════════════════════════════════════════════════════════════ */

/* ── DOMContentLoaded guard ─────────────────────────────────── */
document.addEventListener('DOMContentLoaded', function () {

  /* ── 1. Mobile sidebar toggle ───────────────────────────────── */
  window.toggleSidebar = function () {
    var sidebar = document.querySelector('.sidebar');
    var overlay = document.getElementById('mobileOverlay') ||
                  document.getElementById('mobileOverlayAlt');
    if (!sidebar) return;
    var open = sidebar.classList.toggle('open');
    if (overlay) overlay.classList.toggle('active', open);
  };

  window.closeSidebar = function () {
    var sidebar = document.querySelector('.sidebar');
    var overlay = document.getElementById('mobileOverlay') ||
                  document.getElementById('mobileOverlayAlt');
    if (sidebar)  sidebar.classList.remove('open');
    if (overlay)  overlay.classList.remove('active');
  };

  /* ── 2. Alert dismiss (all patterns) ────────────────────────── */
  function dismissAlert(el) {
    if (!el || !el.parentNode) return;
    el.style.transition = 'opacity .35s';
    el.style.opacity    = '0';
    setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, 380);
  }

  // Auto-dismiss after 5 s
  document.querySelectorAll('.alert-dismissible, [data-auto-dismiss]').forEach(function (el) {
    setTimeout(function () { dismissAlert(el); }, 5000);
  });

  // Manual close buttons
  document.addEventListener('click', function (e) {
    var btn = e.target.closest('.alert-close, [data-dismiss-alert]');
    if (btn) dismissAlert(btn.closest('.alert'));
  });

  /* ── 3. Modal open / close ───────────────────────────────────── */
  document.querySelectorAll('[data-modal-open]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var el = document.getElementById(btn.dataset.modalOpen);
      if (el) el.classList.add('open');
    });
  });

  document.addEventListener('click', function (e) {
    var closer = e.target.closest('[data-modal-close], .modal-close');
    if (closer) {
      var overlay = closer.closest('.modal-overlay');
      if (overlay) overlay.classList.remove('open');
    }
    // Click backdrop to close
    if (e.target.classList.contains('modal-overlay')) {
      e.target.classList.remove('open');
    }
  });

  /* ── 4. Notification dropdown ────────────────────────────────── */
  var notifOpen = false;

  window.toggleNotifDropdown = function (e) {
    if (e) e.stopPropagation();
    var dd = document.getElementById('notifDropdown');
    if (!dd) return;
    notifOpen = !notifOpen;
    dd.style.display = notifOpen ? 'block' : 'none';
    if (notifOpen) loadNotifDropdown();
  };

  function closeNotifDropdown() {
    var dd = document.getElementById('notifDropdown');
    if (dd) dd.style.display = 'none';
    notifOpen = false;
  }

  // Close when clicking outside
  document.addEventListener('click', function (e) {
    var wrap = document.getElementById('notifBellWrap');
    if (wrap && !wrap.contains(e.target)) closeNotifDropdown();
  });

  function loadNotifDropdown() {
    var list = document.getElementById('notifDropdownList');
    if (!list) return;
    list.innerHTML = '<div class="notif-dropdown-loading"><i class="bi bi-hourglass-split"></i> Loading…</div>';

    fetch('/notifications/api/recent')
      .then(function (r) { return r.ok ? r.json() : Promise.reject(r.status); })
      .then(function (items) {
        if (!items || items.length === 0) {
          list.innerHTML = '<div class="notif-dd-empty"><i class="bi bi-bell-slash"></i>No notifications yet</div>';
          return;
        }
        var styles = {
          EXPIRY_WARNING : { bg: 'rgba(245,158,11,.15)',  color: '#f59e0b', icon: 'bi-calendar-x'        },
          PAYMENT_FAILED : { bg: 'rgba(239,68,68,.15)',   color: '#ef4444', icon: 'bi-exclamation-circle' },
          PAYMENT_SUCCESS: { bg: 'rgba(16,185,129,.15)',  color: '#10b981', icon: 'bi-check-circle'       },
          POLICY_CREATED : { bg: 'rgba(59,130,246,.15)',  color: '#3b82f6', icon: 'bi-file-earmark-plus'  },
          SYSTEM         : { bg: 'rgba(6,182,212,.15)',   color: '#06b6d4', icon: 'bi-gear'               },
          DEFAULT        : { bg: 'rgba(148,163,184,.15)', color: '#94a3b8', icon: 'bi-bell'               }
        };
        list.innerHTML = items.slice(0, 8).map(function (n) {
          var s = styles[n.type] || styles.DEFAULT;
          return '<div class="notif-dd-item' + (!n.read ? ' unread' : '') + '">' +
            '<div class="notif-dd-icon" style="background:' + s.bg + ';color:' + s.color + '">' +
              '<i class="bi ' + s.icon + '"></i>' +
            '</div>' +
            '<div class="notif-dd-content">' +
              '<div class="notif-dd-title">' + esc(n.title)   + '</div>' +
              '<div class="notif-dd-msg">'   + esc(n.message) + '</div>' +
              '<div class="notif-dd-time">'  + esc(n.time)    + '</div>' +
            '</div>' +
            (!n.read ? '<div class="notif-dd-dot"></div>' : '') +
          '</div>';
        }).join('');
      })
      .catch(function () {
        list.innerHTML = '<div class="notif-dd-empty"><i class="bi bi-wifi-off"></i>Could not load</div>';
      });
  }

  function esc(s) {
    return String(s || '')
      .replace(/&/g,'&amp;').replace(/</g,'&lt;')
      .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  /* ── 5. Notification badge polling (30 s) ────────────────────── */
  function pollNotifCount() {
    fetch('/notifications/api/unread-count')
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (d) {
        if (!d) return;
        var count = d.count || 0;
        var sideBadge = document.getElementById('notifBadge');
        if (sideBadge) {
          sideBadge.textContent = count;
          sideBadge.style.display = count > 0 ? 'inline-flex' : 'none';
        }
        var dot = document.getElementById('topbarNotifBadge');
        if (dot) dot.style.display = count > 0 ? 'block' : 'none';
      })
      .catch(function () {/* silent */});
  }
  pollNotifCount();
  setInterval(pollNotifCount, 30000);

}); // end DOMContentLoaded
