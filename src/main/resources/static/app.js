const shortenForm = document.getElementById('shorten-form');
const statsForm = document.getElementById('stats-form');

shortenForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hide('shorten-error');
    hide('result');

    const body = { url: document.getElementById('url').value.trim() };
    const expiry = document.getElementById('expiry').value;
    if (expiry) {
        body.expiresInDays = Number(expiry);
    }

    try {
        const response = await fetch('/api/shorten', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        });
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || 'Could not shorten this URL');
        }

        const link = document.getElementById('short-url');
        link.href = data.shortUrl;
        link.textContent = data.shortUrl;
        document.getElementById('expires-text').textContent = data.expiresAt
            ? `Expires on ${formatDate(data.expiresAt)}`
            : 'This link never expires.';
        document.getElementById('code').value = data.shortCode;
        show('result');
    } catch (error) {
        showError('shorten-error', error.message);
    }
});

document.getElementById('copy-btn').addEventListener('click', async () => {
    const button = document.getElementById('copy-btn');
    await navigator.clipboard.writeText(document.getElementById('short-url').textContent);
    button.textContent = 'Copied!';
    setTimeout(() => (button.textContent = 'Copy'), 1500);
});

statsForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hide('stats-error');
    hide('stats');

    // Accept either the code itself or a full short link.
    const code = document.getElementById('code').value.trim().split('/').pop();

    try {
        const response = await fetch(`/api/urls/${encodeURIComponent(code)}/stats`);
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || 'Could not load stats');
        }
        renderStats(data);
        show('stats');
    } catch (error) {
        showError('stats-error', error.message);
    }
});

function renderStats(data) {
    document.getElementById('total-clicks').textContent = data.totalClicks;

    const status = document.getElementById('status');
    status.textContent = data.expired ? 'Expired' : 'Active';
    status.className = `stat-value ${data.expired ? 'status-expired' : 'status-active'}`;

    document.getElementById('last-click').textContent = data.lastClickedAt ? formatDate(data.lastClickedAt) : 'No clicks yet';

    const original = document.getElementById('original-url');
    original.href = data.originalUrl;
    original.textContent = data.originalUrl;

    renderChart(data.clicksPerDay);

    const referrers = document.getElementById('referrers');
    referrers.innerHTML = '';
    if (data.topReferrers.length === 0) {
        referrers.innerHTML = '<li class="muted">No referrers yet</li>';
    }
    for (const { referrer, clicks } of data.topReferrers) {
        const item = document.createElement('li');
        item.textContent = `${referrer}: ${clicks}`;
        referrers.appendChild(item);
    }
}

function renderChart(clicksPerDay) {
    const chart = document.getElementById('chart');
    chart.innerHTML = '';
    const entries = Object.entries(clicksPerDay);
    const max = Math.max(1, ...entries.map(([, count]) => count));

    for (const [day, count] of entries) {
        const column = document.createElement('div');
        column.className = 'bar-col';

        const countLabel = document.createElement('span');
        countLabel.className = 'bar-count';
        countLabel.textContent = count;

        const bar = document.createElement('div');
        bar.className = 'bar';
        bar.style.height = `${(count / max) * 100}%`;

        const dayLabel = document.createElement('span');
        dayLabel.className = 'bar-label';
        dayLabel.textContent = new Date(`${day}T00:00:00`).toLocaleDateString(undefined, { weekday: 'short' });

        column.append(countLabel, bar, dayLabel);
        chart.appendChild(column);
    }
}

function formatDate(iso) {
    return new Date(iso).toLocaleString();
}

function show(id) {
    document.getElementById(id).hidden = false;
}

function hide(id) {
    document.getElementById(id).hidden = true;
}

function showError(id, message) {
    const element = document.getElementById(id);
    element.textContent = message;
    element.hidden = false;
}
