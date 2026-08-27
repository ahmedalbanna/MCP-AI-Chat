/* MCP Terminal — Ultimate */
const $ = s => document.querySelector(s);
const $$ = s => [...document.querySelectorAll(s)];
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
const promptStrip = $('#promptStrip');
const chatListEl = $('#chatList');
const chatCountEl = $('#chatCount');
const crumbTitle = $('#crumbTitle');
const providerSelect = $('#providerSelect');
const modelSelect = $('#modelSelect');
const agentModesEl = $('#agentModes');
const undoBtn = $('#undoBtn');
const redoBtn = $('#redoBtn');
const convMenuBtn = $('#convMenuBtn');
const convMenu = $('#convMenu');
const speechBar = $('#speechBar');
const micBtn = $('#micBtn');
const stopSpeech = $('#stopSpeech');
const todoDraft = $('#todoDraft');
const todoList = $('#todoList');
const todoInput = $('#todoInput');
const todoProgress = $('#todoProgress');
const todoToggle = $('#todoToggle');
const clearTodo = $('#clearTodo');
const addTodoBtn = $('#addTodoBtn');

// drawers
const settingsView = $('#settingsView');
const marketplaceView = $('#marketplaceView');
const promptsView = $('#promptsView');
const profileView = $('#profileView');
const serverSearch = $('#serverSearch');
const serversList = $('#serversList');
const addServerBtn = $('#addServerBtn');
const addServerSidebar = $('#addServerSidebar');
const closeSettings = $('#closeSettings');
const serversBtn = $('#serversBtn');
const settingsBtn = $('#settingsBtn');
const marketplaceBtn = $('#marketplaceBtn');
const promptLibraryBtn = $('#promptLibraryBtn');
const showPromptsLink = $('#showPromptsLink');
const closeMarketplace = $('#closeMarketplace');
const marketplaceBackdrop = $('#marketplaceBackdrop');
const marketGrid = $('#marketGrid');
const marketSearch = $('#marketSearch');
const marketFilter = $('#marketFilter');
const marketSort = $('#marketSort');
const marketTabs = $('#marketTabs');
const marketCount = $('#marketCount');
const marketInstalledCount = $('#marketInstalledCount');
const refreshMarket = $('#refreshMarket');
const marketDetailModal = $('#marketDetailModal');
const marketDetailBackdrop = $('#marketDetailBackdrop');
const marketDetailContent = $('#marketDetailContent');
let activeMarketTab = localStorage.getItem('marketTab') || 'all';
const promptsBackdrop = $('#promptsBackdrop');
const closePrompts = $('#closePrompts');
const promptCats = $('#promptCats');
const promptGrid = $('#promptGrid');
const profileBackdrop = $('#profileBackdrop');
const closeProfile = $('#closeProfile');
const userProfileCard = $('#userProfileCard');
const profileName = $('#profileName');
const profileRole = $('#profileRole');
const profileAvatar = $('#profileAvatar');
const profileLargeAvatar = $('#profileLargeAvatar');
const profileNameInput = $('#profileNameInput');
const profileEmail = $('#profileEmail');
const avatarInput = $('#avatarInput');
const changeAvatar = $('#changeAvatar');
const themeSelect = $('#themeSelect');
const streamToggle = $('#streamToggle');
const ttsToggle = $('#ttsToggle');
const saveProfileBtn = $('#saveProfile');

// modals
const serverModal = $('#serverModal');
const closeServerModalBtn = $('#closeServerModal');
const serverForm = $('#serverForm');
const srvName = $('#srvName');
const srvTransport = $('#srvTransport');
const srvEndpoint = $('#srvEndpoint');
const srvTools = $('#srvTools');
const serverModalTitle = $('#serverModalTitle');
const serverModalBackdrop = $('#serverModalBackdrop');
const cancelServer = $('#cancelServer');
const toolsModal = $('#toolsModal');
const toolsBackdrop = $('#toolsBackdrop');
const closeModalBtn = $('#closeModal');
const quickToolsBtn = $('#quickToolsBtn');
const sidebar = $('#sidebar');
const menuBtn = $('#menuBtn');
const sidebarClose = $('#sidebarClose');
const sidebarSearch = $('#sidebarSearch');
const newChatBtn = $('#newChatBtn');
const newChatTopBtn = $('#newChatTopBtn');
const importChatBtn = $('#importChatBtn');
const importFile = $('#importFile');
const archiveToggle = $('#archiveToggle');
const editMsgModal = $('#editMsgModal');
const editMsgBackdrop = $('#editMsgBackdrop');
const closeEditMsg = $('#closeEditMsg');
const cancelEdit = $('#cancelEdit');
const saveEdit = $('#saveEdit');
const editMsgInput = $('#editMsgInput');

let pending = [];
let isTyping = false;
let editingId = null;
let editingMsgId = null;
let slashIndex = 0, filtered = [];
let activeChatId = null;
let showArchived = false;
let todos = [];
let isRecording = false;
let recognition = null;
let history = { undo: [], redo: [] };

// Providers — extended
let PROVIDERS = JSON.parse(localStorage.getItem('mcp_providers')||'null') || [
  { id:'openai', name:'OpenAI', models:['gpt-4o','gpt-4o-mini','o1-preview'], apiKey:'', baseUrl:'https://api.openai.com/v1', temp:0.7, maxTokens:4096, enabled:true, streaming:true, latency:142 },
  { id:'anthropic', name:'Anthropic', models:['claude-3.5-sonnet','claude-3-opus'], apiKey:'', baseUrl:'https://api.anthropic.com', temp:0.7, maxTokens:8192, enabled:true, streaming:true, latency:210 },
  { id:'google', name:'Google', models:['gemini-1.5-pro','gemini-1.5-flash'], apiKey:'', baseUrl:'https://generativelanguage.googleapis.com', temp:0.8, maxTokens:8192, enabled:true, streaming:true, latency:180 },
  { id:'local', name:'Local', models:['llama-3.1-70B','mistral-large'], apiKey:'', baseUrl:'http://localhost:11434', temp:0.6, maxTokens:4096, enabled:false, streaming:false, latency:45 },
];
let selectedProvider = localStorage.getItem('provider') || 'anthropic';
let selectedModel = localStorage.getItem('model') || 'claude-3.5-sonnet';
let selectedMode = localStorage.getItem('agentMode') || 'Chat';
let fallbackProvider = localStorage.getItem('fallbackProvider') || 'openai';
let globalTemp = parseFloat(localStorage.getItem('globalTemp')||'0.7');
const MODES = [
  {id:'Chat', icon:'message-circle', desc:'General chat'},
  {id:'Code', icon:'code-2', desc:'Code assistant'},
  {id:'Research', icon:'search', desc:'Deep research'},
  {id:'Planning', icon:'map', desc:'Planning'},
];
const saveProviders = ()=> localStorage.setItem('mcp_providers', JSON.stringify(PROVIDERS));

// Chats
let chats = JSON.parse(localStorage.getItem('mcp_chats')||'null') || [
  {id:'c1', title:'PostgreSQL users query', created:Date.now()-3600000, archived:false, messages:[
    {id:'m1', role:'user', text:'Query the remote PostgreSQL database for the last 5 user registrations and format as a table.', time:'10:21 AM'},
    {id:'m2', role:'assistant', html:'pg', time:'10:21 AM', tasks:[{id:'t1', text:'Verify table schema', done:false},{id:'t2', text:'Run SELECT on remote_db.users', done:true},{id:'t3', text:'Format as markdown table', done:true}], todos:[{text:'Export to CSV', done:false},{text:'Share with team', done:false}], status:'done'},
  ]},
  {id:'c2', title:'S3 bucket audit', created:Date.now()-86400000, archived:false, messages:[
    {id:'m3', role:'user', text:'List S3 buckets in aws-us-east', time:'09:12 AM'},
    {id:'m4', role:'assistant', html:'aws', time:'09:12 AM', status:'done'},
  ]},
  {id:'c3', title:'Local filesystem', created:Date.now()-172800000, archived:true, messages:[
    {id:'m5', role:'user', text:'List files in /data', time:'Yesterday'},
  ]},
];
if(!localStorage.getItem('mcp_chats')) localStorage.setItem('mcp_chats', JSON.stringify(chats));
activeChatId = localStorage.getItem('activeChat') || chats[0].id;

// Servers
const DEFAULT_SERVERS = [
  {id:'srv1', name:'aws-us-east', transport:'sse', endpoint:'wss://aws.mcp.local/sse', tools:'s3_list, ec2_status', status:'connected', color:'aws'},
  {id:'srv2', name:'postgres-mcp', transport:'stdio', endpoint:'npx @modelcontextprotocol/server-postgres', tools:'query, list_tables', status:'connected', color:'pg'},
  {id:'srv3', name:'local-fs', transport:'stdio', endpoint:'npx @modelcontextprotocol/server-filesystem /data', tools:'read, write, list', status:'disconnected', color:'fs'},
];
let servers = JSON.parse(localStorage.getItem('mcp_servers')||'null') || DEFAULT_SERVERS;
const saveServers=()=> localStorage.setItem('mcp_servers', JSON.stringify(servers));
const saveChats=()=> localStorage.setItem('mcp_chats', JSON.stringify(chats));

const MARKET = JSON.parse(localStorage.getItem('mcp_market')||'null') || [
  {id:'m1', name:'Postgres Pro', cat:'db', icon:'database', color:'#334155', desc:'Advanced SQL with vector search, migrations & pgvector support', author:'modelcontextprotocol', version:'2.1.0', stars:4.9, installs:12400, installed:true, featured:true, trending:true, isNew:false, tags:['postgres','sql','vector'], tools:['query','list_tables','describe_schema'], updated:'2024-10-12', verified:true},
  {id:'m2', name:'AWS Suite', cat:'cloud', icon:'cloud', color:'#ff9900', desc:'S3, EC2, Lambda, CloudWatch unified with MFA support', author:'aws', version:'1.8.3', stars:4.8, installs:8400, installed:true, featured:true, trending:true, isNew:false, tags:['aws','s3','ec2'], tools:['s3_list','ec2_status','lambda_invoke'], updated:'2024-10-20', verified:true},
  {id:'m3', name:'Filesystem', cat:'fs', icon:'folder', color:'#2563eb', desc:'Secure local file access with sandboxing & watcher', author:'modelcontextprotocol', version:'1.4.2', stars:4.7, installs:6100, installed:true, featured:false, trending:false, isNew:false, tags:['fs','local','watch'], tools:['read','write','list'], updated:'2024-09-28', verified:true},
  {id:'m4', name:'GitHub', cat:'dev', icon:'github', color:'#0f172a', desc:'Repos, PRs, issues, actions, code search', author:'github', version:'3.0.1', stars:4.9, installs:15200, installed:false, featured:true, trending:true, isNew:false, tags:['git','pr','actions'], tools:['list_repos','create_pr','search_code'], updated:'2024-10-18', verified:true},
  {id:'m5', name:'Slack', cat:'cloud', icon:'message-square', color:'#e11d48', desc:'Channels, messages, threads, reactions & workflows', author:'slack', version:'1.2.5', stars:4.6, installs:9000, installed:false, featured:false, trending:false, isNew:true, tags:['chat','slack'], tools:['list_channels','post_message'], updated:'2024-10-22', verified:false},
  {id:'m6', name:'Notion', cat:'fs', icon:'book-open', color:'#000', desc:'Pages, databases, blocks, comments API', author:'notion', version:'0.9.4', stars:4.5, installs:5200, installed:false, featured:false, trending:false, isNew:true, tags:['notion','docs'], tools:['query_db','create_page'], updated:'2024-10-05', verified:false},
  {id:'m7', name:'Redis', cat:'db', icon:'layers', color:'#dc2626', desc:'Cache, pub/sub, streams & cluster management', author:'redis', version:'1.1.0', stars:4.7, installs:4100, installed:false, featured:false, trending:true, isNew:false, tags:['redis','cache'], tools:['get','set','publish'], updated:'2024-09-15', verified:true},
  {id:'m8', name:'Puppeteer', cat:'dev', icon:'globe', color:'#7c3aed', desc:'Browser automation, scraping & PDF gen', author:'browser', version:'2.3.0', stars:4.8, installs:7300, installed:false, featured:false, trending:true, isNew:false, tags:['browser','scrape'], tools:['navigate','screenshot','pdf'], updated:'2024-10-10', verified:true},
  {id:'m9', name:'Stripe', cat:'cloud', icon:'credit-card', color:'#635bff', desc:'Payments, customers, subscriptions, invoices', author:'stripe', version:'1.0.2', stars:4.8, installs:6800, installed:false, featured:false, trending:false, isNew:true, tags:['payments','stripe'], tools:['list_customers','create_charge'], updated:'2024-10-23', verified:true},
  {id:'m10', name:'Linear', cat:'dev', icon:'target', color:'#5e6ad2', desc:'Issues, projects, cycles, roadmap sync', author:'linear', version:'1.3.1', stars:4.6, installs:3100, installed:false, featured:false, trending:false, isNew:false, tags:['linear','issues'], tools:['list_issues','create_issue'], updated:'2024-09-30', verified:false},
];
const saveMarket = ()=> localStorage.setItem('mcp_market', JSON.stringify(MARKET));

const PROMPTS = [
  {cat:'MCP', title:'SQL audit', prompt:'Query the remote PostgreSQL database for the last 5 user registrations and format as a table.', icon:'database'},
  {cat:'MCP', title:'S3 inventory', prompt:'List S3 buckets in aws-us-east and sort by size', icon:'cloud'},
  {cat:'MCP', title:'File explorer', prompt:'List files in /data and summarize CSV headers', icon:'folder-open'},
  {cat:'Analysis', title:'Summarize logs', prompt:'Summarize CloudWatch logs for the last 24h, highlight errors', icon:'bar-chart-3'},
  {cat:'Analysis', title:'User insights', prompt:'Analyze user registrations trend and propose growth actions', icon:'trending-up'},
  {cat:'Code', title:'Generate API', prompt:'Create a REST API for users table with pagination and auth', icon:'code-2'},
  {cat:'Write', title:'Release notes', prompt:'Draft release notes for MCP Terminal v2 with highlights', icon:'pen-tool'},
];

const SLASH = [
  {key:'/sql', desc:'Run SQL via postgres-mcp', hint:'SELECT * FROM users', insert:'/sql SELECT * FROM users LIMIT 5'},
  {key:'/s3', desc:'List S3 buckets', hint:'aws-us-east', insert:'/s3 list buckets'},
  {key:'/fs', desc:'List local files', hint:'/data', insert:'/fs list /data'},
  {key:'/clear', desc:'Clear chat', hint:'', insert:'/clear'},
  {key:'/help', desc:'Show tools', hint:'', insert:'/help'},
  {key:'/todo', desc:'Add checklist', hint:'e.g. review PR', insert:'/todo '},
  {key:'/connect', desc:'Connect server', hint:'<name>', insert:'/connect '},
  {key:'/disconnect', desc:'Disconnect', hint:'<name>', insert:'/disconnect '},
];

const refreshIcons=()=> window.lucide && lucide.createIcons();
const toast=(m,d=2200)=>{ toastEl.textContent=m; toastEl.classList.remove('hidden'); clearTimeout(toast._t); toast._t=setTimeout(()=>toastEl.classList.add('hidden'),d); };
const esc=s=> s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const scrollBottom=()=>{ messagesEl.scrollTop=messagesEl.scrollHeight; };
const uid=()=> Math.random().toString(36).slice(2,9);

// Providers render — complete
function renderProviders(){
  const enabledProviders = PROVIDERS.filter(p=>p.enabled);
  // if selected is disabled, fallback
  if(!enabledProviders.find(p=>p.id===selectedProvider)){
    const fallback = enabledProviders[0];
    if(fallback){ selectedProvider=fallback.id; selectedModel=fallback.models[0]; }
  }
  providerSelect.innerHTML = PROVIDERS.filter(p=>p.enabled).map(p=>`<option value="${p.id}" ${p.id===selectedProvider?'selected':''}>${p.name}</option>`).join('') || `<option>No providers enabled</option>`;
  const prov = PROVIDERS.find(p=>p.id===selectedProvider);
  if(prov){
    modelSelect.innerHTML = prov.models.map(m=>`<option ${m===selectedModel?'selected':''}>${m}</option>`).join('');
    modelSelect.disabled=false;
    // pills
    const pillName=$('#providerPillName'), pillMeta=$('#providerPillMeta');
    if(pillName) pillName.textContent = selectedModel;
    if(pillMeta) pillMeta.textContent = prov.name;
    // status
    const statusText=$('#providerStatusText'), tempEl=$('#providerTemp');
    if(statusText) statusText.textContent = prov.apiKey ? `Ready • ${prov.latency}ms` : `No API key • ${prov.latency}ms`;
    if(tempEl) tempEl.textContent = `${prov.temp} temp`;
    // top status
    const activeName=$('#activeProviderName');
    if(activeName) activeName.textContent = `${prov.name} • ${selectedModel}`;
    const lat=$('#activeProviderLatency');
    if(lat) lat.textContent = prov.apiKey ? `~${prov.latency}ms • streaming ${prov.streaming?'on':'off'}` : `No key • ${prov.latency}ms`;
    const activeStatus=$('#activeProviderStatus');
    if(activeStatus) activeStatus.className = `status ${prov.apiKey?'on':'off'}`;
  } else {
    modelSelect.innerHTML=`<option>No model</option>`;
    modelSelect.disabled=true;
  }
  // global temp
  const gv=$('#globalTempVal');
  if(gv) gv.textContent = String(globalTemp);
  const gt=$('#globalTemp');
  if(gt) gt.value = String(globalTemp);
  // fallback
  const fb=$('#fallbackProvider');
  if(fb) fb.value = fallbackProvider;
  // profile
  if(profileRole) profileRole.textContent = `${$('#profileRoleSelect')?.value || 'Admin'} • ${selectedModel} • ${selectedMode} mode`;
  // usage mock
  const tok=$('#usageTokens'), cost=$('#usageCost'), req=$('#usageReq');
  if(tok) tok.textContent = (Math.floor(Math.random()*5+10))+'k';
  if(cost) cost.textContent = '$'+(Math.random()*1.2).toFixed(2);
  if(req) req.textContent = String(Math.floor(Math.random()*20+20));
  refreshIcons();
}
providerSelect.addEventListener('change', e=>{
  selectedProvider=e.target.value;
  const prov=PROVIDERS.find(p=>p.id===selectedProvider);
  if(!prov || !prov.enabled) return;
  selectedModel=prov.models[0];
  localStorage.setItem('provider', selectedProvider);
  localStorage.setItem('model', selectedModel);
  renderProviders(); renderProviderList(); toast(`Provider: ${prov.name} • ${selectedModel}`);
});
modelSelect.addEventListener('change', e=>{ selectedModel=e.target.value; localStorage.setItem('model', selectedModel); toast(`Model: ${selectedModel}`); renderProviders(); renderMessages(); });

// Provider list drawer
const providerView=$('#providerView'), providerBackdrop=$('#providerBackdrop'), closeProvider=$('#closeProvider'), providerList=$('#providerList'), providerSearch=$('#providerSearch'), addProviderBtn=$('#addProviderBtn'), providerSettingsBtn=$('#providerSettingsBtn'), providerPill=$('#providerPill'), providerTopBtn=$('#providerTopBtn'), testActiveProvider=$('#testActiveProvider'), exportProvidersBtn=$('#exportProvidersBtn'), importProviderBtn=$('#importProviderBtn'), importProviderFile=$('#importProviderFile'), fallbackProviderEl=$('#fallbackProvider'), globalTempEl=$('#globalTemp'), providerModal=$('#providerModal'), providerModalBackdrop=$('#providerModalBackdrop'), closeProviderModal=$('#closeProviderModal'), providerForm=$('#providerForm'), provName=$('#provName'), provId=$('#provId'), provKey=$('#provKey'), provBase=$('#provBase'), provModels=$('#provModels'), provTemp=$('#provTemp'), provTempVal=$('#provTempVal'), provMax=$('#provMax'), provStream=$('#provStream'), provEnabled=$('#provEnabled'), testProviderBtn=$('#testProviderBtn'), cancelProvider=$('#cancelProvider');
let editingProviderId=null;
function renderProviderList(){
  const q=(providerSearch?.value||'').toLowerCase();
  let list=PROVIDERS;
  if(q) list=list.filter(p=> p.name.toLowerCase().includes(q) || p.id.toLowerCase().includes(q) || p.models.join(' ').toLowerCase().includes(q));
  providerList.innerHTML = list.map(p=>{
    const isActive=p.id===selectedProvider;
    const hasKey=!!p.apiKey;
    const logoClass = p.id==='openai'?'openai' : p.id==='anthropic'?'anthropic' : p.id==='google'?'google' : p.id==='local'?'local':'custom';
    return `<div class="provider-card ${isActive?'active':''}" data-id="${p.id}">
      <div class="provider-card-head">
        <div class="provider-logo ${logoClass}">${p.name.slice(0,2).toUpperCase()}</div>
        <div class="provider-meta"><b>${esc(p.name)} ${isActive?'<span style="background:var(--primary);color:#fff;font-size:10px;padding:2px 6px;border-radius:999px;margin-left:6px">ACTIVE</span>':''} ${!p.enabled?'<span style="background:#f1f5f9;color:#64748b;font-size:10px;padding:2px 6px;border-radius:999px;border:1px solid #e2e8f0">DISABLED</span>':''}</b><p>${esc(p.id)} • ${esc(p.baseUrl)} • ${p.models.length} models</p>
          <div class="provider-badges">${p.models.map(m=>`<span class="mono-badge ${m===selectedModel && isActive?'active':''}">${esc(m)}</span>`).join('')}</div>
        </div>
        <label class="switch" title="Enable"><input type="checkbox" ${p.enabled?'checked':''} data-provider-toggle="${p.id}"><span></span></label>
      </div>
      <div class="provider-key"><i data-lucide="key" style="width:12px;height:12px"></i><span>${hasKey ? 'sk-••••••••'+p.apiKey.slice(-4) : 'No API key — add to enable'}</span><span style="margin-left:auto;font-size:11px;color:${hasKey?'var(--ok)':'var(--warn)'}">${hasKey?'Saved':'Missing'}</span></div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">
        <span style="font-size:11px;color:var(--muted);font-family:var(--mono)">temp ${p.temp} • max ${p.maxTokens} • ${p.streaming?'streaming':'no-stream'}</span>
        <span style="margin-left:auto;display:flex;gap:6px">
          <button class="ghost-btn sm" data-provider-test="${p.id}"><i data-lucide="plug" style="width:12px;height:12px"></i> Test</button>
          <button class="ghost-btn sm" data-provider-use="${p.id}"><i data-lucide="check" style="width:12px;height:12px"></i> Use</button>
          <button class="icon-btn sm" data-provider-edit="${p.id}" title="Edit"><i data-lucide="pencil" style="width:12px;height:12px"></i></button>
          <button class="icon-btn sm" data-provider-del="${p.id}" title="Delete" style="color:var(--bad)"><i data-lucide="trash-2" style="width:12px;height:12px"></i></button>
        </span>
      </div>
    </div>`;
  }).join('') || `<p style="text-align:center;color:#94a3b8;padding:16px;background:#fff;border:1px dashed var(--line);border-radius:12px">No providers found<br><small>Try a different search or add a provider</small></p>`;
  refreshIcons();
}
function openProviderView(){ providerView.classList.remove('hidden'); renderProviderList(); renderProviders(); refreshIcons(); }
function closeProviderView(){ providerView.classList.add('hidden'); }
providerSettingsBtn?.addEventListener('click', openProviderView);
providerPill?.addEventListener('click', openProviderView);
providerTopBtn?.addEventListener('click', openProviderView);
providerPill?.addEventListener('keydown', e=>{ if(e.key==='Enter') openProviderView(); });
closeProvider?.addEventListener('click', closeProviderView);
providerBackdrop?.addEventListener('click', closeProviderView);
providerSearch?.addEventListener('input', renderProviderList);
fallbackProviderEl?.addEventListener('change', e=>{ fallbackProvider=e.target.value; localStorage.setItem('fallbackProvider', fallbackProvider); toast(`Fallback: ${fallbackProvider}`); });
globalTempEl?.addEventListener('input', e=>{ globalTemp=parseFloat(e.target.value); localStorage.setItem('globalTemp', String(globalTemp)); renderProviders(); // also update active provider temp if needed
});
provTemp?.addEventListener('input', e=>{ if(provTempVal) provTempVal.textContent=e.target.value; });
testActiveProvider?.addEventListener('click', async ()=>{
  const prov=PROVIDERS.find(p=>p.id===selectedProvider);
  if(!prov) return;
  testActiveProvider.innerHTML=`<i data-lucide="loader-2" style="width:12px;height:12px" class="spin"></i> Testing…`;
  refreshIcons();
  await new Promise(r=>setTimeout(r, 900));
  const ok=!!prov.apiKey;
  toast(ok ? `${prov.name} connected ✓ (${prov.latency}ms)` : `${prov.name}: missing API key`, 2600);
  testActiveProvider.innerHTML=`<i data-lucide="zap" style="width:12px;height:12px"></i> Test`;
  refreshIcons();
});
exportProvidersBtn?.addEventListener('click', ()=>{
  const data=JSON.stringify(PROVIDERS, null, 2);
  const blob=new Blob([data],{type:'application/json'});
  const url=URL.createObjectURL(blob);
  const a=document.createElement('a'); a.href=url; a.download='mcp-providers.json'; a.click(); URL.revokeObjectURL(url); toast('Providers exported');
});
importProviderBtn?.addEventListener('click', ()=> importProviderFile.click());
importProviderFile?.addEventListener('change', e=>{
  const f=e.target.files[0]; if(!f) return;
  const r=new FileReader(); r.onload=()=>{ try{ const arr=JSON.parse(r.result); if(Array.isArray(arr) && arr[0]?.id){ PROVIDERS=arr; saveProviders(); renderProviders(); renderProviderList(); toast('Providers imported'); } else toast('Invalid file'); } catch{ toast('Invalid JSON'); } }; r.readAsText(f); e.target.value='';
});
providerList?.addEventListener('click', e=>{
  const test=e.target.closest('[data-provider-test]'); if(test){ const p=PROVIDERS.find(x=>x.id===test.dataset.providerTest); toast(p.apiKey?`Testing ${p.name}…`:'No API key', 1600); setTimeout(()=> toast(p.apiKey? `${p.name} ✓ ${p.latency}ms` : `${p.name}: add API key first`), 800); return; }
  const use=e.target.closest('[data-provider-use]'); if(use){ selectedProvider=use.dataset.providerUse; const pr=PROVIDERS.find(x=>x.id===selectedProvider); selectedModel=pr.models[0]; localStorage.setItem('provider', selectedProvider); localStorage.setItem('model', selectedModel); renderProviders(); renderProviderList(); renderMessages(); toast(`Switched to ${pr.name}`); return; }
  const edit=e.target.closest('[data-provider-edit]'); if(edit){ const p=PROVIDERS.find(x=>x.id===edit.dataset.providerEdit); if(p){ editingProviderId=p.id; $('#providerModalTitle').textContent='Edit Provider'; provName.value=p.name; provId.value=p.id; provId.disabled=true; provKey.value=p.apiKey; provBase.value=p.baseUrl; provModels.value=p.models.join(', '); provTemp.value=String(p.temp); if(provTempVal) provTempVal.textContent=String(p.temp); provMax.value=String(p.maxTokens); provStream.checked=p.streaming; provEnabled.checked=p.enabled; providerModal.classList.remove('hidden'); refreshIcons(); } return; }
  const del=e.target.closest('[data-provider-del]'); if(del){ const id=del.dataset.providerDel; if(PROVIDERS.length<=1) return toast('Keep at least one provider'); if(confirm('Delete provider?')){ PROVIDERS=PROVIDERS.filter(p=>p.id!==id); if(selectedProvider===id){ selectedProvider=PROVIDERS[0].id; selectedModel=PROVIDERS[0].models[0]; localStorage.setItem('provider', selectedProvider); localStorage.setItem('model', selectedModel); } saveProviders(); renderProviders(); renderProviderList(); renderMessages(); toast('Provider deleted'); } return; }
});
providerList?.addEventListener('change', e=>{
  const t=e.target.closest('[data-provider-toggle]'); if(t){ const p=PROVIDERS.find(x=>x.id===t.dataset.providerToggle); p.enabled=t.checked; saveProviders(); renderProviders(); renderProviderList(); toast(`${p.name} ${p.enabled?'enabled':'disabled'}`); }
});
addProviderBtn?.addEventListener('click', ()=>{
  editingProviderId=null;
  $('#providerModalTitle').textContent='Add AI Provider';
  provName.value=''; provId.value=''; provId.disabled=false; provKey.value=''; provBase.value='https://api.example.com/v1'; provModels.value=''; provTemp.value='0.7'; if(provTempVal) provTempVal.textContent='0.7'; provMax.value='4096'; provStream.checked=true; provEnabled.checked=true;
  providerModal.classList.remove('hidden'); refreshIcons();
});
closeProviderModal?.addEventListener('click', ()=> providerModal.classList.add('hidden'));
providerModalBackdrop?.addEventListener('click', ()=> providerModal.classList.add('hidden'));
cancelProvider?.addEventListener('click', ()=> providerModal.classList.add('hidden'));
testProviderBtn?.addEventListener('click', async ()=>{
  const fake={id:provId.value, name:provName.value, apiKey:provKey.value};
  testProviderBtn.innerHTML=`<i data-lucide="loader-2" style="width:12px;height:12px"></i> Testing…`; refreshIcons();
  await new Promise(r=>setTimeout(r, 800));
  toast(fake.apiKey? `Test ok for ${fake.name} ✓` : 'Missing API key — test failed (mock)', 2300);
  testProviderBtn.innerHTML=`<i data-lucide="plug" style="width:12px;height:12px"></i> Test`; refreshIcons();
});
providerForm?.addEventListener('submit', e=>{
  e.preventDefault();
  const id=provId.value.trim().toLowerCase(), name=provName.value.trim();
  if(!id||!name) return toast('Name and ID required');
  if(!editingProviderId && PROVIDERS.find(p=>p.id===id)) return toast('ID already exists');
  const data={
    id, name,
    apiKey:provKey.value.trim(),
    baseUrl:provBase.value.trim()||'https://api.example.com/v1',
    models: provModels.value.split(',').map(s=>s.trim()).filter(Boolean),
    temp: parseFloat(provTemp.value)||0.7,
    maxTokens: parseInt(provMax.value)||4096,
    streaming: !!provStream.checked,
    enabled: !!provEnabled.checked,
    latency: Math.floor(Math.random()*120+80)
  };
  if(!data.models.length) return toast('Add at least one model');
  if(editingProviderId){
    const idx=PROVIDERS.findIndex(p=>p.id===editingProviderId);
    // if id changed, handle
    data.id=editingProviderId; // keep original id to avoid breaking selectedProvider
    // allow name change but keep id
    PROVIDERS[idx]={...PROVIDERS[idx], ...data, id:editingProviderId};
    toast(`${name} updated`);
  } else {
    PROVIDERS.push(data);
    toast(`${name} added`);
  }
  saveProviders(); renderProviders(); renderProviderList(); providerModal.classList.add('hidden');
  if(data.enabled){ selectedProvider=data.id; selectedModel=data.models[0]; localStorage.setItem('provider', selectedProvider); localStorage.setItem('model', selectedModel); renderProviders(); renderMessages(); }
});

// Modes
function renderModes(){
  agentModesEl.innerHTML = MODES.map(m=>`<button class="mode-pill ${selectedMode===m.id?'active':''}" data-mode="${m.id}"><i data-lucide="${m.icon}" style="width:12px;height:12px"></i> ${m.id}</button>`).join('');
  refreshIcons();
}
agentModesEl.addEventListener('click', e=>{
  const b=e.target.closest('[data-mode]'); if(!b) return;
  selectedMode=b.dataset.mode; localStorage.setItem('agentMode', selectedMode); renderModes(); renderProviders();
  toast(`Agent mode: ${selectedMode}`);
});

// Chats
function getActiveChat(){ return chats.find(c=>c.id===activeChatId); }
function renderChats(){
  const q=(sidebarSearch.value||'').toLowerCase();
  let filtered = chats.filter(c=> showArchived ? true : !c.archived);
  if(q) filtered = filtered.filter(c=> c.title.toLowerCase().includes(q) || c.messages.some(m=>m.text?.toLowerCase().includes(q)));
  // group
  const groups = { Today:[], Yesterday:[], Earlier:[], Archived:[] };
  const now=Date.now();
  filtered.forEach(c=>{
    if(c.archived) groups.Archived.push(c);
    else if(now - c.created < 86400000) groups.Today.push(c);
    else if(now - c.created < 172800000) groups.Yesterday.push(c);
    else groups.Earlier.push(c);
  });
  chatCountEl.textContent = filtered.length;
  crumbTitle.textContent = getActiveChat()?.title || 'MCP Chat';
  chatListEl.innerHTML = Object.entries(groups).filter(([k,v])=>v.length).map(([g, arr])=>`
    <div><p class="group-title">${g.toUpperCase()}</p>
    ${arr.map(c=>`
      <div class="chat-item ${c.id===activeChatId?'active':''} ${c.archived?'archived':''}" data-id="${c.id}">
        <div class="chat-item-main"><b>${esc(c.title)}</b><p>${esc(c.messages[c.messages.length-1]?.text || c.messages[c.messages.length-1]?.html || 'No messages')}</p></div>
        <span class="chat-item-meta">${new Date(c.created).toLocaleDateString()}</span>
        <div class="chat-item-actions">
          <button class="icon-btn xs" data-act="more" data-id="${c.id}"><i data-lucide="more-horizontal" style="width:12px;height:12px"></i></button>
        </div>
      </div>
    `).join('')}
    </div>
  `).join('') || `<p style="font-size:12px;color:#94a3b8;padding:8px">No chats</p>`;
  refreshIcons();
}
chatListEl.addEventListener('click', e=>{
  const item=e.target.closest('.chat-item'); if(!item) return;
  const more=e.target.closest('[data-act="more"]');
  if(more){
    const id=more.dataset.id;
    const chat=chats.find(c=>c.id===id);
    const act=prompt(`Actions for "${chat.title}"\n1 rename\n2 export\n3 archive/unarchive\n4 delete\nType 1-4`);
    if(act==='1'){ const t=prompt('New title', chat.title); if(t){ pushHistory(); chat.title=t; saveChats(); renderChats(); renderMessages(); } }
    else if(act==='2') doExport(chat);
    else if(act==='3'){ chat.archived=!chat.archived; saveChats(); renderChats(); }
    else if(act==='4'){ if(confirm('Delete?')){ pushHistory(); chats=chats.filter(c=>c.id!==id); if(activeChatId===id) activeChatId=chats[0]?.id||null; saveChats(); if(activeChatId) localStorage.setItem('activeChat', activeChatId); renderChats(); renderMessages(); } }
    return;
  }
  activeChatId=item.dataset.id; localStorage.setItem('activeChat', activeChatId); renderChats(); renderMessages(); pushHistory();
});
newChatBtn.addEventListener('click', ()=> createChat());
newChatTopBtn.addEventListener('click', ()=> createChat());
function createChat(){
  pushHistory();
  const id=uid();
  chats.unshift({id, title:'New conversation', created:Date.now(), archived:false, messages:[]});
  activeChatId=id; saveChats(); localStorage.setItem('activeChat', id); renderChats(); renderMessages(); inputEl.focus();
}
function doExport(chat){
  const blob=new Blob([JSON.stringify(chat,null,2)], {type:'application/json'});
  const url=URL.createObjectURL(blob);
  const a=document.createElement('a'); a.href=url; a.download=`${chat.title}.json`; a.click(); URL.revokeObjectURL(url); toast('Exported');
}
convMenuBtn.addEventListener('click', ()=> convMenu.classList.toggle('hidden'));
document.addEventListener('click', e=>{ if(!e.target.closest('.dropdown')) convMenu.classList.add('hidden'); });
convMenu.addEventListener('click', e=>{
  const b=e.target.closest('[data-action]'); if(!b) return;
  const act=b.dataset.action;
  const chat=getActiveChat(); if(!chat) return;
  convMenu.classList.add('hidden');
  if(act==='rename'){ const t=prompt('Rename', chat.title); if(t){ pushHistory(); chat.title=t; saveChats(); renderChats(); renderMessages(); } }
  if(act==='export') doExport(chat);
  if(act==='duplicate'){ pushHistory(); const copy=JSON.parse(JSON.stringify(chat)); copy.id=uid(); copy.title=chat.title+' (copy)'; copy.created=Date.now(); chats.unshift(copy); activeChatId=copy.id; saveChats(); renderChats(); renderMessages(); toast('Duplicated'); }
  if(act==='archive'){ pushHistory(); chat.archived=!chat.archived; saveChats(); renderChats(); renderMessages(); toast(chat.archived?'Archived':'Unarchived'); }
  if(act==='delete'){ if(confirm('Delete conversation?')){ pushHistory(); chats=chats.filter(c=>c.id!==chat.id); activeChatId=chats[0]?.id||null; saveChats(); renderChats(); renderMessages(); toast('Deleted'); } }
});
importChatBtn.addEventListener('click', ()=> importFile.click());
importFile.addEventListener('change', e=>{
  const f=e.target.files[0]; if(!f) return;
  const r=new FileReader(); r.onload=()=>{ try{ const data=JSON.parse(r.result); if(data.title && data.messages){ pushHistory(); data.id=uid(); data.created=Date.now(); chats.unshift(data); activeChatId=data.id; saveChats(); renderChats(); renderMessages(); toast('Imported'); } else toast('Invalid file'); } catch{ toast('Invalid JSON'); } }; r.readAsText(f); e.target.value='';
});
archiveToggle.addEventListener('click', ()=>{ showArchived=!showArchived; archiveToggle.style.background=showArchived?'#0f172a':''; archiveToggle.style.color=showArchived?'#fff':''; renderChats(); toast(showArchived?'Showing archived':'Hiding archived'); });

// Servers render
function renderServers(){
  const q=(serverSearch.value||'').toLowerCase();
  const sq=(sidebarSearch.value||'').toLowerCase();
  topNodes.innerHTML = servers.map(s=>{
    const on=s.status==='connected';
    return `<button class="node-pill ${on?'connected':'disconnected'}" data-id="${s.id}"><span class="d" style="background:${on ? (s.color==='aws'?'#ff9900': s.color==='pg'?'#16a34a':'#2563eb') : '#cbd5e1'}"></span> ${esc(s.name)}</button>`;
  }).join('');
  const sideFiltered = servers.filter(s=> !sq || s.name.toLowerCase().includes(sq));
  sidebarList.innerHTML = sideFiltered.map(s=>{
    const on=s.status==='connected', busy=s.status==='connecting';
    return `<li class="server-item ${s.status}" data-id="${s.id}"><span class="server-ic ${s.color}"><i data-lucide="${s.color==='pg'?'database': s.color==='aws'?'cloud': s.color==='fs'?'folder-open':'server'}" style="width:16px;height:16px"></i></span><span class="server-meta"><strong>${esc(s.name)}</strong><span>${esc(s.transport)} • ${esc(s.endpoint.slice(0,28))}</span></span><span class="chip-dot ${on?'on':busy?'':'off'}"></span></li>`;
  }).join('') || `<li style="font-size:12px;color:#94a3b8;padding:8px">No matches</li>`;
  const filteredDrawer = servers.filter(s=> !q || s.name.toLowerCase().includes(q));
  serversList.innerHTML = filteredDrawer.map(s=>{
    const ic=s.color, iname=s.color==='pg'?'database': s.color==='aws'?'cloud': s.color==='fs'?'folder-open':'server';
    const label=s.status==='connected'?'Connected': s.status==='connecting'?'Connecting':'Disconnected';
    const cls=s.status==='connected'?'on': s.status==='connecting'?'busy':'off';
    return `<div class="server-card" data-id="${s.id}"><div class="server-card-top"><span class="ic ${ic}"><i data-lucide="${iname}" style="width:16px;height:16px"></i></span><span class="meta"><b>${esc(s.name)}</b><p>${esc(s.transport)} • ${esc(s.endpoint)}</p></span><label class="switch"><input type="checkbox" ${s.status==='connected'?'checked':''} data-toggle="${s.id}"><span></span></label></div><div class="server-card-foot"><span class="status ${cls}">${label}</span><span style="flex:1"></span><button class="mini" data-edit="${s.id}"><i data-lucide="pencil" style="width:12px;height:12px"></i> Edit</button><button class="mini danger" data-del="${s.id}"><i data-lucide="trash-2" style="width:12px;height:12px"></i></button></div></div>`;
  }).join('') || `<p style="text-align:center;color:#94a3b8;padding:12px">No servers</p>`;
  const connected=servers.filter(s=>s.status==='connected').length;
  sidebarCount.textContent=servers.length;
  if(activeCount2) activeCount2.textContent=connected;
  if(tunnelBar) tunnelBar.style.width=`${servers.length?Math.round(connected/servers.length*100):0}%`;
  if(tunnelStatus) tunnelStatus.innerHTML=connected? `Secure MCP tunnel • <strong>${connected}</strong> connected` : `No MCP nodes connected`;
  refreshIcons();
}

// Media
function addFiles(files){
  for(const f of files){
    if(pending.length>=6){ toast('Max 6 files'); break; }
    if(f.size>12*1024*1024){ toast(`${f.name} too large`); continue; }
    const id=uid(); const url=f.type.startsWith('image/')||f.type.startsWith('video/')?URL.createObjectURL(f):null;
    pending.push({id,file:f,url});
  }
  drawPreview(); syncComposer();
}
function drawPreview(){
  if(!pending.length){ attachPreview.classList.add('hidden'); attachPreview.innerHTML=''; return; }
  attachPreview.classList.remove('hidden');
  attachPreview.innerHTML=pending.map(p=>{
    let inner='';
    if(p.file.type.startsWith('image/')) inner=`<img src="${p.url}" alt="">`;
    else if(p.file.type.startsWith('video/')) inner=`<video src="${p.url}" muted></video>`;
    else inner=`<div class="fn"><i data-lucide="file"></i><span>${esc(p.file.name.slice(0,18))}</span><small>${(p.file.size/1024).toFixed(1)} KB</small></div>`;
    return `<div class="preview">${inner}<button class="rm" data-rm="${p.id}"><i data-lucide="x"></i></button></div>`;
  }).join(''); refreshIcons();
}
attachPreview.addEventListener('click', e=>{
  const b=e.target.closest('[data-rm]'); if(!b) return;
  const id=b.dataset.rm; const i=pending.findIndex(p=>p.id===id);
  if(i>=0){ if(pending[i].url) URL.revokeObjectURL(pending[i].url); pending.splice(i,1); drawPreview(); syncComposer(); }
});
mediaInput.addEventListener('change', e=>{ if(e.target.files.length) addFiles([...e.target.files]); e.target.value=''; });

// Todo draft
function renderTodo(){
  if(!todos.length && todoDraft.classList.contains('hidden')) return;
  todoList.innerHTML=todos.map((t,i)=>`<li><input type="checkbox" ${t.done?'checked':''} data-i="${i}"><input value="${esc(t.text)}" data-input="${i}"><button class="icon-btn xs" data-del="${i}"><i data-lucide="x" style="width:12px;height:12px"></i></button></li>`).join('');
  const done=todos.filter(t=>t.done).length;
  todoProgress.textContent=`${done}/${todos.length}`;
  refreshIcons();
}
todoToggle.addEventListener('click', ()=>{
  todoDraft.classList.toggle('hidden');
  if(!todoDraft.classList.contains('hidden')) todoInput.focus();
  refreshIcons();
});
clearTodo.addEventListener('click', ()=>{ todos=[]; todoDraft.classList.add('hidden'); renderTodo(); });
addTodoBtn.addEventListener('click', ()=>{
  const v=todoInput.value.trim(); if(!v) return;
  todos.push({text:v, done:false}); todoInput.value=''; renderTodo();
});
todoInput.addEventListener('keydown', e=>{ if(e.key==='Enter'){ e.preventDefault(); addTodoBtn.click(); }});
todoList.addEventListener('change', e=>{
  const c=e.target.closest('[data-i]'); if(c && e.target.type==='checkbox'){ todos[Number(c.dataset.i)].done=e.target.checked; renderTodo(); }
});
todoList.addEventListener('input', e=>{
  const inp=e.target.closest('[data-input]'); if(inp) todos[Number(inp.dataset.input)].text=inp.value;
});
todoList.addEventListener('click', e=>{
  const b=e.target.closest('[data-del]'); if(b){ todos.splice(Number(b.dataset.del),1); renderTodo(); }
});

// Composer
function autoResize(){ inputEl.style.height='auto'; inputEl.style.height=Math.min(inputEl.scrollHeight,120)+'px'; }
function syncComposer(){
  const hasText=inputEl.value.trim().length>0;
  const hasMedia=pending.length>0;
  sendBtn.disabled=(!hasText && !hasMedia && !todos.length) || isTyping;
  charCount.textContent=inputEl.value.length? `${inputEl.value.length}/2000` : '';
  charCount.style.color=inputEl.value.length>1800?'#ef4444':'#94a3b8';
  localStorage.setItem('mcp_draft', inputEl.value);
  // show prompt strip when empty
  if(!hasText && !hasMedia && !isTyping) promptStrip.classList.remove('hidden'); else promptStrip.classList.add('hidden');
}
const PROMPT_CHIPS = ["Summarize S3 buckets","Query users table","List /data files","Draft release notes","Explain TLS tunnel"];
function renderPromptStrip(){
  promptStrip.innerHTML = PROMPT_CHIPS.map(p=>`<button class="prompt-chip" data-prompt="${esc(p)}"><i data-lucide="sparkles"></i> ${esc(p)}</button>`).join('');
  refreshIcons();
}
promptStrip.addEventListener('click', e=>{
  const b=e.target.closest('[data-prompt]'); if(!b) return;
  inputEl.value=b.dataset.prompt; autoResize(); syncComposer(); inputEl.focus();
});
inputEl.value=localStorage.getItem('mcp_draft')||'';
autoResize(); syncComposer(); renderPromptStrip();

inputEl.addEventListener('input', ()=>{
  autoResize(); syncComposer();
  const pos=inputEl.selectionStart; const before=inputEl.value.slice(0,pos);
  const m=before.match(/(^|\s)(\/[a-z]*)$/);
  if(!m){ hideSlash(); return; }
  const q=m[2].toLowerCase();
  filtered=SLASH.filter(c=>c.key.startsWith(q));
  if(!filtered.length){ hideSlash(); return; }
  slashIndex=0; renderSlash();
});
function renderSlash(){
  slashMenu.innerHTML=filtered.map((c,i)=>`<div class="s-item ${i===slashIndex?'active':''}" data-i="${i}"><span class="s-key">${esc(c.key)}</span><span class="s-desc">${esc(c.desc)}</span><span class="s-hint">${esc(c.hint)}</span></div>`).join('');
  slashMenu.classList.remove('hidden');
}
function hideSlash(){ slashMenu.classList.add('hidden'); filtered=[]; }
function pickSlash(cmd){
  if(!cmd) return;
  const pos=inputEl.selectionStart; const before=inputEl.value.slice(0,pos); const after=inputEl.value.slice(pos);
  inputEl.value=before.replace(/\/[a-z]*$/, cmd.insert)+after;
  hideSlash(); autoResize(); syncComposer(); inputEl.focus();
  if(cmd.key==='/clear') clearCurrentChat();
  if(cmd.key==='/help') doSend('/help');
}
slashMenu.addEventListener('click', e=>{ const it=e.target.closest('.s-item'); if(it) pickSlash(filtered[Number(it.dataset.i)]); });
inputEl.addEventListener('keydown', e=>{
  if(!slashMenu.classList.contains('hidden')){
    if(e.key==='ArrowDown'){ e.preventDefault(); slashIndex=(slashIndex+1)%filtered.length; renderSlash(); }
    else if(e.key==='ArrowUp'){ e.preventDefault(); slashIndex=(slashIndex-1+filtered.length)%filtered.length; renderSlash(); }
    else if(e.key==='Enter'||e.key==='Tab'){ e.preventDefault(); pickSlash(filtered[slashIndex]); return; }
    else if(e.key==='Escape') hideSlash();
  }
  if(e.key==='Enter' && !e.shiftKey){
    if(!slashMenu.classList.contains('hidden') && filtered.length){ e.preventDefault(); pickSlash(filtered[slashIndex]); return; }
    e.preventDefault(); doSend();
  }
  if((e.ctrlKey||e.metaKey) && e.key==='z' && !e.shiftKey){ e.preventDefault(); undo(); }
  if((e.ctrlKey||e.metaKey) && (e.key==='y' || (e.key==='z' && e.shiftKey))){ e.preventDefault(); redo(); }
});
document.addEventListener('click', e=>{ if(!slashMenu.contains(e.target) && e.target!==inputEl) hideSlash(); });

// Speech
function initSpeech(){
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  if(!SR){ micBtn.title='Speech not supported in this browser (mock)'; return; }
  recognition=new SR();
  recognition.continuous=true; recognition.interimResults=true; recognition.lang='en-US';
  recognition.onstart=()=>{ isRecording=true; speechBar.classList.remove('hidden'); micBtn.classList.add('active'); };
  recognition.onend=()=>{ isRecording=false; speechBar.classList.add('hidden'); micBtn.classList.remove('active'); };
  recognition.onresult=(e)=>{
    let final='';
    for(let i=e.resultIndex;i<e.results.length;i++){ const r=e.results[i]; if(r.isFinal) final+=r[0].transcript; }
    if(final){ inputEl.value=(inputEl.value+' '+final).trim(); autoResize(); syncComposer(); }
  };
  recognition.onerror=()=>{ isRecording=false; speechBar.classList.add('hidden'); micBtn.classList.remove('active'); toast('Mic error'); };
}
initSpeech();
function toggleSpeech(){
  if(!recognition){
    // mock
    if(!isRecording){ isRecording=true; speechBar.classList.remove('hidden'); micBtn.classList.add('active'); toast('Listening (mock)… say something'); setTimeout(()=>{ if(isRecording){ inputEl.value=(inputEl.value+' list S3 buckets').trim(); autoResize(); syncComposer(); stopSpeechClick(); }}, 1800); }
    else stopSpeechClick();
    return;
  }
  if(isRecording) recognition.stop();
  else recognition.start();
}
function stopSpeechClick(){ if(recognition && isRecording) recognition.stop(); isRecording=false; speechBar.classList.add('hidden'); micBtn.classList.remove('active'); }
micBtn.addEventListener('click', toggleSpeech);
stopSpeech.addEventListener('click', stopSpeechClick);

// Drag & paste
['dragenter','dragover'].forEach(ev=> document.addEventListener(ev, e=>{ e.preventDefault(); if(ev==='dragenter') dropOverlay.classList.remove('hidden'); composerInner.classList.add('drag'); }));
['dragleave','drop'].forEach(ev=> document.addEventListener(ev, e=>{ if(ev==='dragleave' && e.target===dropOverlay) dropOverlay.classList.add('hidden'); if(ev==='drop') dropOverlay.classList.add('hidden'); composerInner.classList.remove('drag'); }));
document.addEventListener('drop', e=>{ e.preventDefault(); if(e.dataTransfer.files.length) addFiles([...e.dataTransfer.files]); });
document.addEventListener('paste', e=>{ const files=[...(e.clipboardData?.files||[])]; if(files.length) addFiles(files); });

// History undo/redo
function pushHistory(){
  const snap=JSON.stringify(chats);
  history.undo.push(snap);
  if(history.undo.length>30) history.undo.shift();
  history.redo=[];
  updateUndoRedo();
}
function updateUndoRedo(){
  undoBtn.disabled=history.undo.length===0;
  redoBtn.disabled=history.redo.length===0;
  undoBtn.style.opacity=history.undo.length?1:0.4;
  redoBtn.style.opacity=history.redo.length?1:0.4;
}
function undo(){
  if(!history.undo.length) return toast('Nothing to undo');
  const cur=JSON.stringify(chats);
  history.redo.push(cur);
  const prev=history.undo.pop();
  chats=JSON.parse(prev); saveChats(); renderChats(); renderMessages(); updateUndoRedo(); toast('Undone');
  activeChatId=chats.find(c=>c.id===activeChatId)?activeChatId:chats[0]?.id; if(activeChatId) localStorage.setItem('activeChat', activeChatId);
}
function redo(){
  if(!history.redo.length) return toast('Nothing to redo');
  const cur=JSON.stringify(chats);
  history.undo.push(cur);
  const nxt=history.redo.pop();
  chats=JSON.parse(nxt); saveChats(); renderChats(); renderMessages(); updateUndoRedo(); toast('Redone');
}
undoBtn.addEventListener('click', undo);
redoBtn.addEventListener('click', redo);
document.addEventListener('keydown', e=>{
  if((e.ctrlKey||e.metaKey) && e.key.toLowerCase()==='z' && !e.shiftKey){ e.preventDefault(); undo(); }
  if((e.ctrlKey||e.metaKey) && (e.key.toLowerCase()==='y' || (e.shiftKey && e.key.toLowerCase()==='z'))){ e.preventDefault(); redo(); }
});

// Messages rendering with tasks/todos/stream
function renderMessages(){
  const chat=getActiveChat();
  if(!chat){ messagesEl.innerHTML=`<div class="date-sep"><span>No conversation selected</span></div><div style="text-align:center;padding:24px"><button class="primary-btn" onclick="document.getElementById('newChatBtn').click()">Start new chat</button></div>`; return; }
  const title=chat.title;
  messagesEl.innerHTML=`<div class="date-sep"><span>${esc(title)} • ${chat.messages.length} messages • ${selectedModel}</span></div>` + chat.messages.map(m=>{
    if(m.role==='user'){
      return `<div class="message user" data-id="${m.id}">
        <div class="msg-head"><span class="who">You</span><span class="when">${m.time||''}</span>
          <button class="msg-action" data-edit="${m.id}"><i data-lucide="pencil" style="width:12px;height:12px"></i> Edit</button>
          <button class="msg-action" data-retry="${m.id}"><i data-lucide="refresh-cw" style="width:12px;height:12px"></i> Retry</button>
        </div>
        ${m.files? `<div class="user-media">${m.files.map(f=> f.type?.startsWith('image/')?`<img src="${f.url}" alt="">`: `<span class="file-chip">${esc(f.name)}</span>`).join('')}</div>`:''}
        ${m.todos && m.todos.length? `<div class="task-card"><div class="task-head"><b>Checklist</b><span>${m.todos.filter(t=>t.done).length}/${m.todos.length}</span></div><ul class="todo-list">${m.todos.map(t=>`<li class="todo-item ${t.done?'done':''}"><input type="checkbox" ${t.done?'checked':''} disabled><span>${esc(t.text)}</span></li>`).join('')}</ul></div>`:''}
        <div class="bubble">${esc(m.text).replace(/\n/g,'<br>')}</div>
      </div>`;
    } else {
      // assistant
      const html = m.html==='pg'? pgHTML(m) : m.html==='aws'? awsHTML() : m.html==='fs'? fsHTML() : generalHTML(m.text||'');
      const tasks = m.tasks? `<div class="task-card"><div class="task-head"><b><i data-lucide="list-checks" style="width:14px;height:14px"></i> Tasks</b><span>${m.tasks.filter(t=>t.done).length}/${m.tasks.length} done</span><div class="task-actions"><button class="accept" data-accept="${m.id}">Accept all</button><button class="cancel" data-cancel="${m.id}">Cancel</button></div></div><ul class="todo-list">${m.tasks.map(t=>`<li class="todo-item ${t.done?'done':''}"><input type="checkbox" ${t.done?'checked':''} data-task="${m.id}:${t.id}"><span>${esc(t.text)}</span><span style="margin-left:auto;font-size:11px;color:${t.status==='accepted'?'#16a34a': t.status==='cancelled'?'#ef4444':'#94a3b8'}">${t.status||''}</span></li>`).join('')}</ul></div>` : '';
      const todosHtml = m.todos? `<div class="task-card"><div class="task-head"><b>Todos</b><span>${m.todos.filter(t=>t.done).length}/${m.todos.length}</span></div><ul class="todo-list">${m.todos.map((t,i)=>`<li class="todo-item ${t.done?'done':''}"><input type="checkbox" ${t.done?'checked':''} data-todo="${m.id}:${i}"><span>${esc(t.text)}</span></li>`).join('')}</ul></div>` : '';
      return `<div class="message assistant" data-id="${m.id}">
        <div class="msg-head"><span class="who"><i data-lucide="bot" style="width:14px;height:14px"></i> ${esc(selectedModel)} • ${esc(selectedMode)}</span><span class="badge">${m.status||'done'}</span><span class="when">${m.time||''}</span>
          <button class="msg-action" data-copy="${m.id}"><i data-lucide="copy" style="width:12px;height:12px"></i> Copy</button>
          <button class="msg-action" data-retry="${m.id}"><i data-lucide="refresh-cw" style="width:12px;height:12px"></i> Retry</button>
          <button class="msg-action danger" data-delete="${m.id}"><i data-lucide="trash-2" style="width:12px;height:12px"></i></button>
        </div>
        <div class="stream-wrap" id="stream-${m.id}">${html}${tasks}${todosHtml}</div>
        <div class="message-assist-actions">
          <button class="msg-action" data-tts="${m.id}"><i data-lucide="volume-2" style="width:12px;height:12px"></i> Speak</button>
          <button class="msg-action" data-exportmsg="${m.id}"><i data-lucide="download" style="width:12px;height:12px"></i> Export</button>
        </div>
      </div>`;
    }
  }).join('') || `<div style="text-align:center;color:#94a3b8;padding:40px"><i data-lucide="message-square" style="width:32px;height:32px;margin:0 auto;display:block;color:#cbd5e1"></i><p style="margin:8px 0 0">No messages yet</p><p style="font-size:12px">Try a prompt below or type “/”</p></div>`;
  refreshIcons(); scrollBottom();
}
function clearCurrentChat(){
  pushHistory();
  const chat=getActiveChat(); if(chat){ chat.messages=[]; saveChats(); renderMessages(); toast('Cleared'); }
}

function pgHTML(m){
  // if streaming not done, show skeleton; but for rendered after done, full table
  return `<div class="card"><div class="card-head"><h4>Retrieved 5 records from <code>remote_db.users</code></h4><p>Parsing metadata • 184ms • TLS 1.3 • ${esc(selectedModel)}</p></div><div class="table-wrap"><table><thead><tr><th>ID</th><th>USERNAME</th><th>REGISTERED</th></tr></thead><tbody><tr><td>101</td><td>k_morgan</td><td>2023-10-21</td></tr><tr><td>102</td><td>j_doe</td><td>2023-10-21</td></tr><tr><td>103</td><td>m_smith</td><td>2023-10-22</td></tr><tr><td>104</td><td>a_wilson</td><td>2023-10-22</td></tr><tr><td>105</td><td>s_chen</td><td>2023-10-23</td></tr></tbody></table></div><div class="card-foot"><button class="chip" data-copy-table><i data-lucide="copy" style="width:12px;height:12px"></i> Copy</button><button class="chip"><i data-lucide="download" style="width:12px;height:12px"></i> CSV</button><span class="tls">✓ TLS 1.3</span></div></div>`;
}
function awsHTML(){ return `<div class="card"><div class="card-head"><h4>Found 3 buckets in <code>us-east-1</code></h4><p>via aws-connector • 184ms</p></div><div class="table-wrap"><table><thead><tr><th>BUCKET</th><th>SIZE</th><th>OBJECTS</th></tr></thead><tbody><tr><td>mcp-app-data</td><td>2.4 GB</td><td>342</td></tr><tr><td>user-avatars-prod</td><td>18.7 GB</td><td>12.4k</td></tr><tr><td>logs-archive-2023</td><td>412 GB</td><td>89k</td></tr></tbody></table></div></div>`; }
function fsHTML(){ return `<div class="card"><div class="card-head"><h4>Listed 5 entries in <code>/data</code></h4><p>read-only mount</p></div><div class="table-wrap"><table><thead><tr><th>NAME</th><th>TYPE</th><th>SIZE</th></tr></thead><tbody><tr><td>config.json</td><td>file</td><td>1.2 KB</td></tr><tr><td>datasets/</td><td>dir</td><td>—</td></tr><tr><td>users.csv</td><td>file</td><td>4.8 KB</td></tr><tr><td>README.md</td><td>file</td><td>2.1 KB</td></tr><tr><td>exports/</td><td>dir</td><td>—</td></tr></tbody></table></div></div>`; }
function generalHTML(t){ return `<div class="card"><div class="card-head"><h4>MCP Router • ${esc(selectedMode)}</h4><p>Received: <code>${esc((t||'').slice(0,80))}</code></p></div><div style="padding:12px"><p style="margin:0 0 8px;font-size:13px">Try <code>/sql</code>, <code>/s3</code>, <code>/fs</code> or upload media.</p><div style="background:#eff6ff;border:1px solid #dbeafe;border-radius:10px;padding:10px;font-size:12px">Suggested: <code>/sql SELECT * FROM users LIMIT 5</code> • <code>/s3 list buckets</code></div></div></div>`; }

// Message actions delegation
messagesEl.addEventListener('click', e=>{
  const edit=e.target.closest('[data-edit]'); if(edit){
    const id=edit.dataset.edit;
    const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===id);
    if(msg){ editingMsgId=id; editMsgInput.value=msg.text; editMsgModal.classList.remove('hidden'); refreshIcons(); }
    return;
  }
  const del=e.target.closest('[data-delete]'); if(del){
    const id=del.dataset.delete;
    const chat=getActiveChat();
    pushHistory();
    chat.messages=chat.messages.filter(m=>m.id!==id);
    saveChats(); renderMessages(); toast('Deleted');
    return;
  }
  const retry=e.target.closest('[data-retry]'); if(retry){
    const id=retry.dataset.retry;
    const chat=getActiveChat(); const idx=chat.messages.findIndex(m=>m.id===id);
    if(idx>=0){
      const userMsg=chat.messages[idx].role==='user' ? chat.messages[idx] : chat.messages[idx-1];
      if(userMsg && userMsg.role==='user'){ doSend(userMsg.text, userMsg.files||[], userMsg.todos||[], true); }
    }
    return;
  }
  const copy=e.target.closest('[data-copy]'); if(copy){
    const id=copy.dataset.copy;
    const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===id);
    if(msg){ const txt= msg.html==='pg' ? 'pg table' : (msg.text||''); navigator.clipboard.writeText(txt); toast('Copied'); }
    return;
  }
  const copyTbl=e.target.closest('[data-copy-table]'); if(copyTbl){
    const tbl=copyTbl.closest('.card')?.querySelector('table');
    if(tbl){ const t=[...tbl.rows].map(r=>[...r.cells].map(c=>c.textContent).join('\t')).join('\n'); navigator.clipboard.writeText(t); toast('Copied table'); }
    return;
  }
  const tts=e.target.closest('[data-tts]'); if(tts){
    const id=tts.dataset.tts;
    const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===id);
    if(msg && 'speechSynthesis' in window){
      const u=new SpeechSynthesisUtterance(msg.text||'Hello from MCP');
      speechSynthesis.speak(u); toast('Speaking…');
    } else toast('TTS not supported');
    return;
  }
  const exp=e.target.closest('[data-exportmsg]'); if(exp){
    const id=exp.dataset.exportmsg;
    const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===id);
    const blob=new Blob([JSON.stringify(msg,null,2)],{type:'application/json'});
    const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download=`msg-${id}.json`; a.click(); URL.revokeObjectURL(url); toast('Exported message');
    return;
  }
  const accept=e.target.closest('[data-accept]'); if(accept){
    const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===accept.dataset.accept);
    if(msg && msg.tasks){ msg.tasks.forEach(t=> t.status='accepted'); saveChats(); renderMessages(); toast('All tasks accepted ✓'); }
    return;
  }
  const cancel=e.target.closest('[data-cancel]'); if(cancel){
    const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===cancel.dataset.cancel);
    if(msg && msg.tasks){ msg.tasks.forEach(t=> t.status='cancelled'); saveChats(); renderMessages(); toast('Tasks cancelled'); }
    return;
  }
});
messagesEl.addEventListener('change', e=>{
  const task=e.target.closest('[data-task]'); if(task){
    const [mid,tid]=task.dataset.task.split(':');
    const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===mid);
    const t=msg.tasks.find(x=>x.id===tid); t.done=e.target.checked; saveChats(); renderMessages();
  }
  const todo=e.target.closest('[data-todo]'); if(todo){
    const [mid,i]=todo.dataset.todo.split(':');
    const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===mid);
    msg.todos[Number(i)].done=e.target.checked; saveChats(); renderMessages();
  }
});
closeEditMsg.addEventListener('click', ()=> editMsgModal.classList.add('hidden'));
cancelEdit.addEventListener('click', ()=> editMsgModal.classList.add('hidden'));
editMsgBackdrop.addEventListener('click', ()=> editMsgModal.classList.add('hidden'));
saveEdit.addEventListener('click', ()=>{
  const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===editingMsgId);
  if(msg){
    pushHistory();
    msg.text=editMsgInput.value;
    saveChats(); renderMessages(); editMsgModal.classList.add('hidden');
    // resend
    doSend(msg.text, msg.files||[], msg.todos||[], true);
  }
});

// Streaming
function streamHTML(targetId, html, doneText){
  const el=document.getElementById(targetId);
  if(!el) return;
  if(!streamToggle.checked){
    el.innerHTML=html; refreshIcons(); return;
  }
  el.innerHTML='';
  let i=0;
  const chunk=2;
  const iv=setInterval(()=>{
    el.innerHTML = html.slice(0,i) + '<span class="stream-cursor"></span>';
    i+=chunk;
    scrollBottom();
    if(i>=html.length){ clearInterval(iv); el.innerHTML=html; refreshIcons(); }
  }, 12);
}

// Do send with todo, files, stream
async function doSend(rawText, rawFiles, rawTodos, isRetry){
  let text = (rawText ?? inputEl.value).trim();
  const files=[... (rawFiles ?? pending)];
  const curTodos = rawTodos ?? (todos.length? [...todos] : []);
  if(!text && !files.length && !curTodos.length) return;
  if(isTyping) return;
  if(text==='/clear'){ clearCurrentChat(); inputEl.value=''; pending=[]; todos=[]; todoDraft.classList.add('hidden'); drawPreview(); syncComposer(); hideSlash(); return; }
  if(text==='/help'){ await sendAndStream(text, files, curTodos, 'general'); return; }
  // todo command
  if(text.startsWith('/todo ')){ const t=text.slice(6).trim(); if(t){ todos.push({text:t, done:false}); renderTodo(); todoDraft.classList.remove('hidden'); toast('Todo added'); } inputEl.value=''; syncComposer(); hideSlash(); autoResize(); return; }
  if(text.startsWith('/connect ') || text.startsWith('/disconnect ')){
    const isCon=text.startsWith('/connect ');
    const name=text.slice(isCon?9:12).trim();
    const s=servers.find(x=>x.name===name);
    if(s) await toggle(s.id, isCon);
    else toast(`Unknown server ${name}`);
    inputEl.value=''; hideSlash(); syncComposer(); autoResize(); return;
  }
  const tool=detect(text);
  const need={pg:'postgres-mcp', aws:'aws-us-east', fs:'local-fs'}[tool];
  if(need){
    const srv=servers.find(s=>s.name===need);
    if(srv && srv.status!=='connected'){ toast(`${need} disconnected — connecting…`); await toggle(srv.id,true); }
  }
  await sendAndStream(text, files, curTodos, tool, isRetry);
}

async function sendAndStream(text, files, curTodos, tool, isRetry){
  const chat=getActiveChat();
  if(!chat){ toast('Create a chat first'); return; }
  pushHistory();
  const time=new Date().toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'});
  // if not retry, add user message
  if(!isRetry){
    const um={id:uid(), role:'user', text, time, files: files.map(f=>({name:f.file.name, url:f.url, type:f.file.type})), todos: curTodos.length? curTodos : undefined};
    chat.messages.push(um);
    if(chat.messages.length===1) chat.title=text.slice(0,32)||'New chat';
  }
  // prepare assistant placeholder
  const aid=uid();
  const placeholder={id:aid, role:'assistant', html: tool==='pg'?'pg': tool==='aws'?'aws': tool==='fs'?'fs':'general', text, time, status:'streaming', tasks: tool==='pg'? [{id:'t1', text:'Verify table schema', done:true},{id:'t2', text:'Run SELECT', done:true},{id:'t3', text:'Format table', done:false}] : null, todos: curTodos.length && tool!=='pg'? curTodos.map((t,i)=>({text:t.text, done:t.done})) : null};
  chat.messages.push(placeholder);
  // clear composer if not retry
  if(!isRetry){
    pending.forEach(p=>{}); pending=[]; todos=[]; todoDraft.classList.add('hidden'); drawPreview();
    inputEl.value=''; hideSlash(); autoResize(); syncComposer();
  }
  saveChats(); renderChats(); renderMessages();
  // now stream
  isTyping=true; syncComposer();
  const targetId=`stream-${aid}`;
  const htmlRaw = tool==='pg'? pgHTML(placeholder) : tool==='aws'? awsHTML() : tool==='fs'? fsHTML() : generalHTML(text);
  // tiny delay to show typing
  await new Promise(r=>setTimeout(r, 300));
  // remove typing if any (we use streaming, not typing dots)
  streamHTML(targetId, htmlRaw);
  await new Promise(r=>setTimeout(r, htmlRaw.length*8 + 400));
  // finalize
  placeholder.status='done';
  if(placeholder.tasks) placeholder.tasks.forEach(t=> t.done=true);
  saveChats(); renderChats(); renderMessages();
  isTyping=false; syncComposer();
  // TTS if enabled
  if(ttsToggle.checked && 'speechSynthesis' in window){
    const u=new SpeechSynthesisUtterance(text.slice(0,120));
    speechSynthesis.speak(u);
  }
  scrollBottom();
}

function detect(q){
  const s=(q||'').toLowerCase();
  const has=(...k)=>k.some(v=>s.includes(v));
  if(has('postgres','sql','user','select','/sql')) return 'pg';
  if(has('aws','s3','bucket','/s3')) return 'aws';
  if(has('file','fs','/data','/fs')) return 'fs';
  return 'general';
}

// Marketplace — Discover (enhanced)
function renderMarket(){
  const q=(marketSearch.value||'').toLowerCase();
  const cat=marketFilter.value;
  const sort=marketSort.value;
  let filtered=MARKET.filter(m=>{
    const matchSearch = !q || m.name.toLowerCase().includes(q) || m.desc.toLowerCase().includes(q) || m.tags.some(t=>t.includes(q)) || m.author.toLowerCase().includes(q);
    const matchCat = cat==='all' || m.cat===cat;
    const matchTab = activeMarketTab==='all' || (activeMarketTab==='featured'&&m.featured) || (activeMarketTab==='trending'&&m.trending) || (activeMarketTab==='new'&&m.isNew) || (activeMarketTab==='installed'&&m.installed);
    return matchSearch && matchCat && matchTab;
  });
  // sort
  if(sort==='popular') filtered.sort((a,b)=> b.installs - a.installs);
  else if(sort==='rating') filtered.sort((a,b)=> b.stars - a.stars);
  else if(sort==='new') filtered.sort((a,b)=> new Date(b.updated)-new Date(a.updated));
  else if(sort==='name') filtered.sort((a,b)=> a.name.localeCompare(b.name));
  // tabs UI
  if(marketTabs){
    marketTabs.querySelectorAll('button').forEach(b=> b.classList.toggle('active', b.dataset.tab===activeMarketTab));
    marketTabs.querySelectorAll('button').forEach(b=>{
      if(b.dataset.tab===activeMarketTab){ b.style.background='var(--text)'; b.style.color='#fff'; b.style.borderColor='var(--text)'; }
      else { b.style.background='#fff'; b.style.color='var(--muted)'; b.style.borderColor='var(--line)'; }
    });
  }
  if(marketCount) marketCount.textContent = `${filtered.length} servers`;
  if(marketInstalledCount) marketInstalledCount.textContent = `${MARKET.filter(m=>m.installed).length} installed`;
  marketGrid.innerHTML = filtered.map(m=>{
    const installs = m.installs>=1000 ? (m.installs/1000).toFixed(1)+'k' : String(m.installs);
    return `
    <div class="mkt-card ${m.featured?'featured':''}" data-detail="${m.id}">
      <div class="mkt-head"><span class="ic" style="background:${m.color}"><i data-lucide="${m.icon}" style="width:16px;height:16px"></i></span>
        <div style="flex:1;min-width:0"><b>${esc(m.name)} ${m.verified?'<span title="Verified" style="background:#eff6ff;color:var(--primary);border:1px solid #dbeafe;padding:1px 5px;border-radius:999px;font-size:9px">✓</span>':''}</b>
        <p>${esc(m.author)} • v${esc(m.version)} • ${esc(m.cat)}</p></div>
        <span style="font-size:11px;background:#fff;border:1px solid var(--line);padding:4px 8px;border-radius:999px;font-weight:700">${m.stars} ★</span>
      </div>
      <p class="mkt-desc">${esc(m.desc)}</p>
      <div class="mkt-meta">${m.tags.map(t=>`<span class="mkt-tag">${esc(t)}</span>`).join('')}</div>
      <div class="mkt-foot">
        <span class="installs"><i data-lucide="download" style="width:11px;height:11px"></i> ${installs}</span>
        <span style="font-size:11px;color:var(--faint);font-family:var(--mono)">${new Date(m.updated).toLocaleDateString()}</span>
        <button class="${m.installed?'ghost-btn sm':'primary-btn sm'}" data-install="${m.id}" style="margin-left:auto" onclick="event.stopPropagation()">${m.installed?'Installed':'Install'}</button>
      </div>
      ${m.installing?`<div class="install-progress"><span style="width:${m.progress||0}%"></span></div>`:''}
    </div>`;
  }).join('') || `<div style="grid-column:1/-1;text-align:center;padding:28px;background:#fff;border:1px dashed var(--line);border-radius:14px"><i data-lucide="search-x" style="width:28px;height:28px;color:#cbd5e1"></i><p style="margin:8px 0 0;font-weight:700">No servers found</p><p style="margin:4px 0 0;font-size:12px;color:var(--muted)">Try adjusting search, category or tab</p><button class="ghost-btn sm" onclick="document.getElementById('marketSearch').value=''; document.getElementById('marketFilter').value='all'; document.getElementById('activeMarketTab')&& (activeMarketTab='all'); renderMarket();" style="margin-top:10px">Clear filters</button></div>`;
  refreshIcons();
}
function openMarketDetail(id){
  const m=MARKET.find(x=>x.id===id); if(!m) return;
  const installs = m.installs>=1000 ? (m.installs/1000).toFixed(1)+'k' : String(m.installs);
  marketDetailContent.innerHTML = `
    <div class="market-detail-hero">
      <div style="display:flex;gap:12px;align-items:center">
        <span class="ic" style="background:${m.color};width:48px;height:48px;border-radius:12px;display:grid;place-items:center;color:#fff"><i data-lucide="${m.icon}" style="width:22px;height:22px"></i></span>
        <div><h3 style="margin:0;font-size:18px;font-weight:800">${esc(m.name)} ${m.verified?'<span style="background:#22c55e;color:#fff;padding:2px 6px;border-radius:999px;font-size:10px">Verified</span>':''}</h3><p style="margin:4px 0 0;font-size:12px;opacity:.8">${esc(m.author)} • v${esc(m.version)} • updated ${new Date(m.updated).toLocaleDateString()} • ${installs} installs</p></div>
        <button class="icon-btn" id="closeMarketDetail" style="margin-left:auto;background:rgba(255,255,255,.12);color:#fff"><i data-lucide="x"></i></button>
      </div>
      <p style="margin:12px 0 0;font-size:13px;line-height:1.6;opacity:.9">${esc(m.desc)}</p>
      <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:10px">${m.tags.map(t=>`<span style="background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.14);padding:4px 8px;border-radius:999px;font-size:11px;font-family:var(--mono)">${esc(t)}</span>`).join('')}</div>
    </div>
    <div class="market-detail-body">
      <div style="display:flex;gap:8px"><span style="font-size:12px;font-weight:700;background:#f1f5f9;padding:6px 10px;border-radius:999px;border:1px solid var(--line)"><i data-lucide="star" style="width:12px;height:12px;color:#f59e0b"></i> ${m.stars} rating</span><span style="font-size:12px;background:#fff;border:1px solid var(--line);padding:6px 10px;border-radius:999px">${esc(m.cat)} • ${m.tools.length} tools</span><span style="margin-left:auto;font-size:11px;color:var(--muted);font-family:var(--mono)">${m.installs} installs</span></div>
      <div>
        <h4 style="margin:0 0 8px;font-size:12px;letter-spacing:.06em;color:var(--muted)">TOOLS</h4>
        <ul class="market-tools">${m.tools.map(t=>`<li><i data-lucide="wrench" style="width:12px;height:12px;color:var(--primary)"></i> ${esc(t)} <span style="margin-left:auto;font-size:11px;color:var(--faint)">via ${esc(m.name)}</span></li>`).join('')}</ul>
      </div>
      <div style="background:#0f172a;color:#e2e8f0;border-radius:10px;padding:12px;font-family:var(--mono);font-size:11px;line-height:1.6">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px"><span style="font-weight:700;letter-spacing:.06em;color:#93c5fd">INSTALL</span><button class="ghost-btn sm" id="copyInstall" style="background:#1e293b;color:#e2e8f0;border-color:#334155;padding:4px 8px;font-size:11px"><i data-lucide="copy" style="width:12px;height:12px"></i> Copy</button></div>
        <div>npx @modelcontextprotocol/server-${esc(m.name.toLowerCase().replace(/\s/g,'-'))}</div>
        <div style="color:#86efac">wss://${esc(m.name.toLowerCase().replace(/\s/g,''))}.mcp.local/sse</div>
      </div>
      <div style="display:flex;gap:8px">
        <button class="${m.installed?'ghost-btn':'primary-btn'}" id="detailInstall" data-install="${m.id}" style="flex:1;justify-content:center">${m.installed?'<i data-lucide="check" style="width:14px;height:14px"></i> Installed':'<i data-lucide="download" style="width:14px;height:14px"></i> Install'}</button>
        <button class="ghost-btn" id="detailConnect" ${!m.installed?'disabled':''} style="flex:1;justify-content:center"><i data-lucide="plug" style="width:14px;height:14px"></i> ${m.installed?'Connect':'Install first'}</button>
      </div>
      ${m.installing?`<div class="install-progress"><span style="width:${m.progress||0}%"></span></div><p style="font-size:11px;color:var(--muted);text-align:center;margin:4px 0 0">${m.progress}% installing…</p>`:''}
    </div>
  `;
  marketDetailModal.classList.remove('hidden');
  refreshIcons();
  // detail listeners
  $('#closeMarketDetail')?.addEventListener('click', ()=> marketDetailModal.classList.add('hidden'));
  $('#copyInstall')?.addEventListener('click', ()=>{ navigator.clipboard.writeText(`npx @modelcontextprotocol/server-${m.name.toLowerCase().replace(/\s/g,'-')}`); toast('Copied install command'); });
  $('#detailInstall')?.addEventListener('click', e=>{
    const id=e.currentTarget.dataset.install;
    handleInstall(id);
    // update detail button
    setTimeout(()=> openMarketDetail(id), 400);
  });
  $('#detailConnect')?.addEventListener('click', ()=>{
    if(!m.installed) return toast('Install first');
    // find server and connect
    const srv=servers.find(s=> s.name===m.name.toLowerCase().replace(/\s/g,'-') || s.name===m.name);
    if(srv) toggle(srv.id, true);
    else toast('Server not found — installing…');
    marketDetailModal.classList.add('hidden');
  });
}
async function handleInstall(id){
  const m=MARKET.find(x=>x.id===id);
  if(!m || m.installed) { if(m?.installed) toast(`${m.name} already installed`); return; }
  m.installing=true; m.progress=0; renderMarket();
  // simulate progress
  for(let p=0; p<=100; p+= 20){
    await new Promise(r=>setTimeout(r, 180));
    m.progress=p; renderMarket();
    if(marketDetailModal && !marketDetailModal.classList.contains('hidden')) {
      const bar=marketDetailContent.querySelector('.install-progress span');
      if(bar) bar.style.width=p+'%';
    }
  }
  m.installing=false; m.progress=100; m.installed=true; saveMarket();
  const nid='srv'+Date.now();
  const col=m.cat==='db'?'pg': m.cat==='cloud'?'aws': m.cat==='ai'?'pg' : m.cat==='dev'?'custom':'fs';
  servers.push({id:nid, name:m.name.toLowerCase().replace(/\s/g,'-'), transport:'sse', endpoint:`wss://${m.name.toLowerCase().replace(/\s/g,'')}.mcp.local/sse`, tools:m.tools.join(', '), status:'disconnected', color:col});
  saveServers(); renderServers();
  renderMarket(); toast(`${m.name} installed — connect in MCP servers ✓`);
}
marketSearch.addEventListener('input', renderMarket);
marketFilter.addEventListener('change', renderMarket);
marketSort?.addEventListener('change', renderMarket);
marketTabs?.addEventListener('click', e=>{
  const b=e.target.closest('[data-tab]'); if(!b) return;
  activeMarketTab=b.dataset.tab;
  localStorage.setItem('marketTab', activeMarketTab);
  renderMarket();
});
refreshMarket?.addEventListener('click', ()=>{ // mock refresh
  refreshMarket.innerHTML=`<i data-lucide="loader-2" style="width:11px;height:11px"></i> Syncing…`;
  refreshIcons();
  setTimeout(()=>{ refreshMarket.innerHTML=`<i data-lucide="refresh-cw" style="width:11px;height:11px"></i> Refresh`; refreshIcons(); toast('Registry synced • 10 servers'); renderMarket(); }, 900);
});
document.getElementById('marketInfoBtn')?.addEventListener('click', ()=> toast('Visit registry.modelcontextprotocol.io'));
marketGrid.addEventListener('click', e=>{
  const card=e.target.closest('[data-detail]');
  const btn=e.target.closest('[data-install]');
  if(btn){
    e.stopPropagation();
    handleInstall(btn.dataset.install);
    return;
  }
  if(card) openMarketDetail(card.dataset.detail);
});
marketDetailBackdrop?.addEventListener('click', ()=> marketDetailModal.classList.add('hidden'));
document.getElementById('closeMarketplace')?.addEventListener('click', ()=> document.getElementById('marketplaceView').classList.add('hidden'));

// Prompts
const promptCatsEl=promptCats, promptGridEl=promptGrid;
let activePromptCat='All';
function renderPromptCats(){
  const cats=['All', ...new Set(PROMPTS.map(p=>p.cat))];
  promptCatsEl.innerHTML=cats.map(c=>`<button class="${activePromptCat===c?'active':''}" data-cat="${c}">${c}</button>`).join('');
}
function renderPromptGrid(){
  const filtered= activePromptCat==='All'? PROMPTS : PROMPTS.filter(p=>p.cat===activePromptCat);
  promptGridEl.innerHTML=filtered.map(p=>`
    <div class="prompt-item" data-prompt="${esc(p.prompt)}">
      <b><i data-lucide="${p.icon}" style="width:14px;height:14px"></i> ${esc(p.title)}</b>
      <p>${esc(p.prompt)}</p>
      <button class="primary-btn sm use">Use prompt</button>
    </div>
  `).join('');
  refreshIcons();
}
promptCatsEl.addEventListener('click', e=>{
  const b=e.target.closest('[data-cat]'); if(!b) return;
  activePromptCat=b.dataset.cat; renderPromptCats(); renderPromptGrid();
});
promptGridEl.addEventListener('click', e=>{
  const card=e.target.closest('.prompt-item'); if(!card) return;
  inputEl.value=card.dataset.prompt; autoResize(); syncComposer();
  promptsView.classList.add('hidden'); toast('Prompt inserted');
  inputEl.focus();
});

// Profile
userProfileCard.addEventListener('click', ()=>{ profileView.classList.remove('hidden'); refreshIcons(); });
profileBackdrop.addEventListener('click', ()=> profileView.classList.add('hidden'));
closeProfile.addEventListener('click', ()=> profileView.classList.add('hidden'));
changeAvatar.addEventListener('click', ()=> avatarInput.click());
avatarInput.addEventListener('change', e=>{
  const f=e.target.files[0]; if(!f) return;
  const url=URL.createObjectURL(f);
  profileAvatar.src=url; profileLargeAvatar.src=url; toast('Avatar updated');
});
saveProfileBtn.addEventListener('click', ()=>{
  profileName.textContent=profileNameInput.value;
  const role=$('#profileRoleSelect').value;
  profileRole.textContent=`${role} • ${selectedModel} • ${selectedMode} mode`;
  localStorage.setItem('profileName', profileNameInput.value);
  localStorage.setItem('profileEmail', profileEmail.value);
  profileView.classList.add('hidden'); toast('Profile saved');
});
themeSelect.addEventListener('change', e=>{
  if(e.target.value==='dark'){ document.documentElement.style.filter='invert(1) hue-rotate(180deg)'; document.body.style.background='#0f172a'; toast('Dark preview — invert filter'); }
  else { document.documentElement.style.filter=''; document.body.style.background=''; toast('Light theme'); }
});

// Servers drawer events
function openSettings(){ settingsView.classList.remove('hidden'); refreshIcons(); }
function closeSettingsView(){ settingsView.classList.add('hidden'); }
serversBtn.addEventListener('click', openSettings);
settingsBtn.addEventListener('click', openSettings);
marketplaceBtn.addEventListener('click', ()=>{ marketplaceView.classList.remove('hidden'); renderMarket(); refreshIcons(); });
promptLibraryBtn.addEventListener('click', ()=>{ promptsView.classList.remove('hidden'); renderPromptCats(); renderPromptGrid(); refreshIcons(); });
showPromptsLink.addEventListener('click', ()=> promptLibraryBtn.click());
closeMarketplace.addEventListener('click', ()=> marketplaceView.classList.add('hidden'));
marketplaceBackdrop.addEventListener('click', ()=> marketplaceView.classList.add('hidden'));
promptsBackdrop.addEventListener('click', ()=> promptsView.classList.add('hidden'));
closePrompts.addEventListener('click', ()=> promptsView.classList.add('hidden'));
settingsBackdrop.addEventListener('click', closeSettingsView);
closeSettings.addEventListener('click', closeSettingsView);
serverSearch.addEventListener('input', renderServers);
sidebarSearch.addEventListener('input', ()=>{ renderChats(); renderServers(); });
addServerBtn.addEventListener('click', ()=>{ editingId=null; serverModalTitle.textContent='Add MCP Server'; srvName.value=''; srvEndpoint.value=''; srvTools.value=''; srvTransport.value='sse'; serverModal.classList.remove('hidden'); refreshIcons(); });
addServerSidebar.addEventListener('click', ()=> addServerBtn.click());
closeServerModalBtn.addEventListener('click', ()=> serverModal.classList.add('hidden'));
cancelServer.addEventListener('click', ()=> serverModal.classList.add('hidden'));
serverModalBackdrop.addEventListener('click', ()=> serverModal.classList.add('hidden'));
serverForm.addEventListener('submit', e=>{
  e.preventDefault();
  const name=srvName.value.trim(), ep=srvEndpoint.value.trim();
  if(!name||!ep) return;
  if(editingId){ const s=servers.find(x=>x.id===editingId); s.name=name; s.transport=srvTransport.value; s.endpoint=ep; s.tools=srvTools.value; toast(`${name} updated`); }
  else { const id='srv'+Date.now(); const col=name.includes('postgres')?'pg': name.includes('aws')?'aws': name.includes('fs')?'fs':'custom'; servers.push({id,name,transport:srvTransport.value,endpoint:ep,tools:srvTools.value,status:'disconnected',color:col}); toast(`${name} added`); }
  saveServers(); renderServers(); serverModal.classList.add('hidden');
});
topNodes.addEventListener('click', e=>{ const p=e.target.closest('[data-id]'); if(!p) return; toggle(p.dataset.id); });
sidebarList.addEventListener('click', e=>{ const p=e.target.closest('[data-id]'); if(!p) return; toggle(p.dataset.id); });
serversList.addEventListener('click', e=>{
  const del=e.target.closest('[data-del]'); if(del){ servers=servers.filter(s=>s.id!==del.dataset.del); saveServers(); renderServers(); toast('Removed'); return; }
  const ed=e.target.closest('[data-edit]'); if(ed){ const s=servers.find(x=>x.id===ed.dataset.edit); if(s){ editingId=s.id; serverModalTitle.textContent='Edit MCP Server'; srvName.value=s.name; srvTransport.value=s.transport; srvEndpoint.value=s.endpoint; srvTools.value=s.tools; serverModal.classList.remove('hidden'); refreshIcons(); } return; }
});
serversList.addEventListener('change', e=>{ const t=e.target.closest('[data-toggle]'); if(t) toggle(t.dataset.toggle, t.checked); });
async function toggle(id, force){
  const s=servers.find(x=>x.id===id); if(!s) return;
  const should= typeof force==='boolean' ? force : s.status!=='connected';
  if(should){ if(s.status==='connected') return; s.status='connecting'; renderServers(); toast(`Connecting ${s.name}…`); await new Promise(r=>setTimeout(r,900)); s.status='connected'; saveServers(); renderServers(); toast(`${s.name} connected ✓`); }
  else { s.status='disconnected'; saveServers(); renderServers(); toast(`${s.name} disconnected`); }
}

// Quick tools sheet
toolsBackdrop.addEventListener('click', ()=> toolsModal.classList.add('hidden'));
closeModalBtn.addEventListener('click', ()=> toolsModal.classList.add('hidden'));
quickToolsBtn.addEventListener('click', ()=> toolsModal.classList.remove('hidden'));
$$('.quick-card, .tool').forEach(b=>{
  b.addEventListener('click', ()=>{
    const t=b.dataset.tool;
    toolsModal.classList.add('hidden');
    if(t==='marketplace'){ marketplaceView.classList.remove('hidden'); renderMarket(); }
    else if(t==='upload') mediaInput.click();
    else if(t==='pg') { inputEl.value='/sql SELECT * FROM users LIMIT 5'; inputEl.focus(); autoResize(); syncComposer(); }
    else if(t==='aws'){ inputEl.value='/s3 list buckets'; inputEl.focus(); autoResize(); syncComposer(); }
    else if(t==='fs'){ inputEl.value='/fs list /data'; inputEl.focus(); autoResize(); syncComposer(); }
  });
});

// Sidebar toggle
menuBtn.addEventListener('click', ()=> sidebar.classList.add('open'));
sidebarClose.addEventListener('click', ()=> sidebar.classList.remove('open'));
document.addEventListener('keydown', e=>{
  if(e.key==='Escape'){ settingsView.classList.add('hidden'); serverModal.classList.add('hidden'); toolsModal.classList.add('hidden'); marketplaceView.classList.add('hidden'); promptsView.classList.add('hidden'); profileView.classList.add('hidden'); editMsgModal.classList.add('hidden'); hideSlash(); sidebar.classList.remove('open'); convMenu.classList.add('hidden'); }
  if((e.metaKey||e.ctrlKey) && e.key.toLowerCase()==='k'){ e.preventDefault(); openSettings(); }
});

// Send
sendBtn.addEventListener('click', ()=> doSend());
attachBtn.addEventListener('click', ()=> mediaInput.click());

// Init
renderProviders(); renderModes(); renderChats(); renderMessages(); renderServers(); renderPromptStrip(); renderMarket(); renderPromptCats(); renderPromptGrid(); refreshIcons(); scrollBottom(); updateUndoRedo();
if(profileNameInput) profileNameInput.value=localStorage.getItem('profileName')||'Alex Rivera';
if(profileEmail) profileEmail.value=localStorage.getItem('profileEmail')||'alex@local.dev';
