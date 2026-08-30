"use client";
import { useStore } from "@/lib/store";
import { Menu, Sparkles, MoreHorizontal, Plus, Download, Trash2, Share2, Copy } from "lucide-react";

export function Topbar() {
  const { chats, activeChatId, selectedMode, modes, setMode, newChat, drawer, setDrawer } = useStore();
  const activeChat = chats.find(c=>c.id===activeChatId);
  const activeMode = modes.find(m=>m.id===selectedMode) || modes[0];

  const handleExport = () => {
    if(!activeChat) return;
    const blob=new Blob([JSON.stringify(activeChat,null,2)],{type:"application/json"});
    const url=URL.createObjectURL(blob);
    const a=document.createElement("a"); a.href=url; a.download=`${activeChat.title}.json`; a.click(); URL.revokeObjectURL(url);
  };

  return (
    <div className="topbar">
      <div className="topbar-left">
        <button className="icon-btn lg:hidden" onClick={()=>useStore.getState().toggleSidebar()} aria-label="Menu"><Menu size={18} /></button>
        <div className="crumb">
          <span id="crumbTitle">{activeChat?.title || "MCP Terminal"}</span>
          <span className="live"><span className="dot" /> LIVE</span>
        </div>
        <div className="agent-modes hide-mobile">
          {modes.slice(0,4).map(m=> (
            <button key={m.id} className={`mode-pill ${m.id===selectedMode?"active":""} ${m.id!==selectedMode?"colored":""}`} style={m.id===selectedMode? { background:m.color, borderColor:m.color}:{ borderColor: m.color, color:m.color}} onClick={()=>setMode(m.id)}>
              {m.name}
            </button>
          ))}
          <button className="mode-pill" onClick={()=>setDrawer("modes")}><MoreHorizontal size={12} /> More</button>
        </div>
      </div>
      <div className="topbar-right">
        <div className="nodes-inline hide-mobile">
          <button className="ghost-btn sm" onClick={()=>setDrawer("servers")}><Sparkles size={14} /> MCP</button>
        </div>
        <div className="divider hide-mobile" />
        <button className="ghost-btn sm hide-mobile" onClick={handleExport}><Download size={14} /> Export</button>
        <button className="primary-btn" onClick={newChat}><Plus size={16} /> New chat</button>
      </div>
    </div>
  );
}
