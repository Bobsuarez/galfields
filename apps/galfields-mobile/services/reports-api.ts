import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';
import { authenticatedFetch } from './authenticated-fetch';

export interface SalesSummary {
  totalSales: number;
  transactionCount: number;
  averageTicket: number;
}

export interface PaymentMethodSales {
  paymentMethodId: number;
  methodName: string;
  totalAmount: number;
  transactionCount: number;
}

export interface InvoiceSummary {
  transactionId: number;
  transactionDate: string;
  employeeName: string;
  totalAmount: number;
  discountAmount: number;
  itemCount: number;
  cancelledAt: string | null;
  /** DIAN invoice number snapshotted by the reporting terminal at sale
   * creation (see backend/pos's CLAUDE.md, "invoicePrefix/invoiceNumber").
   * Null for transactions reported before that existed. */
  invoicePrefix: string | null;
  invoiceNumber: string | null;
}

export interface InvoiceLine {
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface InvoicePayment {
  methodName: string;
  amount: number;
  referenceNumber: string | null;
}

export interface InvoiceDetail {
  transactionId: number;
  transactionDate: string;
  employeeName: string;
  totalAmount: number;
  discountAmount: number;
  taxAmount: number;
  items: InvoiceLine[];
  payments: InvoicePayment[];
  cancelledAt: string | null;
  /** See InvoiceSummary.invoiceNumber. */
  invoicePrefix: string | null;
  invoiceNumber: string | null;
}

export interface InventoryRow {
  variantId: number;
  sku: string;
  productName: string;
  categoryName: string | null;
  locationName: string;
  quantityOnHand: number;
}

export interface Page<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
}

/** Spring's PagedModel nests pagination metadata under a "page" object
 * instead of putting it at the top level — see backend/pos's ReportController. */
interface RemotePage<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

function flattenPage<T>(raw: RemotePage<T>): Page<T> {
  return {
    content: raw.content,
    number: raw.page.number,
    totalPages: raw.page.totalPages,
    totalElements: raw.page.totalElements,
  };
}

/** `from`/`to` as `YYYY-MM-DD` — omit both for "today" (see ReportController). */
export interface DateRange {
  from?: string;
  to?: string;
}

function dateParams(range?: DateRange): URLSearchParams {
  const params = new URLSearchParams();
  if (range?.from) params.set('from', range.from);
  if (range?.to) params.set('to', range.to);
  return params;
}

async function getJson<T>(path: string): Promise<T> {
  const url = `${apiBaseUrl()}${path}`;
  const response = await authenticatedFetch(url);

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[reports-api] GET ${path} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  const result = (await response.json()) as T;
  console.log(`[reports-api] GET ${path} -> ${response.status}`);
  return result;
}

export function fetchSalesSummary(range?: DateRange): Promise<SalesSummary> {
  return getJson(`/api/reports/sales-summary?${dateParams(range).toString()}`);
}

export function fetchSalesByPaymentMethod(range?: DateRange): Promise<PaymentMethodSales[]> {
  return getJson(`/api/reports/sales-by-payment-method?${dateParams(range).toString()}`);
}

export async function fetchInvoices(range?: DateRange, page = 0, size = 20): Promise<Page<InvoiceSummary>> {
  const params = dateParams(range);
  params.set('page', String(page));
  params.set('size', String(size));
  const raw = await getJson<RemotePage<InvoiceSummary>>(`/api/reports/invoices?${params.toString()}`);
  return flattenPage(raw);
}

export function fetchInvoiceDetail(transactionId: number): Promise<InvoiceDetail> {
  return getJson(`/api/reports/invoices/${transactionId}`);
}

/** `invoiceNumber` already includes the prefix (e.g. "FAC-000501" — see
 * apps/galfield-pos's invoices.rs::create_sale). Falls back to the raw
 * transactionId for invoices reported before the cloud tracked this. */
export function formatInvoiceNumber(invoice: { transactionId: number; invoiceNumber: string | null }): string {
  return invoice.invoiceNumber ?? `#${invoice.transactionId}`;
}

/** The only non-GET call in this file — `getJson` is GET-only by
 * construction (plain `fetch(url)`, no method/body), so this builds its
 * own POST instead of widening that helper for a single caller. Hits
 * `/api/sales/{id}/cancel` (SalesController), not `/api/reports/*` — this
 * file is otherwise a read-only mirror of the reports API, but invoice
 * cancellation belongs next to `fetchInvoiceDetail`/`fetchInvoices` from
 * the screen's point of view. */
export async function cancelInvoice(transactionId: number): Promise<void> {
  const path = `/api/sales/${transactionId}/cancel`;
  const url = `${apiBaseUrl()}${path}`;
  const response = await authenticatedFetch(url, { method: 'POST' });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[reports-api] POST ${path} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  console.log(`[reports-api] POST ${path} -> ${response.status}`);
}

export async function fetchInventory(page = 0, size = 20): Promise<Page<InventoryRow>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  const raw = await getJson<RemotePage<InventoryRow>>(`/api/reports/inventory?${params.toString()}`);
  return flattenPage(raw);
}

export async function fetchLowStock(threshold?: number, page = 0, size = 20): Promise<Page<InventoryRow>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (threshold != null) params.set('threshold', String(threshold));
  const raw = await getJson<RemotePage<InventoryRow>>(`/api/reports/low-stock?${params.toString()}`);
  return flattenPage(raw);
}
