import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Product } from '@/types/product';

interface ProductListItemProps {
  product: Product;
  onPress?: (product: Product) => void;
}

function formatPrice(price: number): string {
  return `$${price.toLocaleString('es-CO')}`;
}

export function ProductListItem({ product, onPress }: ProductListItemProps) {
  return (
    <Pressable
      onPress={() => onPress?.(product)}
      style={({ pressed }) => [styles.container, pressed && styles.pressed]}
    >
      <View style={styles.imagePlaceholder}>
        <IconSymbol name="shippingbox.fill" size={26} color={Brand.orange} />
      </View>
      <View style={styles.info}>
        <Text style={styles.name} numberOfLines={1}>
          {product.name}
        </Text>
        <Text style={styles.price}>{formatPrice(product.price)}</Text>
      </View>
      <View style={styles.right}>
        <Text style={styles.stock}>Stock: {product.stock}</Text>
        <IconSymbol name="chevron.right" size={18} color="#CCC" />
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingVertical: 12,
    paddingHorizontal: 16,
    gap: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#EEE8E0',
  },
  pressed: { opacity: 0.7 },
  imagePlaceholder: {
    width: 52,
    height: 52,
    borderRadius: 8,
    backgroundColor: `${Brand.orange}12`,
    alignItems: 'center',
    justifyContent: 'center',
  },
  info: { flex: 1 },
  name: { fontSize: 15, fontWeight: '600', color: '#1A1A1A' },
  price: { fontSize: 14, color: Brand.orange, fontWeight: '600', marginTop: 2 },
  right: { alignItems: 'flex-end', gap: 4 },
  stock: { fontSize: 12, color: '#8A7060' },
});
