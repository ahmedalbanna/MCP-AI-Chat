import { create } from 'zustand';
import { MCPState, MCPTool, MCPServerConfig } from '../types';
import { PRESET_SERVERS } from '../constants/preset-servers';

export const useMcpStore = create<MCPState>((set) => ({
  servers: [...PRESET_SERVERS],

  toggleServer: (id: string) =>
    set((s) => ({
      servers: s.servers.map((srv) =>
        srv.id === id ? { ...srv, enabled: !srv.enabled } : srv
      ),
    })),

  updateServer: (id: string, updates: Partial<MCPServerConfig>) =>
    set((s) => ({
      servers: s.servers.map((srv) =>
        srv.id === id ? { ...srv, ...updates } : srv
      ),
    })),

  setServerTools: (id: string, tools: MCPTool[]) =>
    set((s) => ({
      servers: s.servers.map((srv) =>
        srv.id === id ? { ...srv, tools } : srv
      ),
    })),

  setServerConnected: (id: string, connected: boolean) =>
    set((s) => ({
      servers: s.servers.map((srv) =>
        srv.id === id ? { ...srv, connected } : srv
      ),
    })),
}));
