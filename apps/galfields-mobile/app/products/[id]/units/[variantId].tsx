import { useLocalSearchParams } from 'expo-router';
import { ProductUnitsScreen } from '@/components/products/product-units-screen';

export default function ProductUnitsRoute() {
  const { id, variantId } = useLocalSearchParams<{ id: string; variantId: string }>();
  return <ProductUnitsScreen productId={id} variantId={Number(variantId)} />;
}
