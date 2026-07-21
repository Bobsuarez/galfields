import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from '@/components/reports/report-header';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import {
  imageSearchProvider,
  setImageSearchProvider,
  type ImageSearchProvider,
} from '@/services/image-search-provider';

const OPTIONS: { value: ImageSearchProvider; label: string; subtitle: string }[] = [
  {
    value: 'google',
    label: 'Google Custom Search',
    subtitle: 'Programmable Search Engine — requiere EXPO_PUBLIC_GOOGLE_CSE_API_KEY.',
  },
  {
    value: 'serpapi',
    label: 'SerpApi',
    subtitle: 'Google Images vía serpapi.com — requiere EXPO_PUBLIC_SERPAPI_API_KEY.',
  },
];

/** Lets the app switch which service `services/image-search.ts` calls for
 * "Buscar en internet" without a rebuild — added as a fallback while
 * Google Custom Search JSON API access issues get sorted out on the Google
 * Cloud side, without ripping out that implementation. Persisted in
 * AsyncStorage (see `services/image-search-provider.ts`), same
 * runtime-configurable pattern as Configuración → Servidor. Selecting an
 * option applies immediately — no separate save step, since there's
 * nothing here that can be typed wrong. */
export function ImageSearchProviderScreen() {
  const colors = useThemeColors();
  const [selected, setSelected] = useState<ImageSearchProvider>(imageSearchProvider());

  const handleSelect = (value: ImageSearchProvider) => {
    setSelected(value);
    setImageSearchProvider(value);
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <ReportHeader title="Búsqueda de imágenes" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={[styles.hint, { color: colors.textSecondary }]}>
          Proveedor usado por “Buscar en internet” al elegir la imagen de un producto o variante.
        </Text>

        {OPTIONS.map(option => {
          const active = option.value === selected;
          return (
            <Pressable
              key={option.value}
              onPress={() => handleSelect(option.value)}
              style={[
                styles.option,
                { backgroundColor: colors.card, borderColor: active ? Brand.orange : colors.border },
              ]}
            >
              <View style={styles.optionText}>
                <Text style={[styles.optionLabel, { color: colors.text }]}>{option.label}</Text>
                <Text style={[styles.optionSubtitle, { color: colors.textSecondary }]}>{option.subtitle}</Text>
              </View>
              {active && <IconSymbol name="checkmark.circle.fill" size={22} color={Brand.orange} />}
            </Pressable>
          );
        })}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 16, gap: 12 },
  hint: { fontSize: 12, lineHeight: 18, marginBottom: 4 },
  option: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 14,
    borderWidth: 1.5,
    borderRadius: 12,
  },
  optionText: { flex: 1, gap: 3 },
  optionLabel: { fontSize: 14, fontWeight: '700' },
  optionSubtitle: { fontSize: 11.5, lineHeight: 16 },
});
