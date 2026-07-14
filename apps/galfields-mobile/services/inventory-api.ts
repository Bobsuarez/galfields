import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';
import { generateUuid } from '@/utils/uuid';

interface StockAdjustmentResult {
  variantId: number;
  alreadyProcessed: boolean;
  resultingQuantity: number | null;
}

interface StockAdjustmentBatchResponse {
  clientEventId: string;
  results: StockAdjustmentResult[];
}

/**
 * POST /api/inventory/adjustments only takes a relative `quantityDelta`
 * (see backend/pos's CLAUDE.md) — there's no "set stock to X" endpoint, so
 * the Inventario module (see `app/inventory/`) computes the delta itself
 * from the current stock and the count the user just entered, then reports
 * it here as a one-off manual correction (one item per call). A fresh
 * `clientEventId` per call is required by the endpoint's idempotency
 * design — a real client-generated id, not reused across calls, since each
 * manual edit is its own distinct event, not a retry of a previous one.
 */
export async function adjustVariantStock(variantId: number, quantityDelta: number): Promise<StockAdjustmentResult> {
  const path = '/api/inventory/adjustments';
  const body = {
    clientEventId: generateUuid(),
    items: [{ variantId, quantityDelta }],
  };

  console.log(`[inventory-api] POST ${path}`, body);

  const response = await fetch(`${apiBaseUrl()}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[inventory-api] POST ${path} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  const result: StockAdjustmentBatchResponse = await response.json();
  console.log(`[inventory-api] POST ${path} -> ${response.status}`, result);
  return result.results[0];
}
