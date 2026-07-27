import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';

export interface ReportsAccessCode {
  code: string;
  generatedAt: string;
}

/** Generates a fresh 6-digit code that the desktop POS (apps/galfield-pos's
 * reports_access.rs) validates before letting a cashier into Reportes —
 * this implicitly invalidates whatever code was generated before it (the
 * backend always checks the most recently generated row, see
 * backend/pos's CLAUDE.md, "Reports access code"). */
export async function generateReportsAccessCode(): Promise<ReportsAccessCode> {
  const response = await fetch(`${apiBaseUrl()}/api/reports-access-code`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[reports-access-api] POST /api/reports-access-code -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  return response.json();
}
