import { useState } from 'react';
import {
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { router } from 'expo-router';
import { AppButton } from '@/components/ui/app-button';
import { TextInputField } from '@/components/ui/text-input-field';
import { useAuth } from '@/contexts/auth-context';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';

export default function LoginScreen() {
  const { login } = useAuth();
  const colors = useThemeColors();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async () => {
    if (!username.trim() || !password.trim()) {
      setError('Ingresa usuario y contraseña');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const ok = await login(username.trim(), password);
      if (ok) {
        router.replace('/(tabs)');
      } else {
        setError('Usuario o contraseña incorrectos');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.flex}
      >
        <ScrollView
          contentContainerStyle={styles.container}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          {/* Logo */}
          <View style={styles.logoContainer}>
            <Image
              source={require('@/assets/images/icon.png')}
              style={styles.logo}
              resizeMode="contain"
            />
            <View style={styles.brandRow}>
              <Text style={[styles.brandGar, { color: colors.text }]}>Gar</Text>
              <Text style={styles.brandPOS}>POS</Text>
            </View>
            <Text style={[styles.tagline, { color: colors.textSecondary }]}>PUNTO DE VENTA</Text>
          </View>

          {/* Form */}
          <View style={styles.form}>
            <TextInputField
              leftIcon="person.fill"
              placeholder="Usuario"
              value={username}
              onChangeText={t => { setUsername(t); setError(''); }}
              autoCapitalize="none"
              autoCorrect={false}
              returnKeyType="next"
            />
            <TextInputField
              leftIcon="lock.fill"
              rightIcon={showPassword ? 'eye.slash.fill' : 'eye.fill'}
              onRightIconPress={() => setShowPassword(v => !v)}
              placeholder="Contraseña"
              value={password}
              onChangeText={t => { setPassword(t); setError(''); }}
              secureTextEntry={!showPassword}
              returnKeyType="done"
              onSubmitEditing={handleLogin}
            />

            {error ? <Text style={styles.errorText}>{error}</Text> : null}

            {/* Remember me */}
            <Pressable
              onPress={() => setRememberMe(v => !v)}
              style={styles.rememberRow}
            >
              <View style={[styles.checkbox, { borderColor: colors.border }, rememberMe && styles.checkboxOn]}>
                {rememberMe && <Text style={styles.checkmark}>✓</Text>}
              </View>
              <Text style={[styles.rememberLabel, { color: colors.text }]}>Recordarme</Text>
            </Pressable>

            <AppButton label="Iniciar sesión" onPress={handleLogin} loading={loading} />

            <Pressable style={styles.forgotWrap}>
              <Text style={styles.forgotText}>¿Olvidaste tu contraseña?</Text>
            </Pressable>
          </View>

          <Text style={[styles.version, { color: colors.placeholder }]}>Versión 1.0.0</Text>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
  container: {
    flexGrow: 1,
    paddingHorizontal: 32,
    paddingTop: 48,
    paddingBottom: 32,
    alignItems: 'center',
  },
  logoContainer: { alignItems: 'center', marginBottom: 48 },
  logo: { width: 100, height: 100, marginBottom: 10 },
  brandRow: { flexDirection: 'row', alignItems: 'baseline' },
  brandGar: { fontSize: 36, fontWeight: '800' },
  brandPOS: { fontSize: 36, fontWeight: '800', color: Brand.orange },
  tagline: {
    fontSize: 12,
    fontWeight: '600',
    letterSpacing: 3,
    marginTop: 4,
  },
  form: { width: '100%' },
  errorText: {
    fontSize: 13,
    color: Brand.danger,
    textAlign: 'center',
    marginBottom: 12,
    marginTop: -4,
  },
  rememberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
    gap: 10,
  },
  checkbox: {
    width: 20,
    height: 20,
    borderRadius: 4,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxOn: { backgroundColor: Brand.orange, borderColor: Brand.orange },
  checkmark: { color: '#fff', fontSize: 13, fontWeight: '700', lineHeight: 16 },
  rememberLabel: { fontSize: 14 },
  forgotWrap: { alignItems: 'center', marginTop: 20 },
  forgotText: { fontSize: 14, color: Brand.orange, fontWeight: '500' },
  version: { fontSize: 12, marginTop: 32 },
});
