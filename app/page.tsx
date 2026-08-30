"use client";
import { Sidebar } from "@/components/shell/Sidebar";
import { Topbar } from "@/components/shell/Topbar";
import { ChatView } from "@/components/chat/ChatView";
import { ServersDrawer, MarketplaceDrawer, PromptsDrawer, ProvidersDrawer, ProfileDrawer, ModesDrawer } from "@/components/drawers/Drawers";
import { Onboarding } from "@/components/shell/Onboarding";
import { AuthModal } from "@/components/shell/AuthModal";
import { useEffect, useState } from "react";
import { useStore } from "@/lib/store";
import { Network } from "lucide-react";

export default function Page() {
  const [ready, setReady]=useState(false);
  const [showSplash, setShowSplash]=useState(true);
  const [splashProgress, setSplashProgress]=useState(0);

  useEffect(()=>{
    const t=setTimeout(()=>setReady(true), 80);
    // splash progress animation 0→100 over 900ms
    let p=0;
    const iv=setInterval(()=>{ p+=8; setSplashProgress(Math.min(100,p)); if(p>=100) clearInterval(iv); }, 70);
    const s=setTimeout(()=>setShowSplash(false), 950);
    useStore.getState().hydrateFromDb?.().catch(()=>{});
    // restore onboarded/currentUser from localStorage legacy
    try{
      const ob=localStorage.getItem("mcp_onboarded");
      if(ob==="1" && !useStore.getState().onboarded) useStore.getState().setOnboarded(true);
      const cu=localStorage.getItem("mcp_currentUser");
      if(cu && !useStore.getState().currentUser){
        try{ useStore.getState().setCurrentUser(JSON.parse(cu)); }catch{}
      }
    }catch{}
    return()=>{ clearTimeout(t); clearTimeout(s); clearInterval(iv); };
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
      <Onboarding />
      <AuthModal />

      {showSplash && (
        <div className="splash" aria-hidden="false" style={{position:"fixed", inset:0, zIndex:80, background:"radial-gradient(1100px 520px at 50% -12%, #e8eefd 0%, var(--bg) 58%), var(--bg)", display:"grid", placeItems:"center"}}>
          <div className="splash-inner" style={{textAlign:"center", display:"flex", flexDirection:"column", gap:12, alignItems:"center"}}>
            <div className="splash-mark" style={{width:56,height:56, borderRadius:14, background:"linear-gradient(135deg,#2563eb 0%, #7c3aed 50%, #06b6d4 100%)", display:"grid", placeItems:"center", color:"#fff", boxShadow:"0 12px 24px rgba(37,99,235,.3)"}}><Network size={20} /></div>
            <h1 style={{margin:0, fontSize:20, fontWeight:800, letterSpacing:"-.03em"}}>MCP Terminal</h1>
            <p style={{margin:0, fontSize:11, color:"var(--muted)", fontWeight:700, letterSpacing:".08em"}}>Remote Protocol Active • TLS 1.3</p>
            <div className="splash-bar" style={{width:180,height:6, background:"#e2e8f0", borderRadius:999, overflow:"hidden"}}><span id="splashProgress" style={{display:"block", height:"100%", width:`${splashProgress}%`, background:"linear-gradient(90deg,#22c55e 0%, #38bdf8 100%)", borderRadius:999, transition:"width .12s"}} /></div>
            <small id="splashStatus" style={{fontSize:11, color:"var(--muted)"}}>{splashProgress<30?"Initializing secure tunnel…": splashProgress<60?"Loading workspaces…": splashProgress<90?"Connecting MCP nodes…":"Ready"}</small>
          </div>
        </div>
      )}
    </div>
  );
}
