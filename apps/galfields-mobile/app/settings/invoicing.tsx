import { CatalogScreen } from '@/components/settings/catalog-screen';
import { invoicingRangesApi, type CatalogInvoicingRange, type InvoicingRangeFormData } from '@/services/catalog-api';

export default function InvoicingScreen() {
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
      fromFormValues={values => ({
        terminalCode: values.terminalCode.trim(),
        prefix: values.prefix.trim(),
        rangeStart: Number(values.rangeStart),
        rangeEnd: Number(values.rangeEnd),
      })}
    />
  );
}
