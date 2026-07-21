import { StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { ReportHeader } from './report-header';
import { SettingsMenuRow } from '@/components/settings/settings-menu-row';
import { useThemeColors } from '@/hooks/use-theme-colors';

const REPORT_ITEMS = [
  {
    icon: 'chart.bar.fill',
    label: 'Ventas del día',
    subtitle: 'Total vendido y número de transacciones',
    href: '/reports/sales-daily' as const,
  },
  {
    icon: 'banknote.fill',
    label: 'Cierre de caja',
    subtitle: 'Resumen de ventas por método de pago del día',
    href: '/reports/cash-summary' as const,
  },
  {
    icon: 'creditcard.fill',
    label: 'Ventas por método de pago',
    subtitle: 'Comparativo por rango de fechas',
    href: '/reports/sales-by-payment-method' as const,
  },
  {
    icon: 'tray.fill',
    label: 'Inventario actual',
    subtitle: 'Stock por producto y ubicación',
    href: '/reports/inventory' as const,
  },
  {
    icon: 'exclamationmark.triangle.fill',
    label: 'Productos con stock bajo',
    subtitle: 'Productos que necesitan reabastecimiento',
    href: '/reports/low-stock' as const,
  },
  {
    icon: 'doc.text.fill',
    label: 'Historial de facturas',
    subtitle: 'Ventas registradas, detalle por factura',
    href: '/reports/invoices' as const,
  },
] as const;

export function ReportsScreen() {
  const colors = useThemeColors();
  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <ReportHeader title="Reportes" />
      <View style={styles.content}>
        <View style={styles.card}>
          {REPORT_ITEMS.map(item => (
            <SettingsMenuRow
              key={item.label}
              icon={item.icon}
              label={item.label}
              subtitle={item.subtitle}
              onPress={() => router.push(item.href)}
            />
          ))}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 16 },
  card: {
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 3,
  },
});
