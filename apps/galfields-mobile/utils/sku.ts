/**
 * The SKU is derived, not typed by the user: first 3 letters of each word in
 * the product name, the first 4 characters of the (first) variant attribute
 * value, and the last 5 digits of the barcode — e.g. "Cerveza Light" +
 * "48gr" + "...12547" -> "CER_LIG_48gr_12547". Builds up progressively as
 * each piece is filled in, so the user sees the SKU take shape while
 * completing the form.
 */
function namePrefix(productName: string): string {
  return productName
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map(word => word.slice(0, 3).toUpperCase())
    .join('_');
}

export function buildVariantSku(productName: string, attributeValue: string, barcode: string): string {
  const parts = [namePrefix(productName)];

  const attr = attributeValue.trim().replace(/\s+/g, '').slice(0, 4);
  if (attr) parts.push(attr);

  const barcodeTail = barcode.trim().slice(-5);
  if (barcodeTail) parts.push(barcodeTail);

  return parts.filter(Boolean).join('_');
}
