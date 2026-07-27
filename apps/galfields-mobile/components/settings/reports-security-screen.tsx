import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from '@/components/reports/report-header';
import { AppButton } from '@/components/ui/app-button';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { generateReportsAccessCode } from '@/services/reports-access-api';

/** Configuración → Acceso a Reportes: generates the 6-digit code the
 * desktop POS (apps/galfield-pos) asks for every time a cashier opens
 * Reportes. Generating a new code invalidates the previous one — the
 * backend only ever checks the most recently generated code (see
 * backend/pos's CLAUDE.md, "Reports access code"). Nothing is persisted
 * here: leaving this screen forgets the code on purpose, so it can't be
 * silently re-read later by someone who shouldn't have it. */
export function ReportsSecurityScreen() {
  const colors = useThemeColors();
  const [code, setCode] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState('');

  const handleGenerate = async () => {
    setGenerating(true);
    setError('');
    try {
      const result = await generateReportsAccessCode();
      setCode(result.code);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error desconocido');
    } finally {
      setGenerating(false);
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <ReportHeader title="Acceso a Reportes" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={[styles.hint, { color: colors.textSecondary }]}>
          Genera un código de 6 dígitos que deberás dar a quien necesite entrar al módulo de Reportes en el POS. Se
          pedirá cada vez que alguien intente entrar, y generar uno nuevo invalida el anterior de inmediato.
        </Text>

        {code && (
          <View style={[styles.codeCard, { borderColor: colors.border }]}>
            <Text style={[styles.codeLabel, { color: colors.textSecondary }]}>Código actual</Text>
            <Text style={[styles.codeValue, { color: colors.text }]}>{code.split('').join(' ')}</Text>
          </View>
        )}

        {error ? <Text style={styles.error}>{error}</Text> : null}

        <AppButton
          label={code ? 'Generar otro código' : 'Generar código de acceso'}
          onPress={handleGenerate}
          loading={generating}
        />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 16, gap: 16 },
  hint: { fontSize: 12, lineHeight: 18 },
  codeCard: {
    borderWidth: 1.5,
    borderRadius: 16,
    paddingVertical: 24,
    alignItems: 'center',
    gap: 8,
  },
  codeLabel: {
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.6,
  },
  codeValue: {
    fontSize: 36,
    fontWeight: '800',
    letterSpacing: 4,
  },
  error: { color: '#E53935', fontSize: 13 },
});
