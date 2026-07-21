import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { AppButton } from '@/components/ui/app-button';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { TextInputField } from '@/components/ui/text-input-field';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { searchProductImages, type ImageSearchResult } from '@/services/image-search';

interface ImageSearchModalProps {
  visible: boolean;
  initialQuery: string;
  onSelect: (fullUrl: string, thumbnailUrl: string) => void;
  onClose: () => void;
}

/**
 * Full-screen picker over Google Custom Search's image results (this
 * project's engine: https://cse.google.com/cse?cx=9116fe971734a400e),
 * biased towards white/plain backgrounds — see `services/image-search.ts`.
 * The query is prefilled from the product/variant name but never searched
 * automatically — Custom Search has a limited daily free quota, so a search
 * only fires when the user explicitly taps "Buscar" (or submits the
 * keyboard). Picking a tile hands both its full-size and thumbnail URL back
 * to the caller (`useImagePicker`'s `pickFromUrl`).
 */
export function ImageSearchModal({ visible, initialQuery, onSelect, onClose }: ImageSearchModalProps) {
  const colors = useThemeColors();
  const [query, setQuery] = useState(initialQuery);
  const [results, setResults] = useState<ImageSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searched, setSearched] = useState(false);

  useEffect(() => {
    if (!visible) return;
    setQuery(initialQuery);
    setResults([]);
    setError(null);
    setSearched(false);
  }, [visible, initialQuery]);

  async function runSearch(q: string) {
    if (!q.trim()) return;
    setLoading(true);
    setError(null);
    setSearched(true);
    try {
      setResults(await searchProductImages(q));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo buscar imágenes.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onClose}>
      <View style={[styles.container, { backgroundColor: colors.background }]}>
        <View style={styles.header}>
          <Pressable onPress={onClose} hitSlop={10}>
            <IconSymbol name="xmark" size={22} color={colors.onBrand} />
          </Pressable>
          <Text style={[styles.headerTitle, { color: colors.onBrand }]}>Buscar imagen</Text>
          <View style={styles.headerSpacer} />
        </View>

        <View style={styles.searchRow}>
          <View style={styles.searchInput}>
            <TextInputField
              placeholder="Ej. Camiseta básica blanca"
              value={query}
              onChangeText={setQuery}
              returnKeyType="search"
              onSubmitEditing={() => runSearch(query)}
            />
          </View>
          <View style={styles.searchBtn}>
            <AppButton label="Buscar" onPress={() => runSearch(query)} loading={loading} disabled={!query.trim()} />
          </View>
        </View>

        {error ? (
          <Text style={styles.error}>{error}</Text>
        ) : loading ? (
          <ActivityIndicator color={Brand.orange} style={styles.loader} />
        ) : !searched ? (
          <Text style={[styles.empty, { color: colors.textSecondary }]}>
            Escribe un término y presiona “Buscar” para ver resultados.
          </Text>
        ) : results.length === 0 ? (
          <Text style={[styles.empty, { color: colors.textSecondary }]}>
            Sin resultados. Prueba con otro término de búsqueda.
          </Text>
        ) : (
          <FlatList
            data={results}
            keyExtractor={(item, index) => `${item.fullUrl}-${index}`}
            numColumns={2}
            columnWrapperStyle={styles.row}
            contentContainerStyle={styles.grid}
            renderItem={({ item }) => (
              <Pressable
                style={({ pressed }) => [styles.tile, pressed && styles.tilePressed]}
                onPress={() => onSelect(item.fullUrl, item.thumbnailUrl)}
              >
                <Image source={{ uri: item.thumbnailUrl }} style={styles.tileImage} resizeMode="contain" />
              </Pressable>
            )}
          />
        )}
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: 56,
    paddingHorizontal: 16,
    paddingBottom: 16,
    backgroundColor: Brand.brown,
    gap: 12,
  },
  headerTitle: { flex: 1, fontSize: 18, fontWeight: '700', textAlign: 'center' },
  headerSpacer: { width: 22 },
  searchRow: { flexDirection: 'row', alignItems: 'center', gap: 10, padding: 16, paddingBottom: 4 },
  searchInput: { flex: 1 },
  searchBtn: { width: 100 },
  loader: { marginTop: 40 },
  error: { color: Brand.danger, fontSize: 14, textAlign: 'center', marginTop: 24, paddingHorizontal: 24 },
  empty: { fontSize: 14, textAlign: 'center', marginTop: 40, paddingHorizontal: 24 },
  grid: { padding: 16, gap: 12 },
  row: { gap: 12 },
  tile: {
    flex: 1,
    aspectRatio: 1,
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E8DDD0',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 10,
    overflow: 'hidden',
  },
  tilePressed: { opacity: 0.7 },
  tileImage: { width: '100%', height: '100%' },
});
