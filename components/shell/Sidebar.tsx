"use client";
import { useStore } from "@/lib/store";
import { Search, Plus, Layers, Server, Store, Library, Settings, X, ChevronDown, Plug, Activity, LayoutGrid, MessageSquare, Trash2, Archive, Pencil } from "lucide-react";
import { useState, useMemo } from "react";

export function Sidebar() {
  const {
    chats, activeChatId, selectChat, newChat, deleteChat, archiveChat, showArchived, setShowArchived,
    servers, toggleServer, selectedProvider, selectedModel, selectedMode, providers, setProvider, setModel,
    drawer, setDrawer, sidebarOpen, toggleSidebar, currentUser
  } = useStore();
  const [q, setQ] = useState("");
  const [sectionOpen, setSectionOpen] = useState({ workspace: true, servers: true });

  const activeProvider = providers.find(p=>p.id===selectedProvider);
  const filteredChats = useMemo(()=>{
    let c = chats;
    if (!showArchived) c = c.filter(x=>!x.archived);
    if (q) {
      const lower=q.toLowerCase();
      c=c.filter(x=> x.title.toLowerCase().includes(lower) || x.messages.some(m=> (m.text||"").toLowerCase().includes(lower)));
    }
    return c.sort((a,b)=> b.created - a.created);
  },[chats, q, showArchived]);

  const grouped = useMemo(()=>{
    const now=Date.now();
    const groups: Record<string, typeof filteredChats> = { Today: [], Yesterday: [], Earlier: [], Archived: [] };
    filteredChats.forEach(c=>{
      if (c.archived) { groups.Archived.push(c); return; }
      const diff=now-c.created;
      if(diff< 24*3600*1000) groups.Today.push(c);
      else if(diff< 48*3600*1000) groups.Yesterday.push(c);
      else groups.Earlier.push(c);
    });
    return groups;
  },[filteredChats]);

  const activeServers = servers.filter(s=>s.status==="connected").length;

  return (
    <>
      {/* mobile overlay */}
      {sidebarOpen && <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-40 lg:hidden" onClick={toggleSidebar} />}
      <aside className={`sidebar ${sidebarOpen ? "!flex fixed inset-y-0 left-0 z-50 w-[86%] max-w-[360px] lg:static" : ""} hidden lg:flex`}>
        <div className="sidebar-top">
          <div className="brand">
            <div className="brand-mark"><Layers size={20} /></div>
            <div className="brand-copy">
              <h1>MCP Terminal</h1>
              <p><span className="pulse" /> AI WORKSPACE • v2</p>
            </div>
            <button className="icon-btn xs ml-auto lg:hidden" onClick={toggleSidebar}><X size={16} /></button>
          </div>

          <div className="sidebar-search">
            <Search size={16} />
            <input id="sidebarSearch" placeholder="Search chats & servers" value={q} onChange={e=>setQ(e.target.value)} />
            {q && <button className="search-clear" onClick={()=>setQ("")}><X size={12} /></button>}
            <kbd>⌘K</kbd>
          </div>

          <button className="primary-btn full" onClick={newChat}><Plus size={16} /> New conversation</button>

          <div className="provider-mini">
            <div className="provider-mini-head">
              <label><Activity size={12} /> PROVIDER</label>
              <span className="temp-badge">{activeProvider?.temp ?? 0.7} temp</span>
            </div>
            <div className="provider-mini-selects">
              <select value={selectedProvider} onChange={e=>setProvider(e.target.value)}>
                {providers.filter(p=>p.enabled).map(p=> <option key={p.id} value={p.id}>{p.name}</option>)}
              </select>
              <select value={selectedModel} onChange={e=>setModel(e.target.value)}>
                {activeProvider?.models.map(m=> <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <div className="provider-status">
              <span className="dot" /> {activeProvider?.apiKey ? `Ready • ${activeProvider.latency}ms` : `No key • ${activeProvider?.latency}ms`}
              <span className="ml-auto text-[10px] font-bold tracking-widest text-[var(--faint)]">{servers.filter(s=>s.status==="connected").length}/{servers.length} MCP</span>
            </div>
          </div>
        </div>

        <div className="sidebar-scroll">
          {/* Workspace */}
          <div className={`section ${sectionOpen.workspace ? "" : "collapsed"}`}>
            <div className="section-head">
              <button className="section-toggle" onClick={()=>setSectionOpen(s=>({...s, workspace: !s.workspace}))}>
                <ChevronDown size={14} className="section-chevron" />
                <h3><MessageSquare size={12} /> WORKSPACE</h3>
              </button>
              <span className="count">{filteredChats.length}</span>
              <div className="head-actions">
                <button className="icon-btn xs" title="Toggle archived" onClick={()=>setShowArchived(!showArchived)} style={{ width:28, height:28, minWidth:28, minHeight:28}}><Archive size={14} style={{opacity: showArchived?1:0.5}} /></button>
              </div>
            </div>
            <div className="section-body" style={{ maxHeight: sectionOpen.workspace ? 2000 : 0 }}>
              {filteredChats.length===0 ? (
                <p style={{ fontSize:12, color:"var(--muted)", textAlign:"center", padding:"10px 8px", border:"1px dashed var(--line)", borderRadius:12, background:"#fff"}}>No chats yet<br/><small>Start a new conversation</small></p>
              ) : (
                <div className="chat-groups">
                  {(["Today","Yesterday","Earlier","Archived"] as const).map(g=> grouped[g].length ? (
                    <div key={g}>
                      <p className="group-title">{g} • {grouped[g].length}</p>
                      <div style={{ display:"flex", flexDirection:"column", gap:8}}>
                        {grouped[g].map(c=> (
                          <div key={c.id} className={`chat-item ${c.id===activeChatId?"active":""} ${c.archived?"archived":""}`} onClick={()=>selectChat(c.id)}>
                            <div className="chat-item-main">
                              <b>{c.title}</b>
                              <p>{c.messages[c.messages.length-1]?.text?.replace(/\*\*/g,"").replace(/\|/g," ").replace(/`/g,"").slice(0,64) || "Empty conversation"}</p>
                            </div>
                            <div className="flex flex-col items-end gap-1 shrink-0">
                              <span className="chat-item-meta">{new Date(c.created).toLocaleDateString()}</span>
                              <div className="chat-item-actions">
                                <button className="icon-btn xs" onClick={e=>{e.stopPropagation(); archiveChat(c.id);}} title={c.archived?"Unarchive":"Archive"} style={{width:26,height:26,minWidth:26,minHeight:26}}><Archive size={12} /></button>
                                <button className="icon-btn xs" onClick={e=>{e.stopPropagation(); if(confirm("Delete chat?")) deleteChat(c.id);}} title="Delete" style={{width:26,height:26,minWidth:26,minHeight:26, color:"var(--bad)"}}><Trash2 size={12} /></button>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ):null)}
                </div>
              )}
            </div>
          </div>

          {/* MCP Servers */}
          <div className={`section ${sectionOpen.servers ? "" : "collapsed"}`}>
            <div className="section-head">
              <button className="section-toggle" onClick={()=>setSectionOpen(s=>({...s, servers: !s.servers}))}>
                <ChevronDown size={14} className="section-chevron" />
                <h3><Server size={12} /> MCP SERVERS</h3>
              </button>
              <span className="count" style={{ background: activeServers? "#f0fdf4": "var(--primary-ghost)", color: activeServers? "#16a34a":"var(--primary)", borderColor: activeServers?"#bbf7d0":"#dbeafe"}}>{activeServers}/{servers.length}</span>
              <div className="head-actions">
                <button className="icon-btn xs" onClick={()=>setDrawer("servers")} title="Manage" style={{width:26,height:26,minWidth:26,minHeight:26}}><Settings size={12} /></button>
              </div>
            </div>
            <div className="section-body" style={{ maxHeight: sectionOpen.servers? 2000:0}}>
              <ul className="server-list">
                {servers.map(s=> (
                  <li key={s.id} className={`server-item ${s.status}`} onClick={()=>toggleServer(s.id)}>
                    <div className={`server-ic ${s.color}`}><Server size={16} /></div>
                    <div className="server-meta"><strong>{s.name}</strong><span>{s.transport} • {s.tools.split(",")[0]}</span></div>
                    <span className={`chip-dot ${s.status==="connected"?"on": s.status==="connecting"?"":"off"}`} />
                  </li>
                ))}
              </ul>
              <div className="tunnel-card">
                <div className="tunnel-card-head"><Plug size={16} /> Secure tunnel <span className="badge-ok"><span className="dot" /> Live</span></div>
                <p>TLS 1.3 • {activeServers} nodes • avg {Math.round(servers.reduce((a,b)=>a+b.latency,0)/servers.length)}ms</p>
                <div className="bar"><span style={{ width: `${(activeServers/servers.length)*100}%`}} /></div>
              </div>
              <div className="quick-grid">
                <button className="quick-card" onClick={()=>setDrawer("marketplace")}><Store size={18} /><span>Marketplace</span><small>{servers.length} servers</small></button>
                <button className="quick-card" onClick={()=>setDrawer("prompts")}><Library size={18} /><span>Prompts</span><small>14 templates</small></button>
              </div>
            </div>
          </div>
        </div>

        <div className="sidebar-foot" onClick={()=>setDrawer("profile")} role="button" tabIndex={0}>
          <div className="foot-avatar-wrap">
            <img src={currentUser?.avatar || "https://i.pravatar.cc/100?img=33"} alt="avatar" />
            <span className="foot-status-dot" />
          </div>
          <div className="foot-meta">
            <strong style={{display:"flex", gap:6, alignItems:"center"}}>{currentUser?.name || "Alex Rivera"} {currentUser?.isGuest && <span style={{background:"#f59e0b", color:"#fff", fontSize:9, fontWeight:800, padding:"1px 5px", borderRadius:999}}>GUEST</span>}</strong>
            <p>{currentUser?.role || "Admin"} • {selectedModel} • {selectedMode}</p>
          </div>
          <ChevronDown size={16} className="foot-chevron" />
        </div>
      </aside>
    </>
  );
}
