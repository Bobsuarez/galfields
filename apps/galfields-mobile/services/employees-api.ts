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
    console.error(`[employees-api] ${method} ${path} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  if (response.status === 204) return undefined as T;
  const json = await response.json();
  console.log(`[employees-api] ${method} ${path} -> ${response.status}`);
  return json;
}

interface RemoteEmployee {
  employeeId: number;
  firstName: string;
  lastName: string;
  username: string;
  roleId: number;
  roleName: string;
  terminalIds: number[];
  active: boolean;
}

export interface Employee {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  roleId: number;
  roleName: string;
  terminalIds: number[];
  active: boolean;
}

/** `password` is required on create; on update, blank/undefined leaves the
 * current password_hash untouched (see backend/pos's CLAUDE.md, "Employees
 * CRUD" — the deliberate "admin resets a forgotten password" mechanism). */
export interface EmployeeFormData {
  firstName: string;
  lastName: string;
  username: string;
  password?: string;
  roleId: number;
  terminalIds: number[];
}

const mapEmployee = (r: RemoteEmployee): Employee => ({
  id: r.employeeId,
  firstName: r.firstName,
  lastName: r.lastName,
  username: r.username,
  roleId: r.roleId,
  roleName: r.roleName,
  terminalIds: r.terminalIds,
  active: r.active,
});

export const employeesApi = {
  list: async (): Promise<Employee[]> => (await request<RemoteEmployee[]>('/api/employees')).map(mapEmployee),
  create: async (data: EmployeeFormData): Promise<Employee> =>
    mapEmployee(await request<RemoteEmployee>('/api/employees', { method: 'POST', body: JSON.stringify(data) })),
  update: async (id: number, data: EmployeeFormData): Promise<Employee> =>
    mapEmployee(await request<RemoteEmployee>(`/api/employees/${id}`, { method: 'PUT', body: JSON.stringify(data) })),
  // Soft-deactivate on the backend (flips is_active, doesn't drop the row) -
  // see backend/pos's CLAUDE.md. From this screen's point of view it's
  // still "remove from the active roster", same UX as the other CRUDs.
  remove: (id: number): Promise<void> => request<void>(`/api/employees/${id}`, { method: 'DELETE' }),
};
