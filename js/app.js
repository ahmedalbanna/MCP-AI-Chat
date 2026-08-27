/* MCP Terminal — Premium JS */
const $ = s => document.querySelector(s);
const messagesEl = $('#messages');
const inputEl = $('#promptInput');
const sendBtn = $('#sendBtn');
const attachBtn = $('#attachBtn');
const mediaInput = $('#mediaInput');
const attachPreview = $('#attachPreview');
const slashMenu = $('#slashMenu');
const charCount = $('#charCount');
const composerInner = $('#composerInner');
const topNodes = $('#nodesInline');
const sidebarList = $('#sidebarServerList');
const sidebarCount = $('#sidebarCount');
const activeCount2 = $('#activeCount2');
const tunnelBar = $('#tunnelBar');
const tunnelStatus = $('#tunnelStatus');
const toastEl = $('#toast');
const dropOverlay = $('#dropOverlay');
// drawer
const settingsView = $('#settingsView');
const settingsBackdrop = $('#settingsBackdrop');
const serverSearch = $('#serverSearch');
const serversList = $('#serversList');
const addServerBtn = $('#addServerBtn');
const addServerSidebar = $('#addServerSidebar');
const closeSettings = $('#closeSettings');
const serversBtn = $('#serversBtn');
const settingsBtn = $('#settingsBtn');
const settingsBtn2 = $('#settingsBtn2');
// modal
const serverModal = $('#serverModal');
const serverModalBackdrop = $('#serverModalBackdrop');
const closeServerModalBtn = $('#closeServerModal');
const cancelServer = $('#cancelServer');
const serverForm = $('#serverForm');
const srvName = $('#srvName');
const srvTransport = $('#srvTransport');
const srvEndpoint = $('#srvEndpoint');
const srvTools = $('#srvTools');
const serverModalTitle = $('#serverModalTitle');
// sheet
const toolsModal = $('#toolsModal');
const toolsBackdrop = $('#toolsBackdrop');
const closeModalBtn = $('#closeModal');
const quickToolsBtn = $('#quickToolsBtn');
// sidebar
const sidebar = $('#sidebar');
const menuBtn = $('#menuBtn');
const sidebarClose = $('#sidebarClose');
const sidebarSearch = $('#sidebarSearch');
const newChatBtn = $('#newChatBtn');

let pending = []; // {id,file,url}
let isTyping=false;
let editingId=null;
let slashIndex=0, filtered=[];

const DEFAULT = [
  {id:'srv1', name:'aws-us-east', transport:'sse', endpoint:'wss://aws.mcp.local/sse', tools:'s3_list, ec2_status', status:'connected', color:'aws'},
  {id:'srv2', name:'postgres-mcp', transport:'stdio', endpoint:'npx @modelcontextprotocol/server-postgres', tools:'query, list_tables', status:'connected', color:'pg'},
  {id:'srv3', name:'local-fs', transport:'stdio', endpoint:'npx @modelcontextprotocol/server-filesystem /data', tools:'read, write, list', status:'disconnected', color:'fs'},
];
let servers = JSON.parse(localStorage.getItem('mcp_servers')||'null') || DEFAULT;
const save=()=> localStorage.setItem('mcp_servers', JSON.stringify(servers));

const SLASH = [
  {key:'/sql', desc:'Run SQL via postgres-mcp', hint:'SELECT * FROM users', insert:'/sql SELECT * FROM users LIMIT 5'},
  {key:'/s3', desc:'List S3 buckets', hint:'aws-us-east', insert:'/s3 list buckets'},
  {key:'/fs', desc:'List local files', hint:'/data', insert:'/fs list /data'},
  {key:'/clear', desc:'Clear chat history', hint:'', insert:'/clear'},
  {key:'/help', desc:'Show available tools', hint:'', insert:'/help'},
  {key:'/connect', desc:'Connect a server', hint:'<name>', insert:'/connect '},
  {key:'/disconnect', desc:'Disconnect a server', hint:'<name>', insert:'/disconnect '},
];

const refreshIcons=()=> window.lucide && lucide.createIcons();
const toast=(m,d=2200)=>{ toastEl.textContent=m; toastEl.classList.remove('hidden'); clearTimeout(toast._t); toast._t=setTimeout(()=>toastEl.classList.add('hidden'),d); };
const esc=s=> s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const scrollBottom=()=>{ messagesEl.scrollTop=messagesEl.scrollHeight; };

/* Render servers */
function render(){
  const q=(serverSearch.value||'').toLowerCase();
  const sq=(sidebarSearch.value||'').toLowerCase();
  // top nodes
  topNodes.innerHTML = servers.map(s=>{
    const on=s.status==='connected';
    return `<button class="node-pill ${on?'connected':'disconnected'}" data-id="${s.id}"><span class="d" style="background:${on ? (s.color==='aws'?'#ff9900': s.color==='pg'?'#16a34a':'#2563eb') : '#cbd5e1'}"></span> ${esc(s.name)}</button>`;
  }).join('');
  // sidebar list (filtered)
  const sideFiltered = servers.filter(s=> !sq || s.name.toLowerCase().includes(sq));
  sidebarList.innerHTML = sideFiltered.map(s=>{
    const on=s.status==='connected', busy=s.status==='connecting';
    return `<li class="server-item ${s.status}" data-id="${s.id}" title="${esc(s.endpoint)}">
      <span class="server-ic ${s.color==='pg'?'pg':s.color==='aws'?'aws':s.color==='fs'?'fs':'custom'}"><i data-lucide="${s.color==='pg'?'database': s.color==='aws'?'cloud': s.color==='fs'?'folder-open':'server'}" style="width:16px;height:16px"></i></span>
      <span class="server-meta"><strong>${esc(s.name)}</strong><span>${esc(s.transport)} • ${esc(s.endpoint.slice(0,32))}</span></span>
      <span class="chip-dot ${on?'on': busy?'': 'off'}"></span>
    </li>`;
  }).join('') || `<li style="font-size:12px;color:#94a3b8;padding:8px">No matches</li>`;

  // drawer list
  const filteredDrawer = servers.filter(s=> !q || s.name.toLowerCase().includes(q) || s.endpoint.toLowerCase().includes(q));
  serversList.innerHTML = filteredDrawer.map(s=>{
    const ic = s.color==='pg'?'pg':s.color==='aws'?'aws':s.color==='fs'?'fs':'custom';
    const iname = s.color==='pg'?'database': s.color==='aws'?'cloud': s.color==='fs'?'folder-open':'server';
    const label = s.status==='connected'?'Connected': s.status==='connecting'?'Connecting':'Disconnected';
    const cls = s.status==='connected'?'on': s.status==='connecting'?'busy':'off';
    return `<div class="server-card" data-id="${s.id}">
      <div class="server-card-top">
        <span class="ic ${ic}"><i data-lucide="${iname}" style="width:16px;height:16px"></i></span>
        <span class="meta"><b>${esc(s.name)}</b><p>${esc(s.transport)} • ${esc(s.endpoint)}</p></span>
        <label class="switch"><input type="checkbox" ${s.status==='connected'?'checked':''} data-toggle="${s.id}"><span></span></label>
      </div>
      <div class="server-card-foot">
        <span class="status ${cls}">${label}</span>
        <span style="flex:1"></span>
        <button class="mini" data-edit="${s.id}"><i data-lucide="pencil" style="width:12px;height:12px"></i> Edit</button>
        <button class="mini danger" data-del="${s.id}"><i data-lucide="trash-2" style="width:12px;height:12px"></i></button>
      </div>
    </div>`;
  }).join('') || `<p style="text-align:center;color:#94a3b8;padding:12px">No servers</p>`;

  const connected = servers.filter(s=>s.status==='connected').length;
  sidebarCount.textContent = servers.length;
  if(activeCount2) activeCount2.textContent = connected;
  if(tunnelBar) tunnelBar.style.width = `${Math.round(connected/servers.length*100)}%`;
  if(tunnelStatus) tunnelStatus.innerHTML = connected? `Secure MCP tunnel • <strong>${connected}</strong> connected` : `No MCP nodes connected`;
  refreshIcons();
}

/* Media */
function addFiles(files){
  for(const f of files){
    if(pending.length>=6){ toast('Max 6 files'); break; }
    if(f.size>12*1024*1024){ toast(`${f.name} too large`); continue; }
    const id=Math.random().toString(36).slice(2,8);
    const url = f.type.startsWith('image/') || f.type.startsWith('video/') ? URL.createObjectURL(f) : null;
    pending.push({id,file:f,url});
  }
  drawPreview(); syncComposer();
}
function drawPreview(){
  if(!pending.length){ attachPreview.classList.add('hidden'); attachPreview.innerHTML=''; return; }
  attachPreview.classList.remove('hidden');
  attachPreview.innerHTML = pending.map(p=>{
    let inner='';
    if(p.file.type.startsWith('image/')) inner=`<img src="${p.url}" alt="">`;
    else if(p.file.type.startsWith('video/')) inner=`<video src="${p.url}" muted></video>`;
    else inner=`<div class="fn"><i data-lucide="file"></i><span>${esc(p.file.name.slice(0,18))}</span><small>${(p.file.size/1024).toFixed(1)} KB</small></div>`;
    return `<div class="preview">${inner}<button class="rm" data-rm="${p.id}"><i data-lucide="x"></i></button></div>`;
  }).join('');
  refreshIcons();
}
attachPreview.addEventListener('click', e=>{
  const b=e.target.closest('[data-rm]'); if(!b) return;
  const id=b.dataset.rm; const i=pending.findIndex(p=>p.id===id);
  if(i>=0){ if(pending[i].url) URL.revokeObjectURL(pending[i].url); pending.splice(i,1); drawPreview(); syncComposer(); }
});
mediaInput.addEventListener('change', e=>{ if(e.target.files.length) addFiles([...e.target.files]); e.target.value=''; });

/* Composer */
function autoResize(){
  inputEl.style.height='auto';
  inputEl.style.height=Math.min(inputEl.scrollHeight,120)+'px';
}
function syncComposer(){
  const hasText=inputEl.value.trim().length>0;
  const hasMedia=pending.length>0;
  sendBtn.disabled = (!hasText && !hasMedia) || isTyping;
  charCount.textContent = inputEl.value.length? `${inputEl.value.length}/2000` : '';
  charCount.style.color = inputEl.value.length>1800 ? '#ef4444' : '#94a3b8';
  // draft save
  localStorage.setItem('mcp_draft', inputEl.value);
}
inputEl.value = localStorage.getItem('mcp_draft')||'';
autoResize(); syncComposer();

inputEl.addEventListener('input', ()=>{
  autoResize(); syncComposer();
  const pos=inputEl.selectionStart;
  const before=inputEl.value.slice(0,pos);
  const m=before.match(/(^|\s)(\/[a-z]*)$/);
  if(!m){ hideSlash(); return; }
  const q=m[2].toLowerCase();
  filtered=SLASH.filter(c=>c.key.startsWith(q));
  if(!filtered.length){ hideSlash(); return; }
  slashIndex=0; renderSlash();
});
function renderSlash(){
  slashMenu.innerHTML = filtered.map((c,i)=>`
    <div class="s-item ${i===slashIndex?'active':''}" data-i="${i}"><span class="s-key">${esc(c.key)}</span><span class="s-desc">${esc(c.desc)}</span><span class="s-hint">${esc(c.hint)}</span></div>
  `).join('');
  slashMenu.classList.remove('hidden');
}
function hideSlash(){ slashMenu.classList.add('hidden'); filtered=[]; }
function pickSlash(cmd){
  if(!cmd) return;
  const pos=inputEl.selectionStart;
  const before=inputEl.value.slice(0,pos);
  const after=inputEl.value.slice(pos);
  inputEl.value = before.replace(/\/[a-z]*$/, cmd.insert) + after;
  hideSlash(); autoResize(); syncComposer(); inputEl.focus();
  if(cmd.key==='/clear') clearChat();
  if(cmd.key==='/help') doSend('/help');
}
slashMenu.addEventListener('click', e=>{
  const it=e.target.closest('.s-item'); if(!it) return;
  pickSlash(filtered[Number(it.dataset.i)]);
});
inputEl.addEventListener('keydown', e=>{
  if(!slashMenu.classList.contains('hidden')){
    if(e.key==='ArrowDown'){ e.preventDefault(); slashIndex=(slashIndex+1)%filtered.length; renderSlash(); }
    else if(e.key==='ArrowUp'){ e.preventDefault(); slashIndex=(slashIndex-1+filtered.length)%filtered.length; renderSlash(); }
    else if(e.key==='Enter' || e.key==='Tab'){ e.preventDefault(); pickSlash(filtered[slashIndex]); return; }
    else if(e.key==='Escape'){ hideSlash(); }
  }
  if(e.key==='Enter' && !e.shiftKey){
    if(!slashMenu.classList.contains('hidden') && filtered.length){ e.preventDefault(); pickSlash(filtered[slashIndex]); return; }
    e.preventDefault(); doSend();
  }
});
document.addEventListener('click', e=>{ if(!slashMenu.contains(e.target) && e.target!==inputEl) hideSlash(); });

/* Drag & paste */
['dragenter','dragover'].forEach(ev=>{
  document.addEventListener(ev, e=>{
    e.preventDefault();
    if(ev==='dragenter') dropOverlay.classList.remove('hidden');
    composerInner.classList.add('drag');
  });
});
['dragleave','drop'].forEach(ev=>{
  document.addEventListener(ev, e=>{
    if(ev==='dragleave' && e.target===dropOverlay) dropOverlay.classList.add('hidden');
    if(ev==='drop') dropOverlay.classList.add('hidden');
    composerInner.classList.remove('drag');
  });
});
document.addEventListener('drop', e=>{
  e.preventDefault();
  if(e.dataTransfer.files.length) addFiles([...e.dataTransfer.files]);
});
document.addEventListener('paste', e=>{
  const files=[... (e.clipboardData?.files||[])];
  if(files.length) addFiles(files);
});

/* Messages */
function addUser(text, files){
  const row=document.createElement('div');
  row.className='message user';
  const time=new Date().toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'});
  let media='';
  if(files.length){
    media=`<div class="user-media">` + files.map(p=>{
      if(p.file.type.startsWith('image/')) return `<img src="${p.url}" alt="">`;
      if(p.file.type.startsWith('video/')) return `<video src="${p.url}" controls></video>`;
      return `<span class="file-chip"><i data-lucide="file"></i> ${esc(p.file.name)}</span>`;
    }).join('') + `</div>`;
  }
  const bubble=text? `<div class="bubble">${esc(text).replace(/\n/g,'<br>')}</div>` : '';
  row.innerHTML=`<div class="msg-head"><span class="who">You</span><span class="when">${time}</span></div>${media}${bubble}`;
  messagesEl.appendChild(row);
  refreshIcons(); scrollBottom();
}
function addTyping(){
  const r=document.createElement('div');
  r.className='message assistant'; r.id='typing';
  r.innerHTML=`<div class="msg-head"><span class="who"><i data-lucide="bot" style="width:14px;height:14px"></i> MCP</span></div><div class="typing"><span></span><span></span><span></span></div>`;
  messagesEl.appendChild(r); refreshIcons(); scrollBottom();
}
function rmTyping(){ document.getElementById('typing')?.remove(); }
function detect(q){
  const s=q.toLowerCase();
  const has=(...k)=>k.some(v=>s.includes(v));
  if(has('postgres','sql','user','select','/sql')) return 'pg';
  if(has('aws','s3','bucket','/s3')) return 'aws';
  if(has('file','fs','/data','/fs')) return 'fs';
  return 'general';
}
function pgHTML(){
  return `<div class="card"><div class="card-head"><h4>Retrieved 5 records from <code>remote_db.users</code></h4><p>Parsing metadata • 184ms • TLS 1.3</p></div><div class="table-wrap"><table><thead><tr><th>ID</th><th>USERNAME</th><th>REGISTERED</th></tr></thead><tbody><tr><td>101</td><td>k_morgan</td><td>2023-10-21</td></tr><tr><td>102</td><td>j_doe</td><td>2023-10-21</td></tr><tr><td>103</td><td>m_smith</td><td>2023-10-22</td></tr><tr><td>104</td><td>a_wilson</td><td>2023-10-22</td></tr><tr><td>105</td><td>s_chen</td><td>2023-10-23</td></tr></tbody></table></div><div class="card-foot"><button class="chip" onclick="navigator.clipboard.writeText('SELECT * FROM users LIMIT 5')"><i data-lucide="copy" style="width:12px;height:12px"></i> Copy</button><button class="chip"><i data-lucide="download" style="width:12px;height:12px"></i> CSV</button><span class="tls">✓ TLS 1.3</span></div></div>`;
}
function awsHTML(){ return `<div class="card"><div class="card-head"><h4>Found 3 buckets in <code>us-east-1</code></h4><p>via aws-connector • 184ms</p></div><div class="table-wrap"><table><thead><tr><th>BUCKET</th><th>SIZE</th><th>OBJECTS</th></tr></thead><tbody><tr><td>mcp-app-data</td><td>2.4 GB</td><td>342</td></tr><tr><td>user-avatars-prod</td><td>18.7 GB</td><td>12.4k</td></tr><tr><td>logs-archive-2023</td><td>412 GB</td><td>89k</td></tr></tbody></table></div><div class="card-foot"><button class="chip"><i data-lucide="copy"></i> Copy</button><span class="tls">✓ TLS 1.3</span></div></div>`; }
function fsHTML(){ return `<div class="card"><div class="card-head"><h4>Listed 5 entries in <code>/data</code></h4><p>read-only mount</p></div><div class="table-wrap"><table><thead><tr><th>NAME</th><th>TYPE</th><th>SIZE</th></tr></thead><tbody><tr><td>config.json</td><td>file</td><td>1.2 KB</td></tr><tr><td>datasets/</td><td>dir</td><td>—</td></tr><tr><td>users.csv</td><td>file</td><td>4.8 KB</td></tr><tr><td>README.md</td><td>file</td><td>2.1 KB</td></tr><tr><td>exports/</td><td>dir</td><td>—</td></tr></tbody></table></div></div>`; }
function helpHTML(){ return `<div class="card"><div class="card-head"><h4>Available commands</h4><p>Type <code>/</code> to autocomplete</p></div><div style="padding:12px;display:grid;gap:6px">${SLASH.map(c=>`<div style="display:flex;gap:8px;align-items:center;font-size:13px"><code style="background:#0f172a;color:#fff;padding:3px 7px;border-radius:7px;font-family:var(--mono);font-size:11px">${c.key}</code> ${esc(c.desc)} <span style="color:#94a3b8">${esc(c.hint)}</span></div>`).join('')}</div></div>`; }
function generalHTML(q){ return `<div class="card"><div class="card-head"><h4>MCP Router</h4><p>Received: <code>${esc(q.slice(0,80))}</code></p></div><div style="padding:12px"><p style="margin:0 0 8px;font-size:13px">Try <code>/sql</code>, <code>/s3</code>, <code>/fs</code> or upload media.</p><div style="background:#eff6ff;border:1px solid #dbeafe;border-radius:10px;padding:10px;font-size:12px">Suggested: <code>/sql SELECT * FROM users LIMIT 5</code> • <code>/s3 list buckets</code> • <code>/fs list /data</code></div></div></div>`; }
function addAssistant(html){
  const time=new Date().toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'});
  const row=document.createElement('div');
  row.className='message assistant';
  row.innerHTML=`<div class="msg-head"><span class="who"><i data-lucide="bot" style="width:14px;height:14px"></i> ASSISTANT</span><span class="when">${time}</span></div>${html}`;
  messagesEl.appendChild(row); refreshIcons(); scrollBottom();
}
function clearChat(){ messagesEl.innerHTML=`<div class="date-sep"><span>New conversation</span></div>`; toast('Chat cleared'); }

async function doSend(raw){
  let text = (raw ?? inputEl.value).trim();
  const files=[...pending];
  if(!text && !files.length) return;
  if(isTyping) return;
  if(text==='/clear'){ clearChat(); inputEl.value=''; pending=[]; drawPreview(); syncComposer(); autoResize(); hideSlash(); return; }
  if(text==='/help'){ addUser(text,[]); inputEl.value=''; pending=[]; drawPreview(); syncComposer(); hideSlash(); addAssistant(helpHTML()); return; }
  if(text.startsWith('/connect ')){ const n=text.slice(9).trim(); const s=servers.find(x=>x.name===n); if(s) await toggle(s.id,true); else toast(`Unknown server ${n}`); inputEl.value=''; hideSlash(); syncComposer(); return; }
  if(text.startsWith('/disconnect ')){ const n=text.slice(12).trim(); const s=servers.find(x=>x.name===n); if(s) await toggle(s.id,false); else toast(`Unknown ${n}`); inputEl.value=''; hideSlash(); syncComposer(); return; }

  const tool=detect(text);
  const need={pg:'postgres-mcp', aws:'aws-us-east', fs:'local-fs'}[tool];
  if(need){
    const srv=servers.find(s=>s.name===need);
    if(srv && srv.status!=='connected'){ toast(`${need} disconnected — connecting…`); await toggle(srv.id,true); }
  }

  addUser(text, files);
  const keepUrls=files.map(p=>p.url);
  pending.forEach(p=>{}); pending=[]; drawPreview();
  inputEl.value=''; hideSlash(); autoResize(); syncComposer();
  isTyping=true; syncComposer(); addTyping();
  await new Promise(r=>setTimeout(r, {pg:1100, aws:950, fs:800, general:650}[tool]||700));
  rmTyping();
  let html;
  if(files.length && tool==='general'){
    html=`<div class="card"><div class="card-head"><h4>Analyzed ${files.length} file(s)</h4><p>${files.map(f=>esc(f.file.name)).join(', ')}</p></div><div style="padding:12px"><div style="background:#f8fafc;border:1px solid var(--line);border-radius:10px;padding:10px;font-family:var(--mono);font-size:12px">${files.map(f=>esc(f.file.name)+' • '+(f.file.size/1024).toFixed(1)+'KB').join('<br>')}</div></div></div>`;
    if(text) html+= generalHTML(text);
  } else {
    if(tool==='pg') html=pgHTML();
    else if(tool==='aws') html=awsHTML();
    else if(tool==='fs') html=fsHTML();
    else html=generalHTML(text);
  }
  addAssistant(html);
  isTyping=false; syncComposer(); inputEl.focus();
}

/* Connect toggle */
async function toggle(id, force){
  const s=servers.find(x=>x.id===id); if(!s) return;
  const should = typeof force==='boolean' ? force : s.status!=='connected';
  if(should){
    if(s.status==='connected') return;
    s.status='connecting'; render(); toast(`Connecting ${s.name}…`);
    await new Promise(r=>setTimeout(r,900));
    s.status='connected'; save(); render(); toast(`${s.name} connected ✓`);
  } else {
    s.status='disconnected'; save(); render(); toast(`${s.name} disconnected`);
  }
}

/* Events */
sendBtn.addEventListener('click', ()=>doSend());
attachBtn.addEventListener('click', ()=>mediaInput.click());
quickToolsBtn.addEventListener('click', ()=> toolsModal.classList.remove('hidden'));
toolsBackdrop.addEventListener('click', ()=>toolsModal.classList.add('hidden'));
closeModalBtn.addEventListener('click', ()=>toolsModal.classList.add('hidden'));
document.querySelectorAll('.quick-card, .tool').forEach(b=>{
  b.addEventListener('click', ()=>{
    const t=b.dataset.tool;
    toolsModal.classList.add('hidden');
    if(t==='upload') mediaInput.click();
    else if(t==='pg') { inputEl.value='/sql SELECT * FROM users LIMIT 5'; inputEl.focus(); autoResize(); syncComposer(); }
    else if(t==='aws') { inputEl.value='/s3 list buckets'; inputEl.focus(); autoResize(); syncComposer(); }
    else if(t==='fs') { inputEl.value='/fs list /data'; inputEl.focus(); autoResize(); syncComposer(); }
  });
});
document.querySelectorAll('.hint').forEach(b=>{
  b.addEventListener('click', ()=>{ inputEl.value=b.dataset.insert; inputEl.focus(); autoResize(); syncComposer(); });
});
topNodes.addEventListener('click', e=>{
  const p=e.target.closest('[data-id]'); if(!p) return; toggle(p.dataset.id);
});
sidebarList.addEventListener('click', e=>{
  const p=e.target.closest('[data-id]'); if(!p) return; toggle(p.dataset.id);
});
serversList.addEventListener('click', e=>{
  const del=e.target.closest('[data-del]'); if(del){ servers=servers.filter(s=>s.id!==del.dataset.del); save(); render(); toast('Removed'); return; }
  const ed=e.target.closest('[data-edit]'); if(ed){ const s=servers.find(x=>x.id===ed.dataset.edit); if(s){ editingId=s.id; serverModalTitle.textContent='Edit MCP Server'; srvName.value=s.name; srvTransport.value=s.transport; srvEndpoint.value=s.endpoint; srvTools.value=s.tools; serverModal.classList.remove('hidden'); refreshIcons(); } return; }
});
serversList.addEventListener('change', e=>{
  const t=e.target.closest('[data-toggle]'); if(t) toggle(t.dataset.toggle, t.checked);
});
function openSettings(){ settingsView.classList.remove('hidden'); refreshIcons(); }
function closeSettingsView(){ settingsView.classList.add('hidden'); }
serversBtn.addEventListener('click', openSettings);
settingsBtn.addEventListener('click', openSettings);
settingsBtn2.addEventListener('click', openSettings);
settingsBackdrop.addEventListener('click', closeSettingsView);
closeSettings.addEventListener('click', closeSettingsView);
serverSearch.addEventListener('input', render);
sidebarSearch.addEventListener('input', render);
addServerBtn.addEventListener('click', ()=>{ editingId=null; serverModalTitle.textContent='Add MCP Server'; srvName.value=''; srvEndpoint.value=''; srvTools.value=''; srvTransport.value='sse'; serverModal.classList.remove('hidden'); refreshIcons(); });
addServerSidebar.addEventListener('click', ()=> addServerBtn.click());
closeServerModalBtn.addEventListener('click', ()=> serverModal.classList.add('hidden'));
cancelServer.addEventListener('click', ()=> serverModal.classList.add('hidden'));
serverModalBackdrop.addEventListener('click', ()=> serverModal.classList.add('hidden'));
serverForm.addEventListener('submit', e=>{
  e.preventDefault();
  const name=srvName.value.trim(), ep=srvEndpoint.value.trim();
  if(!name||!ep) return;
  if(editingId){
    const s=servers.find(x=>x.id===editingId); s.name=name; s.transport=srvTransport.value; s.endpoint=ep; s.tools=srvTools.value; toast(`${name} updated`);
  } else {
    const id='srv'+Date.now();
    const col=name.includes('postgres')?'pg': name.includes('aws')?'aws': name.includes('fs')?'fs':'custom';
    servers.push({id,name,transport:srvTransport.value,endpoint:ep,tools:srvTools.value,status:'disconnected',color:col}); toast(`${name} added`);
  }
  save(); render(); serverModal.classList.add('hidden');
});
menuBtn.addEventListener('click', ()=> sidebar.classList.add('open'));
sidebarClose.addEventListener('click', ()=> sidebar.classList.remove('open'));
document.addEventListener('keydown', e=>{
  if(e.key==='Escape'){ settingsView.classList.add('hidden'); serverModal.classList.add('hidden'); toolsModal.classList.add('hidden'); hideSlash(); sidebar.classList.remove('open'); }
  if((e.metaKey||e.ctrlKey) && e.key.toLowerCase()==='k'){ e.preventDefault(); openSettings(); }
});
newChatBtn.addEventListener('click', clearChat);
messagesEl.addEventListener('click', e=>{
  const c=e.target.closest('.chip'); if(c && c.textContent.includes('Copy')){ const tbl=c.closest('.card')?.querySelector('table'); if(tbl){ const t=[...tbl.rows].map(r=>[...r.cells].map(c=>c.textContent).join('\t')).join('\n'); navigator.clipboard.writeText(t); toast('Copied'); } }
});
render(); refreshIcons(); scrollBottom();
