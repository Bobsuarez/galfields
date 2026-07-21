import { useState } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Product } from '@/types/product';

interface ProductGridCardProps {
  product: Product;
  onPress?: (product: Product) => void;
}

function formatPrice(price: number): string {
  return `$${price.toLocaleString('es-CO')}`;
}

/** Grid tile for the Products catalog — two per row (see `app/products/index.tsx`).
 * The photo tile stays white regardless of theme: product photos in this app
 * are sourced (camera, gallery, or "Buscar en internet") expecting a white
 * background, so a white mount is what makes them read as consistent product
 * shots instead of floating oddly on a dark card in dark mode. */
export function ProductGridCard({ product, onPress }: ProductGridCardProps) {
  const colors = useThemeColors();
  const [imageFailed, setImageFailed] = useState(false);

  return (
    <Pressable
      onPress={() => onPress?.(product)}
      style={({ pressed }) => [styles.card, { backgroundColor: colors.card }, pressed && styles.pressed]}
    >
      <View style={styles.imageBox}>
        {product.imageUri && !imageFailed ? (
          <Image
            source={{ uri: product.imageUri }}
            style={styles.image}
            resizeMode="contain"
            onError={() => setImageFailed(true)}
          />
        ) : (
          <IconSymbol name="shippingbox.fill" size={32} color={Brand.orange} />
        )}
        {product.variantCount > 1 && (
          <View style={styles.variantBadge}>
            <Text style={styles.variantBadgeText}>{product.variantCount}</Text>
          </View>
        )}
      </View>

      <View style={styles.info}>
        <Text style={[styles.name, { color: colors.text }]} numberOfLines={2}>
          {product.name}
        </Text>
        <Text style={styles.price}>{formatPrice(product.price)}</Text>
        <Text style={[styles.stock, { color: colors.textSecondary }]}>Stock: {product.stock}</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    flex: 1,
    borderRadius: 16,
    padding: 10,
    gap: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 3,
  },
  pressed: {
    opacity: 0.85,
    transform: [{ scale: 0.98 }],
  },
  imageBox: {
    aspectRatio: 1,
    borderRadius: 12,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  image: { width: '82%', height: '82%' },
  variantBadge: {
    position: 'absolute',
    top: 6,
    right: 6,
    minWidth: 22,
    height: 22,
    borderRadius: 11,
    paddingHorizontal: 5,
    backgroundColor: Brand.orange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  variantBadgeText: { fontSize: 11, fontWeight: '700', color: '#fff' },
  info: { gap: 2 },
  name: { fontSize: 13.5, fontWeight: '600', lineHeight: 17 },
  price: { fontSize: 15, fontWeight: '800', color: Brand.orange },
  stock: { fontSize: 11.5 },
});
