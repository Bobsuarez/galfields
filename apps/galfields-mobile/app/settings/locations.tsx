import { CatalogScreen } from '@/components/settings/catalog-screen';
import { locationsApi, type CatalogLocation, type LocationFormData } from '@/services/catalog-api';

export default function LocationsScreen() {
  return (
    <CatalogScreen<CatalogLocation, LocationFormData>
      title="Ubicaciones"
      entityLabel="Ubicación"
      emptyIcon="mappin.and.ellipse"
      emptyLabel="No se encontraron ubicaciones"
      api={locationsApi}
      fields={[
        { key: 'name', label: 'Nombre', placeholder: 'Ej. Bodega principal', required: true },
        { key: 'address', label: 'Dirección', placeholder: 'Ej. Calle 54 f bis número 88' },
        { key: 'phone', label: 'Teléfono', placeholder: 'Ej. 3057848780', keyboardType: 'phone-pad' },
      ]}
      getSubtitle={item => item.address || item.phone || undefined}
      toFormValues={item => ({ name: item.name, address: item.address, phone: item.phone })}
      fromFormValues={values => ({
        name: values.name.trim(),
        address: values.address.trim(),
        phone: values.phone.trim(),
      })}
    />
  );
}
