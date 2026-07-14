/** Matches the `$${n.toLocaleString('es-CO')}` formatting already used
 * inline in product screens — centralized here since the report screens
 * use it repeatedly. */
export function formatCurrency(amount: number): string {
  return `$${Math.round(amount).toLocaleString('es-CO')}`;
}
