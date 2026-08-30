"use client";
import type { ChatMessage } from "@/lib/types";
import { Copy, Pencil, Trash2, RefreshCw, Check, Terminal, Database, Cloud, FileText } from "lucide-react";
import { useState } from "react";
import { useStore } from "@/lib/store";

function renderMarkdown(text: string) {
  // minimal md: tables, code blocks, bold/italic handled via prototype CSS classes
  const escaped = text;
  // simple table detection -> wrap
  if (escaped.includes("|") && escaped.includes("---")) {
    const lines = escaped.split("\n");
    const tableLines = lines.filter(l=>l.includes("|"));
    if (tableLines.length >= 2) {
      const header = tableLines[0].split("|").map(s=>s.trim()).filter(Boolean);
      const rows = tableLines.slice(2).map(r=> r.split("|").map(s=>s.trim()).filter(Boolean));
      return (
        <>
          <div className="md-text" style={{ whiteSpace:"pre-wrap"}}>{escaped.split("\n").filter(l=>!l.includes("|") || l.includes("---")).join("\n").slice(0,300)}</div>
          <div className="table-wrap has-scroll">
            <table>
              <thead><tr>{header.map((h,i)=><th key={i}>{h}</th>)}</tr></thead>
              <tbody>{rows.map((row,ri)=><tr key={ri}>{row.map((cell,ci)=><td key={ci}>{cell}</td>)}</tr>)}</tbody>
            </table>
          </div>
        </>
      );
    }
  }
  // code block detection
  if (escaped.includes("```")) {
    const parts = escaped.split("```");
    return (
      <div className="md-text">
        {parts.map((p,i)=> i%2===1 ? (
          <div key={i} className="code-block">
            <div className="code-block-head"><span><Terminal size={12} /> code</span><button className="code-copy" onClick={()=>navigator.clipboard.writeText(p)}><Copy size={12} /> Copy</button></div>
            <pre><code>{p.trim()}</code></pre>
          </div>
        ) : <span key={i} style={{whiteSpace:"pre-wrap"}}>{p}</span>)}
      </div>
    );
  }
  return <div className="md-text" style={{whiteSpace:"pre-wrap"}}>{escaped}</div>;
}

export function MessageItem({ msg, onCopy, onEdit, onDelete, onRetry }: { msg: ChatMessage; onCopy:()=>void; onEdit:()=>void; onDelete:()=>void; onRetry:()=>void }) {
  const isUser = msg.role==="user";
  const [copied, setCopied]=useState(false);
  const handleCopy=()=>{
    navigator.clipboard.writeText(msg.text||"");
    setCopied(true); onCopy(); setTimeout(()=>setCopied(false),1500);
  };

  if (isUser) {
    return (
      <div className="message user">
        <div className="msg-head">
          <div className="msg-actions-group">
            <button className="msg-action" onClick={handleCopy}>{copied? <Check size={12} />: <Copy size={12} />}{copied?"Copied":"Copy"}</button>
            <button className="msg-action" onClick={onEdit}><Pencil size={12} /> Edit</button>
            <button className="msg-action danger" onClick={onDelete}><Trash2 size={12} /> Delete</button>
          </div>
          <span className="when">{msg.time}</span>
          <span className="msg-avatar user">AR</span>
          <span className="who">You</span>
        </div>
        {msg.attachments && msg.attachments.length>0 && (
          <div className="user-media">
            {msg.attachments.map(a=> (
              <div key={a.id} className="file-chip">
                <div className={`chip-icon ${a.type.includes("pdf")?"pdf": a.type.includes("csv")?"csv": a.type.includes("json")?"json": a.type.startsWith("audio")?"audio":"generic"}`}>{a.name.split(".").pop()?.slice(0,3).toUpperCase()}</div>
                <div className="chip-meta"><strong>{a.name}</strong><small>{(a.size/1024).toFixed(1)} KB</small></div>
              </div>
            ))}
          </div>
        )}
        <div className="bubble">{msg.text}</div>
      </div>
    );
  }

  // assistant
  const iconMap: Record<string, any> = { pg: Database, aws: Cloud, fs: FileText, generic: Terminal };
  const Icon = iconMap[msg.html||"generic"] || Terminal;
  const colorClass = msg.html==="pg"?"pg": msg.html==="aws"?"aws": msg.html==="fs"?"fs":"gen";

  return (
    <div className="message assistant">
      <div className="msg-head">
        <span className={`msg-avatar assistant ${msg.streaming?"streaming":""}`}><Terminal size={14} /></span>
        <span className="who">Assistant <small>• {msg.html ? `via ${msg.html}-mcp` : "MCP orchestrator"}</small></span>
        {msg.streaming ? <span className="badge streaming">streaming</span> : <span className="badge">done</span>}
        <span className="when">{msg.time}</span>
      </div>
      <div className="card">
        <div className="card-head">
          <div className="card-head-top">
            <div className={`card-icon ${colorClass}`}><Icon size={16} /></div>
            <h4>{msg.html==="pg"?"PostgreSQL": msg.html==="aws"?"AWS S3": msg.html==="fs"?"Filesystem":"Response"} {msg.streaming && <span className="stream-cursor" />}</h4>
            <span className="tls"><span style={{width:6,height:6,background:"var(--ok)",borderRadius:"50%"}} /> TLS 1.3</span>
          </div>
          <p className="card-head-desc">{msg.streaming? "Generating…":"Completed"} • model {useStore.getState().selectedModel} • temp {useStore.getState().providers.find(p=>p.id===useStore.getState().selectedProvider)?.temp}</p>
        </div>
        <div style={{ padding:14 }}>
          {msg.text ? renderMarkdown(msg.text) : <div className="typing"><span /><span /><span /></div>}
        </div>
        {!msg.streaming && (
          <div className="card-foot">
            <button className="chip" onClick={handleCopy}>{copied? <Check size={12} />:<Copy size={12} />}{copied?"Copied":"Copy"}</button>
            <button className="chip" onClick={onRetry}><RefreshCw size={12} /> Retry</button>
            <button className="chip" onClick={onDelete}><Trash2 size={12} /> Delete</button>
          </div>
        )}
      </div>
      {!msg.streaming && (
        <div className="message-assist-actions">
          <button className="msg-action" onClick={handleCopy}><Copy size={12} /> Copy</button>
          <button className="msg-action" onClick={onRetry}><RefreshCw size={12} /> Retry only response</button>
        </div>
      )}
    </div>
  );
}
