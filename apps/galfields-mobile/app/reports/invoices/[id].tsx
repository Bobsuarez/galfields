import { useLocalSearchParams } from 'expo-router';
import { InvoiceDetailScreen } from '@/components/reports/invoice-detail-screen';

export default function InvoiceDetailRoute() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <InvoiceDetailScreen transactionId={Number(id)} />;
}
