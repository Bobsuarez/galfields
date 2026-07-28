import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';
import { authenticatedFetch } from './authenticated-fetch';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = init?.method ?? 'GET';
  const url = `${apiBaseUrl()}${path}`;

  const response = await authenticatedFetch(url, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[terminals-api] ${method} ${path} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  if (response.status === 204) return undefined as T;
  const json = await response.json();
  console.log(`[terminals-api] ${method} ${path} -> ${response.status}`);
  return json;
}

// One row per desktop POS terminal (see backend/pos's CLAUDE.md, "Terminals
// CRUD") - terminalCode is what the terminal owner types once into
// galfield-pos's Configuración. Used to assign employees to terminals for
// desktop login (spec 01-login-empleados-roles, still pending on this side)
// and referenced by Numeración de facturas (see app/settings/invoicing.tsx).
interface RemoteTerminal {
  terminalId: number;
  terminalCode: string;
  name: string | null;
  active: boolean;
}

export interface Terminal {
  id: number;
  terminalCode: string;
  name: string;
  active: boolean;
}

export interface TerminalFormData {
  terminalCode: string;
  name: string;
  active: boolean;
}

const mapTerminal = (r: RemoteTerminal): Terminal => ({
  id: r.terminalId,
  terminalCode: r.terminalCode,
  name: r.name ?? '',
  active: r.active,
});

export const terminalsApi = {
  list: async (): Promise<Terminal[]> => (await request<RemoteTerminal[]>('/api/terminals')).map(mapTerminal),
  create: async (data: TerminalFormData): Promise<Terminal> =>
    mapTerminal(await request<RemoteTerminal>('/api/terminals', { method: 'POST', body: JSON.stringify(data) })),
  update: async (id: number, data: TerminalFormData): Promise<Terminal> =>
    mapTerminal(await request<RemoteTerminal>(`/api/terminals/${id}`, { method: 'PUT', body: JSON.stringify(data) })),
  remove: (id: number): Promise<void> => request<void>(`/api/terminals/${id}`, { method: 'DELETE' }),
};
