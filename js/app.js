/* MCP Terminal — Ultimate */
const $ = s => document.querySelector(s);
const $$ = s => [...document.querySelectorAll(s)];
const topbarEl = document.querySelector('.topbar');
const scrollFab = $('#scrollFab');
const messagesEl = $('#messages');
const inputEl = $('#promptInput');
const sendBtn = $('#sendBtn');
const attachBtn = $('#attachBtn');
const mediaInput = $('#mediaInput');
const attachPreview = $('#attachPreview');
const slashMenu = $('#slashMenu');
const charCount = $('#charCount');
const composerInner = $('#composerInner');
const topNodes = $('#nodesInline'); // removed from header - may be null
const composerModePill = $('#composerModePill');
const composerModeLabel = $('#composerModeLabel');
const composerModeWrap = $('#composerModeWrap');
const composerModeMenu = $('#composerModeMenu');
const composerModeList = $('#composerModeList');
const composerModeIcon = $('#composerModeIcon');
const composerModeTemp = $('#composerModeTemp');
const composerModeCount = $('#composerModeCount');
const composerManageModes = $('#composerManageModes');
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
const manageModesBtn = $('#manageModesBtn');
const modesView = $('#modesView');
const modesBackdrop = $('#modesBackdrop');
const closeModes = $('#closeModes');
const modesGrid = $('#modesGrid');
const modeSearch = $('#modeSearch');
const modeFilter = $('#modeFilter');
const addModeBtn = $('#addModeBtn');
const exportModesBtn = $('#exportModesBtn');
const importModesBtn = $('#importModesBtn');
const importModesFile = $('#importModesFile');
const activeModeName = $('#activeModeName');
const activeModeDesc = $('#activeModeDesc');
const activeModeBadge = $('#activeModeBadge');
const modeModal = $('#modeModal');
const modeModalBackdrop = $('#modeModalBackdrop');
const modeModalTitle = $('#modeModalTitle');
const closeModeModal = $('#closeModeModal');
const cancelMode = $('#cancelMode');
const duplicateModeBtn = $('#duplicateModeBtn');
const modeForm = $('#modeForm');
const modeName = $('#modeName');
const modeId = $('#modeId');
const modeIcon = $('#modeIcon');
const modeColor = $('#modeColor');
const modeAccent = $('#modeAccent');
const modeDesc = $('#modeDesc');
const modePrompt = $('#modePrompt');
const modeTemp = $('#modeTemp');
const modeTempVal = $('#modeTempVal');
const modeTokens = $('#modeTokens');
const modeProvider = $('#modeProvider');
const modeModel = $('#modeModel');
const modeServers = $('#modeServers');
const modeToolSQL = $('#modeToolSQL');
const modeToolFS = $('#modeToolFS');
const modeToolCloud = $('#modeToolCloud');
const modeToolWeb = $('#modeToolWeb');
const undoBtn = $('#undoBtn');
const redoBtn = $('#redoBtn');
const convMenuBtn = $('#convMenuBtn');
const convMenu = $('#convMenu');
const speechBar = $('#speechBar');
const micBtn = $('#micBtn');
const stopSpeech = $('#stopSpeech');
const liveBtn = $('#liveBtn');
const liveOverlay = $('#liveOverlay');
const liveBackdrop = $('#liveBackdrop');
const closeLiveEl = $('#closeLive');
const liveTranscript = $('#liveTranscript');
const liveInterim = $('#liveInterim');
const liveWave = $('#liveWave');
const liveWaveWrap = $('#liveWaveWrap');
const liveStatusText = $('#liveStatusText');
const liveHintText = $('#liveHintText');
const liveHistory = $('#liveHistory');
const liveTimer = $('#liveTimer');
const liveModel = $('#liveModel');
const liveMicBtn = $('#liveMicBtn');
const liveMuteBtn = $('#liveMuteBtn');
const liveSpeakerBtn = $('#liveSpeakerBtn');
const livePauseBtn = $('#livePauseBtn');
const endLiveBtn = $('#endLiveBtn');
const liveSettingsBtn = $('#liveSettingsBtn');
const liveSettings = $('#liveSettings');
const liveVoiceSelect = $('#liveVoiceSelect');
const liveLangSelect = $('#liveLangSelect');
const liveAutoSend = $('#liveAutoSend');
const liveSpeakBack = $('#liveSpeakBack');
const todoDraft = $('#todoDraft');
const todoList = $('#todoList');
const todoInput = $('#todoInput');
const todoProgress = $('#todoProgress');
const todoToggle = $('#todoToggle');
const clearTodo = $('#clearTodo');
const addTodoBtn = $('#addTodoBtn');
const splash = $('#splash');
const splashProgress = $('#splashProgress');
const splashStatus = $('#splashStatus');
const onboarding = $('#onboarding');
const onboardingBackdrop = $('#onboardingBackdrop');
const onStep = $('#onStep');
const onProgress = $('#onProgress');
const onPrev = $('#onPrev');
const onNext = $('#onNext');
const onSkip = $('#onSkip');
const authView = $('#authView');
const authBackdrop = $('#authBackdrop');
const loginForm = $('#loginForm');
const signupForm = $('#signupForm');
const guestForm = $('#guestForm');
const guestBtn = $('#guestBtn');
const authDemoLogin = $('#authDemoLogin');
const loginEmail = $('#loginEmail');
const loginPass = $('#loginPass');
const rememberMe = $('#rememberMe');
const signupName = $('#signupName');
const signupEmail = $('#signupEmail');
const signupPass = $('#signupPass');
const signupPass2 = $('#signupPass2');
const profileHeaderName = $('#profileHeaderName');
const profileHeaderEmail = $('#profileHeaderEmail');
const profileHeaderMeta = $('#profileHeaderMeta');
const profileGuestBadge = $('#profileGuestBadge');
const profileRoleBadge = $('#profileRoleBadge');
const logoutBtn = $('#logoutBtn');
const switchProfileBtn = $('#switchProfileBtn');
const manageProfilesBtn = $('#manageProfilesBtn');
const profilesList = $('#profilesList');
const addProfileBtn = $('#addProfileBtn');
const showOnboardingBtn = $('#showOnboardingBtn');

// drawers
const settingsView = $('#settingsView');
const settingsBackdrop = $('#settingsBackdrop');
const marketplaceView = $('#marketplaceView');
const promptsView = $('#promptsView');
const profileView = $('#profileView');
const serverSearch = $('#serverSearch');
const serversList = $('#serversList');
const addServerBtn = $('#addServerBtn');
const addServerSidebar = $('#addServerSidebar');
const closeSettings = $('#closeSettings');
const serversBtn = $('#serversBtn');
const settingsBtn = $('#settingsBtn'); // may be null (legacy id) — guarded
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

// modals — enhanced
const serverModal = $('#serverModal');
const closeServerModalBtn = $('#closeServerModal');
const serverForm = $('#serverForm');
const srvName = $('#srvName');
const srvTransport = $('#srvTransport');
const srvEndpoint = $('#srvEndpoint');
const srvTools = $('#srvTools');
const srvDesc = $('#srvDesc');
const srvTimeout = $('#srvTimeout');
const srvRetries = $('#srvRetries');
const srvTemplate = $('#srvTemplate');
const srvAuto = $('#srvAuto');
const srvHealth = $('#srvHealth');
const envList = $('#envList');
const addEnvBtn = $('#addEnvBtn');
const testServerBtn = $('#testServerBtn');
const srvEndpointHint = $('#srvEndpointHint');
const serverModalTitle = $('#serverModalTitle');
const serverModalBackdrop = $('#serverModalBackdrop');
const cancelServer = $('#cancelServer');
const serverDetailModal = $('#serverDetailModal');
const serverDetailBackdrop = $('#serverDetailBackdrop');
const serverDetailContent = $('#serverDetailContent');
const srvStatTotal = $('#srvStatTotal');
const srvStatConnected = $('#srvStatConnected');
const srvStatLatency = $('#srvStatLatency');
const serverTabs = $('#serverTabs');
const serverSort = $('#serverSort');
const bulkConnect = $('#bulkConnect');
const bulkDisconnect = $('#bulkDisconnect');
const testAllServers = $('#testAllServers');
const serverCountInfo = $('#serverCountInfo');
const exportServersBtn = $('#exportServersBtn');
const importServersBtn = $('#importServersBtn');
const importServersFile = $('#importServersFile');
const healthPoll = $('#healthPoll');
const autoReconnect = $('#autoReconnect');
let activeServerTab = localStorage.getItem('serverTab') || 'all';
let envDraft = [];
const toolsModal = $('#toolsModal');
const toolsBackdrop = $('#toolsBackdrop');
const closeModalBtn = $('#closeModal');
const quickToolsBtn = $('#quickToolsBtn'); // legacy - replaced by composerModePill
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

const safeParse = (key, fallback) => {
  try { const v = localStorage.getItem(key); if(v===null) return fallback; const p = JSON.parse(v); return p ?? fallback; } catch(e){ console.warn('[mcp] corrupt storage', key, e); try{ localStorage.removeItem(key);}catch{} return fallback; }
};
const safeSet = (key, val) => { try{ localStorage.setItem(key, val); } catch(e){ console.warn('[mcp] storage quota', key, e); }};
const refreshIcons=()=>{ try{ if(window.lucide && window.lucide.createIcons) window.lucide.createIcons(); }catch(e){ /* icon load may fail if CDN blocked */ } };
const toast=(m,d=2200)=>{ const el = (typeof toastEl !== 'undefined' ? toastEl : document.getElementById('toast')); if(!el) return; el.textContent=m; el.classList.remove('hidden'); clearTimeout(toast._t); toast._t=setTimeout(()=>el.classList.add('hidden'),d); };
const esc=s=> String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const scrollBottom=()=>{ const el = (typeof messagesEl !== 'undefined' ? messagesEl : document.getElementById('messages')); if(el) el.scrollTo({top:el.scrollHeight, behavior:'smooth'}); };
const atBottom=()=>{ const el=messagesEl; if(!el) return true; return el.scrollHeight - el.scrollTop - el.clientHeight < 80; };
const updateScrollFab=()=>{ if(!messagesEl || !scrollFab) return; const show = !atBottom() && messagesEl.scrollHeight > messagesEl.clientHeight + 100; scrollFab.classList.toggle('visible', show); if(topbarEl) topbarEl.classList.toggle('scrolled', messagesEl.scrollTop > 6); };
const uid=()=> Date.now().toString(36)+Math.random().toString(36).slice(2,7);
const on = (el, evt, fn, opts) => { if(el) el.addEventListener(evt, fn, opts); };
window.addEventListener('error', e=>{ console.warn('[mcp] unhandled', e.message); if(toastEl) toast('Error: '+ (e.message||'unknown').slice(0,60)); });
window.addEventListener('unhandledrejection', e=>{ console.warn('[mcp] unhandled rejection', e.reason); if(toastEl) toast('Async error: '+ String(e.reason).slice(0,60)); });
// UX: scroll FAB + topbar shadow + Cmd/K search
messagesEl?.addEventListener('scroll', updateScrollFab, {passive:true});
scrollFab?.addEventListener('click', scrollBottom);
document.addEventListener('keydown', e=>{
  if((e.metaKey||e.ctrlKey) && e.key.toLowerCase()==='k'){ e.preventDefault(); const s=document.getElementById('sidebarSearch'); if(s){ s.focus(); s.select(); toast('Search chats & servers'); } }
  if(e.key==='?' && !e.metaKey && !e.ctrlKey && e.target===document.body){ e.preventDefault(); toast('Tips: / commands • ⇧+Enter newline • ⌘K search'); }
});

// Providers — extended
let PROVIDERS = safeParse('mcp_providers', [
  { id:'openai', name:'OpenAI', models:['gpt-4o','gpt-4o-mini','o1-preview'], apiKey:'', baseUrl:'https://api.openai.com/v1', temp:0.7, maxTokens:4096, enabled:true, streaming:true, latency:142 },
  { id:'anthropic', name:'Anthropic', models:['claude-3.5-sonnet','claude-3-opus'], apiKey:'', baseUrl:'https://api.anthropic.com', temp:0.7, maxTokens:8192, enabled:true, streaming:true, latency:210 },
  { id:'google', name:'Google', models:['gemini-1.5-pro','gemini-1.5-flash'], apiKey:'', baseUrl:'https://generativelanguage.googleapis.com', temp:0.8, maxTokens:8192, enabled:true, streaming:true, latency:180 },
  { id:'local', name:'Local', models:['llama-3.1-70B','mistral-large'], apiKey:'', baseUrl:'http://localhost:11434', temp:0.6, maxTokens:4096, enabled:false, streaming:false, latency:45 },
]);
let selectedProvider = localStorage.getItem('provider') || 'anthropic';
let selectedModel = localStorage.getItem('model') || 'claude-3.5-sonnet';
let selectedMode = localStorage.getItem('agentMode') || 'Chat';
let fallbackProvider = localStorage.getItem('fallbackProvider') || 'openai';
let globalTemp = parseFloat(localStorage.getItem('globalTemp')||'0.7');
const saveProviders = ()=> safeSet('mcp_providers', JSON.stringify(PROVIDERS));

// Chats
let chats = safeParse('mcp_chats', [
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
]);
if(!localStorage.getItem('mcp_chats')) safeSet('mcp_chats', JSON.stringify(chats));
activeChatId = localStorage.getItem('activeChat') || chats[0]?.id || null;

// Servers — enhanced with health, env, timeout etc.
const DEFAULT_SERVERS = [
  {id:'srv1', name:'aws-us-east', transport:'sse', endpoint:'wss://aws.mcp.local/sse', tools:'s3_list, ec2_status', status:'connected', color:'aws', desc:'AWS S3 + EC2 in us-east-1', timeout:10000, retries:2, env:[], latency:142, health:'ok', lastSeen:Date.now(), autoConnect:true, healthCheck:true},
  {id:'srv2', name:'postgres-mcp', transport:'stdio', endpoint:'npx @modelcontextprotocol/server-postgres', tools:'query, list_tables, describe_schema', status:'connected', color:'pg', desc:'Postgres — remote_db', timeout:10000, retries:2, env:[{k:'DATABASE_URL', v:'postgres://...'}], latency:68, health:'ok', lastSeen:Date.now(), autoConnect:true, healthCheck:true},
  {id:'srv3', name:'local-fs', transport:'stdio', endpoint:'npx @modelcontextprotocol/server-filesystem /data', tools:'read, write, list', status:'disconnected', color:'fs', desc:'Local filesystem /data sandbox', timeout:8000, retries:1, env:[], latency:12, health:'off', lastSeen:Date.now()-3600000, autoConnect:false, healthCheck:true},
];
let servers = safeParse('mcp_servers', DEFAULT_SERVERS);
// migrate old servers missing fields
servers = servers.map(s=> ({
  desc: s.desc||'',
  timeout: s.timeout||10000,
  retries: s.retries??2,
  env: Array.isArray(s.env)? s.env : (s.env && typeof s.env==='object' ? Object.entries(s.env).map(([k,v])=>({k,v})) : []),
  latency: s.latency||Math.floor(Math.random()*120+40),
  health: s.health|| (s.status==='connected'?'ok':'off'),
  lastSeen: s.lastSeen||Date.now(),
  autoConnect: s.autoConnect?? (s.status==='connected'),
  healthCheck: s.healthCheck??true,
  ...s
}));
const saveServers=()=> safeSet('mcp_servers', JSON.stringify(servers));
const saveChats=()=> safeSet('mcp_chats', JSON.stringify(chats));

const MARKET = safeParse('mcp_market', [
  {id:'m1', name:'Postgres Pro', cat:'db', icon:'database', color:'#334155', desc:'Advanced SQL with vector search, migrations & pgvector support', author:'modelcontextprotocol', version:'2.1.0', stars:4.9, installs:12400, installed:true, featured:true, trending:true, isNew:false, tags:['postgres','sql','vector'], tools:['query','list_tables','describe_schema'], updated:'2024-10-12', verified:true},
  {id:'m2', name:'AWS Suite', cat:'cloud', icon:'cloud', color:'#ff9900', desc:'S3, EC2, Lambda, CloudWatch unified with MFA support', author:'aws', version:'1.8.3', stars:4.8, installs:8400, installed:true, featured:true, trending:true, isNew:false, tags:['aws','s3','ec2'], tools:['s3_list','ec2_status','lambda_invoke'], updated:'2024-10-20', verified:true},
  {id:'m3', name:'Filesystem', cat:'fs', icon:'folder', color:'#2563eb', desc:'Secure local file access with sandboxing & watcher', author:'modelcontextprotocol', version:'1.4.2', stars:4.7, installs:6100, installed:true, featured:false, trending:false, isNew:false, tags:['fs','local','watch'], tools:['read','write','list'], updated:'2024-09-28', verified:true},
  {id:'m4', name:'GitHub', cat:'dev', icon:'git-branch', color:'#0f172a', desc:'Repos, PRs, issues, actions, code search', author:'github', version:'3.0.1', stars:4.9, installs:15200, installed:false, featured:true, trending:true, isNew:false, tags:['git','pr','actions'], tools:['list_repos','create_pr','search_code'], updated:'2024-10-18', verified:true},
  {id:'m5', name:'Slack', cat:'cloud', icon:'message-square', color:'#e11d48', desc:'Channels, messages, threads, reactions & workflows', author:'slack', version:'1.2.5', stars:4.6, installs:9000, installed:false, featured:false, trending:false, isNew:true, tags:['chat','slack'], tools:['list_channels','post_message'], updated:'2024-10-22', verified:false},
  {id:'m6', name:'Notion', cat:'fs', icon:'book-open', color:'#000', desc:'Pages, databases, blocks, comments API', author:'notion', version:'0.9.4', stars:4.5, installs:5200, installed:false, featured:false, trending:false, isNew:true, tags:['notion','docs'], tools:['query_db','create_page'], updated:'2024-10-05', verified:false},
  {id:'m7', name:'Redis', cat:'db', icon:'layers', color:'#dc2626', desc:'Cache, pub/sub, streams & cluster management', author:'redis', version:'1.1.0', stars:4.7, installs:4100, installed:false, featured:false, trending:true, isNew:false, tags:['redis','cache'], tools:['get','set','publish'], updated:'2024-09-15', verified:true},
  {id:'m8', name:'Puppeteer', cat:'dev', icon:'globe', color:'#7c3aed', desc:'Browser automation, scraping & PDF gen', author:'browser', version:'2.3.0', stars:4.8, installs:7300, installed:false, featured:false, trending:true, isNew:false, tags:['browser','scrape'], tools:['navigate','screenshot','pdf'], updated:'2024-10-10', verified:true},
  {id:'m9', name:'Stripe', cat:'cloud', icon:'credit-card', color:'#635bff', desc:'Payments, customers, subscriptions, invoices', author:'stripe', version:'1.0.2', stars:4.8, installs:6800, installed:false, featured:false, trending:false, isNew:true, tags:['payments','stripe'], tools:['list_customers','create_charge'], updated:'2024-10-23', verified:true},
  {id:'m10', name:'Linear', cat:'dev', icon:'target', color:'#5e6ad2', desc:'Issues, projects, cycles, roadmap sync', author:'linear', version:'1.3.1', stars:4.6, installs:3100, installed:false, featured:false, trending:false, isNew:false, tags:['linear','issues'], tools:['list_issues','create_issue'], updated:'2024-09-30', verified:false},
]);
const saveMarket = ()=> safeSet('mcp_market', JSON.stringify(MARKET));

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

// Providers render — complete
function renderProviders(){
  const enabledProviders = PROVIDERS.filter(p=>p.enabled);
  // if selected is disabled, fallback
  if(!enabledProviders.find(p=>p.id===selectedProvider)){
    const fallback = enabledProviders[0];
    if(fallback){
      const prev = selectedProvider;
      selectedProvider=fallback.id; selectedModel=fallback.models[0];
      try{ localStorage.setItem('provider', selectedProvider); localStorage.setItem('model', selectedModel);}catch{}
      if(prev!==selectedProvider) toast(`Provider ${prev} disabled — switched to ${fallback.name}`);
    }
  }
  if(providerSelect) providerSelect.innerHTML = PROVIDERS.filter(p=>p.enabled).map(p=>`<option value="${p.id}" ${p.id===selectedProvider?'selected':''}>${p.name}</option>`).join('') || `<option>No providers enabled</option>`;
  const prov = PROVIDERS.find(p=>p.id===selectedProvider);
  if(prov){
    if(modelSelect){ modelSelect.innerHTML = prov.models.map(m=>`<option ${m===selectedModel?'selected':''}>${m}</option>`).join(''); modelSelect.disabled=false; }
    // pills (header provider pill removed - keep sidebar status only)
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
    if(modelSelect){ modelSelect.innerHTML=`<option>No model</option>`; modelSelect.disabled=true; }
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
providerSelect?.addEventListener('change', e=>{
  selectedProvider=e.target.value;
  const prov=PROVIDERS.find(p=>p.id===selectedProvider);
  if(!prov || !prov.enabled) return;
  selectedModel=prov.models[0];
  localStorage.setItem('provider', selectedProvider);
  localStorage.setItem('model', selectedModel);
  renderProviders(); renderProviderList(); toast(`Provider: ${prov.name} • ${selectedModel}`);
});
modelSelect?.addEventListener('change', e=>{ selectedModel=e.target.value; localStorage.setItem('model', selectedModel); toast(`Model: ${selectedModel}`); renderProviders(); renderMessages(); });

// Provider list drawer
const providerView=$('#providerView'), providerBackdrop=$('#providerBackdrop'), closeProvider=$('#closeProvider'), providerList=$('#providerList'), providerSearch=$('#providerSearch'), addProviderBtn=$('#addProviderBtn'), providerSettingsBtn=$('#providerSettingsBtn'), providerTopBtn=$('#providerTopBtn'), testActiveProvider=$('#testActiveProvider'), exportProvidersBtn=$('#exportProvidersBtn'), importProviderBtn=$('#importProviderBtn'), importProviderFile=$('#importProviderFile'), fallbackProviderEl=$('#fallbackProvider'), globalTempEl=$('#globalTemp'), providerModal=$('#providerModal'), providerModalBackdrop=$('#providerModalBackdrop'), closeProviderModal=$('#closeProviderModal'), providerForm=$('#providerForm'), provName=$('#provName'), provId=$('#provId'), provKey=$('#provKey'), provBase=$('#provBase'), provModels=$('#provModels'), provTemp=$('#provTemp'), provTempVal=$('#provTempVal'), provMax=$('#provMax'), provStream=$('#provStream'), provEnabled=$('#provEnabled'), testProviderBtn=$('#testProviderBtn'), cancelProvider=$('#cancelProvider');
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
function openProviderView(){ providerView?.classList.remove('hidden'); renderProviderList(); renderProviders(); refreshIcons(); }
function closeProviderView(){ providerView?.classList.add('hidden'); }
providerSettingsBtn?.addEventListener('click', openProviderView);
providerTopBtn?.addEventListener('click', openProviderView);
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
  const edit=e.target.closest('[data-provider-edit]'); if(edit){ const p=PROVIDERS.find(x=>x.id===edit.dataset.providerEdit); if(p){ editingProviderId=p.id; $('#providerModalTitle').textContent='Edit Provider'; provName.value=p.name; provId.value=p.id; provId.disabled=true; provKey.value=p.apiKey; provBase.value=p.baseUrl; provModels.value=p.models.join(', '); provTemp.value=String(p.temp); if(provTempVal) provTempVal.textContent=String(p.temp); provMax.value=String(p.maxTokens); provStream.checked=p.streaming; provEnabled.checked=p.enabled; providerModal?.classList.remove('hidden'); refreshIcons(); } return; }
  const del=e.target.closest('[data-provider-del]'); if(del){ const id=del.dataset.providerDel; if(PROVIDERS.length<=1) return toast('Keep at least one provider'); if(confirm('Delete provider?')){ PROVIDERS=PROVIDERS.filter(p=>p.id!==id); if(selectedProvider===id){ selectedProvider=PROVIDERS[0].id; selectedModel=PROVIDERS[0].models[0]; localStorage.setItem('provider', selectedProvider); localStorage.setItem('model', selectedModel); } saveProviders(); renderProviders(); renderProviderList(); renderMessages(); toast('Provider deleted'); } return; }
});
providerList?.addEventListener('change', e=>{
  const t=e.target.closest('[data-provider-toggle]'); if(t){ const p=PROVIDERS.find(x=>x.id===t.dataset.providerToggle); p.enabled=t.checked; saveProviders(); renderProviders(); renderProviderList(); toast(`${p.name} ${p.enabled?'enabled':'disabled'}`); }
});
addProviderBtn?.addEventListener('click', ()=>{
  editingProviderId=null;
  $('#providerModalTitle').textContent='Add AI Provider';
  provName.value=''; provId.value=''; provId.disabled=false; provKey.value=''; provBase.value='https://api.example.com/v1'; provModels.value=''; provTemp.value='0.7'; if(provTempVal) provTempVal.textContent='0.7'; provMax.value='4096'; provStream.checked=true; provEnabled.checked=true;
  providerModal?.classList.remove('hidden'); refreshIcons();
});
closeProviderModal?.addEventListener('click', ()=> providerModal?.classList.add('hidden'));
providerModalBackdrop?.addEventListener('click', ()=> providerModal?.classList.add('hidden'));
cancelProvider?.addEventListener('click', ()=> providerModal?.classList.add('hidden'));
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
  saveProviders(); renderProviders(); renderProviderList(); providerModal?.classList.add('hidden');
  if(data.enabled){ selectedProvider=data.id; selectedModel=data.models[0]; localStorage.setItem('provider', selectedProvider); localStorage.setItem('model', selectedModel); renderProviders(); renderMessages(); }
});

// Modes — enhanced with custom, prompts, servers, provider overrides
const DEFAULT_MODES = [
  {id:'Chat', name:'Chat', icon:'message-circle', color:'#2563eb', desc:'Balanced, helpful general assistant', prompt:'You are a helpful, concise assistant. Use MCP tools when needed.', temp:0.7, tokens:4096, provider:'', model:'', servers:[], tools:{sql:true, fs:true, cloud:true, web:false}, builtin:true},
  {id:'Code', name:'Code', icon:'code-2', color:'#0f172a', desc:'Senior engineer — clean code, tests, review', prompt:'You are a senior software engineer. Produce clean, tested code. Prefer MCP: read files, run tools, explain diffs.', temp:0.2, tokens:8192, provider:'', model:'', servers:[], tools:{sql:true, fs:true, cloud:false, web:false}, builtin:true},
  {id:'Research', name:'Research', icon:'search', color:'#7c3aed', desc:'Deep research with citations & MCP browsing', prompt:'You are a research assistant. Search, read, synthesize with citations. Use MCP tools to gather evidence.', temp:0.5, tokens:8192, provider:'', model:'', servers:[], tools:{sql:true, fs:true, cloud:true, web:true}, builtin:true},
  {id:'Planning', name:'Planning', icon:'map', color:'#0891b2', desc:'Project planning, roadmap, todos & deps', prompt:'You are a planning assistant. Break goals into todos, identify deps, estimate, and track with checklists.', temp:0.4, tokens:4096, provider:'', model:'', servers:[], tools:{sql:false, fs:true, cloud:false, web:false}, builtin:true},
  {id:'Write', name:'Write', icon:'pen-tool', color:'#db2777', desc:'Clear writing, editing, tone & structure', prompt:'You are an expert writer. Improve clarity, tone, structure. Be concise and engaging.', temp:0.8, tokens:4096, provider:'', model:'', servers:[], tools:{sql:false, fs:true, cloud:false, web:false}, builtin:true},
  {id:'Debug', name:'Debug', icon:'bug', color:'#dc2626', desc:'Root-cause analysis, logs & fixes', prompt:'You are a debugging assistant. Analyze errors, logs, and MCP outputs to find root cause and fix.', temp:0.3, tokens:8192, provider:'', model:'', servers:[], tools:{sql:true, fs:true, cloud:true, web:false}, builtin:true},
  {id:'Shield', name:'Shield', icon:'shield', color:'#16a34a', desc:'Secure, privacy-aware, minimal tools', prompt:'You are a security-aware assistant. Minimize tool use, avoid sensitive data, explain risks.', temp:0.3, tokens:4096, provider:'', model:'', servers:[], tools:{sql:false, fs:false, cloud:false, web:false}, builtin:true},
  {id:'Creative', name:'Creative', icon:'palette', color:'#f59e0b', desc:'Brainstorm, ideas, variations & play', prompt:'You are a creative partner. Generate ideas, variations, playful tone. Use tools sparingly.', temp:0.9, tokens:4096, provider:'', model:'', servers:[], tools:{sql:false, fs:false, cloud:false, web:true}, builtin:true},
];
let MODES = safeParse('mcp_modes', DEFAULT_MODES);
let editingModeId = null;
const saveModes = ()=> safeSet('mcp_modes', JSON.stringify(MODES));
function getMode(id){ return MODES.find(m=>m.id===id) || MODES[0]; }
function getActiveMode(){ return getMode(selectedMode); }
function renderModes(){
  const active = getActiveMode();
  // topbar pills — legacy header (removed, keep composer-only). Guard for null.
  if(agentModesEl){
  const visible = MODES.slice(0,5);
  const overflow = MODES.length - visible.length;
  agentModesEl.innerHTML = visible.map(m=>{
    const isActive = selectedMode===m.id;
    return `<button class="mode-pill ${isActive?'active':''} ${m.color?'colored':''}" data-mode="${m.id}" style="${isActive?`background:${m.color};border-color:${m.color};color:#fff`:`border-color:${m.color}20;color:${m.color}`}" title="${esc(m.desc)}"><i data-lucide="${m.icon}" style="width:12px;height:12px"></i> ${esc(m.name)}</button>`;
  }).join('') + (overflow>0? `<span style="font-size:11px;color:var(--muted);padding:6px">+${overflow}</span>`:``) + ` <span style="margin-left:4px;font-size:11px;color:var(--faint);font-family:var(--mono);display:none" class="hide-mobile">${active ? esc(active.prompt.slice(0,24))+'…' : ''}</span>`;
  refreshIcons();
  }
  // update composer placeholder per mode
  if(inputEl){
    const placeholders = {
      Chat:'Ask anything… type “/” for commands',
      Code:'Ask to code, review, or refactor — I’ll use MCP tools',
      Research:'Ask to research — I’ll search & cite with MCP',
      Planning:'Describe goal — I’ll break into todos',
      Write:'Ask to write or edit — tone & clarity',
      Debug:'Paste error or logs — I’ll diagnose',
      Shield:'Ask securely — minimal tools',
      Creative:'Ask to brainstorm — ideas & variations'
    };
    inputEl.placeholder = placeholders[active.id] || `Ask ${active.name}… • type “/” for commands`;
  }
  // update profile role line
  if(profileRole) profileRole.textContent = `${$('#profileRoleSelect')?.value || 'Admin'} • ${selectedModel} • ${active.name} mode`;
  // filter MCP nodes visibility per mode's servers
  if(active.servers && active.servers.length){
    // highlight allowed servers in topNodes
    $$('.node-pill').forEach(el=>{
      const id=el.dataset.id;
      const s=servers.find(x=>x.id===id);
      if(s && !active.servers.includes(s.name) && !active.servers.includes(s.id)){
        el.style.opacity='.35'; el.title=`Not allowed in ${active.name} mode`;
      } else {
        el.style.opacity=''; el.title='';
      }
    });
  } else {
    $$('.node-pill').forEach(el=>{ el.style.opacity=''; el.title=''; });
  }
  // inline composer pill — enhanced
  if(composerModeLabel) composerModeLabel.textContent = active.name;
  if(composerModePill){
    composerModePill.style.borderColor = active.color + '40';
    composerModePill.style.color = active.color;
    composerModePill.setAttribute('aria-label', `Agent mode: ${active.name} — ${active.desc}`);
    if(composerModeIcon){
      composerModeIcon.style.background = active.color;
      const iconEl = composerModeIcon.querySelector('[data-lucide]');
      if(iconEl) iconEl.setAttribute('data-lucide', active.icon);
    }
  }
  if(composerModeTemp) composerModeTemp.textContent = String(active.temp ?? 0.7);
  if(composerModeCount) composerModeCount.textContent = `${MODES.length} modes`;
  renderComposerModeMenu();
  refreshIcons();
  // update active card in drawer
  if(activeModeName) activeModeName.textContent = active.name;
  if(activeModeDesc) activeModeDesc.textContent = active.desc;
  if(activeModeBadge) activeModeBadge.textContent = active.name.toUpperCase();
  if(liveModel) liveModel.textContent = `${selectedModel} • ${active.name}`;
}
function renderModesGrid(){
  const q=(modeSearch?.value||'').toLowerCase();
  const f=modeFilter?.value||'all';
  let list=MODES;
  if(q) list=list.filter(m=> m.name.toLowerCase().includes(q) || m.desc.toLowerCase().includes(q) || m.id.toLowerCase().includes(q) || m.prompt.toLowerCase().includes(q));
  if(f==='built-in') list=list.filter(m=>m.builtin);
  if(f==='custom') list=list.filter(m=>!m.builtin);
  modesGrid.innerHTML = list.map(m=>{
    const isActive=selectedMode===m.id;
    return `<div class="mode-card ${isActive?'active':''}" data-mode-id="${m.id}">
      <div class="mode-card-head">
        <div class="mode-icon" style="background:${m.color}"><i data-lucide="${m.icon}" style="width:18px;height:18px"></i></div>
        <div class="mode-meta"><b>${esc(m.name)} ${isActive?'<span style="background:var(--primary);color:#fff;font-size:10px;padding:2px 6px;border-radius:999px">ACTIVE</span>':''} ${m.builtin?'<span style="background:#f1f5f9;color:var(--muted);font-size:10px;padding:2px 6px;border-radius:999px;border:1px solid var(--line)">BUILT-IN</span>':''}</b><p>${esc(m.desc)}</p></div>
        <label class="switch" title="Select"><input type="radio" name="modeActive" ${isActive?'checked':''} data-mode-radio="${m.id}"><span></span></label>
      </div>
      <div class="mode-badges">
        <span class="mode-badge" style="border-color:${m.color}30;color:${m.color}">temp ${m.temp}</span>
        <span class="mode-badge">${m.tokens} tok</span>
        ${m.provider?`<span class="mode-badge" style="background:var(--primary-ghost);color:var(--primary)">${esc(m.provider)}</span>`:''}
        ${m.servers?.length?`<span class="mode-badge">${m.servers.length} MCP</span>`:''}
        ${Object.values(m.tools||{}).filter(Boolean).length?`<span class="mode-badge">${Object.values(m.tools).filter(Boolean).length} tools</span>`:''}
      </div>
      <div style="font-family:var(--mono);font-size:11px;background:#f8fafc;border:1px solid var(--line);border-radius:8px;padding:8px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:var(--muted)">${esc(m.prompt.slice(0,72))}…</div>
      <div class="mode-card-actions">
        <button class="ghost-btn sm" data-mode-use="${m.id}"><i data-lucide="play" style="width:12px;height:12px"></i> Use</button>
        <button class="ghost-btn sm" data-mode-edit="${m.id}"><i data-lucide="pencil" style="width:12px;height:12px"></i> Edit</button>
        <button class="ghost-btn sm" data-mode-dup="${m.id}"><i data-lucide="copy" style="width:12px;height:12px"></i></button>
        <button class="icon-btn sm" data-mode-del="${m.id}" style="color:var(--bad)" ${m.builtin?'disabled title="Built-in cannot be deleted"':''}><i data-lucide="trash-2" style="width:12px;height:12px"></i></button>
      </div>
    </div>`;
  }).join('') || `<p style="grid-column:1/-1;text-align:center;color:var(--faint);padding:20px;background:#fff;border:1px dashed var(--line);border-radius:12px">No modes match<br><small>Try another search</small></p>`;
  refreshIcons();
}
function openModesView(){ modesView?.classList.remove('hidden'); renderModesGrid(); refreshIcons(); }
function populateModeServers(){
  if(!modeServers) return;
  const active= editingModeId ? MODES.find(m=>m.id===editingModeId) : null;
  const selected = active ? (active.servers||[]) : [];
  modeServers.innerHTML = servers.map(s=>`
    <label style="display:flex;align-items:center;gap:6px;background:#fff;border:1px solid var(--line);padding:6px 8px;border-radius:999px;font-size:12px;cursor:pointer">
      <input type="checkbox" value="${esc(s.id)}" data-mode-server ${selected.includes(s.id)||selected.includes(s.name)?'checked':''} /> ${esc(s.name)}
    </label>
  `).join('') || `<span style="font-size:11px;color:var(--faint)">No servers yet</span>`;
}
agentModesEl?.addEventListener('click', e=>{
  const b=e.target.closest('[data-mode]'); if(!b) return;
  selectedMode=b.dataset.mode; localStorage.setItem('agentMode', selectedMode);
  const m=getMode(selectedMode);
  // apply provider/model override if set
  if(m.provider){
    const p=PROVIDERS.find(x=>x.id===m.provider);
    if(p && p.enabled){ selectedProvider=p.id; localStorage.setItem('provider', selectedProvider); if(m.model){ selectedModel=m.model; localStorage.setItem('model', selectedModel); } renderProviders(); }
  }
  renderModes(); renderModesGrid(); toast(`Mode: ${m.name} — ${m.desc}`);
});
// Composer mode popover — quick switch without leaving input
function renderComposerModeMenu(){
  if(!composerModeList) return;
  composerModeList.innerHTML = MODES.map(m=>{
    const isActive = selectedMode===m.id;
    return `<button class="composer-mode-option ${isActive?'active':''}" role="menuitem" data-composer-mode="${m.id}" title="${esc(m.desc)}">
      <span class="mi" style="background:${m.color}"><i data-lucide="${m.icon}" style="width:16px;height:16px"></i></span>
      <span style="flex:1;min-width:0;text-align:left"><b>${esc(m.name)} ${m.builtin?'<span style="font-size:10px;background:#f1f5f9;border:1px solid var(--line);padding:1px 5px;border-radius:999px;color:var(--muted)">built-in</span>':''}</b><p>${esc(m.desc)}</p></span>
      <span class="check">${isActive?'<i data-lucide="check" style="width:12px;height:12px"></i>':''}</span>
    </button>`;
  }).join('');
  refreshIcons();
}
function toggleComposerModeMenu(force){
  if(!composerModeMenu || !composerModePill) return;
  const willOpen = typeof force==='boolean' ? force : composerModeMenu.classList.contains('hidden');
  if(willOpen){
    renderComposerModeMenu();
    composerModeMenu.classList.remove('hidden');
    composerModePill.setAttribute('aria-expanded','true');
    refreshIcons();
  } else {
    composerModeMenu.classList.add('hidden');
    composerModePill.setAttribute('aria-expanded','false');
  }
}
function closeComposerModeMenu(){ toggleComposerModeMenu(false); }
manageModesBtn?.addEventListener('click', openModesView);
composerModePill?.addEventListener('click', (e)=>{ e.stopPropagation(); toggleComposerModeMenu(); });
composerManageModes?.addEventListener('click', (e)=>{ e.stopPropagation(); closeComposerModeMenu(); openModesView(); });
composerModeList?.addEventListener('click', (e)=>{
  const b=e.target.closest('[data-composer-mode]'); if(!b) return;
  const id=b.dataset.composerMode;
  if(id===selectedMode){ closeComposerModeMenu(); return; }
  selectedMode=id; localStorage.setItem('agentMode', selectedMode);
  const m=getMode(selectedMode);
  if(m.provider){
    const p=PROVIDERS.find(x=>x.id===m.provider);
    if(p && p.enabled){ selectedProvider=p.id; localStorage.setItem('provider', selectedProvider); if(m.model){ selectedModel=m.model; localStorage.setItem('model', selectedModel); } renderProviders(); }
  }
  renderModes(); renderModesGrid(); closeComposerModeMenu(); toast(`Mode: ${m.name} — ${m.desc}`);
  inputEl?.focus();
});
document.addEventListener('click', (e)=>{ if(!e.target.closest('#composerModeWrap') && !e.target.closest('#composerModeMenu')) closeComposerModeMenu(); });
document.addEventListener('keydown', (e)=>{ if(e.key==='Escape') closeComposerModeMenu(); });
modesBackdrop?.addEventListener('click', ()=> modesView?.classList.add('hidden'));
closeModes?.addEventListener('click', ()=> modesView?.classList.add('hidden'));
modeSearch?.addEventListener('input', renderModesGrid);
modeFilter?.addEventListener('change', renderModesGrid);
modesGrid?.addEventListener('click', e=>{
  const use=e.target.closest('[data-mode-use]'); if(use){ selectedMode=use.dataset.modeUse; localStorage.setItem('agentMode', selectedMode); renderModes(); renderModesGrid(); const m=getMode(selectedMode); if(m.provider){ const p=PROVIDERS.find(x=>x.id===m.provider); if(p){ selectedProvider=p.id; localStorage.setItem('provider', p.id); if(m.model) { selectedModel=m.model; localStorage.setItem('model', m.model); } renderProviders(); } } toast(`Switched to ${getMode(selectedMode).name}`); return; }
  const radio=e.target.closest('[data-mode-radio]'); if(radio){ selectedMode=radio.dataset.modeRadio; localStorage.setItem('agentMode', selectedMode); renderModes(); renderModesGrid(); return; }
  const edit=e.target.closest('[data-mode-edit]'); if(edit){ openEditMode(edit.dataset.modeEdit); return; }
  const dup=e.target.closest('[data-mode-dup]'); if(dup){ const src=MODES.find(m=>m.id===dup.dataset.modeDup); const copy={...JSON.parse(JSON.stringify(src)), id:src.id+'-copy', name:src.name+' Copy', builtin:false}; MODES.push(copy); saveModes(); renderModes(); renderModesGrid(); toast(`Duplicated ${src.name}`); return; }
  const del=e.target.closest('[data-mode-del]'); if(del){ const id=del.dataset.modeDel; const m=MODES.find(x=>x.id===id); if(m.builtin) return toast('Built-in cannot be deleted'); if(confirm(`Delete mode "${m.name}"?`)){ MODES=MODES.filter(x=>x.id!==id); if(selectedMode===id){ selectedMode=MODES[0].id; localStorage.setItem('agentMode', selectedMode); } saveModes(); renderModes(); renderModesGrid(); toast('Mode deleted'); } return; }
  const card=e.target.closest('.mode-card'); if(card && !e.target.closest('button') && !e.target.closest('input')){ const id=card.dataset.modeId; selectedMode=id; localStorage.setItem('agentMode', selectedMode); renderModes(); renderModesGrid(); }
});
addModeBtn?.addEventListener('click', ()=>{
  editingModeId=null;
  modeModalTitle.textContent='New Agent Mode';
  modeName.value=''; modeId.value=''; modeIcon.value='message-circle'; modeColor.value='#2563eb'; modeAccent.value='solid'; modeDesc.value=''; modePrompt.value=''; modeTemp.value='0.7'; if(modeTempVal) modeTempVal.textContent='0.7'; modeTokens.value='4096'; modeProvider.value=''; modeModel.value=''; modeToolSQL.checked=true; modeToolFS.checked=true; modeToolCloud.checked=true; modeToolWeb.checked=false; populateModeServers(); duplicateModeBtn.style.display='none';
  modeModal?.classList.remove('hidden'); refreshIcons();
});
exportModesBtn?.addEventListener('click', ()=>{
  const blob=new Blob([JSON.stringify(MODES,null,2)],{type:'application/json'});
  const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download='mcp-modes.json'; a.click(); URL.revokeObjectURL(url); toast('Modes exported');
});
importModesBtn?.addEventListener('click', ()=> importModesFile.click());
importModesFile?.addEventListener('change', e=>{
  const f=e.target.files[0]; if(!f) return;
  const r=new FileReader(); r.onerror=()=> toast('File read error'); r.onload=()=>{ try{ const arr=JSON.parse(r.result); if(Array.isArray(arr)){ MODES=arr; saveModes(); renderModes(); renderModesGrid(); toast('Modes imported'); } else toast('Invalid file'); } catch{ toast('Invalid JSON'); } }; r.readAsText(f); e.target.value='';
});
function openEditMode(id){
  const m=MODES.find(x=>x.id===id); if(!m) return;
  editingModeId=id;
  modeModalTitle.textContent='Edit Mode — '+m.name;
  modeName.value=m.name; modeId.value=m.id; modeId.disabled=true; modeIcon.value=m.icon; modeColor.value=m.color; modeAccent.value=m.accent||'solid'; modeDesc.value=m.desc; modePrompt.value=m.prompt; modeTemp.value=String(m.temp); if(modeTempVal) modeTempVal.textContent=String(m.temp); modeTokens.value=String(m.tokens); modeProvider.value=m.provider||''; modeModel.value=m.model||''; modeToolSQL.checked=!!m.tools.sql; modeToolFS.checked=!!m.tools.fs; modeToolCloud.checked=!!m.tools.cloud; modeToolWeb.checked=!!m.tools.web; populateModeServers(); duplicateModeBtn.style.display='';
  modeModal?.classList.remove('hidden'); refreshIcons();
}
modeTemp?.addEventListener('input', e=>{ if(modeTempVal) modeTempVal.textContent=e.target.value; });
closeModeModal?.addEventListener('click', ()=> modeModal?.classList.add('hidden'));
modeModalBackdrop?.addEventListener('click', ()=> modeModal?.classList.add('hidden'));
cancelMode?.addEventListener('click', ()=> modeModal?.classList.add('hidden'));
duplicateModeBtn?.addEventListener('click', ()=>{
  if(!editingModeId) return;
  const src=MODES.find(m=>m.id===editingModeId);
  const copy={...JSON.parse(JSON.stringify(src)), id:src.id+'-copy', name:src.name+' Copy', builtin:false};
  MODES.push(copy); saveModes(); renderModes(); renderModesGrid(); modeModal?.classList.add('hidden'); toast('Duplicated '+src.name);
});
modeForm?.addEventListener('submit', e=>{
  e.preventDefault();
  const id=modeId.value.trim().toLowerCase(), name=modeName.value.trim();
  if(!id||!name) return toast('Name and ID required');
  if(!/^[a-z0-9-]+$/.test(id)) return toast('ID must be lowercase slug');
  if(!editingModeId && MODES.find(m=>m.id===id)) return toast('ID exists');
  const selectedServers=[...modeServers.querySelectorAll('[data-mode-server]:checked')].map(el=>el.value);
  const data={
    id, name, icon:modeIcon.value, color:modeColor.value, accent:modeAccent.value,
    desc:modeDesc.value.trim(), prompt:modePrompt.value.trim()||'You are a helpful assistant.',
    temp:parseFloat(modeTemp.value)||0.7, tokens:parseInt(modeTokens.value)||4096,
    provider:modeProvider.value, model:modeModel.value.trim(),
    servers:selectedServers,
    tools:{sql:!!modeToolSQL.checked, fs:!!modeToolFS.checked, cloud:!!modeToolCloud.checked, web:!!modeToolWeb.checked},
    builtin:false
  };
  if(editingModeId){
    const idx=MODES.findIndex(m=>m.id===editingModeId);
    data.id=editingModeId;
    MODES[idx]={...MODES[idx], ...data, id:editingModeId};
    toast(`${name} updated`);
  } else {
    MODES.push(data);
    toast(`${name} created`);
  }
  saveModes(); renderModes(); renderModesGrid(); modeModal?.classList.add('hidden');
  selectedMode=data.id; localStorage.setItem('agentMode', selectedMode); renderModes();
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
  `).join('') || `<div style="text-align:center;padding:20px 12px;background:#fff;border:1px dashed var(--line);border-radius:12px"><i data-lucide="inbox" style="width:20px;height:20px;color:#cbd5e1;margin:0 auto;display:block"></i><p style="font-size:12px;color:var(--muted);margin:8px 0 0">${q?'No matches':'No chats yet'}</p><button class="ghost-btn sm" style="margin-top:8px" onclick="document.getElementById('newChatBtn').click()">New chat</button></div>`;
  refreshIcons(); updateScrollFab();
}
chatListEl?.addEventListener('click', e=>{
  const item=e.target.closest('.chat-item'); if(!item) return;
  const more=e.target.closest('[data-act="more"]');
  if(more){
    const id=more.dataset.id;
    const chat=chats.find(c=>c.id===id);
    const act=prompt(`Actions for "${chat.title}"\n1 rename\n2 export\n3 archive/unarchive\n4 delete\nType 1-4`);
    if(act==='1'){ const t=prompt('New title', chat.title); if(t){ pushHistory(); chat.title=t; saveChats(); renderChats(); renderMessages(); } }
    else if(act==='2') doExport(chat);
    else if(act==='3'){ chat.archived=!chat.archived; saveChats(); renderChats(); }
    else if(act==='4'){ if(confirm('Delete?')){ pushHistory(); chats=chats.filter(c=>c.id!==id); if(activeChatId===id) activeChatId=chats[0]?.id||null; saveChats(); if(activeChatId) try{ localStorage.setItem('activeChat', activeChatId);}catch{} else try{ localStorage.removeItem('activeChat');}catch{}; renderChats(); renderMessages(); } }
    return;
  }
  activeChatId=item.dataset.id; localStorage.setItem('activeChat', activeChatId); renderChats(); renderMessages(); pushHistory();
});
newChatBtn?.addEventListener('click', ()=> createChat());
newChatTopBtn?.addEventListener('click', ()=> createChat());
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
convMenuBtn?.addEventListener('click', ()=> convMenu.classList.toggle('hidden'));
document.addEventListener('click', e=>{ if(!e.target.closest('.dropdown')) convMenu.classList.add('hidden'); });
convMenu?.addEventListener('click', e=>{
  const b=e.target.closest('[data-action]'); if(!b) return;
  const act=b.dataset.action;
  const chat=getActiveChat(); if(!chat) return;
  convMenu.classList.add('hidden');
  if(act==='rename'){ const t=prompt('Rename', chat.title); if(t){ pushHistory(); chat.title=t; saveChats(); renderChats(); renderMessages(); } }
  if(act==='export') doExport(chat);
  if(act==='duplicate'){ pushHistory(); const copy=JSON.parse(JSON.stringify(chat)); copy.id=uid(); copy.title=chat.title+' (copy)'; copy.created=Date.now(); chats.unshift(copy); activeChatId=copy.id; saveChats(); renderChats(); renderMessages(); toast('Duplicated'); }
  if(act==='archive'){ pushHistory(); chat.archived=!chat.archived; saveChats(); renderChats(); renderMessages(); toast(chat.archived?'Archived':'Unarchived'); }
  if(act==='delete'){ if(confirm('Delete conversation?')){ pushHistory(); chats=chats.filter(c=>c.id!==chat.id); activeChatId=chats[0]?.id||null; saveChats(); if(activeChatId) try{ localStorage.setItem('activeChat', activeChatId);}catch{} else try{ localStorage.removeItem('activeChat');}catch{}; renderChats(); renderMessages(); toast('Deleted'); } }
});
importChatBtn?.addEventListener('click', ()=> importFile.click());
importFile?.addEventListener('change', e=>{
  const f=e.target.files[0]; if(!f) return;
  const r=new FileReader(); r.onerror=()=> toast('File read error'); r.onload=()=>{ try{ const data=JSON.parse(r.result); if(data.title && data.messages){ pushHistory(); data.id=uid(); data.created=Date.now(); chats.unshift(data); activeChatId=data.id; saveChats(); renderChats(); renderMessages(); toast('Imported'); } else toast('Invalid file'); } catch{ toast('Invalid JSON'); } }; r.readAsText(f); e.target.value='';
});
archiveToggle?.addEventListener('click', ()=>{ showArchived=!showArchived; archiveToggle.style.background=showArchived?'#0f172a':''; archiveToggle.style.color=showArchived?'#fff':''; renderChats(); toast(showArchived?'Showing archived':'Hiding archived'); });

// Servers render — enhanced
let serverHealthTimer=null;
function getHealthClass(s){
  if(s.status!=='connected') return 'off';
  if(s.health==='ok') return 'ok';
  if(s.health==='warn') return 'warn';
  if(s.health==='bad') return 'bad';
  return 'ok';
}
function getLatencyClass(lat){
  if(lat<100) return 'ok';
  if(lat<250) return 'slow';
  return 'bad';
}
function renderServers(){
  const q=(serverSearch?.value||'').toLowerCase();
  const sq=(sidebarSearch?.value||'').toLowerCase();
  const sort=serverSort?.value||'name';
  // top nodes — removed from header (clean header)
  if(topNodes) topNodes.innerHTML = '';
  // sidebar — filtered
  const sideFiltered = servers.filter(s=> !sq || s.name.toLowerCase().includes(sq) || s.tools.toLowerCase().includes(sq) || s.endpoint.toLowerCase().includes(sq) || (s.desc||'').toLowerCase().includes(sq));
  sidebarList.innerHTML = sideFiltered.map(s=>{
    const on=s.status==='connected', busy=s.status==='connecting';
    const latCls=getLatencyClass(s.latency||0);
    return `<li class="server-item ${s.status}" data-id="${s.id}" title="${esc(s.desc||s.endpoint)}"><span class="server-ic ${s.color}"><i data-lucide="${s.color==='pg'?'database': s.color==='aws'?'cloud': s.color==='fs'?'folder-open':'server'}" style="width:16px;height:16px"></i></span><span class="server-meta"><strong>${esc(s.name)}</strong><span>${esc(s.transport)} • ${esc(s.endpoint.slice(0,28))} ${s.env?.length?`• ${s.env.length} env`:''}</span></span><span class="health ${getHealthClass(s)}" title="${s.health}"></span><span class="chip-dot ${on?'on':busy?'':'off'}" style="display:none"></span></li>`;
  }).join('') || `<li style="font-size:12px;color:#94a3b8;padding:8px">No matches</li>`;
  // drawer — filtered by tab + search + sort
  let filtered = servers.filter(s=>{
    const matchSearch = !q || s.name.toLowerCase().includes(q) || s.tools.toLowerCase().includes(q) || s.endpoint.toLowerCase().includes(q) || (s.desc||'').toLowerCase().includes(q);
    const matchTab = activeServerTab==='all' || (activeServerTab==='connected'&&s.status==='connected') || (activeServerTab==='disconnected'&&s.status!=='connected') || (activeServerTab===s.transport);
    return matchSearch && matchTab;
  });
  if(sort==='name') filtered.sort((a,b)=> a.name.localeCompare(b.name));
  else if(sort==='status') filtered.sort((a,b)=> (a.status==='connected'?0:1) - (b.status==='connected'?0:1) || a.name.localeCompare(b.name));
  else if(sort==='recent') filtered.sort((a,b)=> (b.lastSeen||0)-(a.lastSeen||0));
  else if(sort==='latency') filtered.sort((a,b)=> (a.latency||999)-(b.latency||999));
  // tabs ui
  if(serverTabs){
    if(serverTabs) serverTabs.querySelectorAll('button').forEach(b=> b.classList.toggle('active', b.dataset.stab===activeServerTab));
    serverTabs.querySelectorAll('button').forEach(b=>{
      if(b.dataset.stab===activeServerTab){ b.style.background='var(--text)'; b.style.color='#fff'; b.style.borderColor='var(--text)'; }
      else { b.style.background='#fff'; b.style.color='var(--muted)'; b.style.borderColor='var(--line)'; }
    });
  }
  serversList.innerHTML = filtered.map(s=>{
    const ic=s.color, iname=s.color==='pg'?'database': s.color==='aws'?'cloud': s.color==='fs'?'folder-open':'server';
    const label=s.status==='connected'?'Connected': s.status==='connecting'?'Connecting':'Disconnected';
    const cls=s.status==='connected'?'on': s.status==='connecting'?'busy':'off';
    const healthCls=getHealthClass(s);
    const tools = s.tools ? s.tools.split(',').map(t=>t.trim()).filter(Boolean) : [];
    const latCls=getLatencyClass(s.latency||0);
    const envCount=s.env?.length||0;
    return `<div class="server-card" data-id="${s.id}" style="${s.status==='connected'?'border-color:#bbf7d0':''}">
      <div class="server-card-top">
        <span class="ic ${ic}"><i data-lucide="${iname}" style="width:16px;height:16px"></i></span>
        <span class="meta" style="flex:1"><b style="display:flex;align-items:center;gap:6px">${esc(s.name)} <span class="health ${healthCls}" title="Health: ${s.health}"></span> ${s.autoConnect?'<span style="font-size:10px;background:#eff6ff;color:var(--primary);border:1px solid #dbeafe;padding:2px 6px;border-radius:999px">AUTO</span>':''}</b>
          <p>${esc(s.transport)} • ${esc(s.endpoint)}${s.desc?` • ${esc(s.desc)}`:''}</p>
          <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:6px">
            ${tools.slice(0,3).map(t=>`<span class="tools">${esc(t)}</span>`).join('')}${tools.length>3?`<span class="tools">+${tools.length-3}</span>`:''}
            ${envCount?`<span class="tools" title="Env vars">${envCount} env</span>`:''}
            <span class="latency ${latCls}">${s.latency||'?'}ms</span>
            <span style="font-size:11px;color:var(--faint);font-family:var(--mono)">${s.lastSeen? new Date(s.lastSeen).toLocaleTimeString():''}</span>
          </div>
        </span>
        <label class="switch"><input type="checkbox" ${s.status==='connected'?'checked':''} data-toggle="${s.id}"><span></span></label>
      </div>
      <div class="server-card-foot">
        <span class="status ${cls}"><span class="health ${healthCls}" style="width:6px;height:6px"></span> ${label}</span>
        <span style="flex:1"></span>
        <button class="mini" data-test="${s.id}"><i data-lucide="activity" style="width:12px;height:12px"></i> Test</button>
        <button class="mini" data-logs="${s.id}"><i data-lucide="scroll-text" style="width:12px;height:12px"></i> Logs</button>
        <button class="mini" data-edit="${s.id}"><i data-lucide="pencil" style="width:12px;height:12px"></i></button>
        <button class="mini" data-dup="${s.id}"><i data-lucide="copy" style="width:12px;height:12px"></i></button>
        <button class="mini danger" data-del="${s.id}"><i data-lucide="trash-2" style="width:12px;height:12px"></i></button>
      </div>
    </div>`;
  }).join('') || `<div style="text-align:center;padding:24px;background:#fff;border:1px dashed var(--line);border-radius:12px"><i data-lucide="search-x" style="width:24px;height:24px;color:#cbd5e1"></i><p style="margin:8px 0 0;font-weight:700">No servers match</p><p style="font-size:12px;color:var(--muted)">Try another search or add a new server</p></div>`;
  // stats
  const connected=servers.filter(s=>s.status==='connected').length;
  const avgLat = servers.length? Math.round(servers.reduce((a,s)=>a+(s.latency||0),0)/servers.length):0;
  if(srvStatTotal) srvStatTotal.textContent=String(servers.length);
  if(srvStatConnected) srvStatConnected.textContent=String(connected);
  if(srvStatLatency) srvStatLatency.textContent= avgLat ? avgLat+'ms' : '—';
  if(sidebarCount) sidebarCount.textContent=String(servers.length);
  if(activeCount2) activeCount2.textContent=String(connected);
  if(tunnelBar) tunnelBar.style.width=`${servers.length?Math.round(connected/servers.length*100):0}%`;
  if(tunnelStatus) tunnelStatus.innerHTML=connected? `Secure MCP tunnel • <strong>${connected}</strong> connected` : `No MCP nodes connected`;
  if(serverCountInfo) serverCountInfo.textContent=`${filtered.length} of ${servers.length} servers`;
  refreshIcons();
}
function initHealthPoll(){
  if(serverHealthTimer){ clearInterval(serverHealthTimer); serverHealthTimer=null; }
  if(healthPoll?.checked){
    serverHealthTimer=setInterval(()=> doHealthCheck(false), 30000);
  }
}
function doHealthCheck(showToast=true){
  // mock: randomize latency/health for connected servers
  let changed=false;
  servers.forEach(s=>{
    if(s.status==='connected' && s.healthCheck){
      const prev=s.health;
      const roll=Math.random();
      if(roll>0.85) s.health='warn';
      else if(roll>0.97) s.health='bad';
      else s.health='ok';
      s.latency = Math.floor(40 + Math.random()*180);
      s.lastSeen=Date.now();
      if(prev!==s.health) changed=true;
    }
  });
  if(changed || showToast){
    saveServers(); renderServers();
    if(showToast) toast('Health check complete ✓');
  } else {
    saveServers(); renderServers();
  }
}
function openServerDetail(id){
  const s=servers.find(x=>x.id===id); if(!s) return;
  const tools=s.tools.split(',').map(t=>t.trim()).filter(Boolean);
  const env=s.env||[];
  serverDetailContent.innerHTML=`
    <div class="server-detail-hero">
      <div style="display:flex;gap:12px;align-items:center">
        <div style="width:42px;height:42px;border-radius:12px;background:rgba(255,255,255,.12);display:grid;place-items:center"><i data-lucide="${s.color==='pg'?'database': s.color==='aws'?'cloud':'folder-open'}" style="width:20px;height:20px;color:#fff"></i></div>
        <div><h3 style="margin:0;font-size:16px;font-weight:800">${esc(s.name)}</h3><p style="margin:4px 0 0;font-size:12px;opacity:.8">${esc(s.transport)} • ${esc(s.endpoint)} • ${s.status}</p></div>
        <button class="icon-btn" id="closeServerDetail" style="margin-left:auto;background:rgba(255,255,255,.12);color:#fff"><i data-lucide="x"></i></button>
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:12px">
        <span style="background:rgba(34,197,94,.14);color:#86efac;border:1px solid rgba(34,197,94,.2);padding:4px 8px;border-radius:999px;font-size:11px;font-weight:700">${s.health==='ok'?'✓ Healthy': s.health==='warn'?'⚠ Degraded':'● '+s.health} • ${s.latency}ms</span>
        <span style="background:rgba(255,255,255,.12);padding:4px 8px;border-radius:999px;font-size:11px;font-family:var(--mono)">timeout ${s.timeout}ms • retries ${s.retries}</span>
        ${s.autoConnect?'<span style="background:#eff6ff;color:var(--primary);padding:4px 8px;border-radius:999px;font-size:11px;font-weight:700">Auto-connect</span>':''}
      </div>
    </div>
    <div class="server-detail-body">
      ${s.desc?`<p style="margin:0;font-size:13px;color:var(--muted);line-height:1.6">${esc(s.desc)}</p>`:''}
      <div>
        <h4 style="margin:0 0 8px;font-size:11px;letter-spacing:.06em;color:var(--muted)">TOOLS (${tools.length})</h4>
        <div style="display:flex;gap:6px;flex-wrap:wrap">${tools.map(t=>`<span class="tools" style="background:#fff">${esc(t)}</span>`).join('') || '<span style="font-size:12px;color:var(--faint)">Auto-discover on connect</span>'}</div>
      </div>
      ${env.length?`<div><h4 style="margin:0 0 8px;font-size:11px;letter-spacing:.06em;color:var(--muted)">ENVIRONMENT</h4><div style="display:flex;flex-direction:column;gap:6px">${env.map(e=>`<div style="display:flex;gap:8px;align-items:center;background:#f8fafc;border:1px solid var(--line);padding:8px 10px;border-radius:8px;font-family:var(--mono);font-size:11px"><span style="color:var(--muted)">${esc(e.k)}=</span><span style="flex:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(e.v)}</span></div>`).join('')}</div></div>`:''}
      <div>
        <h4 style="margin:0 0 8px;font-size:11px;letter-spacing:.06em;color:var(--muted)">LOGS (mock)</h4>
        <div class="log-box">
          <div class="muted">[${new Date(s.lastSeen).toLocaleTimeString()}] → handshake ${s.transport} ${esc(s.endpoint)}</div>
          <div class="${s.status==='connected'?'ok':'err'}">[${new Date().toLocaleTimeString()}] ${s.status==='connected'?'✓ Connected • tools: '+tools.slice(0,2).join(', '):'○ Disconnected • last error: timeout'}</div>
          <div class="muted">[${new Date().toLocaleTimeString()}] latency ${s.latency}ms • health ${s.health}</div>
          <div class="ok">[${new Date().toLocaleTimeString()}] ready for MCP invoke</div>
        </div>
      </div>
      <div style="background:#f8fafc;border:1px solid var(--line);border-radius:10px;padding:10px;font-family:var(--mono);font-size:11px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px"><span style="font-weight:700;letter-spacing:.06em;color:var(--muted)">CONFIG</span><button class="ghost-btn sm" id="copyServerConfig" style="padding:4px 8px;font-size:11px"><i data-lucide="copy" style="width:12px;height:12px"></i> Copy</button></div>
        <div style="white-space:pre-wrap;word-break:break-all" id="serverConfigJson">${esc(JSON.stringify({name:s.name, transport:s.transport, endpoint:s.endpoint, tools:s.tools, timeout:s.timeout, retries:s.retries, env:env.reduce((o,e)=> (o[e.k]=e.v,o),{})}, null, 2))}</div>
      </div>
      <div style="display:flex;gap:8px">
        <button class="primary-btn" id="detailToggle" style="flex:1;justify-content:center"><i data-lucide="${s.status==='connected'?'plug-zap':'plug'}" style="width:14px;height:14px"></i> ${s.status==='connected'?'Disconnect':'Connect'}</button>
        <button class="ghost-btn" id="detailEdit" style="flex:1;justify-content:center"><i data-lucide="pencil" style="width:14px;height:14px"></i> Edit</button>
      </div>
    </div>
  `;
  serverDetailModal?.classList.remove('hidden'); refreshIcons();
  const _closeDetail = $('#closeServerDetail'); if(_closeDetail) _closeDetail.onclick = ()=> serverDetailModal?.classList.add('hidden');
  const _copyCfg = $('#copyServerConfig'); if(_copyCfg) _copyCfg.onclick = ()=>{ navigator.clipboard.writeText($('#serverConfigJson')?.textContent||'').catch(()=>{}); toast('Config copied'); };
  const _detToggle = $('#detailToggle'); if(_detToggle) _detToggle.onclick = async ()=>{ await toggle(s.id); serverDetailModal?.classList.add('hidden'); };
  const _detEdit = $('#detailEdit'); if(_detEdit) _detEdit.onclick = ()=>{ serverDetailModal?.classList.add('hidden'); openEditServer(s.id); };
}
function openEditServer(id){
  const s=servers.find(x=>x.id===id); if(!s) return;
  editingId=s.id;
  serverModalTitle.textContent='Edit MCP Server';
  srvName.value=s.name; srvTransport.value=s.transport; srvEndpoint.value=s.endpoint; srvTools.value=s.tools; if(srvDesc) srvDesc.value=s.desc||''; if(srvTimeout) srvTimeout.value=String(s.timeout); if(srvRetries) srvRetries.value=String(s.retries); if(srvAuto) srvAuto.checked=!!s.autoConnect; if(srvHealth) srvHealth.checked=!!s.healthCheck;
  envDraft=[...(s.env||[])];
  renderEnv(); updateEndpointHint(); serverModal?.classList.remove('hidden'); refreshIcons();
}
function renderEnv(){
  if(!envList) return;
  envList.innerHTML= envDraft.map((e,i)=>`
    <div class="env-row"><input placeholder="KEY" value="${esc(e.k)}" data-ek="${i}"><input placeholder="value" value="${esc(e.v)}" data-ev="${i}"><button type="button" data-rm-env="${i}"><i data-lucide="x" style="width:12px;height:12px"></i></button></div>
  `).join('') || `<div style="font-size:12px;color:var(--faint);text-align:center;padding:8px">No env vars — click Add</div>`;
  refreshIcons();
}
function updateEndpointHint(){
  if(!srvEndpointHint || !srvTransport) return;
  if(srvTransport.value==='stdio') srvEndpointHint.textContent='STDIO: npx command — e.g. npx @modelcontextprotocol/server-postgres';
  else if(srvTransport.value==='sse') srvEndpointHint.textContent='SSE: wss:// or https:// URL for Server-Sent Events';
  else srvEndpointHint.textContent='HTTP: https:// URL for Streamable HTTP';
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
  if(!pending.length){ attachPreview?.classList.add('hidden'); attachPreview.innerHTML=''; return; }
  attachPreview?.classList.remove('hidden');
  attachPreview.innerHTML=pending.map(p=>{
    let inner='';
    if(p.file.type.startsWith('image/')) inner=`<img src="${p.url}" alt="">`;
    else if(p.file.type.startsWith('video/')) inner=`<video src="${p.url}" muted></video>`;
    else inner=`<div class="fn"><i data-lucide="file"></i><span>${esc(p.file.name.slice(0,18))}</span><small>${(p.file.size/1024).toFixed(1)} KB</small></div>`;
    return `<div class="preview">${inner}<button class="rm" data-rm="${p.id}"><i data-lucide="x"></i></button></div>`;
  }).join(''); refreshIcons();
}
attachPreview?.addEventListener('click', e=>{
  const b=e.target.closest('[data-rm]'); if(!b) return;
  const id=b.dataset.rm; const i=pending.findIndex(p=>p.id===id);
  if(i>=0){ if(pending[i].url) URL.revokeObjectURL(pending[i].url); pending.splice(i,1); drawPreview(); syncComposer(); }
});
mediaInput?.addEventListener('change', e=>{ if(e.target.files.length) addFiles([...e.target.files]); e.target.value=''; });

// Todo draft
function renderTodo(){
  if(todoDraft && !todos.length && todoDraft?.classList.contains('hidden')){ if(todoProgress) todoProgress.textContent=`0/0`; return; }
  if(!todoDraft || !todoList) return;
  todoList.innerHTML=todos.map((t,i)=>`<li><input type="checkbox" ${t.done?'checked':''} data-i="${i}"><input value="${esc(t.text)}" data-input="${i}"><button class="icon-btn xs" data-del="${i}"><i data-lucide="x" style="width:12px;height:12px"></i></button></li>`).join('');
  const done=todos.filter(t=>t.done).length;
  todoProgress.textContent=`${done}/${todos.length}`;
  refreshIcons();
}
todoToggle?.addEventListener('click', ()=>{
  todoDraft?.classList.toggle('hidden');
  if(!todoDraft?.classList.contains('hidden')) todoInput.focus();
  refreshIcons();
});
clearTodo?.addEventListener('click', ()=>{ todos=[]; todoDraft?.classList.add('hidden'); renderTodo(); });
addTodoBtn?.addEventListener('click', ()=>{
  const v=todoInput.value.trim(); if(!v) return;
  todos.push({text:v, done:false}); todoInput.value=''; renderTodo();
});
todoInput?.addEventListener('keydown', e=>{ if(e.key==='Enter'){ e.preventDefault(); addTodoBtn.click(); }});
todoList?.addEventListener('change', e=>{
  const c=e.target.closest('[data-i]'); if(c && e.target.type==='checkbox'){ todos[Number(c.dataset.i)].done=e.target.checked; renderTodo(); }
});
todoList?.addEventListener('input', e=>{
  const inp=e.target.closest('[data-input]'); if(inp) todos[Number(inp.dataset.input)].text=inp.value;
});
todoList?.addEventListener('click', e=>{
  const b=e.target.closest('[data-del]'); if(b){ todos.splice(Number(b.dataset.del),1); renderTodo(); }
});

// Composer
function autoResize(){ if(!inputEl) return; inputEl.style.height='auto'; inputEl.style.height=Math.min(inputEl.scrollHeight,120)+'px'; }
function syncComposer(){
  const hasText=(inputEl?.value||'').trim().length>0;
  const hasMedia=pending.length>0;
  if(sendBtn) sendBtn.disabled=(!hasText && !hasMedia && !todos.length) || isTyping;
  if(charCount){ charCount.textContent=inputEl?.value?.length? `${inputEl.value.length}/2000` : ''; charCount.style.color=(inputEl?.value?.length||0)>1800?'#ef4444':'#94a3b8'; }
  try{ localStorage.setItem('mcp_draft', inputEl?.value||''); }catch{}
  // show prompt strip when empty
  if(promptStrip){ if(!hasText && !hasMedia && !isTyping) promptStrip?.classList.remove('hidden'); else promptStrip?.classList.add('hidden'); }
}
const PROMPT_CHIPS = ["Summarize S3 buckets","Query users table","List /data files","Draft release notes","Explain TLS tunnel"];
function renderPromptStrip(){
  promptStrip.innerHTML = PROMPT_CHIPS.map(p=>`<button class="prompt-chip" data-prompt="${esc(p)}"><i data-lucide="sparkles"></i> ${esc(p)}</button>`).join('');
  refreshIcons();
}
promptStrip?.addEventListener('click', e=>{
  const b=e.target.closest('[data-prompt]'); if(!b) return;
  inputEl.value=b.dataset.prompt; autoResize(); syncComposer(); inputEl.focus();
});
try{ inputEl.value=localStorage.getItem('mcp_draft')||''; }catch{ inputEl.value=''; }
autoResize(); syncComposer(); renderPromptStrip();

inputEl?.addEventListener('input', ()=>{
  autoResize(); syncComposer();
  const pos=inputEl.selectionStart; const before=inputEl.value.slice(0,pos);
  const m=before.match(/(^|\s)(\/[a-z0-9-]*)$/i);
  if(!m){ hideSlash(); return; }
  const q=m[2].toLowerCase();
  filtered=SLASH.filter(c=>c.key.toLowerCase().startsWith(q));
  if(!filtered.length){ hideSlash(); return; }
  slashIndex=0; renderSlash();
});
function renderSlash(){
  slashMenu.innerHTML=filtered.map((c,i)=>`<div class="s-item ${i===slashIndex?'active':''}" data-i="${i}"><span class="s-key">${esc(c.key)}</span><span class="s-desc">${esc(c.desc)}</span><span class="s-hint">${esc(c.hint)}</span></div>`).join('');
  slashMenu?.classList.remove('hidden');
}
function hideSlash(){ slashMenu?.classList.add('hidden'); filtered=[]; }
function pickSlash(cmd){
  if(!cmd) return;
  const pos=inputEl.selectionStart; const before=inputEl.value.slice(0,pos); const after=inputEl.value.slice(pos);
  inputEl.value=before.replace(/\/[a-z0-9-]*$/i, cmd.insert)+after;
  hideSlash(); autoResize(); syncComposer(); inputEl.focus();
  if(cmd.key==='/clear') clearCurrentChat();
  if(cmd.key==='/help') doSend('/help');
}
slashMenu?.addEventListener('click', e=>{ const it=e.target.closest('.s-item'); if(it) pickSlash(filtered[Number(it.dataset.i)]); });
inputEl?.addEventListener('keydown', e=>{
  if(!slashMenu?.classList.contains('hidden')){
    if(e.key==='ArrowDown'){ e.preventDefault(); slashIndex=(slashIndex+1)%filtered.length; renderSlash(); }
    else if(e.key==='ArrowUp'){ e.preventDefault(); slashIndex=(slashIndex-1+filtered.length)%filtered.length; renderSlash(); }
    else if(e.key==='Enter'||e.key==='Tab'){ e.preventDefault(); pickSlash(filtered[slashIndex]); return; }
    else if(e.key==='Escape') hideSlash();
  }
  if(e.key==='Enter' && !e.shiftKey){
    if(!slashMenu?.classList.contains('hidden') && filtered.length){ e.preventDefault(); pickSlash(filtered[slashIndex]); return; }
    e.preventDefault(); doSend();
  }
  if((e.ctrlKey||e.metaKey) && e.key==='z' && !e.shiftKey){ e.preventDefault(); undo(); }
  if((e.ctrlKey||e.metaKey) && (e.key==='y' || (e.key==='z' && e.shiftKey))){ e.preventDefault(); redo(); }
});
document.addEventListener('click', e=>{ if(!slashMenu || !inputEl) return; if(!slashMenu.contains(e.target) && e.target!==inputEl) hideSlash(); });

// Speech — single + Live Conversation (complete)
let liveActive=false, liveMuted=false, livePaused=false, liveSpeaker=true;
let liveRecognition=null, liveFinal='', liveInterimText='', livePauseTimer=null, liveStartTime=null, liveTimerInt=null;
let speakingUtterance=null;
function initSpeech(){
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  if(!SR){
    micBtn.title='Speech not supported — mock mode';
    liveBtn.title='Live (mock) — click to start';
    if(liveVoiceSelect) populateVoices();
    return;
  }
  recognition=new SR();
  recognition.continuous=true; recognition.interimResults=true; recognition.lang= liveLangSelect?.value || 'en-US';
  recognition.onstart=()=>{ isRecording=true; speechBar?.classList.remove('hidden'); micBtn.classList.add('active'); liveStatusText && (liveStatusText.textContent='Listening…'); };
  recognition.onend=()=>{ if(!liveActive){ isRecording=false; speechBar?.classList.add('hidden'); micBtn.classList.remove('active'); } else if(liveActive && !liveMuted && !livePaused){ try{ recognition.start(); }catch{} }};
  recognition.onresult=(e)=>{
    let interim='', final='';
    for(let i=e.resultIndex;i<e.results.length;i++){
      const r=e.results[i];
      if(r.isFinal) final+=r[0].transcript+' ';
      else interim+=r[0].transcript;
    }
    if(liveActive){
      // live mode: show interim, handle auto-send
      if(interim){
        liveInterim.textContent=interim;
        liveInterim.style.opacity='1';
        // interrupt TTS if speaking
        if(speechSynthesis.speaking && !liveMuted){ speechSynthesis.cancel(); setLiveWave('listening'); liveStatusText.textContent='Listening… (interrupted)'; }
      }
      if(final){
        final=final.trim();
        liveInterim.textContent='';
        // append to transcript
        if(liveTranscript){
          if(liveTranscript.querySelector('.live-hint')) liveTranscript.innerHTML='';
          const div=document.createElement('div');
          div.className='live-bubble user';
          div.innerHTML=`${esc(final)}<div class="meta">You • ${new Date().toLocaleTimeString()}</div>`;
          liveTranscript.appendChild(div);
          liveTranscript.scrollTop=liveTranscript.scrollHeight;
          // also add to liveHistory
          addLiveHistory('user', final);
          // show wave thinking
          setLiveWave('thinking'); liveStatusText.textContent='Thinking…';
          // auto-send after short pause if enabled
          if(liveAutoSend?.checked){
            clearTimeout(livePauseTimer);
            livePauseTimer=setTimeout(()=> { handleLiveSend(final); }, 650);
          } else {
            // put into input for manual send
            inputEl.value=(inputEl.value+' '+final).trim(); autoResize(); syncComposer();
          }
        } else {
          inputEl.value=(inputEl.value+' '+final).trim(); autoResize(); syncComposer();
        }
      }
    } else {
      // single mode
      if(interim) liveInterim && (liveInterim.textContent=interim);
      if(final){ inputEl.value=(inputEl.value+' '+final).trim(); autoResize(); syncComposer(); if(liveInterim) liveInterim.textContent=''; }
    }
  };
  recognition.onerror=()=>{ isRecording=false; if(speechBar) speechBar?.classList.add('hidden'); if(micBtn) micBtn.classList.remove('active'); if(liveActive) setLiveWave('paused'); toast('Mic error'); };
  if(liveLangSelect) liveLangSelect.addEventListener('change', ()=>{ recognition.lang=liveLangSelect.value; toast('Language: '+liveLangSelect.value); });
}
initSpeech();
function populateVoices(){
  if(!('speechSynthesis' in window) || !liveVoiceSelect) return;
  const voices=speechSynthesis.getVoices();
  if(!voices.length) { setTimeout(populateVoices, 300); return; }
  liveVoiceSelect.innerHTML='<option>System default</option>'+voices.map(v=>`<option value="${esc(v.name)}">${esc(v.name)} (${v.lang})</option>`).join('');
}
if('speechSynthesis' in window){
  speechSynthesis.onvoiceschanged=populateVoices;
  populateVoices();
}
function setLiveWave(state){
  if(!liveWave) return;
  liveWave.className='live-wave '+state;
  if(liveWaveWrap){
    if(state==='listening') liveWaveWrap.style.opacity='1';
    if(state==='speaking') liveWaveWrap.style.opacity='1';
    if(state==='thinking'){ liveWave.className='live-wave paused'; liveWaveWrap.style.opacity='.9'; }
    if(state==='paused'){ liveWave.className='live-wave paused'; }
  }
}
function openLive(){
  liveActive=true; liveMuted=false; livePaused=false;
  liveOverlay?.classList.remove('hidden');
  liveBtn.classList.add('live-active');
  liveBtn.title='Live active — click to end';
  liveTranscript.innerHTML=`<div class="live-hint"><i data-lucide="mic" style="width:28px;height:28px;color:var(--primary)"></i><p>Live listening — speak naturally<br><small>Pause to send • I’ll respond with voice</small></p></div>`;
  liveHistory.innerHTML='';
  liveInterim.textContent='';
  liveStatusText.textContent='Listening…';
  setLiveWave('listening');
  liveStartTime=Date.now();
  liveTimerInt=setInterval(()=>{
    const s=Math.floor((Date.now()-liveStartTime)/1000);
    const m=String(Math.floor(s/60)).padStart(2,'0'), sec=String(s%60).padStart(2,'0');
    if(liveTimer) liveTimer.textContent=`${m}:${sec}`;
  },1000);
  if(liveModel) liveModel.textContent=`${selectedModel} • ${selectedMode}`;
  refreshIcons();
  // start recognition
  if(recognition){
    try{ recognition.lang=liveLangSelect?.value||'en-US'; recognition.start(); }catch{ isRecording=true; }
  } else {
    // mock live: simulate listening
    isRecording=true; toast('Live (mock) started — try speaking');
    // mock interim simulation? keep simple
  }
  toast('Live conversation started — hands-free');
}
function closeLive(){
  liveActive=false; isRecording=false;
  liveOverlay?.classList.add('hidden');
  liveBtn.classList.remove('live-active');
  liveBtn.title='Live conversation — talk hands-free';
  setLiveWave('paused');
  clearInterval(liveTimerInt);
  clearTimeout(livePauseTimer);
  liveInterim.textContent='';
  if(recognition){ try{ recognition.stop(); }catch{} }
  if(speechSynthesis.speaking) speechSynthesis.cancel();
  speechBar?.classList.add('hidden'); micBtn.classList.remove('active');
  toast('Live ended');
}
function toggleLive(){ liveActive ? closeLive() : openLive(); }
function addLiveHistory(role, text){
  if(!liveHistory) return;
  const div=document.createElement('div');
  div.className=`live-bubble ${role}`;
  const who= role==='user' ? 'You' : `${selectedModel}`;
  div.innerHTML=`${esc(text)}<div class="meta">${esc(who)} • ${new Date().toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}</div>`;
  liveHistory.appendChild(div);
  liveHistory.scrollTop=liveHistory.scrollHeight;
}
async function handleLiveSend(finalText){
  if(!finalText.trim()) return;
  // add to main chat as well
  const curTodos = todos.length? [...todos] : [];
  // clear interim
  liveInterim.textContent='';
  setLiveWave('thinking'); liveStatusText.textContent='Thinking…';
  // send via existing flow but capture assistant response to speak
  const chat=getActiveChat();
  if(!chat){ toast('Create a chat first'); setLiveWave('listening'); liveStatusText.textContent='Listening…'; return; }
  // use sendAndStream but we need to intercept for live
  const prevLen=chat.messages.length;
  await sendAndStream(finalText, [], curTodos, detect(finalText), false);
  // get last assistant message
  const last=chat.messages[chat.messages.length-1];
  let speakText = last?.text || (last?.html==='pg' ? 'Retrieved 5 records from remote_db.users' : last?.html==='aws' ? 'Found 3 buckets in us-east-1' : last?.html==='fs' ? 'Listed 5 entries in data' : 'Done');
  if(last?.tasks) speakText += ' — tasks updated';
  // add to live history
  addLiveHistory('assistant', speakText.slice(0,180));
  // speak if enabled
  if(liveSpeakBack?.checked && 'speechSynthesis' in window && liveSpeaker){
    speakLive(speakText);
  } else {
    setLiveWave('listening'); liveStatusText.textContent='Listening…';
  }
  // update live transcript area to show assistant snippet
  if(liveTranscript && last){
    const div=document.createElement('div');
    div.className='live-bubble assistant';
    div.innerHTML=`${esc(speakText.slice(0,160))}<div class="meta">${esc(selectedModel)} • now</div>`;
    liveTranscript.appendChild(div);
    liveTranscript.scrollTop=liveTranscript.scrollHeight;
  }
}
function speakLive(text){
  if(!('speechSynthesis' in window)) return;
  speechSynthesis.cancel();
  const utter=new SpeechSynthesisUtterance(text);
  const voices=speechSynthesis.getVoices();
  const chosen= liveVoiceSelect?.value;
  if(chosen && chosen!=='System default'){
    const v=voices.find(x=>x.name===chosen);
    if(v) utter.voice=v;
  }
  utter.rate=1; utter.pitch=1;
  utter.onstart=()=>{ setLiveWave('speaking'); liveStatusText.textContent='Speaking… (speak to interrupt)'; };
  utter.onend=()=>{ if(liveActive && !livePaused && !liveMuted){ setLiveWave('listening'); liveStatusText.textContent='Listening…'; try{ if(recognition && liveActive) recognition.start(); }catch{} } };
  utter.onerror=()=>{ setLiveWave('listening'); liveStatusText.textContent='Listening…'; };
  speakingUtterance=utter;
  setLiveWave('speaking'); liveStatusText.textContent='Speaking…';
  speechSynthesis.speak(utter);
}
function toggleSpeech(){
  if(liveActive){
    // in live, mic toggle is mute
    liveMuted=!liveMuted;
    if(liveMuted){
      if(recognition && isRecording) try{ recognition.stop(); }catch{}
      setLiveWave('paused'); liveStatusText.textContent='Muted — tap to unmute';
      liveMicBtn.classList.add('muted'); liveMuteBtn.style.background='#fef2f2'; liveMuteBtn.style.color='var(--bad)';
      toast('Mic muted');
    } else {
      setLiveWave('listening'); liveStatusText.textContent='Listening…';
      liveMicBtn.classList.remove('muted'); liveMuteBtn.style.background='#fff'; liveMuteBtn.style.color='';
      if(recognition) try{ recognition.start(); }catch{}
      toast('Mic unmuted');
    }
    return;
  }
  // single mode
  if(!recognition){
    if(!isRecording){ isRecording=true; speechBar?.classList.remove('hidden'); micBtn.classList.add('active'); toast('Listening (mock)… say something'); setTimeout(()=>{ if(isRecording){ inputEl.value=(inputEl.value+' list S3 buckets').trim(); autoResize(); syncComposer(); stopSpeechClick(); }}, 1800); }
    else stopSpeechClick();
    return;
  }
  if(isRecording) recognition.stop();
  else recognition.start();
}
function stopSpeechClick(){ if(recognition && isRecording) recognition.stop(); isRecording=false; speechBar?.classList.add('hidden'); micBtn.classList.remove('active'); }
micBtn?.addEventListener('click', toggleSpeech);
stopSpeech?.addEventListener('click', stopSpeechClick);
// Live controls
liveBtn?.addEventListener('click', toggleLive);
liveBackdrop?.addEventListener('click', closeLive);
closeLiveEl?.addEventListener('click', closeLive);
endLiveBtn?.addEventListener('click', closeLive);
liveMicBtn?.addEventListener('click', ()=>{
  if(!liveActive) return openLive();
  liveMuted=!liveMuted;
  if(liveMuted){
    liveMicBtn.classList.add('muted'); liveMicBtn.innerHTML='<i data-lucide="mic-off"></i>';
    liveMuteBtn.style.background='#fef2f2';
    setLiveWave('paused'); liveStatusText.textContent='Muted';
    if(recognition && isRecording) try{ recognition.stop(); }catch{}
    if(speechSynthesis.speaking) speechSynthesis.cancel();
  } else {
    liveMicBtn.classList.remove('muted'); liveMicBtn.innerHTML='<i data-lucide="mic"></i>';
    liveMuteBtn.style.background='#fff';
    setLiveWave('listening'); liveStatusText.textContent='Listening…';
    if(recognition) try{ recognition.start(); }catch{}
  }
  refreshIcons();
});
liveMuteBtn?.addEventListener('click', ()=> liveMicBtn.click());
liveSpeakerBtn?.addEventListener('click', ()=>{
  liveSpeaker=!liveSpeaker;
  liveSpeakerBtn.style.background= liveSpeaker ? '#fff' : '#f1f5f9';
  liveSpeakerBtn.style.color= liveSpeaker ? '' : 'var(--faint)';
  if(!liveSpeaker && speechSynthesis.speaking) speechSynthesis.cancel();
  toast(liveSpeaker?'Speaker on':'Speaker off');
});
livePauseBtn?.addEventListener('click', ()=>{
  livePaused=!livePaused;
  if(livePaused){
    livePauseBtn.innerHTML='<i data-lucide="play" style="width:12px;height:12px"></i> Resume';
    setLiveWave('paused'); liveStatusText.textContent='Paused';
    if(recognition && isRecording) try{ recognition.stop(); }catch{}
    if(speechSynthesis.speaking) speechSynthesis.pause();
  } else {
    livePauseBtn.innerHTML='<i data-lucide="pause" style="width:12px;height:12px"></i> Pause';
    setLiveWave('listening'); liveStatusText.textContent='Listening…';
    if(speechSynthesis.paused) speechSynthesis.resume();
    else if(recognition) try{ recognition.start(); }catch{}
  }
  refreshIcons();
});
liveSettingsBtn?.addEventListener('click', ()=> liveSettings.classList.toggle('hidden'));
liveVoiceSelect?.addEventListener('change', ()=> toast('Voice: '+liveVoiceSelect.value));
liveLangSelect?.addEventListener('change', ()=>{ if(recognition) recognition.lang=liveLangSelect.value; });

// Drag & paste
;;['dragenter','dragover'].forEach(ev=> document.addEventListener(ev, e=>{ e.preventDefault(); if(ev==='dragenter') dropOverlay?.classList.remove('hidden'); composerInner?.classList.add('drag'); }));
;['dragleave','drop'].forEach(ev=> document.addEventListener(ev, e=>{ if(ev==='dragleave' && e.target===dropOverlay) dropOverlay?.classList.add('hidden'); if(ev==='drop') dropOverlay?.classList.add('hidden'); composerInner?.classList.remove('drag'); }));
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
  try{ chats=JSON.parse(prev); }catch{ return toast('Undo data corrupt'); }
  saveChats(); if(!chats.find(c=>c.id===activeChatId)) activeChatId=chats[0]?.id||null; if(activeChatId) try{ localStorage.setItem('activeChat', activeChatId);}catch{} else try{ localStorage.removeItem('activeChat');}catch{}
  renderChats(); renderMessages(); updateUndoRedo(); toast('Undone');
}
function redo(){
  if(!history.redo.length) return toast('Nothing to redo');
  const cur=JSON.stringify(chats);
  history.undo.push(cur);
  const nxt=history.redo.pop();
  try{ chats=JSON.parse(nxt); }catch{ return toast('Redo data corrupt'); }
  saveChats(); if(!chats.find(c=>c.id===activeChatId)) activeChatId=chats[0]?.id||null; if(activeChatId) try{ localStorage.setItem('activeChat', activeChatId);}catch{} 
  renderChats(); renderMessages(); updateUndoRedo(); toast('Redone');
}
undoBtn?.addEventListener('click', undo);
redoBtn?.addEventListener('click', redo);
document.addEventListener('keydown', e=>{
  if((e.ctrlKey||e.metaKey) && e.key.toLowerCase()==='z' && !e.shiftKey){ e.preventDefault(); undo(); }
  if((e.ctrlKey||e.metaKey) && (e.key.toLowerCase()==='y' || (e.shiftKey && e.key.toLowerCase()==='z'))){ e.preventDefault(); redo(); }
});

// Messages rendering with tasks/todos/stream
function renderMessages(){
  const chat=getActiveChat();
  if(!chat || !messagesEl){ if(messagesEl) messagesEl.innerHTML=`<div class="date-sep"><span>No conversation selected</span></div><div style="text-align:center;padding:24px"><button class="primary-btn" onclick="document.getElementById('newChatBtn')?.click()">Start new chat</button></div>`; return; }
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
  }).join('') || `<div class="chat-empty"><div class="chat-empty-icon"><i data-lucide="sparkles" style="width:26px;height:26px"></i></div><h3>Start a conversation</h3><p>Ask MCP to run a tool, query your database, or explore files. Try an example below or type <code>/</code> for commands.</p><div class="chat-empty-actions"><button class="ghost-btn sm" onclick="document.getElementById('promptInput').value='/sql SELECT * FROM users LIMIT 5'; document.getElementById('promptInput').dispatchEvent(new Event('input'))"><i data-lucide="database" style="width:12px;height:12px"></i> Try SQL</button><button class="ghost-btn sm" onclick="document.getElementById('promptInput').value='/s3 list buckets'; document.getElementById('promptInput').dispatchEvent(new Event('input'))"><i data-lucide="cloud" style="width:12px;height:12px"></i> List S3</button><button class="ghost-btn sm" onclick="document.getElementById('promptInput').value='/fs list /data'; document.getElementById('promptInput').dispatchEvent(new Event('input'))"><i data-lucide="folder-open" style="width:12px;height:12px"></i> Browse FS</button></div></div>`;
  refreshIcons(); scrollBottom(); updateScrollFab();
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
messagesEl?.addEventListener('click', e=>{
  const edit=e.target.closest('[data-edit]'); if(edit){
    const id=edit.dataset.edit;
    const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===id);
    if(msg){ editingMsgId=id; editMsgInput.value=msg.text; editMsgModal?.classList.remove('hidden'); refreshIcons(); }
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
    if(msg){ const txt= msg.html==='pg' ? 'pg table' : (msg.text||''); navigator.clipboard.writeText(txt).catch(()=>{}); toast('Copied'); }
    return;
  }
  const copyTbl=e.target.closest('[data-copy-table]'); if(copyTbl){
    const tbl=copyTbl.closest('.card')?.querySelector('table');
    if(tbl){ const t=[...tbl.rows].map(r=>[...r.cells].map(c=>c.textContent).join('\t')).join('\n'); navigator.clipboard.writeText(t).catch(()=>{}); toast('Copied table'); }
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
messagesEl?.addEventListener('change', e=>{
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
closeEditMsg?.addEventListener('click', ()=> editMsgModal?.classList.add('hidden'));
cancelEdit?.addEventListener('click', ()=> editMsgModal?.classList.add('hidden'));
editMsgBackdrop?.addEventListener('click', ()=> editMsgModal?.classList.add('hidden'));
saveEdit?.addEventListener('click', ()=>{
  const chat=getActiveChat(); const msg=chat.messages.find(m=>m.id===editingMsgId);
  if(msg){
    pushHistory();
    msg.text=editMsgInput.value;
    saveChats(); renderMessages(); editMsgModal?.classList.add('hidden');
    // resend
    doSend(msg.text, msg.files||[], msg.todos||[], true);
  }
});

// Streaming
let _streamIntervals = {};
function streamHTML(targetId, html, doneText){
  const el=document.getElementById(targetId);
  if(!el) return;
  if(_streamIntervals[targetId]){ clearInterval(_streamIntervals[targetId]); delete _streamIntervals[targetId]; }
  if(!streamToggle?.checked){
    el.innerHTML=html; refreshIcons(); return;
  }
  el.innerHTML='';
  let i=0;
  const chunk=6;
  const iv=setInterval(()=>{
    if(!document.getElementById(targetId)){ clearInterval(iv); delete _streamIntervals[targetId]; return; }
    el.innerHTML = html.slice(0,i) + '<span class="stream-cursor"></span>';
    i+=chunk;
    scrollBottom();
    if(i>=html.length){ clearInterval(iv); delete _streamIntervals[targetId]; el.innerHTML=html; refreshIcons(); }
  }, 10);
  _streamIntervals[targetId]=iv;
}

// Do send with todo, files, stream
async function doSend(rawText, rawFiles, rawTodos, isRetry){
  let text = (rawText ?? inputEl.value).trim();
  const files=[... (rawFiles ?? pending)];
  const curTodos = rawTodos ?? (todos.length? [...todos] : []);
  if(!text && !files.length && !curTodos.length) return;
  if(isTyping){ toast('Please wait — still generating'); return; }
  if(text==='/clear'){ clearCurrentChat(); if(inputEl) inputEl.value=''; pending.forEach(pr=>{ if(pr.url) try{ URL.revokeObjectURL(pr.url);}catch{} }); pending=[]; todos=[]; if(todoDraft) todoDraft?.classList.add('hidden'); drawPreview(); syncComposer(); hideSlash(); return; }
  if(text==='/help'){ await sendAndStream(text, files, curTodos, 'general'); return; }
  // todo command
  if(text.startsWith('/todo ')){ const t=text.slice(6).trim(); if(t){ todos.push({text:t, done:false}); renderTodo(); todoDraft?.classList.remove('hidden'); toast('Todo added'); } inputEl.value=''; syncComposer(); hideSlash(); autoResize(); return; }
  if(text.startsWith('/connect ') || text.startsWith('/disconnect ')){
    const isCon=text.startsWith('/connect ');
    const name=text.slice(isCon?9:12).trim();
    const s=servers.find(x=>x.name===name);
    if(s) await toggle(s.id, isCon);
    else toast(`Unknown server ${name}`);
    if(inputEl) inputEl.value=''; hideSlash(); syncComposer(); autoResize(); return;
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
    pending.forEach(pr=>{ if(pr.url) try{ URL.revokeObjectURL(pr.url);}catch{} }); pending=[]; todos=[]; if(todoDraft) todoDraft?.classList.add('hidden'); drawPreview();
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
  if(ttsToggle?.checked && 'speechSynthesis' in window){
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
    if(marketTabs) marketTabs.querySelectorAll('button').forEach(b=> b.classList.toggle('active', b.dataset.tab===activeMarketTab));
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
  marketDetailModal?.classList.remove('hidden');
  refreshIcons();
  // detail listeners
  const _closeM = $('#closeMarketDetail'); if(_closeM) _closeM.onclick = ()=> marketDetailModal?.classList.add('hidden');
  const _copyI = $('#copyInstall'); if(_copyI) _copyI.onclick = ()=>{ navigator.clipboard.writeText(`npx @modelcontextprotocol/server-${m.name.toLowerCase().replace(/\s/g,'-')}`).catch(()=>{}); toast('Copied install command'); };
  const _detInst = $('#detailInstall'); if(_detInst) _detInst.onclick = e=>{
    const id=e.currentTarget.dataset.install;
    handleInstall(id);
    setTimeout(()=> openMarketDetail(id), 400);
  };
  const _detConn = $('#detailConnect'); if(_detConn) _detConn.onclick = ()=>{
    if(!m.installed) return toast('Install first');
    // find server and connect
    const srv=servers.find(s=> s.name===m.name.toLowerCase().replace(/\s/g,'-') || s.name===m.name);
    if(srv) toggle(srv.id, true);
    else toast('Server not found — installing…');
    marketDetailModal?.classList.add('hidden');
  };
}
async function handleInstall(id){
  const m=MARKET.find(x=>x.id===id);
  if(!m || m.installed) { if(m?.installed) toast(`${m.name} already installed`); return; }
  m.installing=true; m.progress=0; renderMarket();
  // simulate progress
  for(let p=0; p<=100; p+= 20){
    await new Promise(r=>setTimeout(r, 180));
    m.progress=p; renderMarket();
    if(marketDetailModal && !marketDetailModal?.classList.contains('hidden')) {
      const bar=marketDetailContent.querySelector('.install-progress span');
      if(bar) bar.style.width=p+'%';
    }
  }
  m.installing=false; m.progress=100; m.installed=true; saveMarket();
  const nid='srv'+Date.now()+Math.random().toString(36).slice(2,5);
  const col=m.cat==='db'?'pg': m.cat==='cloud'?'aws': m.cat==='ai'?'pg' : m.cat==='dev'?'custom':'fs';
  const srvName=m.name.toLowerCase().replace(/\s/g,'-');
  if(!servers.find(s=>s.name===srvName)) servers.push({id:nid, name:srvName, transport:'sse', endpoint:`wss://${m.name.toLowerCase().replace(/\s/g,'')}.mcp.local/sse`, tools:m.tools.join(', '), status:'disconnected', color:col, desc:m.desc||'', timeout:10000, retries:2, env:[], latency:Math.floor(Math.random()*80+40), health:'off', lastSeen:Date.now(), autoConnect:false, healthCheck:true});
  saveServers(); renderServers();
  renderMarket(); toast(`${m.name} installed — connect in MCP servers ✓`);
}
marketSearch?.addEventListener('input', renderMarket);
marketFilter?.addEventListener('change', renderMarket);
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
marketGrid?.addEventListener('click', e=>{
  const card=e.target.closest('[data-detail]');
  const btn=e.target.closest('[data-install]');
  if(btn){
    e.stopPropagation();
    handleInstall(btn.dataset.install);
    return;
  }
  if(card) openMarketDetail(card.dataset.detail);
});
marketDetailBackdrop?.addEventListener('click', ()=> marketDetailModal?.classList.add('hidden'));
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
promptCatsEl?.addEventListener('click', e=>{
  const b=e.target.closest('[data-cat]'); if(!b) return;
  activePromptCat=b.dataset.cat; renderPromptCats(); renderPromptGrid();
});
promptGridEl?.addEventListener('click', e=>{
  const card=e.target.closest('.prompt-item'); if(!card) return;
  inputEl.value=card.dataset.prompt; autoResize(); syncComposer();
  promptsView?.classList.add('hidden'); toast('Prompt inserted');
  inputEl.focus();
});

// Profile
userProfileCard?.addEventListener('click', ()=>{ profileView?.classList.remove('hidden'); refreshIcons(); });
profileBackdrop?.addEventListener('click', ()=> profileView?.classList.add('hidden'));
closeProfile?.addEventListener('click', ()=> profileView?.classList.add('hidden'));
changeAvatar?.addEventListener('click', ()=> avatarInput.click());
avatarInput?.addEventListener('change', e=>{
  const f=e.target.files[0]; if(!f) return;
  const url=URL.createObjectURL(f);
  profileAvatar.src=url; profileLargeAvatar.src=url; toast('Avatar updated');
});
saveProfileBtn?.addEventListener('click', ()=>{
  const newName=profileNameInput.value.trim()||'User';
  const newEmail=profileEmail.value.trim()||'user@local.dev';
  const role=$('#profileRoleSelect')?.value||'Admin';
  // update currentUser if exists
  if(currentUser){
    currentUser.name=newName;
    currentUser.email=newEmail;
    currentUser.role=role;
    // also update in users array
    const idx=users.findIndex(u=>u.id===currentUser.id);
    if(idx>=0) users[idx]= {...users[idx], name:newName, email:newEmail, role};
    saveUsers();
    setCurrentUser(currentUser);
  } else {
    profileName.textContent=newName;
    profileRole.textContent=`${role} • ${selectedModel} • ${getActiveMode().name} mode`;
    localStorage.setItem('profileName', newName);
    localStorage.setItem('profileEmail', newEmail);
  }
  // also update server detail etc.
  profileView?.classList.add('hidden'); toast('Profile saved ✓');
  renderProviders(); renderModes();
});
themeSelect?.addEventListener('change', e=>{
  const v=e.target.value;
  try{ localStorage.setItem('mcp_theme', v); }catch{}
  if(v==='dark'){ document.documentElement.style.filter='invert(1) hue-rotate(180deg)'; document.body.style.background='#0f172a'; toast('Dark preview — invert filter'); }
  else { document.documentElement.style.filter=''; document.body.style.background=''; toast('Light theme'); }
});

// Servers drawer events
function openSettings(){ settingsView?.classList.remove('hidden'); refreshIcons(); }
function closeSettingsView(){ settingsView?.classList.add('hidden'); }
serversBtn?.addEventListener('click', openSettings);
settingsBtn?.addEventListener('click', openSettings);
marketplaceBtn?.addEventListener('click', ()=>{ marketplaceView?.classList.remove('hidden'); renderMarket(); refreshIcons(); });
promptLibraryBtn?.addEventListener('click', ()=>{ promptsView?.classList.remove('hidden'); renderPromptCats(); renderPromptGrid(); refreshIcons(); });
showPromptsLink?.addEventListener('click', ()=> promptLibraryBtn.click());
closeMarketplace?.addEventListener('click', ()=> marketplaceView?.classList.add('hidden'));
marketplaceBackdrop?.addEventListener('click', ()=> marketplaceView?.classList.add('hidden'));
promptsBackdrop?.addEventListener('click', ()=> promptsView?.classList.add('hidden'));
closePrompts?.addEventListener('click', ()=> promptsView?.classList.add('hidden'));
settingsBackdrop?.addEventListener('click', closeSettingsView);
closeSettings?.addEventListener('click', closeSettingsView);
serverSearch?.addEventListener('input', renderServers);
sidebarSearch?.addEventListener('input', ()=>{ renderChats(); renderServers(); });
serverSort?.addEventListener('change', renderServers);
serverTabs?.addEventListener('click', e=>{
  const b=e.target.closest('[data-stab]'); if(!b) return;
  activeServerTab=b.dataset.stab; localStorage.setItem('serverTab', activeServerTab); renderServers();
});
bulkConnect?.addEventListener('click', async ()=>{
  const toConnect=servers.filter(s=>s.status!=='connected');
  if(!toConnect.length) return toast('All connected');
  for(const s of toConnect) await toggle(s.id, true);
});
bulkDisconnect?.addEventListener('click', async ()=>{
  const toDisc=servers.filter(s=>s.status==='connected');
  if(!toDisc.length) return toast('All disconnected');
  for(const s of toDisc) await toggle(s.id, false);
});
testAllServers?.addEventListener('click', ()=> doHealthCheck(true));
exportServersBtn?.addEventListener('click', ()=>{
  const blob=new Blob([JSON.stringify(servers,null,2)],{type:'application/json'});
  const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download='mcp-servers.json'; a.click(); URL.revokeObjectURL(url); toast('Servers exported');
});
importServersBtn?.addEventListener('click', ()=> importServersFile.click());
importServersFile?.addEventListener('change', e=>{
  const f=e.target.files[0]; if(!f) return;
  const r=new FileReader(); r.onerror=()=> toast('File read error'); r.onload=()=>{ try{ const arr=JSON.parse(r.result); if(Array.isArray(arr)){ servers=arr.map(s=> ({...s, id: s.id||'srv'+Date.now()+Math.random().toString(36).slice(2), health:s.health||'off', latency:s.latency||0, lastSeen:s.lastSeen||Date.now()})); saveServers(); renderServers(); toast('Servers imported'); } else toast('Invalid file'); } catch{ toast('Invalid JSON'); } }; r.readAsText(f); e.target.value='';
});
healthPoll?.addEventListener('change', ()=>{ initHealthPoll(); renderServers(); toast(healthPoll.checked?'Health polling on':'Health polling off'); });
addServerBtn?.addEventListener('click', ()=>{
  editingId=null;
  serverModalTitle.textContent='Add MCP Server';
  srvName.value=''; srvEndpoint.value=''; srvTools.value=''; if(srvDesc) srvDesc.value=''; if(srvTimeout) srvTimeout.value='10000'; if(srvRetries) srvRetries.value='2'; if(srvAuto) srvAuto.checked=true; if(srvHealth) srvHealth.checked=true;
  srvTransport.value='sse'; if(srvTemplate) srvTemplate.value='';
  envDraft=[]; renderEnv(); updateEndpointHint();
  serverModal?.classList.remove('hidden'); refreshIcons();
});
addServerSidebar?.addEventListener('click', ()=> addServerBtn.click());
closeServerModalBtn?.addEventListener('click', ()=> serverModal?.classList.add('hidden'));
cancelServer?.addEventListener('click', ()=> serverModal?.classList.add('hidden'));
serverModalBackdrop?.addEventListener('click', ()=> serverModal?.classList.add('hidden'));
serverDetailBackdrop?.addEventListener('click', ()=> serverDetailModal?.classList.add('hidden'));
srvTemplate?.addEventListener('change', ()=>{
  const v=srvTemplate.value;
  const map={
    postgres:{transport:'stdio', endpoint:'npx @modelcontextprotocol/server-postgres', tools:'query, list_tables, describe_schema', desc:'Postgres with pgvector'},
    filesystem:{transport:'stdio', endpoint:'npx @modelcontextprotocol/server-filesystem /data', tools:'read, write, list', desc:'Local filesystem sandbox'},
    fetch:{transport:'stdio', endpoint:'npx @modelcontextprotocol/server-fetch', tools:'fetch', desc:'HTTP fetch'},
    github:{transport:'stdio', endpoint:'npx @modelcontextprotocol/server-github', tools:'list_repos, create_pr', desc:'GitHub API'},
    sse:{transport:'sse', endpoint:'wss://example.mcp.local/sse', tools:'', desc:'SSE remote'},
    http:{transport:'http', endpoint:'https://example.mcp.local/mcp', tools:'', desc:'HTTP streamable'},
  };
  if(map[v]){
    srvTransport.value=map[v].transport;
    srvEndpoint.value=map[v].endpoint;
    srvTools.value=map[v].tools;
    if(srvDesc) srvDesc.value=map[v].desc;
    updateEndpointHint();
  }
});
srvTransport?.addEventListener('change', updateEndpointHint);
addEnvBtn?.addEventListener('click', ()=>{ envDraft.push({k:'',v:''}); renderEnv(); });
envList?.addEventListener('input', e=>{
  const ek=e.target.closest('[data-ek]'); if(ek){ envDraft[Number(ek.dataset.ek)].k=ek.value; }
  const ev=e.target.closest('[data-ev]'); if(ev){ envDraft[Number(ev.dataset.ev)].v=ev.value; }
});
envList?.addEventListener('click', e=>{
  const b=e.target.closest('[data-rm-env]'); if(b){ envDraft.splice(Number(b.dataset.rmEnv),1); renderEnv(); }
});
testServerBtn?.addEventListener('click', async ()=>{
  const ep=srvEndpoint.value.trim();
  if(!ep) return toast('Enter endpoint first');
  testServerBtn.innerHTML=`<i data-lucide="loader-2" style="width:12px;height:12px"></i> Testing…`; refreshIcons();
  await new Promise(r=>setTimeout(r, 900));
  const ok= ep.startsWith('wss://') || ep.startsWith('https://') || ep.startsWith('npx ');
  toast(ok? '✓ Connection test passed (mock)':'✗ Test failed — check endpoint', ok?2200:3000);
  testServerBtn.innerHTML=`<i data-lucide="activity" style="width:12px;height:12px"></i> Test`; refreshIcons();
});
serverForm?.addEventListener('submit', e=>{
  e.preventDefault();
  const name=srvName.value.trim(), ep=srvEndpoint.value.trim();
  if(!name||!ep) return toast('Name and endpoint required');
  if(!/^[a-z0-9-]+$/.test(name)) return toast('Name must be lowercase slug (a-z, 0-9, -)');
  const tools=srvTools.value.trim();
  const desc=srvDesc?.value.trim()||'';
  const timeout=parseInt(srvTimeout?.value)||10000;
  const retries=parseInt(srvRetries?.value)||2;
  const autoConnect=!!srvAuto?.checked;
  const healthCheck=!!srvHealth?.checked;
  // collect env, filter empty keys
  const env=envDraft.filter(x=>x.k.trim()).map(x=>({k:x.k.trim(), v:x.v}));
  if(editingId){
    const s=servers.find(x=>x.id===editingId);
    s.name=name; s.transport=srvTransport.value; s.endpoint=ep; s.tools=tools; s.desc=desc; s.timeout=timeout; s.retries=retries; s.env=env; s.autoConnect=autoConnect; s.healthCheck=healthCheck;
    s.color= name.includes('postgres')?'pg': name.includes('aws')?'aws': name.includes('fs')||name.includes('file')?'fs':'custom';
    toast(`${name} updated`);
  } else {
    if(servers.find(s=>s.name===name)) return toast('Name already exists');
    const id='srv'+Date.now();
    const col=name.includes('postgres')?'pg': name.includes('aws')?'aws': name.includes('fs')||name.includes('file')?'fs':'custom';
    servers.push({id,name,transport:srvTransport.value,endpoint:ep,tools,status:'disconnected',color:col, desc, timeout, retries, env, latency:Math.floor(Math.random()*120+40), health:'off', lastSeen:Date.now(), autoConnect, healthCheck});
    toast(`${name} added`);
  }
  saveServers(); renderServers(); serverModal?.classList.add('hidden');
});
topNodes?.addEventListener('click', e=>{ const p=e.target.closest('[data-id]'); if(!p) return; const s=servers.find(x=>x.id===p.dataset.id); if(s) openServerDetail(s.id); else toggle(p.dataset.id); });
sidebarList?.addEventListener('click', e=>{ const p=e.target.closest('[data-id]'); if(!p) return; const s=servers.find(x=>x.id===p.dataset.id); if(s) openServerDetail(s.id); });
serversList?.addEventListener('click', e=>{
  const test=e.target.closest('[data-test]'); if(test){ const s=servers.find(x=>x.id===test.dataset.test); toast(`Testing ${s.name}…`); setTimeout(()=>{ s.latency=Math.floor(30+Math.random()*180); s.health= s.latency<100?'ok': s.latency<250?'warn':'bad'; s.lastSeen=Date.now(); saveServers(); renderServers(); toast(`${s.name}: ${s.health} • ${s.latency}ms`); }, 700); return; }
  const logs=e.target.closest('[data-logs]'); if(logs){ openServerDetail(logs.dataset.logs); return; }
  const dup=e.target.closest('[data-dup]'); if(dup){ const s=servers.find(x=>x.id===dup.dataset.dup); const copy={...JSON.parse(JSON.stringify(s)), id:'srv'+Date.now(), name:s.name+'-copy', status:'disconnected', health:'off', lastSeen:Date.now()}; servers.push(copy); saveServers(); renderServers(); toast(`Duplicated ${s.name}`); return; }
  const del=e.target.closest('[data-del]'); if(del){ if(confirm('Delete server?')){ servers=servers.filter(s=>s.id!==del.dataset.del); saveServers(); renderServers(); toast('Removed'); } return; }
  const ed=e.target.closest('[data-edit]'); if(ed){ openEditServer(ed.dataset.edit); return; }
  const card=e.target.closest('.server-card'); if(card && !e.target.closest('button') && !e.target.closest('label')){ openServerDetail(card.dataset.id); }
});
serversList?.addEventListener('change', e=>{ const t=e.target.closest('[data-toggle]'); if(t) toggle(t.dataset.toggle, t.checked); });
const _toggleLock = new Set();
async function toggle(id, force){
  const s=servers.find(x=>x.id===id); if(!s) return;
  if(_toggleLock.has(id)) return toast(`${s.name} already toggling`);
  const should= typeof force==='boolean' ? force : s.status!=='connected';
  if(should){
    if(s.status==='connected' || s.status==='connecting') return;
    _toggleLock.add(id);
    s.status='connecting'; s.health='off'; renderServers(); toast(`Connecting ${s.name}…`);
    await new Promise(r=>setTimeout(r, 700 + Math.random()*600));
    // simulate success/failure based on endpoint
    const fail = s.endpoint.includes('fail') || Math.random()<0.06;
    if(fail){
      s.status='disconnected'; s.health='bad'; s.latency=999; s.lastSeen=Date.now();
      saveServers(); renderServers(); toast(`${s.name} failed — check endpoint`, 3000);
      _toggleLock.delete(id);
    } else {
      s.status='connected'; s.health='ok'; s.latency=Math.floor(30+Math.random()*120); s.lastSeen=Date.now();
      saveServers(); renderServers(); toast(`${s.name} connected ✓ • ${s.latency}ms`);
      _toggleLock.delete(id);
    }
    _toggleLock.delete(id);
  } else {
    s.status='disconnected'; s.health='off'; s.lastSeen=Date.now();
    _toggleLock.delete(id);
    saveServers(); renderServers(); toast(`${s.name} disconnected`);
  }
}

// Quick tools sheet
toolsBackdrop?.addEventListener('click', ()=> toolsModal?.classList.add('hidden'));
closeModalBtn?.addEventListener('click', ()=> toolsModal?.classList.add('hidden'));
// quickToolsBtn removed - replaced by composerModePill (kept for sidebar quick-cards)
quickToolsBtn?.addEventListener('click', ()=> toolsModal?.classList.remove('hidden'));
$$('.quick-card, .tool').forEach(b=>{
  b.addEventListener('click', ()=>{
    const t=b.dataset.tool;
    toolsModal?.classList.add('hidden');
    if(t==='marketplace'){ marketplaceView?.classList.remove('hidden'); renderMarket(); }
    else if(t==='upload') mediaInput?.click();
    else if(t==='pg') { inputEl.value='/sql SELECT * FROM users LIMIT 5'; inputEl.focus(); autoResize(); syncComposer(); }
    else if(t==='aws'){ inputEl.value='/s3 list buckets'; inputEl.focus(); autoResize(); syncComposer(); }
    else if(t==='fs'){ inputEl.value='/fs list /data'; inputEl.focus(); autoResize(); syncComposer(); }
  });
});

// Sidebar toggle
menuBtn?.addEventListener('click', ()=> sidebar.classList.add('open'));
sidebarClose?.addEventListener('click', ()=> sidebar.classList.remove('open'));
document.addEventListener('keydown', e=>{
  if(e.key==='Escape'){ settingsView?.classList.add('hidden'); serverModal?.classList.add('hidden'); toolsModal?.classList.add('hidden'); marketplaceView?.classList.add('hidden'); promptsView?.classList.add('hidden'); profileView?.classList.add('hidden'); providerView?.classList.add('hidden'); providerModal?.classList.add('hidden'); serverDetailModal?.classList.add('hidden'); marketDetailModal?.classList.add('hidden'); modesView?.classList.add('hidden'); modeModal?.classList.add('hidden'); liveOverlay?.classList.add('hidden'); editMsgModal?.classList.add('hidden'); hideSlash(); sidebar.classList.remove('open'); convMenu.classList.add('hidden'); }
  if((e.metaKey||e.ctrlKey) && e.key.toLowerCase()==='k'){ e.preventDefault(); openSettings(); }
});

// Send
sendBtn?.addEventListener('click', ()=> doSend());
attachBtn?.addEventListener('click', ()=> mediaInput.click());

// ——— Auth, Splash & Onboarding ———
const ONBOARD_STEPS = [
  {icon:'network', color:'#2563eb', title:'Welcome to MCP Terminal', desc:'Connect AI models to tools and local resources via the Model Context Protocol — securely, locally, fast.', bullets:['TLS 1.3 secure tunnel','MCP nodes: Postgres, S3, FS & more','One chat, many tools']},
  {icon:'cpu', color:'#7c3aed', title:'AI Providers', desc:'Bring your own key — OpenAI, Anthropic, Google or local LLM. Switch instantly, per-mode overrides.', bullets:['Multiple providers & models','Per-mode provider & temp','Streaming & fallback']},
  {icon:'store', color:'#ff9900', title:'MCP Marketplace & Servers', desc:'Discover verified MCP servers, install with one click, connect / disconnect, health checks & logs.', bullets:['Browse 10+ verified servers','SSE / STDIO / HTTP','Env vars, timeout & retries']},
  {icon:'bot', color:'#16a34a', title:'Agent Modes & Live', desc:'Modes shape tone & tools. Live speech gives hands-free conversation with turn-taking and TTS.', bullets:['8 modes: Chat, Code, Research…','Custom modes & prompts','Live mic — interrupt anytime']},
];
let onIndex = 0;
function renderOnStep(){
  const s=ONBOARD_STEPS[onIndex];
  onStep.innerHTML = `
    <div class="on-step">
      <div class="on-icon" style="background:${s.color}"><i data-lucide="${s.icon}" style="width:24px;height:24px"></i></div>
      <h3 class="on-title">${esc(s.title)}</h3>
      <p class="on-desc">${esc(s.desc)}</p>
      <ul class="on-bullets">${s.bullets.map(b=>`<li><i data-lucide="check" style="width:14px;height:14px"></i> ${esc(b)}</li>`).join('')}</ul>
      <div class="on-dots">${ONBOARD_STEPS.map((_,i)=>`<span class="${i===onIndex?'active':''}"></span>`).join('')}</div>
    </div>`;
  onProgress.style.width = `${((onIndex+1)/ONBOARD_STEPS.length)*100}%`;
  onPrev.style.display = onIndex===0?'none':'inline-flex';
  onNext.innerHTML = onIndex===ONBOARD_STEPS.length-1 ? `Get started <i data-lucide="arrow-right" style="width:14px;height:14px"></i>` : `Next <i data-lucide="arrow-right" style="width:14px;height:14px"></i>`;
  refreshIcons();
}
function openOnboarding(){ onIndex=0; renderOnStep(); authView?.classList.add('hidden'); onboarding?.classList.remove('hidden'); refreshIcons(); }
function closeOnboarding(){ onboarding?.classList.add('hidden'); try{ localStorage.setItem('mcp_onboarded','1'); }catch{} }
onNext?.addEventListener('click', ()=>{ if(onIndex < ONBOARD_STEPS.length-1){ onIndex++; renderOnStep(); } else { closeOnboarding(); if(!currentUser) showAuth(); } });
onPrev?.addEventListener('click', ()=>{ if(onIndex>0){ onIndex--; renderOnStep(); }});
onSkip?.addEventListener('click', ()=>{ closeOnboarding(); if(!currentUser) showAuth(); });
onboardingBackdrop?.addEventListener('click', ()=>{ closeOnboarding(); if(!currentUser) showAuth(); });
showOnboardingBtn?.addEventListener('click', openOnboarding);

// Users / Auth
let users = safeParse('mcp_users', [
  {id:'u_alex', name:'Alex Rivera', email:'alex@local.dev', pass:'demo123', avatar:'https://i.pravatar.cc/100?img=32', role:'Admin', isGuest:false},
  {id:'u_sara', name:'Sara Chen', email:'sara@local.dev', pass:'demo123', avatar:'https://i.pravatar.cc/100?img=15', role:'Developer', isGuest:false},
]);
let currentUserId = localStorage.getItem('mcp_currentUserId') || null;
let currentUser = users.find(u=>u.id===currentUserId) || null;
const saveUsers = ()=> { safeSet('mcp_users', JSON.stringify(users)); if(currentUser) safeSet('mcp_currentUserId', currentUser.id); else try{ localStorage.removeItem('mcp_currentUserId');}catch{} };
function showAuth(){ if(onboarding) onboarding?.classList.add('hidden'); authView?.classList.remove('hidden'); refreshIcons(); }
function hideAuth(){ authView?.classList.add('hidden'); }
function renderAuthTabs(tab){
  document.querySelectorAll('.auth-tab').forEach(b=> b.classList.toggle('active', b.dataset.tab===tab));
  loginForm.classList.toggle('hidden', tab!=='login');
  signupForm.classList.toggle('hidden', tab!=='signup');
  guestForm.classList.toggle('hidden', tab!=='guest');
}
document.querySelectorAll('.auth-tab').forEach(b=> b.addEventListener('click', ()=> renderAuthTabs(b.dataset.tab)));
renderAuthTabs('login');
function setCurrentUser(user){
  currentUser=user; currentUserId=user.id;
  saveUsers();
  // update profile UI
  profileName.textContent=user.name;
  profileHeaderName.textContent=user.name;
  profileHeaderEmail.textContent=user.email + (user.isGuest?' • guest':'');
  profileHeaderMeta.textContent=user.isGuest?'Guest • local only':'Signed in • local';
  profileGuestBadge.classList.toggle('hidden', !user.isGuest);
  profileRoleBadge.textContent=user.role||'Admin';
  profileAvatar.src=user.avatar;
  profileLargeAvatar.src=user.avatar;
  profileNameInput.value=user.name;
  profileEmail.value=user.email;
  if(document.getElementById('profileRoleSelect')) document.getElementById('profileRoleSelect').value=user.role||'Admin';
  // persist role badge in top
  if(profileRole) profileRole.textContent=`${user.role||'Admin'} • ${selectedModel} • ${getActiveMode().name} mode`;
  // per-user chats key (optional: keep global for demo but could namespace)
  // For demo, keep chats global but personalize greeting
  renderProfilesList();
  hideAuth();
  // do not auto-close onboarding here — let initApp handle first-run flag
  try{ localStorage.setItem('mcp_onboarded','1'); }catch{}
  onboarding?.classList.add('hidden');
  toast(`Welcome, ${user.name}${user.isGuest?' (guest)':''} ✓`);
  renderMessages();
}
function renderProfilesList(){
  if(!profilesList) return;
  profilesList.innerHTML = users.map(u=>`
    <div class="profile-row ${u.id===currentUserId?'active':''}" data-switch="${u.id}" style="display:flex;gap:10px;align-items:center;padding:10px;background:${u.id===currentUserId?'var(--primary-ghost)':'#fff'};border:1px solid ${u.id===currentUserId?'#dbeafe':'var(--line)'};border-radius:12px;cursor:pointer">
      <img src="${u.avatar}" style="width:36px;height:36px;border-radius:50%;object-fit:cover" />
      <div style="flex:1;min-width:0"><b style="font-size:13px">${esc(u.name)} ${u.isGuest?'<span style="background:#f59e0b;color:#fff;font-size:10px;padding:2px 6px;border-radius:999px">GUEST</span>':''}</b><div style="font-size:11px;color:var(--muted);font-family:var(--mono);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(u.email)}</div></div>
      <span style="font-size:11px;color:var(--muted)">${u.role||''}</span>
      ${u.id===currentUserId?'<span style="background:var(--ok);color:#fff;padding:3px 7px;border-radius:999px;font-size:10px;font-weight:700">ACTIVE</span>':''}
    </div>
  `).join('') || `<p style="font-size:11px;color:var(--faint);text-align:center">No profiles</p>`;
  refreshIcons();
}
profilesList?.addEventListener('click', e=>{
  const row=e.target.closest('[data-switch]'); if(!row) return;
  const u=users.find(x=>x.id===row.dataset.switch);
  if(u && u.id!==currentUserId){ setCurrentUser(u); profileView?.classList.add('hidden'); }
});
switchProfileBtn?.addEventListener('click', ()=>{
  profileView?.classList.add('hidden');
  showAuth(); renderAuthTabs('login');
});
manageProfilesBtn?.addEventListener('click', ()=>{ renderProfilesList(); toast('Select a profile to switch'); });
addProfileBtn?.addEventListener('click', ()=>{
  // create new empty profile and switch to signup
  profileView?.classList.add('hidden');
  showAuth(); renderAuthTabs('signup');
  signupName.value=''; signupEmail.value=''; signupPass.value=''; signupPass2.value='';
});
logoutBtn?.addEventListener('click', ()=>{
  if(!currentUser) return;
  if(confirm(`Logout ${currentUser.name}? ${currentUser.isGuest?'Guest data will be lost.':''}`)){
    if(currentUser.isGuest){
      // remove guest user
      users=users.filter(u=>u.id!==currentUser.id);
      saveUsers();
    }
    currentUser=null; currentUserId=null;
    localStorage.removeItem('mcp_currentUserId');
    profileView?.classList.add('hidden');
    showAuth(); renderAuthTabs('login');
    toast('Logged out');
    renderProfilesList();
  }
});
// Auth form handlers
loginForm?.addEventListener('submit', e=>{
  e.preventDefault();
  const email=loginEmail.value.trim().toLowerCase(), pass=loginPass.value.trim();
  const u=users.find(x=>x.email.toLowerCase()===email);
  if(!u) return toast('No account with that email — try Sign up or Guest', 2600);
  if(u.pass!==pass) return toast('Wrong password (demo: any ≥6 chars, original is demo123 if not changed)', 2600);
  // success
  setCurrentUser(u);
  // remember me handling: if unchecked, session only (for demo keep)
  if(!rememberMe.checked) toast('Session only — will clear on close (mock)');
});
signupForm?.addEventListener('submit', e=>{
  e.preventDefault();
  const name=signupName.value.trim(), email=signupEmail.value.trim().toLowerCase(), pass=signupPass.value, pass2=signupPass2.value;
  if(!name||!email||!pass) return toast('Fill all fields');
  if(pass.length<6) return toast('Password ≥6 chars');
  if(pass!==pass2) return toast('Passwords do not match');
  if(users.find(u=>u.email.toLowerCase()===email)) return toast('Email already exists — try Login');
  const newUser={id:'u_'+Date.now(), name, email, pass, avatar:`https://i.pravatar.cc/100?img=${Math.floor(Math.random()*70)}`, role:'Developer', isGuest:false};
  users.push(newUser); saveUsers();
  setCurrentUser(newUser);
  toast('Account created ✓');
});
guestBtn?.addEventListener('click', ()=>{
  const guest={id:'guest_'+Date.now(), name:'Guest User', email:'guest@local', avatar:`https://i.pravatar.cc/100?img=${Math.floor(Math.random()*70)}`, role:'Viewer', isGuest:true, pass:''};
  users.push(guest); saveUsers();
  setCurrentUser(guest);
});
authDemoLogin?.addEventListener('click', ()=>{
  // demo login as Alex
  const demo=users.find(u=>u.email==='alex@local.dev') || users[0];
  if(demo){ setCurrentUser(demo); }
});
authBackdrop?.addEventListener('click', ()=>{
  if(currentUser) hideAuth(); // only allow close if already logged in
});
// Splash
function runSplash(){
  if(!splash) return Promise.resolve();
  splash?.classList.remove('hidden');
  const statuses=['Initializing secure tunnel…','Loading MCP nodes…','Syncing providers…','Ready'];
  let idx=0;
  const iv=setInterval(()=>{
    if(idx < statuses.length && splashStatus) splashStatus.textContent=statuses[idx++];
  }, 380);
  if(splashProgress) splashProgress.style.width='100%';
  return new Promise(res=>{
    setTimeout(()=>{
      clearInterval(iv);
      splash?.classList.add('hidden');
      splash.setAttribute('aria-hidden','true');
      res();
    }, 1600);
  });
}
// Init — with splash/onboarding/auth gate
async function initApp(){
  await runSplash();
  const hasOnboarded = !!localStorage.getItem('mcp_onboarded');
  if(currentUser){
    // refresh UI without overwriting onboarding flag logic
    hideAuth(); onboarding?.classList.add('hidden');
    profileName.textContent=currentUser.name;
    profileHeaderName.textContent=currentUser.name;
    profileHeaderEmail.textContent=currentUser.email + (currentUser.isGuest?' • guest':'');
    profileHeaderMeta.textContent=currentUser.isGuest?'Guest • local only':'Signed in • local';
    if(profileGuestBadge) profileGuestBadge.classList.toggle('hidden', !currentUser.isGuest);
    if(profileRoleBadge) profileRoleBadge.textContent=currentUser.role||'Admin';
    if(profileAvatar) profileAvatar.src=currentUser.avatar;
    if(profileLargeAvatar) profileLargeAvatar.src=currentUser.avatar;
    if(profileNameInput) profileNameInput.value=currentUser.name;
    if(profileEmail) profileEmail.value=currentUser.email;
    renderProfilesList();
    if(!hasOnboarded){ openOnboarding(); }
  } else {
    if(!hasOnboarded){ openOnboarding(); }
    else { showAuth(); }
  }
  renderProviders(); renderModes(); renderChats(); renderMessages(); renderServers(); initHealthPoll(); renderPromptStrip(); renderMarket(); renderPromptCats(); renderPromptGrid(); renderProfilesList(); refreshIcons(); scrollBottom(); updateScrollFab(); updateUndoRedo();
  if(profileNameInput && !profileNameInput.value) profileNameInput.value=currentUser ? currentUser.name : (localStorage.getItem('profileName')||'Alex Rivera');
  if(profileEmail && !profileEmail.value) profileEmail.value=currentUser ? currentUser.email : (localStorage.getItem('profileEmail')||'alex@local.dev');
  // restore theme
  try{ const th=localStorage.getItem('mcp_theme'); if(th==='dark' && themeSelect){ themeSelect.value='dark'; document.documentElement.style.filter='invert(1) hue-rotate(180deg)'; document.body.style.background='#0f172a'; } }catch{}
}
initApp();

// keep original init for fallback (but we replaced)
 // renderProviders(); renderModes(); renderChats(); renderMessages(); renderServers(); renderPromptStrip(); renderMarket(); renderPromptCats(); renderPromptGrid(); refreshIcons(); scrollBottom(); updateUndoRedo();
 // if(profileNameInput) profileNameInput.value=localStorage.getItem('profileName')||'Alex Rivera';
 // if(profileEmail) profileEmail.value=localStorage.getItem('profileEmail')||'alex@local.dev';
