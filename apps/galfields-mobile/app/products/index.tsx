import { useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ProductListItem } from '@/components/products/product-list-item';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { useProducts } from '@/contexts/products-context';
import { Brand } from '@/constants/theme';
import type { Product } from '@/types/product';

export default function ProductsScreen() {
  const { products, searchProducts } = useProducts();
  const [query, setQuery] = useState('');
  const insets = useSafeAreaInsets();

  const displayed = query.trim() ? searchProducts(query) : products;

  const handleProductPress = (_product: Product) => {
    // TODO: Navigate to product detail screen
  };

  return (
    <View style={[styles.container, { paddingTop: 0 }]}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Pressable onPress={() => router.back()} hitSlop={10}>
          <IconSymbol name="arrow.left" size={24} color="#fff" />
        </Pressable>
        <Text style={styles.headerTitle}>Productos</Text>
        <View style={styles.headerSpacer} />
      </View>

      {/* Search + Add */}
      <View style={styles.searchRow}>
        <View style={styles.searchBar}>
          <IconSymbol name="magnifyingglass" size={20} color="#8A7060" />
          <TextInput
            style={styles.searchInput}
            placeholder="Buscar producto..."
            placeholderTextColor="#B0A090"
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
        renderItem={({ item }) => (
          <ProductListItem product={item} onPress={handleProductPress} />
        )}
        contentContainerStyle={displayed.length === 0 && styles.emptyContent}
        ListEmptyComponent={
          <View style={styles.empty}>
            <IconSymbol name="shippingbox.fill" size={48} color="#E8DDD0" />
            <Text style={styles.emptyText}>No se encontraron productos</Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Brand.cream },
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
  searchRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 10,
    backgroundColor: '#fff',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E8DDD0',
  },
  searchBar: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Brand.cream,
    borderRadius: 10,
    paddingHorizontal: 12,
    gap: 8,
    height: 44,
    borderWidth: 1,
    borderColor: '#E8DDD0',
  },
  searchInput: { flex: 1, fontSize: 15, color: '#1A1A1A', paddingVertical: 0 },
  addBtn: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: Brand.orange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addBtnPressed: { opacity: 0.8 },
  emptyContent: { flex: 1 },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 80,
    gap: 12,
  },
  emptyText: { fontSize: 15, color: '#8A7060' },
});
