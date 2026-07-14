import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';

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
  const response = await fetch(url);

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
