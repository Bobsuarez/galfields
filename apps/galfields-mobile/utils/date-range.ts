export type QuickRange = 'today' | 'yesterday' | 'week' | 'month' | 'custom';

/** The four presets `quickRangeDates` can compute — `'custom'` has no fixed
 * formula, its `from`/`to` come from `DateRangePicker` instead. */
type PresetRange = Exclude<QuickRange, 'custom'>;

export const QUICK_RANGE_LABELS: Record<QuickRange, string> = {
  today: 'Hoy',
  yesterday: 'Ayer',
  week: 'Semana',
  month: 'Mes',
  custom: 'Personalizado',
};

export interface SimpleDateRange {
  from: string;
  to: string;
}

/** Local calendar date as `YYYY-MM-DD` — NOT `Date#toISOString()`, which
 * converts to UTC first and can land on the wrong day for timezones behind
 * UTC (e.g. Colombia, UTC-5) once it's past ~7pm local time. */
export function toIsoLocal(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Inverse of `toIsoLocal` — parses as a local date, not UTC midnight
 * (`new Date('YYYY-MM-DD')` parses as UTC and can shift a day in negative
 * offsets), so a date picker's `value` round-trips to the same day shown. */
export function parseIsoLocal(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** `{ from, to }` (inclusive, `YYYY-MM-DD`) for a quick-filter chip —
 * matches backend/pos's ReportController date params. */
export function quickRangeDates(range: PresetRange): SimpleDateRange {
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

/** Starting point offered when the user first switches to "Personalizado" —
 * same week-long window as the "Semana" chip, so the picker doesn't open on
 * a zero-length today-to-today range. */
export function defaultCustomRange(): SimpleDateRange {
  return quickRangeDates('week');
}
