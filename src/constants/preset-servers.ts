import { MCPServerConfig } from '../types';

export const PRESET_SERVERS: MCPServerConfig[] = [
  {
    id: 'erpnext-gateway',
    name: 'ERPNext MCP Gateway',
    url: 'http://185.190.140.93:8022/mcp',
    type: 'remote',
    enabled: false,
    headers: {
      Authorization: 'Bearer change-me-admin-token',
    },
  },
];
