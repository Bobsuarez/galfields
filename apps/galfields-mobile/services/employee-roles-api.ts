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
    console.error(`[employee-roles-api] ${method} ${path} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  if (response.status === 204) return undefined as T;
  const json = await response.json();
  console.log(`[employee-roles-api] ${method} ${path} -> ${response.status}`);
  return json;
}

/** The 4 modules this spec gates (see backend/pos's CLAUDE.md, "Employee
 * roles CRUD") — the backend's `permissions` map isn't restricted to these
 * keys (a future module can be added without a backend code change), but
 * this is the fixed set the spec asks the mobile grid to show/edit. */
export const PERMISSION_MODULES = [
  { key: 'pos', label: 'Punto de venta' },
  { key: 'inventario', label: 'Inventario' },
  { key: 'reportes', label: 'Reportes' },
  { key: 'sync', label: 'Sincronización' },
] as const;

interface RemoteEmployeeRole {
  roleId: number;
  roleName: string;
  permissions: Record<string, boolean>;
  canLoginMobile: boolean;
  canLoginDesktop: boolean;
}

export interface EmployeeRole {
  id: number;
  roleName: string;
  permissions: Record<string, boolean>;
  canLoginMobile: boolean;
  canLoginDesktop: boolean;
}

export interface EmployeeRoleFormData {
  roleName: string;
  permissions: Record<string, boolean>;
  canLoginMobile: boolean;
  canLoginDesktop: boolean;
}

const mapEmployeeRole = (r: RemoteEmployeeRole): EmployeeRole => ({
  id: r.roleId,
  roleName: r.roleName,
  permissions: r.permissions,
  canLoginMobile: r.canLoginMobile,
  canLoginDesktop: r.canLoginDesktop,
});

export const employeeRolesApi = {
  list: async (): Promise<EmployeeRole[]> =>
    (await request<RemoteEmployeeRole[]>('/api/employee-roles')).map(mapEmployeeRole),
  create: async (data: EmployeeRoleFormData): Promise<EmployeeRole> =>
    mapEmployeeRole(await request<RemoteEmployeeRole>('/api/employee-roles', { method: 'POST', body: JSON.stringify(data) })),
  update: async (id: number, data: EmployeeRoleFormData): Promise<EmployeeRole> =>
    mapEmployeeRole(await request<RemoteEmployeeRole>(`/api/employee-roles/${id}`, { method: 'PUT', body: JSON.stringify(data) })),
  remove: (id: number): Promise<void> => request<void>(`/api/employee-roles/${id}`, { method: 'DELETE' }),
};
