"use client";
import { useStore } from "@/lib/store";
import { SLASH } from "@/lib/mockData";
import { Paperclip, Mic, Send, X, Bot, ChevronDown, ListChecks, Trash2 } from "lucide-react";
import { useState, useRef, useEffect } from "react";
import { uid, fmtSize } from "@/lib/utils";

export function Composer() {
  const { sendMessage, selectedMode, modes, setMode } = useStore();
  const [text, setText]=useState("");
  const [slashOpen, setSlashOpen]=useState(false);
  const [slashIdx, setSlashIdx]=useState(0);
  const [pending, setPending]=useState<{id:string; name:string; size:number; type:string; url?:string}[]>([]);
  const [modeOpen, setModeOpen]=useState(false);
  const [todoDraft, setTodoDraft]=useState<{text:string; done:boolean}[]>(()=>{
    try{ const v=JSON.parse(localStorage.getItem("mcp_todos_draft")||"null"); return Array.isArray(v)? v:[];}catch{return[];}
  });
  const [todoInput, setTodoInput]=useState("");
  const inputRef=useRef<HTMLTextAreaElement>(null);
  const fileRef=useRef<HTMLInputElement>(null);

  const filtered = text.startsWith("/") ? SLASH.filter(s=> s.key.startsWith(text.split(" ")[0].toLowerCase()) || s.desc.toLowerCase().includes(text.toLowerCase())) : [];
  const activeMode = modes.find(m=>m.id===selectedMode);

  useEffect(()=>{
    localStorage.setItem("mcp_todos_draft", JSON.stringify(todoDraft));
  },[todoDraft]);

  const handleSend=()=>{
    if(!text.trim() && pending.length===0 && todoDraft.length===0) return;
    let finalText=text;
    if(todoDraft.length){
      finalText += (finalText? "\n\n":"") + "TODO:\n" + todoDraft.map((t,i)=> `${t.done?"[x]":"[ ]"} ${i+1}. ${t.text}`).join("\n");
    }
    const attachments = pending.map(p=>({ name:p.name, size:p.size, type:p.type}));
    sendMessage(finalText, attachments);
    setText(""); setPending([]); setSlashOpen(false);
  };

  const onKeyDown=(e:React.KeyboardEvent)=>{
    if(slashOpen && filtered.length){
      if(e.key==="ArrowDown"){ e.preventDefault(); setSlashIdx(i=> (i+1)%filtered.length); }
      else if(e.key==="ArrowUp"){ e.preventDefault(); setSlashIdx(i=> (i-1+filtered.length)%filtered.length); }
      else if(e.key==="Enter" && !e.shiftKey){ e.preventDefault(); const s=filtered[slashIdx]; if(s){ setText(s.insert); setSlashOpen(false); setTimeout(()=>inputRef.current?.focus(),0); } return;}
      else if(e.key==="Escape"){ setSlashOpen(false); }
    }
    if(e.key==="Enter" && !e.shiftKey){ e.preventDefault(); handleSend(); }
  };

  useEffect(()=>{
    if(text.startsWith("/")) { setSlashOpen(true); setSlashIdx(0); } else setSlashOpen(false);
  },[text]);

  const handleFiles=(files:FileList | null)=>{
    if(!files) return;
    const arr=[...files].slice(0, 6 - pending.length);
    const mapped = arr.map(f=> ({ id: uid(), name: f.name, size: f.size, type: f.type||"application/octet-stream", url: f.type.startsWith("image/")? URL.createObjectURL(f): undefined }));
    setPending(p=> [...p, ...mapped]);
  };

  return (
    <div className="composer">
      {/* slash suggest */}
      {slashOpen && filtered.length>0 && (
        <div className="prompt-suggest">
          <div className="prompt-suggest-head"><b>Slash commands</b><kbd>↑↓ Enter</kbd></div>
          <div className="prompt-suggest-list">
            {filtered.slice(0,6).map((s,i)=>(
              <button key={s.key} className={`prompt-suggest-item ${i===slashIdx?"active":""}`} onClick={()=>{ setText(s.insert); setSlashOpen(false); }}>
                <span className="ps-icon" style={{ background: s.cat==="MCP"?"#2563eb": s.cat==="Chat"?"#0f172a":"#64748b"}}>{s.key.slice(1,2).toUpperCase()}</span>
                <span className="ps-text"><b>{s.key} <small style={{marginLeft:6, color:"var(--muted)", fontWeight:600}}>{s.cat}</small></b><small>{s.desc} • {s.hint}</small></span>
                <span className="ps-hint">{s.key}</span>
              </button>
            ))}
          </div>
          <div className="prompt-suggest-foot"><kbd>Tab</kbd> autocomplete • <kbd>Esc</kbd> close</div>
        </div>
      )}

      {/* attach preview */}
      {pending.length>0 && (
        <div className="attach-preview">
          <div className="attach-preview-head">
            <span className="attach-count">{pending.length}</span> Attachments
            <small>{pending.reduce((a,b)=>a+b.size,0) >0 ? fmtSize(pending.reduce((a,b)=>a+b.size,0)):""}</small>
            <button className="ghost-btn sm attach-clear" onClick={()=>setPending([])}><Trash2 size={12} /> Clear</button>
          </div>
          <div className="attach-preview-grid">
            {pending.map(p=> (
              <div key={p.id} className={`preview ${p.url?"":"preview--file"}`}>
                {p.url ? <img src={p.url} alt={p.name} /> : <><div className={`preview-icon ${p.type.includes("pdf")?"pdf": p.type.includes("csv")?"csv": p.type.includes("json")?"json":"generic"}`}>{p.name.split(".").pop()?.slice(0,3).toUpperCase()}</div><div className="fn"><strong>{p.name}</strong><small>{fmtSize(p.size)}</small></div></>}
                <button className="rm" onClick={()=>setPending(x=>x.filter(y=>y.id!==p.id))}><X size={12} /></button>
                {p.url && <div className="preview-meta">{p.name.slice(0,14)} <span className="size">{fmtSize(p.size)}</span></div>}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* todo draft mini */}
      {(todoDraft.length>0 || todoInput) && (
        <div className="task-card" style={{ marginBottom:10 }}>
          <div className="task-head"><b><ListChecks size={14} /> Checklist</b><span>{todoDraft.filter(t=>t.done).length}/{todoDraft.length}</span>
            <div className="task-actions">
              <button className="ghost-btn sm" onClick={()=>setTodoDraft([])}>Clear</button>
            </div>
          </div>
          {todoDraft.length>0 && <ul className="todo-list">
            {todoDraft.map((t,i)=>(
              <li key={i} className={`todo-item ${t.done?"done":""}`}>
                <input type="checkbox" checked={t.done} onChange={()=>setTodoDraft(d=> d.map((x,idx)=> idx===i? {...x, done:!x.done}:x))} />
                <span>{t.text}</span>
                <button className="icon-btn xs" onClick={()=>setTodoDraft(d=> d.filter((_,idx)=> idx!==i))} style={{width:24,height:24,minWidth:24,minHeight:24}}><X size={12} /></button>
              </li>
            ))}
          </ul>}
          <div style={{ display:"flex", gap:8, padding:"0 12px 12px"}}>
            <input value={todoInput} onChange={e=>setTodoInput(e.target.value)} onKeyDown={e=>{ if(e.key==="Enter" && todoInput.trim()){ setTodoDraft(d=>[...d, {text: todoInput.trim().slice(0,120), done:false}]); setTodoInput(""); }}} placeholder="Add todo — press Enter" style={{ flex:1, border:"1px solid var(--line)", borderRadius:999, padding:"8px 12px", fontSize:13, outline:"none"}} />
            <button className="primary-btn" onClick={()=>{ if(todoInput.trim()){ setTodoDraft(d=>[...d,{text:todoInput.trim(),done:false}]); setTodoInput("");}}}>Add</button>
          </div>
        </div>
      )}

      <div className="composer-inner" style={{ display:"flex", gap:8, alignItems:"flex-end", background:"#fff", border:"1px solid var(--line)", borderRadius:18, padding:"10px 12px", boxShadow:"var(--shadow-sm)" }}>
        <button className="icon-btn attach-btn" onClick={()=>fileRef.current?.click()} title="Attach">
          <Paperclip size={18} />
          {pending.length>0 && <span className="attach-count-badge">{pending.length}</span>}
        </button>
        <input ref={fileRef} type="file" multiple hidden onChange={e=> handleFiles(e.target.files)} />

        <div style={{ flex:1, position:"relative"}}>
          <textarea
            ref={inputRef}
            id="promptInput"
            value={text}
            onChange={e=>setText(e.target.value)}
            onKeyDown={onKeyDown}
            placeholder={activeMode ? `Message in ${activeMode.name} mode…  ( / for commands, ⇧+Enter newline)` : "Message MCP…"}
            rows={1}
            style={{ width:"100%", border:"none", outline:"none", resize:"none", fontSize:14, lineHeight:"1.6", maxHeight:120, minHeight:24, fontFamily:"inherit", background:"transparent"}}
            onInput={e=>{ const t=e.target as HTMLTextAreaElement; t.style.height="auto"; t.style.height=Math.min(t.scrollHeight,120)+"px"; }}
          />
          <div style={{ display:"flex", gap:6, alignItems:"center", marginTop:6 }}>
            <div className="relative">
              <button className="mode-pill active" style={{ background: activeMode?.color, borderColor: activeMode?.color, padding:"5px 10px", fontSize:11 }} onClick={()=>setModeOpen(o=>!o)}>
                <Bot size={12} /> {activeMode?.name} <ChevronDown size={12} />
              </button>
              {modeOpen && (
                <div style={{ position:"absolute", bottom:"110%", left:0, background:"#fff", border:"1px solid var(--line)", borderRadius:12, boxShadow:"var(--shadow-lg)", padding:6, minWidth:200, zIndex:20 }}>
                  {modes.map(m=>(
                    <button key={m.id} onClick={()=>{ setMode(m.id); setModeOpen(false);}} style={{ display:"flex", alignItems:"center", gap:8, width:"100%", padding:"8px 10px", borderRadius:8, border:m.id===selectedMode?"1px solid #bfdbfe":"1px solid transparent", background: m.id===selectedMode?"#eff6ff":"#fff", textAlign:"left"}}>
                      <span style={{width:28,height:28, borderRadius:8, background:m.color, display:"grid", placeItems:"center", color:"#fff", fontSize:11, fontWeight:800}}>{m.name[0]}</span>
                      <span style={{flex:1}}><b style={{fontSize:12}}>{m.name}</b><br/><small style={{color:"var(--muted)", fontSize:11}}>{m.desc.slice(0,36)}</small></span>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <span style={{ fontSize:11, color:"var(--faint)", marginLeft:"auto"}}>{text.length}/4000</span>
          </div>
        </div>

        <button className="icon-btn" title="Voice" onClick={()=>{/* voice placeholder */}} style={{ background:"#f8fafc", border:"1px solid var(--line)"}}><Mic size={18} /></button>
        <button id="sendBtn" className="primary-btn" onClick={handleSend} disabled={!text.trim() && pending.length===0} style={{ borderRadius:12, padding:"10px 14px"}}><Send size={16} /> Send</button>
      </div>
      <div style={{ display:"flex", gap:8, marginTop:8, justifyContent:"center"}}>
        <button className="ghost-btn sm" onClick={()=>setTodoDraft(d=> d.length? []: [{text:"Review PG query",done:false}])} style={{ fontSize:11, padding:"6px 10px"}}><ListChecks size={12} /> {todoDraft.length? "Hide todo":"Todo"}</button>
        <span style={{ fontSize:11, color:"var(--faint)", alignSelf:"center"}}>Press <kbd style={{fontFamily:"var(--mono)", fontSize:10, background:"#fff", border:"1px solid var(--line)", padding:"1px 4px", borderRadius:4}}>Enter</kbd> to send • <kbd style={{fontFamily:"var(--mono)", fontSize:10, background:"#fff", border:"1px solid var(--line)", padding:"1px 4px", borderRadius:4}}>⇧+Enter</kbd> newline</span>
      </div>
    </div>
  );
}
