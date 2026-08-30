"use client";
import { useStore } from "@/lib/store";
import { X, Server, Plug, Search, Plus, Trash2, Database, Cloud, Folder, Settings, Store, Puzzle, Star, Verified, Download, Layers, BookOpen, User, LogOut, Palette, Zap, Shield } from "lucide-react";
import { PROMPTS } from "@/lib/mockData";

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
  const { drawer, setDrawer, servers, toggleServer, serverTab, setServerTab } = useStore();
  const open = drawer==="servers";
  const filtered = servers.filter(s=> {
    if(serverTab==="connected") return s.status==="connected";
    if(serverTab==="disconnected") return s.status==="disconnected";
    return true;
  });
  return (
    <DrawerShell open={open} onClose={()=>setDrawer(null)} title="MCP Servers">
      <div style={{ display:"flex", gap:8, marginBottom:14}}>
        {["all","connected","disconnected"].map(t=>(
          <button key={t} className={serverTab===t? "primary-btn":"ghost-btn"} style={{ textTransform:"capitalize", padding:"7px 12px", fontSize:13}} onClick={()=>setServerTab(t)}>{t} • {t==="all"? servers.length: servers.filter(s=> s.status===t as any).length}</button>
        ))}
        <button className="ghost-btn" style={{marginLeft:"auto"}} onClick={()=>servers.forEach(s=>{ if(s.status==="disconnected") toggleServer(s.id); })}><Plug size={14}/> Connect all</button>
      </div>
      <div style={{ display:"flex", flexDirection:"column", gap:10}}>
        {filtered.map(s=>(
          <div key={s.id} className={`server-item ${s.status}`} style={{ cursor:"pointer"}} onClick={()=>toggleServer(s.id)}>
            <div className={`server-ic ${s.color}`}><Server size={16} /></div>
            <div className="server-meta"><strong>{s.name}</strong><span>{s.desc} • {s.endpoint.slice(0,48)}</span><small style={{color:"var(--muted)", fontFamily:"var(--mono)", fontSize:11}}>latency {s.latency}ms • {s.health} • {s.tools}</small></div>
            <span className={`chip-dot ${s.status==="connected"?"on":"off"}`} />
          </div>
        ))}
      </div>
      <div style={{ marginTop:16, padding:12, background:"#f8fafc", border:"1px solid var(--line)", borderRadius:12}}>
        <b style={{fontSize:13, display:"flex", gap:6, alignItems:"center"}}><Settings size={14} /> Add server</b>
        <p style={{fontSize:12, color:"var(--muted)"}}>Transport: sse | stdio | http — supports env, timeout, retries, healthCheck</p>
        <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginTop:8}}>
          <input placeholder="name" style={{border:"1px solid var(--line)", borderRadius:8, padding:"8px 10px", fontSize:13}} />
          <select style={{border:"1px solid var(--line)", borderRadius:8, padding:"8px 10px", fontSize:13}}><option>sse</option><option>stdio</option><option>http</option></select>
          <input placeholder="endpoint / command" style={{gridColumn:"1 / -1", border:"1px solid var(--line)", borderRadius:8, padding:"8px 10px", fontSize:13}} />
        </div>
        <button className="primary-btn" style={{marginTop:10, width:"100%", justifyContent:"center"}} onClick={()=>alert("Server creation wired to POST /api/servers (mock)")}><Plus size={14} /> Add server</button>
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
                const k=prompt("API key", p.apiKey||"");
                if(k!==null) updateProvider(p.id,{apiKey:k});
              }}><Shield size={12} /> {p.apiKey? "Update key":"Add key"}</button>
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
  const { drawer, setDrawer } = useStore();
  const open = drawer==="profile";
  return (
    <DrawerShell open={open} onClose={()=>setDrawer(null)} title="Profile & Settings">
      <div style={{ display:"flex", gap:14, alignItems:"center", padding:14, background:"linear-gradient(135deg,#eff6ff,#f5f3ff)", border:"1px solid #bfdbfe", borderRadius:14}}>
        <img src="https://i.pravatar.cc/100?img=33" alt="avatar" style={{width:56,height:56, borderRadius:"50%", border:"2px solid #fff", boxShadow:"var(--shadow-sm)"}} />
        <div><b style={{fontSize:15}}>Alex Rivera</b><br/><small style={{color:"var(--muted)"}}>alex@mcphub.dev • Admin • Pro plan</small><br/><span style={{fontSize:11, background:"#fff", border:"1px solid var(--line)", padding:"2px 7px", borderRadius:999, fontFamily:"var(--mono)"}}>claude-3.5-sonnet • Chat mode</span></div>
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
      <button className="ghost-btn" style={{ width:"100%", marginTop:14, justifyContent:"center", color:"var(--bad)", borderColor:"#fecaca"}} onClick={()=>{ localStorage.clear(); location.reload();}}><LogOut size={14} /> Reset & logout (clear storage)</button>
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
