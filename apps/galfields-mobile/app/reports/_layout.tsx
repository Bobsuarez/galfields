import { Redirect, Stack } from 'expo-router';
import { useAuth } from '@/contexts/auth-context';

export default function ReportsLayout() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Redirect href="/login" />;
  }

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="index" />
      <Stack.Screen name="sales-daily" />
      <Stack.Screen name="cash-summary" />
      <Stack.Screen name="sales-by-payment-method" />
      <Stack.Screen name="inventory" />
      <Stack.Screen name="low-stock" />
      <Stack.Screen name="invoices/index" />
      <Stack.Screen name="invoices/[id]" />
    </Stack>
  );
}
