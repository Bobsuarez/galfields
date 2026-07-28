import { useEffect, useState } from 'react';
import { CatalogScreen } from '@/components/settings/catalog-screen';
import { invoicingRangesApi, type CatalogInvoicingRange, type InvoicingRangeFormData } from '@/services/catalog-api';
import { terminalsApi, type Terminal } from '@/services/terminals-api';

export default function InvoicingScreen() {
  // A range is assigned to a real terminals row (terminalId), not a loose
  // string (see services/catalog-api.ts) - the admin still just types the
  // terminal_code they already know (same field as before this changed),
  // and this list is what resolves that typed code back to the id the
  // backend actually needs.
  const [terminals, setTerminals] = useState<Terminal[]>([]);

  useEffect(() => {
    terminalsApi.list().then(setTerminals).catch(() => {});
  }, []);

  return (
    <CatalogScreen<CatalogInvoicingRange, InvoicingRangeFormData>
      title="Numeración de facturas"
      entityLabel="Rango"
      emptyIcon="doc.text.fill"
      emptyLabel="No hay rangos de facturación asignados"
      api={invoicingRangesApi}
      fields={[
        { key: 'terminalCode', label: 'Código de terminal', placeholder: 'Ej. CAJA-01', required: true },
        { key: 'prefix', label: 'Prefijo', placeholder: 'Ej. FAC-', required: true },
        { key: 'rangeStart', label: 'Inicio de rango', placeholder: 'Ej. 1', required: true, keyboardType: 'numeric' },
        { key: 'rangeEnd', label: 'Fin de rango', placeholder: 'Ej. 5000000', required: true, keyboardType: 'numeric' },
      ]}
      getSubtitle={item => `${item.prefix} · ${item.rangeStart}–${item.rangeEnd}`}
      toFormValues={item => ({
        terminalCode: item.name,
        prefix: item.prefix,
        rangeStart: String(item.rangeStart),
        rangeEnd: String(item.rangeEnd),
      })}
      fromFormValues={values => {
        const code = values.terminalCode.trim();
        const terminal = terminals.find(t => t.terminalCode.toLowerCase() === code.toLowerCase());
        if (!terminal) {
          throw new Error(`No existe una terminal con código "${code}". Créala primero en Configuración → Terminales.`);
        }
        return {
          terminalId: terminal.id,
          prefix: values.prefix.trim(),
          rangeStart: Number(values.rangeStart),
          rangeEnd: Number(values.rangeEnd),
        };
      }}
    />
  );
}
