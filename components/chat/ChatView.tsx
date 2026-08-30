"use client";
import { useStore } from "@/lib/store";
import { MessageItem } from "./MessageItem";
import { Composer } from "./Composer";
import { PromptStrip } from "./PromptStrip";
import { Sparkles, Database, Cloud, FolderOpen, ArrowDown } from "lucide-react";
import { useEffect, useRef, useState } from "react";

export function ChatView() {
  const { chats, activeChatId, editMessage, deleteMessage, retryLast } = useStore();
  const chat = chats.find(c=>c.id===activeChatId);
  const messages = chat?.messages || [];
  const scrollRef = useRef<HTMLDivElement>(null);
  const [showFab, setShowFab]=useState(false);
  const [toast, setToast]=useState<string|null>(null);

  useEffect(()=>{
    const el=scrollRef.current;
    if(!el) return;
    const onScroll=()=>{
      const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
      setShowFab(!atBottom && el.scrollHeight > el.clientHeight+100);
      const topbar=document.querySelector(".topbar");
      if(topbar) topbar.classList.toggle("scrolled", el.scrollTop>6);
    };
    el.addEventListener("scroll", onScroll, { passive:true });
    return()=>el.removeEventListener("scroll", onScroll);
  },[messages.length]);

  useEffect(()=>{
    // auto scroll to bottom when new message
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior:"smooth"});
  },[messages.length, messages[messages.length-1]?.text?.length]);

  const showToast=(m:string)=>{ setToast(m); setTimeout(()=>setToast(null),2200); };

  if(!chat){
    return (
      <div className="chat-wrap">
        <div className="messages">
          <div className="chat-empty">
            <div className="chat-empty-icon"><Sparkles size={22} /></div>
            <h3>Welcome to MCP Terminal</h3>
            <p>Connect MCP servers, choose a provider &amp; start chatting. Your workspace is ready.</p>
            <div className="chat-empty-actions">
              <button className="primary-btn" onClick={()=>useStore.getState().newChat()}>New conversation</button>
              <button className="ghost-btn" onClick={()=>useStore.getState().setDrawer("marketplace")}>Browse marketplace</button>
            </div>
          </div>
        </div>
        <PromptStrip />
        <Composer />
      </div>
    );
  }

  return (
    <div className="chat-wrap">
      <div className="messages" ref={scrollRef} id="messages">
        <div className="date-sep"><span>Today • {new Date().toLocaleDateString(undefined,{ month:"long", day:"numeric", year:"numeric"})} • {chat.title}</span></div>

        {messages.length===0 ? (
          <div className="chat-empty">
            <div className="chat-empty-icon"><Sparkles size={22} /></div>
            <h3>Start a conversation</h3>
            <p>Try “{chat.title}” or pick a prompt below. Attach files, use / commands, or talk in {useStore.getState().selectedMode} mode.</p>
            <div className="chat-empty-actions">
              <button className="chip" onClick={()=>useStore.getState().sendMessage("Query the remote PostgreSQL database for the last 5 user registrations")}><Database size={14} /> Query PostgreSQL</button>
              <button className="chip" onClick={()=>useStore.getState().sendMessage("List S3 buckets in aws-us-east")}><Cloud size={14} /> List S3</button>
              <button className="chip" onClick={()=>useStore.getState().sendMessage("List files in /data")}><FolderOpen size={14} /> Browse /data</button>
            </div>
          </div>
        ) : (
          messages.map(m=> (
            <MessageItem
              key={m.id}
              msg={m}
              onCopy={()=>showToast("Copied")}
              onEdit={()=>{
                const next=prompt("Edit message", m.text||"");
                if(next!==null) editMessage(m.id, next);
              }}
              onDelete={()=> { if(confirm("Delete message?")) deleteMessage(m.id);}}
              onRetry={retryLast}
            />
          ))
        )}
      </div>

      <button className={`scroll-fab ${showFab?"visible":""}`} onClick={()=>scrollRef.current?.scrollTo({top: scrollRef.current.scrollHeight, behavior:"smooth"})} aria-label="Scroll to bottom"><ArrowDown size={18} /></button>

      <PromptStrip />
      <Composer />

      {toast && <div style={{ position:"fixed", bottom: 88, left:"50%", transform:"translateX(-50%)", background:"#0f172a", color:"#fff", padding:"8px 14px", borderRadius:999, fontSize:13, fontWeight:600, boxShadow:"0 8px 24px rgba(0,0,0,.18)", zIndex:50}}>{toast}</div>}
    </div>
  );
}
