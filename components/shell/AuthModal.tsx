"use client";
import { useState, useEffect } from "react";
import { useStore } from "@/lib/store";
import { Network, LogIn, UserPlus, User, Zap } from "lucide-react";

export function AuthModal(){
  const { currentUser, setCurrentUser, onboarded } = useStore();
  const [tab, setTab]=useState<"login"|"signup"|"guest">("login");
  const [loginEmail,setLoginEmail]=useState("alex@local.dev");
  const [loginPass,setLoginPass]=useState("");
  const [signupName,setSignupName]=useState("");
  const [signupEmail,setSignupEmail]=useState("");
  const [signupPass,setSignupPass]=useState("");
  const [signupPass2,setSignupPass2]=useState("");
  const [open, setOpen]=useState(false);

  useEffect(()=>{
    // show if not onboarded not yet, then after onboarded show auth if no user
    const hasOnboarded = onboarded || (typeof window!=="undefined" && localStorage.getItem("mcp_onboarded")==="1");
    const hasUser = currentUser || (typeof window!=="undefined" && localStorage.getItem("mcp_currentUser"));
    if(!hasOnboarded) { setOpen(false); return; }
    if(!hasUser) setOpen(true); else setOpen(false);
  },[onboarded, currentUser]);

  // also sync currentUser from legacy localStorage on mount
  useEffect(()=>{
    if(!currentUser && typeof window!=="undefined"){
      try{
        const raw=localStorage.getItem("mcp_currentUser");
        if(raw){ const u=JSON.parse(raw); if(u?.email) setCurrentUser(u); }
        else {
          const id=localStorage.getItem("mcp_currentUserId");
          const usersRaw=localStorage.getItem("mcp_users");
          if(id && usersRaw){
            const users=JSON.parse(usersRaw);
            const u=users.find((x:any)=>x.id===id);
            if(u) setCurrentUser({id:u.id, name:u.name, email:u.email, avatar:u.avatar||`https://i.pravatar.cc/100?img=32`, role:u.role||"Admin", isGuest:!!u.isGuest});
          }
        }
      }catch{}
    }
  },[]);

  if(!open) return null;

  const handleLogin=(e:React.FormEvent)=>{
    e.preventDefault();
    if(!loginEmail.includes("@") || loginPass.length<6) return alert("Demo: any email + password length ≥6");
    const user={ id:"u_"+Date.now().toString(36), name: loginEmail.split("@")[0]||"Alex Rivera", email:loginEmail, avatar:`https://i.pravatar.cc/100?img=32`, role:"Admin", isGuest:false };
    setCurrentUser(user as any);
    setOpen(false);
  };
  const handleSignup=(e:React.FormEvent)=>{
    e.preventDefault();
    if(!signupName.trim() || !signupEmail.includes("@") || signupPass.length<6 || signupPass!==signupPass2) return alert("Check fields: name, email, pass ≥6 and confirm");
    const user={ id:"u_"+Date.now().toString(36), name:signupName, email:signupEmail, avatar:`https://i.pravatar.cc/100?img=15`, role:"Developer", isGuest:false };
    // also persist to mcp_users for legacy
    try{
      const raw=localStorage.getItem("mcp_users");
      const arr= raw? JSON.parse(raw):[];
      arr.push({...user, pass:signupPass});
      localStorage.setItem("mcp_users", JSON.stringify(arr));
    }catch{}
    setCurrentUser(user as any);
    setOpen(false);
  };
  const handleGuest=()=>{
    const user={ id:"guest_"+Date.now().toString(36), name:"Guest", email:`guest-${Date.now().toString(36)}@local`, avatar:`https://i.pravatar.cc/100?img=8`, role:"Viewer", isGuest:true };
    setCurrentUser(user as any);
    setOpen(false);
  };
  const handleDemo=()=>{
    const user={ id:"u_alex", name:"Alex Rivera", email:"alex@local.dev", avatar:`https://i.pravatar.cc/100?img=32`, role:"Admin", isGuest:false };
    setCurrentUser(user as any);
    setOpen(false);
  };

  return (
    <div style={{position:"fixed", inset:0, zIndex:75, display:"grid", placeItems:"center"}}>
      <div style={{position:"absolute", inset:0, background:"rgba(15,23,42,.45)", backdropFilter:"blur(6px)"}} />
      <div style={{position:"relative", width:"min(480px,92vw)", background:"#fff", borderRadius:16, overflow:"hidden", boxShadow:"0 20px 60px rgba(15,23,42,.18)", border:"1px solid var(--line)"}}>
        <div style={{padding:"20px 20px 14px", textAlign:"center", borderBottom:"1px solid var(--line-2)"}}>
          <div style={{width:48,height:48, borderRadius:14, background:"linear-gradient(135deg,#2563eb 0%, #7c3aed 50%, #06b6d4 100%)", color:"#fff", display:"grid", placeItems:"center", margin:"0 auto 10px"}}><Network size={20} /></div>
          <h2 style={{margin:"0 0 4px", fontSize:18, fontWeight:800}}>Welcome to MCP Terminal</h2>
          <p style={{margin:0, fontSize:12, color:"var(--muted)"}}>Sign in to sync workspaces • or continue as guest</p>
        </div>
        <div style={{display:"flex", gap:6, padding:"10px 16px 0"}}>
          {(["login","signup","guest"] as const).map(t=>(
            <button key={t} onClick={()=>setTab(t)} style={{flex:1, padding:"8px 10px", borderRadius:999, border:"1px solid var(--line)", background: tab===t? "var(--text)":"#fff", color: tab===t?"#fff":"var(--text)", fontWeight:700, fontSize:12, textTransform:"capitalize"}}>{t}</button>
          ))}
        </div>

        {tab==="login" && (
          <form onSubmit={handleLogin} style={{padding:16, display:"flex", flexDirection:"column", gap:12}}>
            <label style={{fontSize:12, fontWeight:700}}>Email<input value={loginEmail} onChange={e=>setLoginEmail(e.target.value)} type="email" placeholder="alex@local.dev" required style={{display:"block", width:"100%", marginTop:6, border:"1px solid var(--line)", borderRadius:10, padding:"10px 12px", fontSize:13}} /></label>
            <label style={{fontSize:12, fontWeight:700}}>Password<input value={loginPass} onChange={e=>setLoginPass(e.target.value)} type="password" placeholder="••••••••" required style={{display:"block", width:"100%", marginTop:6, border:"1px solid var(--line)", borderRadius:10, padding:"10px 12px", fontSize:13}} /></label>
            <label style={{fontSize:12, display:"flex", gap:8, alignItems:"center"}}><input type="checkbox" defaultChecked /> Remember me</label>
            <button type="submit" className="primary-btn" style={{justifyContent:"center"}}><LogIn size={14} /> Login</button>
            <p style={{fontSize:11, color:"var(--faint)", textAlign:"center"}}>Demo: any email + password length ≥6 works (mock)</p>
          </form>
        )}
        {tab==="signup" && (
          <form onSubmit={handleSignup} style={{padding:16, display:"flex", flexDirection:"column", gap:10}}>
            <label style={{fontSize:12, fontWeight:700}}>Name<input value={signupName} onChange={e=>setSignupName(e.target.value)} placeholder="Alex Rivera" required style={{display:"block", width:"100%", marginTop:6, border:"1px solid var(--line)", borderRadius:10, padding:"10px 12px", fontSize:13}} /></label>
            <label style={{fontSize:12, fontWeight:700}}>Email<input value={signupEmail} onChange={e=>setSignupEmail(e.target.value)} type="email" placeholder="alex@local.dev" required style={{display:"block", width:"100%", marginTop:6, border:"1px solid var(--line)", borderRadius:10, padding:"10px 12px", fontSize:13}} /></label>
            <label style={{fontSize:12, fontWeight:700}}>Password<input value={signupPass} onChange={e=>setSignupPass(e.target.value)} type="password" placeholder="••••••••" required minLength={6} style={{display:"block", width:"100%", marginTop:6, border:"1px solid var(--line)", borderRadius:10, padding:"10px 12px", fontSize:13}} /></label>
            <label style={{fontSize:12, fontWeight:700}}>Confirm<input value={signupPass2} onChange={e=>setSignupPass2(e.target.value)} type="password" placeholder="••••••••" required style={{display:"block", width:"100%", marginTop:6, border:"1px solid var(--line)", borderRadius:10, padding:"10px 12px", fontSize:13}} /></label>
            <button type="submit" className="primary-btn" style={{justifyContent:"center"}}><UserPlus size={14} /> Create account</button>
          </form>
        )}
        {tab==="guest" && (
          <div style={{padding:16, textAlign:"center"}}>
            <div style={{background:"#f8fafc", border:"1px solid var(--line)", borderRadius:12, padding:14, textAlign:"left"}}>
              <b style={{fontSize:13, display:"flex", gap:6, alignItems:"center"}}><User size={14} /> Guest mode</b>
              <p style={{fontSize:12, color:"var(--muted)", lineHeight:1.5, margin:"6px 0 0"}}>No account needed. Chats stored locally. You can create an account later and keep your data.</p>
              <ul style={{fontSize:11, color:"var(--muted)", margin:"8px 0 0", paddingLeft:16, lineHeight:1.6}}><li>✓ Full MCP access</li><li>✓ Local workspaces</li><li>• No cloud sync</li></ul>
            </div>
            <button className="primary-btn" style={{width:"100%", marginTop:12, justifyContent:"center"}} onClick={handleGuest}><User size={14} /> Continue as Guest</button>
          </div>
        )}

        <div style={{display:"flex", gap:8, justifyContent:"center", padding:"0 16px 14px"}}>
          <button className="ghost-btn sm" onClick={handleDemo}><Zap size={12} /> Demo login</button>
        </div>
      </div>
    </div>
  );
}
