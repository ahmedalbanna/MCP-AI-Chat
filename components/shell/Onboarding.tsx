"use client";
import { useState } from "react";
import { Network, Cpu, Store, Bot, Check, ArrowRight } from "lucide-react";
import { useStore } from "@/lib/store";

const STEPS = [
  { icon: Network, color:"#2563eb", title:"Welcome to MCP Terminal", desc:"Connect AI models to tools and local resources via the Model Context Protocol — securely, locally, fast.", bullets:["TLS 1.3 secure tunnel","MCP nodes: Postgres, S3, FS & more","One chat, many tools"]},
  { icon: Cpu, color:"#7c3aed", title:"AI Providers", desc:"Bring your own key — OpenAI, Anthropic, Google or local LLM. Switch instantly, per-mode overrides.", bullets:["Multiple providers & models","Per-mode provider & temp","Streaming & fallback"]},
  { icon: Store, color:"#ff9900", title:"MCP Marketplace & Servers", desc:"Discover verified MCP servers, install with one click, connect / disconnect, health checks & logs.", bullets:["Browse 10+ verified servers","SSE / STDIO / HTTP","Env vars, timeout & retries"]},
  { icon: Bot, color:"#16a34a", title:"Agent Modes & Live", desc:"Modes shape tone & tools. Live speech gives hands-free conversation with turn-taking and TTS.", bullets:["8 modes: Chat, Code, Research…","Type / for 16 commands • 2+ letters for prompts","Live mic — interrupt anytime"]},
];

export function Onboarding(){
  const { onboarded, setOnboarded, setDrawer } = useStore();
  const [idx, setIdx]=useState(0);
  if(onboarded) return null;
  // also respect localStorage legacy key
  if(typeof window!=="undefined" && localStorage.getItem("mcp_onboarded")==="1") {
    // sync to store
    if(!onboarded) setTimeout(()=>setOnboarded(true),0);
    return null;
  }
  const s=STEPS[idx];
  const Icon=s.icon;
  return (
    <div className="onboarding" role="dialog" aria-modal="true" aria-label="Onboarding" style={{position:"fixed", inset:0, zIndex:70, display:"grid", placeItems:"center"}}>
      <div onClick={()=>{ setOnboarded(true); }} style={{position:"absolute", inset:0, background:"rgba(15,23,42,.45)", backdropFilter:"blur(6px)"}} />
      <div style={{position:"relative", width:"min(520px,92vw)", background:"#fff", borderRadius:16, overflow:"hidden", boxShadow:"0 20px 60px rgba(15,23,42,.18)", border:"1px solid var(--line)"}}>
        <div style={{height:4, background:"#e2e8f0"}}>
          <div style={{height:"100%", width:`${((idx+1)/STEPS.length)*100}%`, background:"linear-gradient(90deg,#2563eb,#7c3aed)", transition:"width .3s"}} />
        </div>
        <div style={{padding:24, textAlign:"center"}}>
          <div style={{width:56,height:56, borderRadius:14, background:s.color, color:"#fff", display:"grid", placeItems:"center", margin:"0 auto 12px", boxShadow:"0 8px 20px rgba(0,0,0,.12)"}}><Icon size={24} /></div>
          <h3 style={{margin:"0 0 6px", fontSize:18, fontWeight:800, letterSpacing:"-.02em"}}>{s.title}</h3>
          <p style={{margin:0, fontSize:13, color:"var(--muted)", lineHeight:1.6}}>{s.desc}</p>
          <ul style={{listStyle:"none", padding:0, margin:"14px 0 0", display:"flex", flexDirection:"column", gap:8, textAlign:"left"}}>
            {s.bullets.map(b=>(
              <li key={b} style={{display:"flex", gap:8, alignItems:"center", fontSize:13, background:"#f8fafc", border:"1px solid var(--line)", padding:"8px 10px", borderRadius:10}}><Check size={14} color="var(--ok)" /> {b}</li>
            ))}
          </ul>
          <div style={{display:"flex", gap:6, justifyContent:"center", marginTop:14}}>
            {STEPS.map((_,i)=><span key={i} style={{width: idx===i? 20:8, height:8, borderRadius:999, background: idx===i? "var(--primary)":"#e2e8f0", transition:"all .2s"}} />)}
          </div>
        </div>
        <div style={{display:"flex", justifyContent:"space-between", padding:"14px 16px", borderTop:"1px solid var(--line)", background:"#fcfcfe"}}>
          <button className="ghost-btn sm" onClick={()=> setOnboarded(true)}>Skip</button>
          <div style={{display:"flex",gap:8}}>
            {idx>0 && <button className="ghost-btn sm" onClick={()=> setIdx(i=>i-1)}>Back</button>}
            <button className="primary-btn sm" onClick={()=>{ if(idx < STEPS.length-1) setIdx(i=>i+1); else { setOnboarded(true); const hasUser = useStore.getState().currentUser; if(!hasUser) { /* will trigger auth via drawer */ } } }}>{idx===STEPS.length-1? "Get started":"Next"} <ArrowRight size={14} /></button>
          </div>
        </div>
      </div>
    </div>
  );
}
