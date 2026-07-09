import { CatalogScreen } from '@/components/settings/catalog-screen';
import { brandsApi, type BrandFormData, type CatalogBrand } from '@/services/catalog-api';

export default function BrandsScreen() {
  return (
    <CatalogScreen<CatalogBrand, BrandFormData>
      title="Marcas"
      entityLabel="Marca"
      emptyIcon="building.2.fill"
      emptyLabel="No se encontraron marcas"
      api={brandsApi}
      fields={[{ key: 'name', label: 'Nombre', placeholder: 'Ej. Bavaria', required: true }]}
      toFormValues={item => ({ name: item.name })}
      fromFormValues={values => ({ name: values.name.trim() })}
    />
  );
}
