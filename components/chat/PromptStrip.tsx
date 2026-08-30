"use client";
import { useStore } from "@/lib/store";
import { PROMPTS } from "@/lib/mockData";
import { Database, Cloud, FolderOpen, Activity, BarChart3, TrendingUp, Wallet, Code2, Bug, Wrench, PenTool, BookOpen, Lightbulb, GitBranch, Sparkles } from "lucide-react";
import { useMemo } from "react";

const iconMap: Record<string, any> = {
  database: Database, cloud: Cloud, "folder-open": FolderOpen, activity: Activity, "bar-chart-3": BarChart3, "trending-up": TrendingUp, wallet: Wallet, "code-2": Code2, bug: Bug, wrench: Wrench, "pen-tool": PenTool, "book-open": BookOpen, lightbulb: Lightbulb, "git-branch": GitBranch,
};

export function PromptStrip() {
  const { selectedMode, sendMessage, promptRecent, recordPromptUse } = useStore();
  const chips = useMemo(()=>{
    const recent = promptRecent.map(r=> PROMPTS.find(p=>p.prompt===r)).filter(Boolean) as typeof PROMPTS;
    const base = PROMPTS.filter(p=> p.modes.includes(selectedMode)).slice(0,6);
    const merged = [...recent.slice(0,2), ...base].filter((v,i,a)=> a.findIndex(x=>x.prompt===v.prompt)===i).slice(0,6);
    return merged;
  },[selectedMode, promptRecent]);

  return (
    <div className="prompt-strip" id="promptStrip">
      {chips.map(p=>{
        const Icon = iconMap[p.icon] || Sparkles;
        const isRecent = promptRecent.includes(p.prompt);
        return (
          <button key={p.title} className={`prompt-chip ${isRecent?"recent":""}`} onClick={()=>{ recordPromptUse(p.prompt); sendMessage(p.prompt); }}>
            <span className="prompt-chip-dot" style={{ background: isRecent? "var(--primary)":"var(--line)"}} />
            <Icon size={13} />
            <span className="prompt-chip-label">{p.title}</span>
            <small>{p.cat}</small>
          </button>
        );
      })}
      <button className="prompt-chip prompt-chip-more" onClick={()=>useStore.getState().setDrawer("prompts")}><LibraryIcon /> More</button>
    </div>
  );
}

function LibraryIcon(){ return <span style={{width:13,height:13,display:"grid",placeItems:"center"}}>◈</span>; }
