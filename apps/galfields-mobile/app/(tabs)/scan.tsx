import { Redirect } from 'expo-router';

// The barcode scanner lives inside products/add.
// This route exists only to satisfy expo-router's tab file discovery.
export default function ScanScreen() {
  return <Redirect href="/products/add" />;
}
