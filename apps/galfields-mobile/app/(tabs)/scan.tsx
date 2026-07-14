import { ComingSoonScreen } from '@/components/ui/coming-soon-screen';

// Will become the barcode-scan-to-sale flow: scan a product's barcode to
// start a new sale. Not built yet — see the sale-creation flow follow-up.
export default function ScanScreen() {
  return (
    <ComingSoonScreen
      icon="barcode.viewfinder"
      title="Registrar venta"
      message="Aquí vas a poder escanear el código de barras de un producto para iniciar una venta."
    />
  );
}
