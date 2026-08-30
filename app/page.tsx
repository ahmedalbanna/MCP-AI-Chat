"use client";
import { Sidebar } from "@/components/shell/Sidebar";
import { Topbar } from "@/components/shell/Topbar";
import { ChatView } from "@/components/chat/ChatView";
import { ServersDrawer, MarketplaceDrawer, PromptsDrawer, ProvidersDrawer, ProfileDrawer, ModesDrawer } from "@/components/drawers/Drawers";
import { useEffect, useState } from "react";

export default function Page() {
  const [ready, setReady]=useState(false);
  const [showSplash, setShowSplash]=useState(true);

  useEffect(()=>{
    // hydrate guard for zustand persist
    const t=setTimeout(()=>setReady(true), 80);
    const s=setTimeout(()=>setShowSplash(false), 900);
    return()=>{ clearTimeout(t); clearTimeout(s); };
  },[]);

  if(!ready){
    return <div style={{ height:"100dvh", display:"grid", placeItems:"center", background:"var(--bg)"}}><div className="typing"><span /><span /><span /></div></div>;
  }

  return (
    <div className="app">
      <Sidebar />
      <div className="main">
        <Topbar />
        <ChatView />
      </div>

      <ServersDrawer />
      <MarketplaceDrawer />
      <PromptsDrawer />
      <ProvidersDrawer />
      <ProfileDrawer />
      <ModesDrawer />

      {showSplash && (
        <div style={{ position:"fixed", inset:0, zIndex:80, background:"radial-gradient(1100px 520px at 50% -12%, #e8eefd 0%, var(--bg) 58%), var(--bg)", display:"grid", placeItems:"center", transition:"opacity .4s"}}>
          <div style={{ textAlign:"center", display:"flex", flexDirection:"column", gap:14, alignItems:"center"}}>
            <div style={{ width:56, height:56, borderRadius:16, background:"linear-gradient(135deg,#2563eb 0%, #7c3aed 50%, #06b6d4 100%)", display:"grid", placeItems:"center", color:"#fff", boxShadow:"0 12px 24px rgba(37,99,235,.3)"}}>◆</div>
            <b style={{ fontSize:18, letterSpacing:"-.03em"}}>MCP Terminal</b>
            <small style={{ color:"var(--muted)", fontWeight:600, letterSpacing:".08em", fontSize:10}}>PRODUCTION • NEXT.JS 15 • ZUSTAND • MCP SDK</small>
            <div style={{ width:160, height:6, background:"#e2e8f0", borderRadius:999, overflow:"hidden"}}><span style={{ display:"block", height:"100%", width:"100%", background:"linear-gradient(90deg,#2563eb,#06b6d4)", animation:"shimmer 1s infinite"}} /></div>
          </div>
        </div>
      )}
    </div>
  );
}
