(function () {
    var KEY = 'xxy-theme';

    function apply(theme) {
        var t = theme === 'light' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', t);
        try {
            localStorage.setItem(KEY, t);
        } catch (e) { /* ignore */ }
        var nextLabel = t === 'dark' ? '切换为浅色' : '切换为深色';
        document.querySelectorAll('[data-theme-toggle]').forEach(function (el) {
            el.textContent = nextLabel;
            el.setAttribute('aria-label', '主题：' + (t === 'dark' ? '深色' : '浅色') + '，点击' + nextLabel);
        });
    }

    var saved = null;
    try {
        saved = localStorage.getItem(KEY);
    } catch (e) { /* ignore */ }
    if (saved !== 'light' && saved !== 'dark') {
        saved = 'dark';
    }
    apply(saved);

    document.addEventListener('click', function (e) {
        var btn = e.target.closest('[data-theme-toggle]');
        if (!btn) {
            return;
        }
        e.preventDefault();
        var cur = document.documentElement.getAttribute('data-theme') || 'dark';
        apply(cur === 'light' ? 'dark' : 'light');
    });
})();
