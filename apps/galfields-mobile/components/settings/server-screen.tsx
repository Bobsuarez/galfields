import { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { ReportHeader } from '@/components/reports/report-header';
import { AppButton } from '@/components/ui/app-button';
import { TextInputField } from '@/components/ui/text-input-field';
import { Brand } from '@/constants/theme';
import { currentApiBaseUrl, resetApiBaseUrl, setApiBaseUrl } from '@/services/api-base-url';

/** Lets the app point at a different backend without a rebuild — same
 * intent as the desktop POS's Configuración → Reglas y Sincronización
 * "URL del servidor" field (see apps/galfield-pos's CLAUDE.md). Persisted
 * in AsyncStorage (see services/api-base-url.ts), not `EXPO_PUBLIC_*` env
 * vars, since those are baked in at build time and can't change at
 * runtime. */
export function ServerScreen() {
  const [url, setUrl] = useState(currentApiBaseUrl() ?? '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    setError('');
    try {
      await setApiBaseUrl(url);
      Alert.alert('Guardado', 'La app ya está usando la nueva URL del servidor.', [
        { text: 'OK', onPress: () => router.back() },
      ]);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error desconocido');
    } finally {
      setSaving(false);
    }
  };

  const handleReset = () => {
    Alert.alert('Restablecer', '¿Volver a la URL por defecto de esta instalación?', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Restablecer',
        onPress: async () => {
          await resetApiBaseUrl();
          setUrl(currentApiBaseUrl() ?? '');
        },
      },
    ]);
  };

  return (
    <View style={styles.container}>
      <ReportHeader title="Servidor" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.hint}>
          A dónde se conecta esta app para leer y guardar catálogo, ventas e inventario. Solo cámbiala si sabes lo
          que haces — un valor incorrecto deja la app sin poder sincronizar nada.
        </Text>

        <TextInputField
          label="URL del servidor"
          placeholder="https://galfields.kinforgeworks.com"
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="url"
          value={url}
          onChangeText={t => {
            setUrl(t);
            setError('');
          }}
          error={error}
        />

        <View style={styles.actions}>
          <View style={styles.actionBtn}>
            <AppButton label="Restablecer" variant="outline" onPress={handleReset} disabled={saving} />
          </View>
          <View style={styles.actionBtn}>
            <AppButton label="Guardar" onPress={handleSave} loading={saving} />
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Brand.cream },
  content: { padding: 16, gap: 16 },
  hint: { fontSize: 12, color: '#8A7060', lineHeight: 18 },
  actions: { flexDirection: 'row', gap: 12, marginTop: 8 },
  actionBtn: { flex: 1 },
});
