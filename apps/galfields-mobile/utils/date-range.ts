export type QuickRange = 'today' | 'yesterday' | 'week' | 'month';

export const QUICK_RANGE_LABELS: Record<QuickRange, string> = {
  today: 'Hoy',
  yesterday: 'Ayer',
  week: 'Semana',
  month: 'Mes',
};

/** Local calendar date as `YYYY-MM-DD` — NOT `Date#toISOString()`, which
 * converts to UTC first and can land on the wrong day for timezones behind
 * UTC (e.g. Colombia, UTC-5) once it's past ~7pm local time. */
function toIsoLocal(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** `{ from, to }` (inclusive, `YYYY-MM-DD`) for a quick-filter chip —
 * matches backend/pos's ReportController date params. */
export function quickRangeDates(range: QuickRange): { from: string; to: string } {
  const today = new Date();
  const to = toIsoLocal(today);

  switch (range) {
    case 'today':
      return { from: to, to };
    case 'yesterday': {
      const yesterday = new Date(today);
      yesterday.setDate(yesterday.getDate() - 1);
      const iso = toIsoLocal(yesterday);
      return { from: iso, to: iso };
    }
    case 'week': {
      const start = new Date(today);
      start.setDate(start.getDate() - 6);
      return { from: toIsoLocal(start), to };
    }
    case 'month': {
      const start = new Date(today);
      start.setDate(start.getDate() - 29);
      return { from: toIsoLocal(start), to };
    }
  }
}
