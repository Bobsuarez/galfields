import { useState } from 'react';
import { defaultCustomRange, quickRangeDates, QuickRange, SimpleDateRange } from '@/utils/date-range';

/**
 * Shared date-range state for report screens that offer a "Personalizado"
 * range alongside the quick-filter chips: `dates` is always the resolved
 * `{ from, to }` to send to the reports API, whether the active mode is a
 * preset chip or an explicit range picked with `DateRangePicker`.
 */
export function useReportDateRange(initial: QuickRange = 'today') {
  const [range, setRange] = useState<QuickRange>(initial);
  const [customRange, setCustomRange] = useState<SimpleDateRange>(defaultCustomRange);

  const dates: SimpleDateRange = range === 'custom' ? customRange : quickRangeDates(range);

  return { range, setRange, customRange, setCustomRange, dates };
}
