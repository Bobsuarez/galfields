import { useEffect, useState } from 'react';
import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import * as SplashScreen from 'expo-splash-screen';
import 'react-native-reanimated';

import { useColorScheme } from '@/hooks/use-color-scheme';
import { AuthProvider } from '@/contexts/auth-context';
import { ProductsProvider } from '@/contexts/products-context';
import { initApiBaseUrl } from '@/services/api-base-url';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const [configReady, setConfigReady] = useState(false);

  useEffect(() => {
    // ProductsProvider below fetches immediately on mount, and every
    // services/*-api.ts call is a synchronous apiBaseUrl() read - the
    // server URL (possibly a Configuración → Servidor override in
    // AsyncStorage) must be loaded before any of that can render.
    initApiBaseUrl().finally(() => {
      setConfigReady(true);
      SplashScreen.hideAsync();
    });
  }, []);

  if (!configReady) {
    return null;
  }

  return (
    <AuthProvider>
      <ProductsProvider>
        <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
          <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name="login" />
            <Stack.Screen name="(tabs)" />
            <Stack.Screen name="products" />
            <Stack.Screen name="settings" />
            <Stack.Screen name="reports" />
            <Stack.Screen name="inventory" />
          </Stack>
          <StatusBar style="auto" />
        </ThemeProvider>
      </ProductsProvider>
    </AuthProvider>
  );
}
