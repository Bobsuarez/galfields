import { CatalogScreen } from '@/components/settings/catalog-screen';
import { categoriesApi, type CatalogCategory, type CategoryFormData } from '@/services/catalog-api';

export default function CategoriesScreen() {
  return (
    <CatalogScreen<CatalogCategory, CategoryFormData>
      title="Categorías"
      entityLabel="Categoría"
      emptyIcon="tag.fill"
      emptyLabel="No se encontraron categorías"
      api={categoriesApi}
      fields={[
        { key: 'name', label: 'Nombre', placeholder: 'Ej. Lácteos', required: true },
        {
          key: 'description',
          label: 'Descripción',
          placeholder: 'Ej. Leche, quesos y derivados',
          multiline: true,
        },
      ]}
      getSubtitle={item => item.description || undefined}
      toFormValues={item => ({ name: item.name, description: item.description })}
      fromFormValues={values => ({ name: values.name.trim(), description: values.description.trim() })}
    />
  );
}
