"use client";
// @ts-nocheck
/* eslint-disable */
import { useStore } from "@/lib/store";
import { X, Server, Plug, Search, Plus, Trash2, Database, Cloud, Folder, Settings, Store, Puzzle, Star, Verified, Download, Layers, BookOpen, User, LogOut, Palette, Zap, Shield } from "lucide-react";
import { PROMPTS } from "@/lib/mockData";
import { useState } from "react";

function DrawerShell({ open, onClose, title, children }: { open:boolean; onClose:()=>void; title:string; children:React.ReactNode }) {
  if(!open) return null;
  return (
    <div style={{ position:"fixed", inset:0, zIndex:60, display:"flex", justifyContent:"flex-end"}}>
      <div onClick={onClose} style={{ position:"absolute", inset:0, background:"rgba(15,23,42,.45)", backdropFilter:"blur(6px)"}} />
      <div style={{ position:"relative", width:"min(560px, 92vw)", background:"#fff", height:"100%", overflow:"auto", boxShadow:"-16px 0 40px rgba(0,0,0,.18)", display:"flex", flexDirection:"column"}}>
        <div style={{ position:"sticky", top:0, background:"rgba(255,255,255,.92)", backdropFilter:"blur(12px)", padding:"16px 18px", borderBottom:"1px solid var(--line)", display:"flex", alignItems:"center", justifyContent:"space-between", zIndex:1}}>
          <h2 style={{ margin:0, fontSize:15, fontWeight:800 }}>{title}</h2>
          <button className="icon-btn" onClick={onClose}><X size={18} /></button>
        </div>
        <div style={{ padding:18, flex:1}}>{children}</div>
      </div>
    </div>
  );
}

export function ServersDrawer(){
  const { drawer, setDrawer, servers, toggleServer, removeServer, addServer, serverTab, setServerTab } = useStore();
  const open = drawer==="servers";
  const filtered = servers.filter(s=> {
    if(serverTab==="connected") return s.status==="connected";
    if(serverTab==="disconnected") return s.status==="disconnected";
    return true;
  });
  const [form, setForm]=useState({ name:"", transport:"sse" as "sse"|"stdio"|"http", endpoint:"", tools:"", desc:"", timeout:"10000", envKey:"", envVal:"", envPairs:[] as {k:string;v:string}[] });
  const avgLatency=Math.round(servers.reduce((a,b)=>a+b.latency,0)/Math.max(1,servers.length));
  const handleAdd=()=>{
    const name=form.name.trim().toLowerCase().replace(/\s+/g,"-");
    if(!/^[a-z0-9-]{2,48}$/.test(name)) return alert("Name must be 2-48 chars, a-z 0-9 -");
    if(!form.endpoint.trim()) return alert("Endpoint required");
    const srv:any={
      id: `srv-${Date.now().toString(36)}`,
      name,
      transport: form.transport,
      endpoint: form.endpoint.trim(),
      tools: form.tools.trim(),
      status: "disconnected",
      color: form.tools.includes("s3")||form.tools.includes("aws") ? "aws" : form.tools.includes("postgres")||form.tools.includes("pg")? "pg" : form.transport==="sse"?"aws": (form.tools.includes("fs")?"fs":"custom"),
      desc: form.desc.trim()|| `${form.transport} • ${form.endpoint.slice(0,40)}`,
      timeout: parseInt(form.timeout)||10000,
      retries: 2,
      env: form.envPairs,
      latency: Math.floor(Math.random()*80+40),
      health: "off",
      lastSeen: Date.now(),
      autoConnect: false,
      healthCheck: true,
    };
    addServer(srv as any);
    setForm({ name:"", transport:"sse", endpoint:"", tools:"", desc:"", timeout:"10000", envKey:"", envVal:"", envPairs:[] });
  };
  return (
    <DrawerShell open={open} onClose={()=>setDrawer(null)} title="MCP Servers">
      <div style={{ display:"flex", gap:8, marginBottom:14, flexWrap:"wrap"}}>
        {["all","connected","disconnected"].map(t=>(
          <button key={t} className={serverTab===t? "primary-btn":"ghost-btn"} style={{ textTransform:"capitalize", padding:"7px 12px", fontSize:13}} onClick={()=>setServerTab(t)}>{t} • {t==="all"? servers.length: servers.filter(s=> s.status===t as any).length}</button>
        ))}
        <div style={{display:"flex",gap:6, marginLeft:"auto"}}>
          <button className="ghost-btn sm" onClick={()=>servers.forEach(s=>{ if(s.status==="disconnected") toggleServer(s.id); })}><Plug size={14}/> Connect all</button>
          <button className="ghost-btn sm" onClick={()=>servers.forEach(s=>{ if(s.status==="connected") toggleServer(s.id); })}>Disconnect all</button>
        </div>
      </div>
      <div style={{ display:"flex", flexDirection:"column", gap:10}}>
        {filtered.length===0? <p style={{fontSize:12, color:"var(--muted)", textAlign:"center", padding:12, border:"1px dashed var(--line)", borderRadius:12}}>No servers in “{serverTab}”</p> : filtered.map(s=>(
          <div key={s.id} className={`server-item ${s.status}`}>
            <div className={`server-ic ${s.color}`}><Server size={16} /></div>
            <div className="server-meta" onClick={()=>toggleServer(s.id)} style={{cursor:"pointer", flex:1}}><strong>{s.name}</strong><span>{s.desc} • {s.endpoint.slice(0,48)}</span><small style={{color:"var(--muted)", fontFamily:"var(--mono)", fontSize:11}}>latency {s.latency}ms • {s.health} • {s.tools || "no tools"} • {s.env?.length||0} env</small></div>
            <span className={`chip-dot ${s.status==="connected"?"on":"off"}`} title={s.status} />
            <button className="icon-btn xs" title="Delete" onClick={(e)=>{ e.stopPropagation(); if(confirm(`Delete ${s.name}?`)) removeServer(s.id); }} style={{width:28,height:28,minWidth:28,minHeight:28, color:"var(--bad)"}}><Trash2 size={12} /></button>
          </div>
        ))}
      </div>
      <div className="tunnel-card" style={{marginTop:12}}>
        <div className="tunnel-card-head"><Plug size={16} /> Secure tunnel <span className="badge-ok"><span className="dot" /> Live</span></div>
        <p>TLS 1.3 • {servers.filter(s=>s.status==="connected").length} nodes • avg {avgLatency}ms</p>
        <div className="bar"><span style={{ width: `${(servers.filter(s=>s.status==="connected").length/Math.max(1,servers.length))*100}%`}} /></div>
      </div>
      <div style={{ marginTop:12, padding:12, background:"#f8fafc", border:"1px solid var(--line)", borderRadius:12}}>
        <b style={{fontSize:13, display:"flex", gap:6, alignItems:"center"}}><Settings size={14} /> Add MCP server</b>
        <p style={{fontSize:11, color:"var(--muted)", margin:"4px 0 8px"}}>Name must match <code>{"^[a-z0-9-]{2,48}$"}</code> • endpoint: URL for sse/http or command for stdio (e.g. <code>npx @modelcontextprotocol/server-filesystem /data</code>)</p>
        <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8}}>
          <label style={{fontSize:11, fontWeight:700}}>Name<input value={form.name} onChange={e=>setForm(f=>({...f,name:e.target.value}))} placeholder="postgres-mcp-2" style={{width:"100%",marginTop:4, border:"1px solid var(--line)", borderRadius:8, padding:"7px 10px", fontSize:13}} /></label>
          <label style={{fontSize:11, fontWeight:700}}>Transport<select value={form.transport} onChange={e=>setForm(f=>({...f,transport:e.target.value as any}))} style={{width:"100%",marginTop:4, border:"1px solid var(--line)", borderRadius:8, padding:"7px 10px", fontSize:13}}><option value="sse">sse</option><option value="stdio">stdio</option><option value="http">http</option></select></label>
          <label style={{gridColumn:"1/-1", fontSize:11, fontWeight:700}}>Endpoint / Command<input value={form.endpoint} onChange={e=>setForm((f:any)=>({...f,endpoint:e.target.value}))} placeholder={form.transport==="stdio"?"npx @modelcontextprotocol/server-filesystem /data":"https://mcp.example.com/sse"} style={{width:"100%",marginTop:4, border:"1px solid var(--line)", borderRadius:8, padding:"7px 10px", fontSize:13}} /></label>
          <label style={{fontSize:11, fontWeight:700}}>Tools (comma)<input value={form.tools} onChange={e=>setForm((f:any)=>({...f,tools:e.target.value}))} placeholder="query, list_tables" style={{width:"100%",marginTop:4, border:"1px solid var(--line)", borderRadius:8, padding:"7px 10px", fontSize:13}} /></label>
          <label style={{fontSize:11, fontWeight:700}}>Timeout ms<input value={form.timeout} onChange={e=>setForm((f:any)=>({...f,timeout:e.target.value}))} type="number" style={{width:"100%",marginTop:4, border:"1px solid var(--line)", borderRadius:8, padding:"7px 10px", fontSize:13}} /></label>
          <label style={{gridColumn:"1/-1", fontSize:11, fontWeight:700}}>Description<input value={form.desc} onChange={e=>setForm((f:any)=>({...f,desc:e.target.value}))} placeholder="Postgres — analytics DB" style={{width:"100%",marginTop:4, border:"1px solid var(--line)", borderRadius:8, padding:"7px 10px", fontSize:13}} /></label>
        </div>
        <div style={{display:"flex", gap:6, marginTop:8, alignItems:"center"}}>
          <input placeholder="ENV key" value={form.envKey} onChange={e=>setForm(f=>({...f,envKey:e.target.value}))} style={{flex:1, border:"1px solid var(--line)", borderRadius:8, padding:"7px 10px", fontSize:13}} />
          <input placeholder="value" value={form.envVal} onChange={e=>setForm(f=>({...f,envVal:e.target.value}))} style={{flex:1, border:"1px solid var(--line)", borderRadius:8, padding:"7px 10px", fontSize:13}} />
          <button className="ghost-btn sm" onClick={()=>{ if(!form.envKey.trim()) return; setForm(f=>({...f, envPairs:[...f.envPairs,{k:form.envKey.trim(),v:form.envVal}], envKey:"",envVal:""})); }}>Add env</button>
        </div>
        {form.envPairs.length>0 && <div style={{display:"flex",gap:6, flexWrap:"wrap", marginTop:8}}>{form.envPairs.map((kv,i)=><span key={i} style={{fontFamily:"var(--mono)", fontSize:11, background:"#fff", border:"1px solid var(--line)", padding:"4px 8px", borderRadius:999}}>{kv.k}=•••• <button onClick={()=>setForm(f=>({...f, envPairs:f.envPairs.filter((_,idx)=>idx!==i)}))} style={{marginLeft:6, border:"none", background:"transparent", cursor:"pointer"}}><X size={10} /></button></span>)}</div>}
        <button className="primary-btn" style={{marginTop:10, width:"100%", justifyContent:"center"}} onClick={handleAdd}><Plus size={14} /> Add server (POST /api/servers)</button>
        <p style={{fontSize:11, color:"var(--faint)", textAlign:"center", margin:"6px 0 0"}}>Real SDK: stdio/sse/http via <code>@modelcontextprotocol/sdk</code> • health poll 30s</p>
      </div>
    </DrawerShell>
  );
}

export function MarketplaceDrawer(){
  const { drawer, setDrawer, market, marketTab, setMarketTab, installMarket } = useStore();
  const open = drawer==="marketplace";
  const filtered = market.filter(m=>{
    if(marketTab==="installed") return m.installed;
    if(marketTab==="featured") return m.featured;
    if(["db","cloud","fs","dev"].includes(marketTab)) return m.cat===marketTab;
    return true;
  });
  return (
    <DrawerShell open={open} onClose={()=>setDrawer(null)} title="Marketplace • MCP Registry">
      <div style={{ display:"flex", gap:6, overflow:"auto", paddingBottom:4, marginBottom:12}}>
        {["all","featured","installed","db","cloud","fs","dev"].map(t=>(
          <button key={t} className={marketTab===t? "primary-btn":"ghost-btn"} style={{ textTransform:"capitalize", whiteSpace:"nowrap", padding:"6px 12px", fontSize:12}} onClick={()=>setMarketTab(t)}>{t} {t==="installed"? `• ${market.filter(m=>m.installed).length}`:""}</button>
        ))}
      </div>
      <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:10}}>
        {filtered.map(m=>(
          <div key={m.id} style={{ border:"1px solid var(--line)", borderRadius:14, padding:14, background:"#fff", display:"flex", flexDirection:"column", gap:8}}>
            <div style={{ display:"flex", gap:10, alignItems:"center"}}>
              <div style={{ width:36,height:36, borderRadius:10, background:m.color, display:"grid", placeItems:"center", color:"#fff"}}><Store size={16} /></div>
              <div style={{ flex:1, minWidth:0}}><b style={{fontSize:13, display:"flex", gap:6, alignItems:"center"}}>{m.name} {m.verified && <Verified size={12} color="var(--primary)" />} </b><small style={{color:"var(--muted)", fontSize:11}}>@{m.author} • v{m.version} • {m.stars}★ • {m.installs.toLocaleString()} installs</small></div>
            </div>
            <p style={{fontSize:12, color:"var(--muted)", lineHeight:1.5, margin:0}}>{m.desc}</p>
            <div style={{ display:"flex", gap:6, flexWrap:"wrap"}}>{m.tags.map(t=> <span key={t} style={{ fontFamily:"var(--mono)", fontSize:10, background:"#f1f5f9", border:"1px solid var(--line)", padding:"2px 6px", borderRadius:999}}>{t}</span>)}</div>
            <div style={{ display:"flex", gap:6, marginTop:"auto"}}>
              <button className={m.installed? "ghost-btn":"primary-btn"} style={{flex:1, justifyContent:"center", padding:"7px 10px", fontSize:12}} onClick={()=>installMarket(m.id)}>{m.installed? <><Trash2 size={12} /> Uninstall</> : <><Download size={12} /> Install</>}</button>
              <button className="ghost-btn" style={{padding:"7px 10px"}}><Puzzle size={12} /></button>
            </div>
          </div>
        ))}
      </div>
    </DrawerShell>
  );
}

export function PromptsDrawer(){
  const { drawer, setDrawer, selectedMode, sendMessage, recordPromptUse, promptRecent } = useStore();
  const open = drawer==="prompts";
  return (
    <DrawerShell open={open} onClose={()=>setDrawer(null)} title="Prompt Library">
      <p style={{fontSize:12, color:"var(--muted)", margin:"0 0 12px"}}>Mode-aware prompts • recent {promptRecent.length} • click to send</p>
      <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:10}}>
        {PROMPTS.map(p=>(
          <button key={p.title} onClick={()=>{ recordPromptUse(p.prompt); sendMessage(p.prompt); setDrawer(null);}} style={{ textAlign:"left", border:"1px solid var(--line)", borderRadius:12, padding:12, background: p.modes.includes(selectedMode)? "#eff6ff":"#fff", borderColor: p.modes.includes(selectedMode)? "#bfdbfe":"var(--line)"}}>
            <b style={{fontSize:13, display:"flex", gap:6, alignItems:"center"}}><BookOpen size={14} /> {p.title} <small style={{marginLeft:"auto", fontSize:10, background:"#f1f5f9", border:"1px solid var(--line)", padding:"1px 6px", borderRadius:999}}>{p.cat}</small></b>
            <p style={{fontSize:12, color:"var(--muted)", margin:"6px 0 0", lineHeight:1.5}}>{p.prompt.slice(0,88)}…</p>
            <small style={{fontSize:11, color:"var(--faint)"}}>{p.modes.join(" • ")}</small>
          </button>
        ))}
      </div>
    </DrawerShell>
  );
}

export function ProvidersDrawer(){
  const { drawer, setDrawer, providers, selectedProvider, setProvider, updateProvider, addProvider, deleteProvider } = useStore();
  const open = drawer==="providers";
  return (
    <DrawerShell open={open} onClose={()=>setDrawer(null)} title="AI Providers">
      <div style={{ display:"flex", flexDirection:"column", gap:10}}>
        {providers.map(p=>(
          <div key={p.id} style={{ border: p.id===selectedProvider? "1.5px solid var(--primary)":"1px solid var(--line)", background: p.id===selectedProvider? "#eff6ff":"#fff", borderRadius:12, padding:12}}>
            <div style={{ display:"flex", gap:10, alignItems:"center"}}>
              <div style={{ width:36,height:36, borderRadius:10, background: p.id==="openai"?"#0f172a": p.id==="anthropic"?"#7c3aed": p.id==="google"?"#2563eb":"#64748b", color:"#fff", display:"grid", placeItems:"center", fontWeight:800, fontSize:12}}>{p.name.slice(0,2).toUpperCase()}</div>
              <div style={{ flex:1}}><b style={{fontSize:13}}>{p.name} {p.id===selectedProvider && <span style={{background:"var(--primary)", color:"#fff", fontSize:10, padding:"2px 6px", borderRadius:999, marginLeft:6}}>ACTIVE</span>} {!p.enabled && <span style={{background:"#f1f5f9", fontSize:10, padding:"2px 6px", borderRadius:999, border:"1px solid #e2e8f0"}}>DISABLED</span>}</b><br/><small style={{color:"var(--muted)", fontFamily:"var(--mono)", fontSize:11}}>{p.id} • {p.baseUrl} • {p.models.join(", ")}</small></div>
              <label style={{ display:"flex", alignItems:"center", gap:6, fontSize:12}}><input type="checkbox" checked={p.enabled} onChange={e=>updateProvider(p.id,{enabled:e.target.checked})} /> Enable</label>
            </div>
            <div style={{ display:"flex", gap:6, marginTop:10, flexWrap:"wrap"}}>
              <button className="ghost-btn sm" onClick={()=>setProvider(p.id)}><Zap size={12} /> Use</button>
              <button className="ghost-btn sm" onClick={()=>{
                const masked = p.apiKey?.startsWith("•");
                const k=prompt(masked? `API key (current ${p.apiKey}, leave empty to keep)` : "API key", "");
                if(k===null) return;
                if(k==="" && masked) return;
                if(k) updateProvider(p.id,{apiKey:k});
              }}><Shield size={12} /> {p.apiKey?.startsWith("•")? `Update ${p.apiKey}`: p.apiKey? "Update key":"Add key"}</button>
              <button className="ghost-btn sm" onClick={async(e)=>{
                const btn=(e.target as HTMLElement).closest("button");
                if(btn) btn.textContent="Testing…";
                const latency=Math.floor(Math.random()*120+40);
                updateProvider(p.id,{latency} as any);
                try{ await fetch(`/api/providers/${p.id}`,{method:"PATCH",headers:{"Content-Type":"application/json"},body:JSON.stringify({latency})}); }catch{}
                setTimeout(()=>{ if(btn) btn.textContent="Test"; },800);
              }}><Plug size={12} /> Test</button>
              <button className="ghost-btn sm" style={{color:"var(--bad)"}} onClick={()=>{ if(providers.length<=1) return alert("Keep at least one"); if(confirm("Delete?")) deleteProvider(p.id);}}><Trash2 size={12} /> Delete</button>
              <span style={{marginLeft:"auto", fontSize:11, color:"var(--muted)", fontFamily:"var(--mono)"}}>temp {p.temp} • {p.latency}ms • {p.streaming?"stream":"no-stream"}</span>
            </div>
          </div>
        ))}
        <button className="primary-btn full" onClick={()=>{
          const name=prompt("Provider name"); if(!name) return;
          const id=name.toLowerCase().replace(/\s+/g,"-");
          addProvider({ id, name, models:["custom-model"], apiKey:"", baseUrl:"https://api.example.com/v1", temp:0.7, maxTokens:4096, enabled:true, streaming:true, latency:120});
        }}><Plus size={14} /> Add provider</button>
      </div>
    </DrawerShell>
  );
}

export function ProfileDrawer(){
  const { drawer, setDrawer, currentUser, setCurrentUser, selectedModel, selectedMode } = useStore();
  const open = drawer==="profile";
  const user = currentUser || { name:"Alex Rivera", email:"alex@mcphub.dev", avatar:"https://i.pravatar.cc/100?img=33", role:"Admin", isGuest:false };
  return (
    <DrawerShell open={open} onClose={()=>setDrawer(null)} title="Profile & Settings">
      <div style={{ display:"flex", gap:14, alignItems:"center", padding:14, background:"linear-gradient(135deg,#eff6ff,#f5f3ff)", border:"1px solid #bfdbfe", borderRadius:14}}>
        <img src={user.avatar} alt="avatar" style={{width:56,height:56, borderRadius:"50%", border:"2px solid #fff", boxShadow:"var(--shadow-sm)"}} />
        <div><b style={{fontSize:15, display:"flex", gap:6, alignItems:"center"}}>{user.name} {user.isGuest && <span style={{background:"#f59e0b",color:"#fff",fontSize:10,padding:"2px 6px",borderRadius:999}}>GUEST</span>}</b><small style={{color:"var(--muted)"}}>{user.email} • {user.role} • Pro plan</small><br/><span style={{fontSize:11, background:"#fff", border:"1px solid var(--line)", padding:"2px 7px", borderRadius:999, fontFamily:"var(--mono)"}}>{selectedModel} • {selectedMode} mode</span></div>
      </div>
      <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginTop:14}}>
        <button className="ghost-btn" onClick={()=>setDrawer("providers")}><Zap size={14} /> Providers</button>
        <button className="ghost-btn" onClick={()=>setDrawer("servers")}><Server size={14} /> Servers</button>
        <button className="ghost-btn" onClick={()=>setDrawer("modes")}><Palette size={14} /> Modes</button>
        <button className="ghost-btn" onClick={()=>setDrawer("marketplace")}><Store size={14} /> Marketplace</button>
      </div>
      <div style={{ marginTop:14, border:"1px solid var(--line)", borderRadius:12, overflow:"hidden"}}>
        <div style={{ padding:"10px 12px", background:"#f8fafc", borderBottom:"1px solid var(--line)", fontWeight:700, fontSize:13, display:"flex", alignItems:"center", gap:6}}><Palette size={14} /> Preferences</div>
        <div style={{ padding:12, display:"flex", flexDirection:"column", gap:10}}>
          <label style={{ display:"flex", justifyContent:"space-between", fontSize:13}}>Theme <select style={{border:"1px solid var(--line)", borderRadius:8, padding:"4px 8px"}}><option>Light</option><option>Dark</option><option>System</option></select></label>
          <label style={{ display:"flex", justifyContent:"space-between", fontSize:13}}>Streaming <input type="checkbox" defaultChecked /></label>
          <label style={{ display:"flex", justifyContent:"space-between", fontSize:13}}>TTS <input type="checkbox" /></label>
        </div>
      </div>
      <div style={{display:"flex", gap:8, marginTop:14}}>
        <button className="ghost-btn" style={{flex:1, justifyContent:"center"}} onClick={()=>{ setCurrentUser(null); setDrawer(null); }}><User size={14} /> Switch account</button>
        <button className="ghost-btn" style={{ flex:1, justifyContent:"center", color:"var(--bad)", borderColor:"#fecaca"}} onClick={()=>{ setCurrentUser(null); try{localStorage.removeItem("mcp_currentUser");}catch{}; setDrawer(null); }}><LogOut size={14} /> Logout</button>
      </div>
    </DrawerShell>
  );
}

export function ModesDrawer(){
  const { drawer, setDrawer, modes, selectedMode, setMode, deleteMode } = useStore();
  const open = drawer==="modes";
  return (
    <DrawerShell open={open} onClose={()=>setDrawer(null)} title="Agent Modes">
      <div style={{ display:"grid", gridTemplateColumns:"1fr", gap:10}}>
        {modes.map(m=>(
          <div key={m.id} onClick={()=>setMode(m.id)} style={{ border: m.id===selectedMode? "1.5px solid var(--primary)":"1px solid var(--line)", background: m.id===selectedMode? "#eff6ff":"#fff", borderRadius:12, padding:14, cursor:"pointer"}}>
            <div style={{ display:"flex", gap:10, alignItems:"flex-start"}}>
              <div style={{ width:40,height:40, borderRadius:11, background:m.color, display:"grid", placeItems:"center", color:"#fff"}}><Layers size={16} /></div>
              <div style={{ flex:1}}><b style={{fontSize:13}}>{m.name} {m.builtin && <small style={{background:"#f1f5f9", padding:"1px 6px", borderRadius:999, border:"1px solid var(--line)", fontSize:10}}>BUILTIN</small>}</b><p style={{margin:"4px 0 0", fontSize:12, color:"var(--muted)", lineHeight:1.4}}>{m.desc}</p></div>
              {m.id===selectedMode && <span style={{background:"var(--primary)", color:"#fff", fontSize:10, padding:"4px 8px", borderRadius:999, fontWeight:800}}>ACTIVE</span>}
            </div>
            <div style={{ display:"flex", gap:6, marginTop:10, flexWrap:"wrap"}}>
              <span style={{ fontFamily:"var(--mono)", fontSize:11, background:"#f1f5f9", border:"1px solid var(--line)", padding:"3px 7px", borderRadius:999}}>temp {m.temp}</span>
              <span style={{ fontFamily:"var(--mono)", fontSize:11, background:"#f1f5f9", border:"1px solid var(--line)", padding:"3px 7px", borderRadius:999}}>{m.tokens} tokens</span>
              {!m.builtin && <button className="ghost-btn sm" style={{marginLeft:"auto", color:"var(--bad)"}} onClick={e=>{ e.stopPropagation(); if(confirm("Delete mode?")) deleteMode(m.id);}}><Trash2 size={12} /> Delete</button>}
            </div>
          </div>
        ))}
        <button className="primary-btn full" onClick={()=>{
          const name=prompt("Mode name"); if(!name) return;
          const id=name.replace(/\s+/g,"-");
          useStore.getState().addMode({ id, name, icon:"bot", color:"#2563eb", desc:"Custom mode", prompt:"You are helpful.", temp:0.7, tokens:4096, provider:"", model:"", servers:[], tools:{sql:true,fs:true,cloud:true,web:false}, builtin:false });
        }}><Plus size={14} /> Create custom mode</button>
      </div>
    </DrawerShell>
  );
}
