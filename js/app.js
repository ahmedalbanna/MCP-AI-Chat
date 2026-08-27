const messagesEl = document.getElementById('messages');
const inputEl = document.getElementById('promptInput');
const sendBtn = document.getElementById('sendBtn');
const attachBtn = document.getElementById('attachBtn');
const mediaInput = document.getElementById('mediaInput');
const attachPreview = document.getElementById('attachPreview');
const slashMenu = document.getElementById('slashMenu');
const charCount = document.getElementById('charCount');
const composerInner = document.getElementById('composerInner');
const chatMain = document.getElementById('chatMain');
const toolsModal = document.getElementById('toolsModal');
const toolsBackdrop = document.getElementById('toolsBackdrop');
const closeModalBtn = document.getElementById('closeModal');
const toastEl = document.getElementById('toast');
const nodesList = document.getElementById('nodesList');
const tunnelText = document.getElementById('tunnelText');
const activeCountEl = document.getElementById('activeCount');
// settings
const settingsView = document.getElementById('settingsView');
const settingsBtn = document.getElementById('settingsBtn');
const serversBtn = document.getElementById('serversBtn');
const closeSettings = document.getElementById('closeSettings');
const serversList = document.getElementById('serversList');
const serverSearch = document.getElementById('serverSearch');
const addServerBtn = document.getElementById('addServerBtn');
const addNodeMini = document.getElementById('addNodeMini');
const serverModal = document.getElementById('serverModal');
const serverModalBackdrop = document.getElementById('serverModalBackdrop');
const closeServerModal = document.getElementById('closeServerModal');
const cancelServer = document.getElementById('cancelServer');
const serverForm = document.getElementById('serverForm');
const srvName = document.getElementById('srvName');
const srvTransport = document.getElementById('srvTransport');
const srvEndpoint = document.getElementById('srvEndpoint');
const srvTools = document.getElementById('srvTools');
const serverModalTitle = document.getElementById('serverModalTitle');

let isTyping = false;
let pendingFiles = []; // {id, file, url, type}
let slashIndex = 0;
let filteredCommands = [];
let editingId = null;

const DEFAULT_SERVERS = [
  { id:'srv1', name:'aws-us-east', transport:'sse', endpoint:'wss://aws.mcp.local/sse', tools:'s3_list, ec2_status', status:'connected', color:'aws' },
  { id:'srv2', name:'postgres-mcp', transport:'stdio', endpoint:'npx @modelcontextprotocol/server-postgres', tools:'query, list_tables', status:'connected', color:'postgres' },
  { id:'srv3', name:'local-fs', transport:'stdio', endpoint:'npx @modelcontextprotocol/server-filesystem /data', tools:'read, write, list', status:'disconnected', color:'fs' },
];
let servers = JSON.parse(localStorage.getItem('mcp_servers') || 'null') || DEFAULT_SERVERS;
function saveServers(){ localStorage.setItem('mcp_servers', JSON.stringify(servers)); }

const SLASH_COMMANDS = [
  { key:'/sql', desc:'Run SQL via postgres-mcp', hint:'SELECT * FROM users LIMIT 5', insert:'/sql SELECT * FROM users LIMIT 5' },
  { key:'/s3', desc:'List S3 buckets', hint:'aws-us-east', insert:'/s3 list buckets' },
  { key:'/fs', desc:'List local files', hint:'/data', insert:'/fs list /data' },
  { key:'/clear', desc:'Clear chat history', hint:'', insert:'/clear' },
  { key:'/help', desc:'Show available tools', hint:'', insert:'/help' },
  { key:'/connect', desc:'Connect a server', hint:'<name>', insert:'/connect ' },
  { key:'/disconnect', desc:'Disconnect a server', hint:'<name>', insert:'/disconnect ' },
];

function refreshLucide(){ if(window.lucide) lucide.createIcons(); }

function scrollToBottom(){ messagesEl.scrollTop = messagesEl.scrollHeight; }
function showToast(msg, duration=2200){
  toastEl.textContent = msg;
  toastEl.classList.remove('hidden');
  clearTimeout(showToast._t);
  showToast._t = setTimeout(()=>toastEl.classList.add('hidden'), duration);
}
function escapeHtml(s){ return s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }

/* ——— Nodes rendering ——— */
function renderNodes(){
  nodesList.innerHTML = servers.map(s=>{
    const dotClass = s.status==='connected'? (s.color==='aws'?'dot-orange': s.color==='postgres'?'dot-green':'dot-blue') : 'dot-gray';
    const cls = s.status==='connected' ? 'connected' : s.status==='connecting' ? 'connecting' : 'disconnected';
    return `<li class="node-pill ${cls}" data-id="${s.id}" title="${escapeHtml(s.endpoint)}"><span class="dot ${dotClass}"></span> ${escapeHtml(s.name)}</li>`;
  }).join('');
  const connected = servers.filter(s=>s.status==='connected').length;
  if(activeCountEl) activeCountEl.textContent = connected;
  if(tunnelText){
    if(connected===0) tunnelText.textContent = 'No MCP nodes connected • check settings';
    else tunnelText.textContent = `Secure MCP tunnel established via TLS 1.3 • ${connected} node${connected>1?'s':''} connected`;
  }
  refreshLucide();
}
function renderServers(){
  const q = (serverSearch.value||'').toLowerCase();
  const filtered = servers.filter(s=> !q || s.name.toLowerCase().includes(q) || s.endpoint.toLowerCase().includes(q));
  serversList.innerHTML = filtered.map(s=>{
    const iconCls = s.color==='postgres'?'postgres': s.color==='aws'?'aws': s.color==='fs'?'fs':'custom';
    const statusCls = s.status;
    const iconName = s.color==='postgres'?'database': s.color==='aws'?'cloud': s.color==='fs'?'folder-open':'server';
    return `<div class="server-card" data-id="${s.id}">
      <div class="server-card-head">
        <div class="server-icon ${iconCls}"><i data-lucide="${iconName}" style="width:18px;height:18px"></i></div>
        <div class="server-meta"><h4>${escapeHtml(s.name)}</h4><p>${escapeHtml(s.transport)} • ${escapeHtml(s.endpoint)}</p></div>
        <label class="switch" title="Connect / Disconnect"><input type="checkbox" ${s.status==='connected'?'checked':''} data-toggle="${s.id}"><span class="slider"></span></label>
      </div>
      <div class="server-foot">
        <span class="status-dot ${statusCls}">${s.status==='connected'?'● Connected': s.status==='connecting'?'◐ Connecting':'○ Disconnected'}</span>
        <span style="flex:1"></span>
        <button class="mini-btn" data-edit="${s.id}"><i data-lucide="pencil" style="width:12px;height:12px"></i> Edit</button>
        <button class="mini-btn danger" data-delete="${s.id}"><i data-lucide="trash-2" style="width:12px;height:12px"></i></button>
      </div>
    </div>`;
  }).join('') || `<p style="text-align:center;color:#9ca3af;font-size:13px;padding:16px">No servers found</p>`;
  refreshLucide();
}

/* ——— Media upload ——— */
function addFiles(files){
  for(const file of files){
    if(pendingFiles.length>=6){ showToast('Max 6 files'); break; }
    if(file.size> 12*1024*1024){ showToast(`${file.name} too large (12MB max)`); continue; }
    const id = Math.random().toString(36).slice(2,8);
    const url = file.type.startsWith('image/') || file.type.startsWith('video/') ? URL.createObjectURL(file) : null;
    pendingFiles.push({id,file,url,type:file.type});
  }
  renderAttachPreview();
  updateComposerState();
}
function renderAttachPreview(){
  if(pendingFiles.length===0){ attachPreview.classList.add('hidden'); attachPreview.innerHTML=''; return; }
  attachPreview.classList.remove('hidden');
  attachPreview.innerHTML = pendingFiles.map(p=>{
    let inner='';
    if(p.type.startsWith('image/')) inner=`<img src="${p.url}" alt="${escapeHtml(p.file.name)}">`;
    else if(p.type.startsWith('video/')) inner=`<video src="${p.url}" muted></video>`;
    else inner=`<div class="fname"><i data-lucide="file" style="width:18px;height:18px"></i><span>${escapeHtml(p.file.name.slice(0,14))}</span></div>`;
    return `<div class="preview-chip">${inner}<button class="remove" data-remove="${p.id}" aria-label="Remove"><i data-lucide="x"></i></button></div>`;
  }).join('');
  refreshLucide();
}
attachPreview.addEventListener('click', e=>{
  const btn = e.target.closest('[data-remove]');
  if(!btn) return;
  const id=btn.dataset.remove;
  const idx=pendingFiles.findIndex(p=>p.id===id);
  if(idx>=0){
    if(pendingFiles[idx].url) URL.revokeObjectURL(pendingFiles[idx].url);
    pendingFiles.splice(idx,1);
    renderAttachPreview();
    updateComposerState();
  }
});
mediaInput.addEventListener('change', e=>{
  if(e.target.files.length) addFiles([...e.target.files]);
  e.target.value='';
});

/* ——— Composer improvements ——— */
function autoResize(){
  inputEl.style.height='auto';
  inputEl.style.height = Math.min(inputEl.scrollHeight, 110) + 'px';
}
function updateComposerState(){
  const hasText = inputEl.value.trim().length>0;
  const hasMedia = pendingFiles.length>0;
  sendBtn.disabled = !hasText && !hasMedia || isTyping;
  charCount.textContent = inputEl.value.length ? `${inputEl.value.length}/2000` : '';
  if(inputEl.value.length>1800) charCount.style.color='#b91c1c'; else charCount.style.color='#9ca3af';
}
inputEl.addEventListener('input', ()=>{
  autoResize();
  updateComposerState();
  handleSlash(inputEl.value);
});
inputEl.addEventListener('keydown', e=>{
  if(!slashMenu.classList.contains('hidden')){
    if(e.key==='ArrowDown'){ e.preventDefault(); slashIndex=(slashIndex+1)%filteredCommands.length; renderSlash(); }
    else if(e.key==='ArrowUp'){ e.preventDefault(); slashIndex=(slashIndex-1+filteredCommands.length)%filteredCommands.length; renderSlash(); }
    else if(e.key==='Enter' || e.key==='Tab'){ e.preventDefault(); selectSlash(filteredCommands[slashIndex]); return; }
    else if(e.key==='Escape'){ hideSlash(); }
  }
  if(e.key==='Enter' && !e.shiftKey){
    if(!slashMenu.classList.contains('hidden') && filteredCommands.length) { e.preventDefault(); selectSlash(filteredCommands[slashIndex]); return; }
    e.preventDefault();
    handleSend();
  }
});

/* ——— Slash commands ——— */
function handleSlash(val){
  const cursorPos = inputEl.selectionStart;
  const textBefore = val.slice(0,cursorPos);
  // show if starts with / and no space before? or contains / at word start
  const match = textBefore.match(/(^|\s)(\/[a-z]*)$/);
  if(!match){ hideSlash(); return; }
  const query = match[2].toLowerCase();
  filteredCommands = SLASH_COMMANDS.filter(c=> c.key.startsWith(query));
  if(filteredCommands.length===0){ hideSlash(); return; }
  slashIndex=0;
  renderSlash();
}
function renderSlash(){
  slashMenu.innerHTML = filteredCommands.map((c,i)=>`
    <div class="slash-item ${i===slashIndex?'active':''}" data-index="${i}" role="option">
      <span class="slash-key">${escapeHtml(c.key)}</span>
      <span class="slash-desc">${escapeHtml(c.desc)}</span>
      <span class="slash-hint">${escapeHtml(c.hint)}</span>
    </div>
  `).join('');
  slashMenu.classList.remove('hidden');
}
function hideSlash(){ slashMenu.classList.add('hidden'); filteredCommands=[]; }
function selectSlash(cmd){
  if(!cmd) return;
  const val = inputEl.value;
  const pos = inputEl.selectionStart;
  const before = val.slice(0,pos);
  const after = val.slice(pos);
  const newBefore = before.replace(/\/[a-z]*$/, cmd.insert);
  inputEl.value = newBefore + after;
  hideSlash();
  autoResize();
  updateComposerState();
  inputEl.focus();
  // special instant actions
  if(cmd.key==='/clear') { clearChat(); hideSlash(); }
  if(cmd.key==='/help') { handleSend('/help'); hideSlash(); }
}
slashMenu.addEventListener('click', e=>{
  const item = e.target.closest('.slash-item');
  if(!item) return;
  selectSlash(filteredCommands[Number(item.dataset.index)]);
});
document.addEventListener('click', e=>{
  if(!slashMenu.contains(e.target) && e.target!==inputEl) hideSlash();
});

/* ——— Drag & Drop ——— */
['dragenter','dragover'].forEach(ev=>{
  composerInner.addEventListener(ev, e=>{ e.preventDefault(); composerInner.classList.add('drag-over'); });
});
['dragleave','drop'].forEach(ev=>{
  composerInner.addEventListener(ev, e=>{ e.preventDefault(); if(ev!=='drop') composerInner.classList.remove('drag-over'); });
});
composerInner.addEventListener('drop', e=>{
  composerInner.classList.remove('drag-over');
  if(e.dataTransfer.files.length) addFiles([...e.dataTransfer.files]);
});
messagesEl.addEventListener('dragover', e=> e.preventDefault());
messagesEl.addEventListener('drop', e=>{
  e.preventDefault();
  if(e.dataTransfer.files.length) addFiles([...e.dataTransfer.files]);
  showToast('Files added — review before send');
});

/* ——— Messages ——— */
function addUserMessage(text, files){
  const row = document.createElement('div');
  row.className='message-row user-row';
  let mediaHtml='';
  if(files.length){
    mediaHtml = `<div class="user-media">` + files.map(p=>{
      if(p.type.startsWith('image/')) return `<img src="${p.url}" alt="${escapeHtml(p.file.name)}">`;
      if(p.type.startsWith('video/')) return `<video src="${p.url}" controls muted style="width:180px;height:120px"></video>`;
      return `<span class="file-chip"><i data-lucide="file" style="width:14px;height:14px"></i> ${escapeHtml(p.file.name)} (${(p.file.size/1024).toFixed(1)} KB)</span>`;
    }).join('') + `</div>`;
  }
  const bubble = text ? `<div class="bubble user-bubble">${escapeHtml(text)}</div>` : '';
  row.innerHTML = mediaHtml + bubble;
  messagesEl.appendChild(row);
  refreshLucide();
  scrollToBottom();
}
function addTypingIndicator(){
  const row=document.createElement('div');
  row.className='message-row assistant-row';
  row.id='typingIndicator';
  row.innerHTML=`<div class="avatar"><i data-lucide="bot" style="width:16px;height:16px;color:white"></i></div><div class="typing-dots"><span></span><span></span><span></span></div>`;
  messagesEl.appendChild(row);
  refreshLucide();
  scrollToBottom();
}
function removeTyping(){ document.getElementById('typingIndicator')?.remove(); }

function detectTool(query){
  const q=query.toLowerCase();
  const has=(...keys)=>keys.some(k=>q.includes(k));
  if(has('postgres','sql','user','registration','table','select','database','db.','users','/sql')) return 'postgres';
  if(has('aws','s3','bucket','ec2','cloudwatch','aws-us-east','/s3')) return 'aws';
  if(has('file','local','fs','/data','directory','list files','read file','/fs')) return 'fs';
  return 'general';
}
function buildPostgresResponse(query){
  const isUsers = query.toLowerCase().includes('user');
  const rows=isUsers?[[101,'k_morgan','2023-10-21'],[102,'j_doe','2023-10-21'],[103,'m_smith','2023-10-22'],[104,'a_wilson','2023-10-22'],[105,'s_chen','2023-10-23']]:[[201,'orders','1,248 rows'],[202,'products','342 rows'],[203,'users','5 rows'],[204,'sessions','892 rows'],[205,'logs','12k rows']];
  const tableRows=rows.map(r=>`<tr><td>${r[0]}</td><td>${r[1]}</td><td>${r[2]}</td></tr>`).join('');
  const title=isUsers? 'Retrieved <strong>5</strong> records from <span class="mono">remote_db.users</span>.' : `Listed <strong>${rows.length}</strong> relations in <span class="mono">remote_db.public</span>.`;
  const subtitle=isUsers?'Parsing metadata...':'Schema introspection complete.';
  return `<div class="assistant-card"><div class="card-header"><span class="card-source">POSTGRES-MCP</span><span class="card-executing">Executing SELECT...</span></div><div class="card-body"><p class="card-line">${title}</p><p class="card-line muted">${subtitle}</p><div class="table-wrap"><table class="mono-table"><thead><tr><th>ID</th><th>${isUsers?'USERNAME':'RELATION'}</th><th>${isUsers?'REGISTERED':'DETAILS'}</th></tr></thead><tbody>${tableRows}</tbody></table></div></div></div>`;
}
function buildAwsResponse(){
  return `<div class="assistant-card"><div class="card-header"><span class="card-source" style="color:#ff9900">AWS-US-EAST</span><span class="card-executing">Listing S3 buckets...</span></div><div class="card-body"><p class="card-line">Found <strong>3</strong> buckets in <span class="mono">us-east-1</span>.</p><p class="card-line muted">via MCP aws-connector • latency 184ms</p><div class="tool-result"><ul class="file-list"><li>⬢ <strong>mcp-app-data</strong> <span style="margin-left:auto;color:#6b7280">~2.4 GB • 342 objects</span></li><li>⬢ <strong>user-avatars-prod</strong> <span style="margin-left:auto;color:#6b7280">~18.7 GB • 12.4k objects</span></li><li>⬢ <strong>logs-archive-2023</strong> <span style="margin-left:auto;color:#6b7280">~412 GB • 89k objects</span></li></ul></div></div></div>`;
}
function buildFsResponse(){
  return `<div class="assistant-card"><div class="card-header"><span class="card-source" style="color:#6b7280">LOCAL-FS</span><span class="card-executing">Reading directory...</span></div><div class="card-body"><p class="card-line">Listed <strong>5</strong> entries in <span class="mono">/data</span>.</p><div class="tool-result"><ul class="file-list"><li>📄 config.json <span style="margin-left:auto;color:#9ca3af">1.2 KB</span></li><li>📁 datasets/ <span style="margin-left:auto">—</span></li><li>📄 users.csv <span style="margin-left:auto">4.8 KB</span></li><li>📄 README.md <span style="margin-left:auto">2.1 KB</span></li><li>📁 exports/ <span style="margin-left:auto">—</span></li></ul></div></div></div>`;
}
function buildGeneralResponse(query){
  return `<div class="assistant-card"><div class="card-header"><span class="card-source">MCP ROUTER</span><span class="card-executing">Thinking...</span></div><div class="card-body"><p class="card-line">I can route your request to an MCP tool. Try <code>/help</code> or use <code>/sql</code>, <code>/s3</code>, <code>/fs</code>.</p><div class="tool-result" style="background:#eff6ff;border-color:#dbeafe"><ul style="margin:0;padding-left:16px;font-size:12px;line-height:1.7;color:#374151"><li><code>/sql SELECT * FROM users LIMIT 5</code></li><li><code>/s3 list buckets</code></li><li><code>/fs list /data</code></li></ul></div><p class="card-line muted" style="margin-top:8px">Received: <span class="mono">${escapeHtml(query.slice(0,80))}</span></p></div></div>`;
}
function buildHelpResponse(){
  return `<div class="assistant-card"><div class="card-header"><span class="card-source">MCP HELP</span><span class="card-executing">Available commands</span></div><div class="card-body"><div class="tool-result"><ul style="margin:0;padding:0;list-style:none;display:grid;gap:6px;font-size:12px">${SLASH_COMMANDS.map(c=>`<li><span class="mono" style="background:#111827;color:#fff;padding:2px 6px;border-radius:4px">${c.key}</span> — ${escapeHtml(c.desc)} <span style="color:#9ca3af">${escapeHtml(c.hint)}</span></li>`).join('')}</ul></div><p class="card-line muted" style="margin-top:8px">Tip: upload images/PDFs via <i data-lucide="plus"></i> or drag & drop before sending.</p></div></div>`;
}
function addAssistantMessage(html){
  const row=document.createElement('div');
  row.className='message-row assistant-row';
  row.innerHTML=`<div class="avatar"><i data-lucide="bot" style="width:16px;height:16px;color:white"></i></div>${html}`;
  messagesEl.appendChild(row);
  refreshLucide();
  scrollToBottom();
}
function clearChat(){
  messagesEl.innerHTML='';
  showToast('Chat cleared');
}
async function handleSend(raw){
  let text = (raw ?? inputEl.value).trim();
  const files = [...pendingFiles];
  if(!text && files.length===0) return;
  if(isTyping) return;
  if(text==='/clear'){ clearChat(); inputEl.value=''; pendingFiles=[]; renderAttachPreview(); updateComposerState(); autoResize(); return; }
  if(text==='/help'){ addUserMessage(text,[]); inputEl.value=''; hideSlash(); addAssistantMessage(buildHelpResponse()); updateComposerState(); autoResize(); return; }
  // handle /connect /disconnect
  if(text.startsWith('/connect ')){ const name=text.slice(9).trim(); const srv=servers.find(s=>s.name===name); if(srv){ toggleServer(srv.id,true); } else showToast(`Server "${name}" not found`); inputEl.value=''; hideSlash(); updateComposerState(); return; }
  if(text.startsWith('/disconnect ')){ const name=text.slice(12).trim(); const srv=servers.find(s=>s.name===name); if(srv){ toggleServer(srv.id,false);} else showToast(`Server "${name}" not found`); inputEl.value=''; hideSlash(); updateComposerState(); return; }
  // strip slash prefix for tool detection but keep visible
  let displayText = text;
  // connection guard: if needed tool is disconnected, warn
  const tool = detectTool(text);
  const toolMap = {postgres:'postgres-mcp', aws:'aws-us-east', fs:'local-fs'};
  const needed = toolMap[tool];
  if(needed){
    const srv=servers.find(s=>s.name===needed);
    if(srv && srv.status!=='connected'){
      showToast(`${needed} is disconnected — connecting...`);
      await toggleServer(srv.id,true);
    }
  }
  isTyping=true; updateComposerState();
  const cleanFiles = files.map(p=>({file:p.file, url:p.url, type:p.type}));
  addUserMessage(displayText, cleanFiles);
  // clear composer
  pendingFiles.forEach(p=>{/* keep urls for history, not revoke yet */});
  pendingFiles=[];
  renderAttachPreview();
  inputEl.value='';
  hideSlash();
  autoResize();
  updateComposerState();
  addTypingIndicator();
  const delays={postgres:1100, aws:950, fs:800, general:650};
  await new Promise(r=>setTimeout(r, delays[tool]??700));
  removeTyping();
  // if files attached, show analysis
  let html;
  if(cleanFiles.length>0 && tool==='general'){
    html=`<div class="assistant-card"><div class="card-header"><span class="card-source">MCP VISION</span><span class="card-executing">Analyzing ${cleanFiles.length} file(s)...</span></div><div class="card-body"><p class="card-line">Received <strong>${cleanFiles.length}</strong> file(s): ${cleanFiles.map(f=>escapeHtml(f.file.name)).join(', ')}</p><p class="card-line muted">Extracted text / preview ready for tool routing.</p><div class="tool-result">${cleanFiles.map(f=>`<div style="display:flex;gap:8px;align-items:center;padding:4px 0;border-bottom:1px solid #f3f4f6"><i data-lucide="file" style="width:14px;height:14px"></i> ${escapeHtml(f.file.name)} • ${(f.file.size/1024).toFixed(1)} KB</div>`).join('')}</div></div></div>`;
    if(text) html+= buildGeneralResponse(text);
  } else {
    if(tool==='postgres') html=buildPostgresResponse(text);
    else if(tool==='aws') html=buildAwsResponse(text);
    else if(tool==='fs') html=buildFsResponse(text);
    else if(tool==='general' && text.startsWith('/')) html=buildGeneralResponse(text);
    else html=buildGeneralResponse(text);
  }
  addAssistantMessage(html);
  isTyping=false; updateComposerState(); inputEl.focus();
}

/* ——— Connect / Disconnect ——— */
async function toggleServer(id, force){
  const srv=servers.find(s=>s.id===id);
  if(!srv) return;
  const shouldConnect = typeof force==='boolean' ? force : srv.status!=='connected';
  if(shouldConnect){
    if(srv.status==='connected') return;
    srv.status='connecting'; renderNodes(); renderServers();
    showToast(`Connecting ${srv.name}…`);
    await new Promise(r=>setTimeout(r, 900));
    srv.status='connected'; saveServers(); renderNodes(); renderServers(); showToast(`${srv.name} connected ✓`);
  } else {
    if(srv.status==='disconnected') return;
    srv.status='disconnected'; saveServers(); renderNodes(); renderServers(); showToast(`${srv.name} disconnected`);
  }
}

/* ——— Events ——— */
sendBtn.addEventListener('click', ()=>handleSend());
inputEl.addEventListener('focus', updateComposerState);
attachBtn.addEventListener('click', ()=> mediaInput.click());
closeModalBtn?.addEventListener('click', ()=> toolsModal.classList.add('hidden'));
toolsBackdrop?.addEventListener('click', ()=> toolsModal.classList.add('hidden'));
document.addEventListener('keydown', e=>{ if(e.key==='Escape'){ toolsModal.classList.add('hidden'); settingsView.classList.add('hidden'); serverModal.classList.add('hidden'); hideSlash(); }});
document.querySelectorAll('.tool-option').forEach(btn=>{
  btn.addEventListener('click', ()=>{
    const tool=btn.dataset.tool;
    toolsModal.classList.add('hidden');
    if(tool==='postgres') inputEl.value='/sql SELECT * FROM users LIMIT 5';
    if(tool==='aws') inputEl.value='/s3 list buckets';
    if(tool==='fs') inputEl.value='/fs list /data';
    if(tool==='upload') { mediaInput.click(); return; }
    inputEl.focus(); autoResize(); updateComposerState(); showToast(`Ready: ${tool}`);
  });
});
nodesList.addEventListener('click', e=>{
  const pill=e.target.closest('.node-pill');
  if(!pill) return;
  const id=pill.dataset.id;
  toggleServer(id);
});
function openSettings(){ settingsView.classList.remove('hidden'); renderServers(); refreshLucide(); }
function closeSettingsView(){ settingsView.classList.add('hidden'); }
settingsBtn.addEventListener('click', openSettings);
serversBtn.addEventListener('click', openSettings);
closeSettings.addEventListener('click', closeSettingsView);
serverSearch.addEventListener('input', renderServers);
serversList.addEventListener('click', e=>{
  const del=e.target.closest('[data-delete]');
  if(del){ const id=del.dataset.delete; servers=servers.filter(s=>s.id!==id); saveServers(); renderNodes(); renderServers(); showToast('Server removed'); return; }
  const edit=e.target.closest('[data-edit]');
  if(edit){ const id=edit.dataset.edit; const srv=servers.find(s=>s.id===id); if(srv){ editingId=id; serverModalTitle.textContent='Edit MCP Server'; srvName.value=srv.name; srvTransport.value=srv.transport; srvEndpoint.value=srv.endpoint; srvTools.value=srv.tools; serverModal.classList.remove('hidden'); refreshLucide(); } return; }
});
serversList.addEventListener('change', e=>{
  const tog=e.target.closest('[data-toggle]');
  if(tog){ toggleServer(tog.dataset.toggle, tog.checked); }
});
function openServerModal(){
  editingId=null;
  serverModalTitle.textContent='Add MCP Server';
  srvName.value=''; srvEndpoint.value=''; srvTools.value=''; srvTransport.value='sse';
  serverModal.classList.remove('hidden');
  refreshLucide();
}
addServerBtn.addEventListener('click', openServerModal);
addNodeMini.addEventListener('click', openServerModal);
closeServerModal.addEventListener('click', ()=> serverModal.classList.add('hidden'));
cancelServer.addEventListener('click', ()=> serverModal.classList.add('hidden'));
serverModalBackdrop.addEventListener('click', ()=> serverModal.classList.add('hidden'));
serverForm.addEventListener('submit', e=>{
  e.preventDefault();
  const name=srvName.value.trim();
  const endpoint=srvEndpoint.value.trim();
  if(!name||!endpoint) return;
  if(editingId){
    const srv=servers.find(s=>s.id===editingId);
    srv.name=name; srv.transport=srvTransport.value; srv.endpoint=endpoint; srv.tools=srvTools.value;
    showToast(`${name} updated`);
  } else {
    const id='srv'+Date.now();
    const color = name.includes('postgres')?'postgres': name.includes('aws')?'aws': name.includes('fs')||name.includes('file')?'fs':'custom';
    servers.push({id,name,transport:srvTransport.value,endpoint,tools:srvTools.value,status:'disconnected',color});
    showToast(`${name} added — toggle to connect`);
  }
  saveServers(); renderNodes(); renderServers(); serverModal.classList.add('hidden');
});

// Init
renderNodes();
renderServers();
scrollToBottom();
updateComposerState();
refreshLucide();
inputEl.focus();
