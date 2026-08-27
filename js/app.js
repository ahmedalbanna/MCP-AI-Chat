const messagesEl = document.getElementById('messages');
const inputEl = document.getElementById('promptInput');
const sendBtn = document.getElementById('sendBtn');
const attachBtn = document.getElementById('attachBtn');
const chatMain = document.getElementById('chatMain');
const toolsModal = document.getElementById('toolsModal');
const toolsBackdrop = document.getElementById('toolsBackdrop');
const closeModalBtn = document.getElementById('closeModal');
const toastEl = document.getElementById('toast');
const nodesList = document.getElementById('nodesList');

/* ——— State ——— */
let isTyping = false;

/* ——— Helpers ——— */
function scrollToBottom() {
  messagesEl.scrollTop = messagesEl.scrollHeight;
  // also ensure chatMain scrolls
  chatMain.scrollTop = chatMain.scrollHeight;
}
function showToast(msg, duration = 2200) {
  toastEl.textContent = msg;
  toastEl.classList.remove('hidden');
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => toastEl.classList.add('hidden'), duration);
}
function escapeHtml(s) {
  return s.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

/* ——— Message Builders ——— */
function addUserMessage(text) {
  const row = document.createElement('div');
  row.className = 'message-row user-row';
  row.innerHTML = `<div class="bubble user-bubble">${escapeHtml(text)}</div>`;
  messagesEl.appendChild(row);
  scrollToBottom();
}

function addTypingIndicator() {
  const row = document.createElement('div');
  row.className = 'message-row assistant-row';
  row.id = 'typingIndicator';
  row.innerHTML = `
    <div class="avatar" aria-hidden="true">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="7" width="18" height="10" rx="2"></rect>
        <path d="M8 7V5a4 4 0 0 1 8 0v2"></path>
        <circle cx="9" cy="12" r="1" fill="white" stroke="none"></circle>
        <circle cx="15" cy="12" r="1" fill="white" stroke="none"></circle>
      </svg>
    </div>
    <div class="typing-dots"><span></span><span></span><span></span></div>
  `;
  messagesEl.appendChild(row);
  scrollToBottom();
  return row;
}

function removeTyping() {
  const el = document.getElementById('typingIndicator');
  if (el) el.remove();
}

/* ——— Mock MCP Tool Logic ——— */
function detectTool(query) {
  const q = query.toLowerCase();
  const has = (...keys) => keys.some(k => q.includes(k));
  if (has('postgres', 'sql', 'user', 'registration', 'table', 'select', 'database', 'db.', 'users')) return 'postgres';
  if (has('aws', 's3', 'bucket', 'ec2', 'cloudwatch', 'aws-us-east')) return 'aws';
  if (has('file', 'local', 'fs', '/data', 'directory', 'list files', 'read file')) return 'fs';
  return 'general';
}

function buildPostgresResponse(query) {
  const isUsers = query.toLowerCase().includes('user');
  const count = 5;
  const rows = isUsers ? [
    [101, 'k_morgan', '2023-10-21'],
    [102, 'j_doe', '2023-10-21'],
    [103, 'm_smith', '2023-10-22'],
    [104, 'a_wilson', '2023-10-22'],
    [105, 's_chen', '2023-10-23'],
  ] : [
    [201, 'orders', '1,248 rows'],
    [202, 'products', '342 rows'],
    [203, 'users', '5 rows'],
    [204, 'sessions', '892 rows'],
    [205, 'logs', '12k rows'],
  ];
  const tableRows = rows.map(r => `<tr><td>${r[0]}</td><td>${r[1]}</td><td>${r[2]}</td></tr>`).join('');
  const title = isUsers ? 'Retrieved <strong>5</strong> records from <span class="mono">remote_db.users</span>.' : `Listed <strong>${rows.length}</strong> relations in <span class="mono">remote_db.public</span>.`;
  const subtitle = isUsers ? 'Parsing metadata...' : 'Schema introspection complete.';
  const queryEcho = escapeHtml(query).slice(0,120);
  return `
    <div class="assistant-card">
      <div class="card-header">
        <span class="card-source">POSTGRES-MCP</span>
        <span class="card-executing">Executing SELECT...</span>
      </div>
      <div class="card-body">
        <p class="card-line">${title}</p>
        <p class="card-line muted">${subtitle}</p>
        <div class="table-wrap">
          <table class="mono-table">
            <thead><tr><th>ID</th><th>${isUsers ? 'USERNAME' : 'RELATION'}</th><th>${isUsers ? 'REGISTERED' : 'DETAILS'}</th></tr></thead>
            <tbody>${tableRows}</tbody>
          </table>
        </div>
        <div style="margin-top:8px;display:flex;gap:6px;flex-wrap:wrap">
          <span class="badge">TLS 1.3 ✓</span>
          <span class="badge" style="background:#f0fdf4;color:#15803d;border-color:#bbf7d0">query: ${queryEcho}...</span>
        </div>
      </div>
    </div>
  `;
}

function buildAwsResponse(query) {
  return `
    <div class="assistant-card">
      <div class="card-header">
        <span class="card-source" style="color:#ff9900">AWS-US-EAST</span>
        <span class="card-executing">Listing S3 buckets...</span>
      </div>
      <div class="card-body">
        <p class="card-line">Found <strong>3</strong> buckets in <span class="mono">us-east-1</span>.</p>
        <p class="card-line muted">via MCP aws-connector • latency 184ms</p>
        <div class="tool-result">
          <ul class="file-list">
            <li><span class="f-icon">⬢</span> <strong>mcp-app-data</strong> <span style="margin-left:auto;color:#6b7280">~2.4 GB • 342 objects</span></li>
            <li><span class="f-icon">⬢</span> <strong>user-avatars-prod</strong> <span style="margin-left:auto;color:#6b7280">~18.7 GB • 12.4k objects</span></li>
            <li><span class="f-icon">⬢</span> <strong>logs-archive-2023</strong> <span style="margin-left:auto;color:#6b7280">~412 GB • 89k objects</span></li>
          </ul>
        </div>
        <pre style="margin-top:8px;background:#f8fafc;border:1px solid #e5e7eb;border-radius:8px;padding:8px 10px;font-size:11px;color:#4b5563;overflow:auto">aws s3 ls --region us-east-1
2023-10-20 09:12:04 mcp-app-data
2023-08-14 11:02:31 user-avatars-prod
2022-12-01 03:44:10 logs-archive-2023</pre>
      </div>
    </div>
  `;
}

function buildFsResponse(query) {
  return `
    <div class="assistant-card">
      <div class="card-header">
        <span class="card-source" style="color:#6b7280">LOCAL-FS</span>
        <span class="card-executing">Reading directory...</span>
      </div>
      <div class="card-body">
        <p class="card-line">Listed <strong>5</strong> entries in <span class="mono">/data</span>.</p>
        <p class="card-line muted">Permissions: read-only mount</p>
        <div class="tool-result">
          <ul class="file-list">
            <li><span class="f-icon">📄</span> config.json <span style="margin-left:auto;color:#9ca3af">1.2 KB</span></li>
            <li><span class="f-icon">📁</span> datasets/ <span style="margin-left:auto;color:#9ca3af">—</span></li>
            <li><span class="f-icon">📄</span> users.csv <span style="margin-left:auto;color:#9ca3af">4.8 KB</span></li>
            <li><span class="f-icon">📄</span> README.md <span style="margin-left:auto;color:#9ca3af">2.1 KB</span></li>
            <li><span class="f-icon">📁</span> exports/ <span style="margin-left:auto;color:#9ca3af">—</span></li>
          </ul>
        </div>
      </div>
    </div>
  `;
}

function buildGeneralResponse(query) {
  return `
    <div class="assistant-card">
      <div class="card-header">
        <span class="card-source">MCP ROUTER</span>
        <span class="card-executing">Thinking...</span>
      </div>
      <div class="card-body">
        <p class="card-line">I can route your request to an MCP tool. Try one of these:</p>
        <div class="tool-result" style="background:#eff6ff;border-color:#dbeafe">
          <p style="margin:0 0 6px;font-size:12px;font-weight:600;color:#1e40af">Suggested prompts:</p>
          <ul style="margin:0;padding-left:16px;font-size:12px;line-height:1.7;color:#374151">
            <li><code>Query the remote PostgreSQL database for the last 5 user registrations</code></li>
            <li><code>List S3 buckets in aws-us-east</code></li>
            <li><code>List files in /data via local-fs</code></li>
          </ul>
        </div>
        <p class="card-line muted" style="margin-top:8px">Received: <span class="mono">${escapeHtml(query.slice(0,80))}</span></p>
      </div>
    </div>
  `;
}

function addAssistantMessage(html) {
  const row = document.createElement('div');
  row.className = 'message-row assistant-row';
  row.innerHTML = `
    <div class="avatar" aria-hidden="true">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="7" width="18" height="10" rx="2"></rect>
        <path d="M8 7V5a4 4 0 0 1 8 0v2"></path>
        <circle cx="9" cy="12" r="1" fill="white" stroke="none"></circle>
        <circle cx="15" cy="12" r="1" fill="white" stroke="none"></circle>
        <path d="M7 11H5M19 11h-2" opacity="0.8"></path>
      </svg>
    </div>
    ${html}
  `;
  messagesEl.appendChild(row);
  scrollToBottom();
}

/* ——— Main Handler ——— */
async function handleSend(raw) {
  const text = raw.trim();
  if (!text || isTyping) return;
  isTyping = true;
  sendBtn.disabled = true;
  inputEl.value = '';

  addUserMessage(text);
  const typingEl = addTypingIndicator();

  // Simulate MCP tool latency based on tool
  const tool = detectTool(text);
  const delays = { postgres: 1100, aws: 950, fs: 800, general: 650 };
  const delay = delays[tool] ?? 700;

  await new Promise(r => setTimeout(r, delay));

  removeTyping();

  let html;
  if (tool === 'postgres') html = buildPostgresResponse(text);
  else if (tool === 'aws') html = buildAwsResponse(text);
  else if (tool === 'fs') html = buildFsResponse(text);
  else html = buildGeneralResponse(text);

  addAssistantMessage(html);
  isTyping = false;
  sendBtn.disabled = false;
  inputEl.focus();
}

/* ——— Events ——— */
sendBtn.addEventListener('click', () => handleSend(inputEl.value));
inputEl.addEventListener('keydown', e => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend(inputEl.value);
  }
});
inputEl.addEventListener('input', () => {
  // auto-resize hint? no
});

// Attach / Tools modal
function openTools() {
  toolsModal.classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}
function closeTools() {
  toolsModal.classList.add('hidden');
  document.body.style.overflow = '';
}
attachBtn.addEventListener('click', openTools);
closeModalBtn.addEventListener('click', closeTools);
toolsBackdrop.addEventListener('click', closeTools);
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !toolsModal.classList.contains('hidden')) closeTools();
});

document.querySelectorAll('.tool-option').forEach(btn => {
  btn.addEventListener('click', () => {
    const tool = btn.dataset.tool;
    closeTools();
    if (tool === 'postgres') inputEl.value = 'Query the remote PostgreSQL database for the last 5 user registrations and format as a table.';
    if (tool === 'aws') inputEl.value = 'List S3 buckets in aws-us-east';
    if (tool === 'fs') inputEl.value = 'List files in /data';
    inputEl.focus();
    showToast(`Selected ${btn.querySelector('.tool-name').textContent} tool`);
  });
});

// Nodes interaction
nodesList.addEventListener('click', e => {
  const pill = e.target.closest('.node-pill');
  if (!pill) return;
  pill.classList.toggle('active');
  const name = pill.dataset.node;
  const active = pill.classList.contains('active');
  showToast(`${name} ${active ? 'connected' : 'disconnected'}`);
});

// Settings
document.querySelector('.settings-btn').addEventListener('click', () => {
  showToast('Settings — MCP endpoint: wss://mcp.local:8443 (TLS 1.3)');
});

// Init: focus and scroll
scrollToBottom();
inputEl.focus();

// Demo: if empty hint, clicking hint fills input
document.getElementById('composerHint').addEventListener('click', () => {
  inputEl.value = 'Show aws s3 buckets';
  inputEl.focus();
});
