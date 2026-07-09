/** "cerveza light" / "CERVEZA LIGHT" -> "Cerveza Light". Handles Spanish
 * accented letters (á, é, í, ó, ú, ñ) via the unicode letter class. */
export function toTitleCase(value: string): string {
  return value.replace(/\p{L}+/gu, word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase());
}
