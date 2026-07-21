import { useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ProductGridCard } from '@/components/products/product-grid-card';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { useProducts } from '@/contexts/products-context';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import type { Product } from '@/types/product';

export default function ProductsScreen() {
  const { products, loading, loadingMore, error, refresh, loadMore, searchProducts } = useProducts();
  const [query, setQuery] = useState('');
  const insets = useSafeAreaInsets();
  const colors = useThemeColors();

  const isSearching = query.trim().length > 0;
  const displayed = isSearching ? searchProducts(query) : products;

  const handleProductPress = (product: Product) => {
    router.push({ pathname: '/products/[id]/edit', params: { id: product.id } });
  };

  return (
    <View style={[styles.container, { paddingTop: 0, backgroundColor: colors.background }]}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Pressable onPress={() => router.back()} hitSlop={10}>
          <IconSymbol name="arrow.left" size={24} color="#fff" />
        </Pressable>
        <Text style={styles.headerTitle}>Productos</Text>
        <View style={styles.headerSpacer} />
      </View>

      {error ? (
        <View style={styles.errorBanner}>
          <Text style={styles.errorBannerText}>{error}</Text>
        </View>
      ) : null}

      {/* Search + Add */}
      <View style={[styles.searchRow, { backgroundColor: colors.card, borderBottomColor: colors.border }]}>
        <View style={[styles.searchBar, { backgroundColor: colors.background, borderColor: colors.border }]}>
          <IconSymbol name="magnifyingglass" size={20} color={colors.textSecondary} />
          <TextInput
            style={[styles.searchInput, { color: colors.text }]}
            placeholder="Buscar producto..."
            placeholderTextColor={colors.placeholder}
            value={query}
            onChangeText={setQuery}
            returnKeyType="search"
            clearButtonMode="while-editing"
          />
        </View>
        <Pressable
          onPress={() => router.push('/products/add')}
          style={({ pressed }) => [styles.addBtn, pressed && styles.addBtnPressed]}
        >
          <IconSymbol name="plus" size={22} color="#fff" />
        </Pressable>
      </View>

      <FlatList
        data={displayed}
        keyExtractor={item => item.id}
        numColumns={2}
        columnWrapperStyle={styles.gridRow}
        renderItem={({ item }) => (
          <ProductGridCard product={item} onPress={handleProductPress} />
        )}
        contentContainerStyle={[styles.gridContent, displayed.length === 0 && styles.emptyContent]}
        refreshing={loading}
        onRefresh={refresh}
        onEndReached={() => {
          if (!isSearching) loadMore();
        }}
        onEndReachedThreshold={0.4}
        ListFooterComponent={
          loadingMore ? (
            <ActivityIndicator style={styles.footerLoader} color={Brand.orange} />
          ) : null
        }
        ListEmptyComponent={
          loading ? null : (
            <View style={styles.empty}>
              <IconSymbol name="shippingbox.fill" size={48} color={colors.border} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>No se encontraron productos</Text>
            </View>
          )
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingBottom: 16,
    backgroundColor: Brand.brown,
    gap: 12,
  },
  headerTitle: {
    flex: 1,
    fontSize: 20,
    fontWeight: '700',
    color: '#fff',
    textAlign: 'center',
  },
  headerSpacer: { width: 24 },
  errorBanner: {
    backgroundColor: `${Brand.danger}14`,
    paddingVertical: 10,
    paddingHorizontal: 16,
  },
  errorBannerText: { color: Brand.danger, fontSize: 13 },
  footerLoader: { paddingVertical: 20 },
  searchRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  searchBar: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 10,
    paddingHorizontal: 12,
    gap: 8,
    height: 44,
    borderWidth: 1,
  },
  searchInput: { flex: 1, fontSize: 15, paddingVertical: 0 },
  addBtn: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: Brand.orange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addBtnPressed: { opacity: 0.8 },
  gridContent: { padding: 16, gap: 12 },
  gridRow: { gap: 12 },
  emptyContent: { flex: 1 },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 80,
    gap: 12,
  },
  emptyText: { fontSize: 15 },
});
