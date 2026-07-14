import { Redirect, Stack } from 'expo-router';
import { useAuth } from '@/contexts/auth-context';

export default function InventoryLayout() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Redirect href="/login" />;
  }

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="index" />
    </Stack>
  );
}
