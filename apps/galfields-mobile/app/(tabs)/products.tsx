import { Redirect } from 'expo-router';

// Products already lives at /products (outside the tabs group, so it can
// push its own stack: list -> add -> edit). This route exists only to give
// it a tab bar entry.
export default function ProductsTabScreen() {
  return <Redirect href="/products" />;
}
