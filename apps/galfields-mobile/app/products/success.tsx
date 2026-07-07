import { StyleSheet, Text, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { AppButton } from '@/components/ui/app-button';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';

function formatPrice(price: string): string {
  const n = Number(price);
  return isNaN(n) ? price : `$${n.toLocaleString('es-CO')}`;
}

export default function ProductSuccessScreen() {
  const { name, price, stock } = useLocalSearchParams<{
    name: string;
    price: string;
    stock: string;
  }>();
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.container, { paddingTop: insets.top, paddingBottom: insets.bottom + 24 }]}>
      {/* Success icon */}
      <View style={styles.iconWrap}>
        <IconSymbol name="checkmark.circle.fill" size={88} color={Brand.success} />
      </View>

      <Text style={styles.title}>¡Producto agregado{'\n'}exitosamente!</Text>

      {/* Product card preview */}
      <View style={styles.productCard}>
        <View style={styles.productIconBox}>
          <IconSymbol name="shippingbox.fill" size={36} color={Brand.orange} />
        </View>
        <View style={styles.productInfo}>
          <Text style={styles.productName}>{name}</Text>
          <Text style={styles.productPrice}>{formatPrice(price)}</Text>
          <Text style={styles.productStock}>Stock inicial: {stock}</Text>
        </View>
      </View>

      {/* Actions */}
      <View style={styles.actions}>
        <AppButton
          label="Agregar otro producto"
          onPress={() => router.replace('/products/add')}
        />
        <AppButton
          label="Ver productos"
          variant="outline"
          onPress={() => router.replace('/products')}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 32,
    gap: 24,
  },
  iconWrap: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: `${Brand.success}12`,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
    color: '#1A1A1A',
    textAlign: 'center',
    lineHeight: 32,
  },
  productCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Brand.cream,
    borderRadius: 16,
    padding: 16,
    gap: 14,
    width: '100%',
  },
  productIconBox: {
    width: 60,
    height: 60,
    borderRadius: 12,
    backgroundColor: `${Brand.orange}15`,
    alignItems: 'center',
    justifyContent: 'center',
  },
  productInfo: { flex: 1 },
  productName: { fontSize: 16, fontWeight: '700', color: '#1A1A1A' },
  productPrice: { fontSize: 15, color: Brand.orange, fontWeight: '600', marginTop: 2 },
  productStock: { fontSize: 12, color: '#8A7060', marginTop: 2 },
  actions: { width: '100%', gap: 12 },
});
