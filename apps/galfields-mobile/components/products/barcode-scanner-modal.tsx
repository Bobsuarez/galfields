import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { IconSymbol } from '@/components/ui/icon-symbol';

interface BarcodeScannerModalProps {
  visible: boolean;
  onScan: (code: string) => void;
  onClose: () => void;
}

const SCAN_WIDTH = 260;
const SCAN_HEIGHT = 160;
const CORNER_SIZE = 24;
const CORNER_BORDER = 3;
const OVERLAY = 'rgba(0,0,0,0.65)';

export function BarcodeScannerModal({ visible, onScan, onClose }: BarcodeScannerModalProps) {
  const colors = useThemeColors();
  const [permission, requestPermission] = useCameraPermissions();
  const [scanned, setScanned] = useState(false);
  const insets = useSafeAreaInsets();

  // Reset scan lock each time the modal opens
  useEffect(() => {
    if (visible) setScanned(false);
  }, [visible]);

  const handleScanned = ({ data }: { data: string }) => {
    if (scanned) return;
    setScanned(true);
    onScan(data);
    onClose();
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      statusBarTranslucent
      onRequestClose={onClose}
    >
      <View style={styles.container}>
        {/* Header */}
        <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
          <Text style={styles.headerTitle}>Escanear código de barras</Text>
          <Pressable onPress={onClose} hitSlop={10}>
            <IconSymbol name="xmark" size={24} color="#fff" />
          </Pressable>
        </View>

        {/* Loading while permission status is unknown */}
        {!permission ? (
          <View style={styles.centered}>
            <ActivityIndicator size="large" color={Brand.orange} />
          </View>

        /* Permission not granted yet */
        ) : !permission.granted ? (
          <View style={[styles.centered, { backgroundColor: colors.background }]}>
            <View style={styles.permissionIcon}>
              <IconSymbol name="camera.fill" size={40} color={Brand.orange} />
            </View>
            <Text style={[styles.permissionTitle, { color: colors.text }]}>Acceso a cámara requerido</Text>
            <Text style={[styles.permissionBody, { color: colors.textSecondary }]}>
              Necesitamos acceso a tu cámara para escanear códigos de barras de productos.
            </Text>
            <Pressable onPress={requestPermission} style={styles.grantBtn}>
              <Text style={styles.grantBtnText}>Permitir acceso</Text>
            </Pressable>
          </View>

        /* Camera ready */
        ) : (
          <View style={styles.cameraWrap}>
            <CameraView
              style={StyleSheet.absoluteFill}
              facing="back"
              barcodeScannerSettings={{
                barcodeTypes: ['ean13', 'ean8', 'code128', 'code39', 'qr', 'upc_a', 'upc_e'],
              }}
              onBarcodeScanned={scanned ? undefined : handleScanned}
            />

            {/* Dark overlay: top panel */}
            <View style={styles.overlayTop} />

            {/* Dark overlay: sides + transparent scan box */}
            <View style={styles.overlayRow}>
              <View style={styles.overlaySide} />
              <View style={styles.scanBox}>
                <View style={[styles.corner, styles.cornerTL]} />
                <View style={[styles.corner, styles.cornerTR]} />
                <View style={[styles.corner, styles.cornerBL]} />
                <View style={[styles.corner, styles.cornerBR]} />
              </View>
              <View style={styles.overlaySide} />
            </View>

            {/* Dark overlay: bottom panel + hint */}
            <View style={styles.overlayBottom}>
              <Text style={styles.hint}>Apunta al código de barras del producto</Text>
            </View>
          </View>
        )}
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' },

  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 16,
    backgroundColor: Brand.brown,
  },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#fff' },

  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 32,
    gap: 14,
  },
  permissionIcon: {
    width: 80,
    height: 80,
    borderRadius: 20,
    backgroundColor: `${Brand.orange}18`,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  permissionTitle: { fontSize: 18, fontWeight: '700', textAlign: 'center' },
  permissionBody: { fontSize: 14, textAlign: 'center', lineHeight: 22 },
  grantBtn: {
    marginTop: 8,
    paddingVertical: 14,
    paddingHorizontal: 32,
    backgroundColor: Brand.orange,
    borderRadius: 12,
  },
  grantBtnText: { fontSize: 15, fontWeight: '600', color: '#fff' },

  cameraWrap: { flex: 1 },

  // Overlay panels that darken everything except the scan box
  overlayTop: { flex: 1, backgroundColor: OVERLAY },
  overlayRow: { height: SCAN_HEIGHT, flexDirection: 'row' },
  overlaySide: { flex: 1, backgroundColor: OVERLAY },
  scanBox: { width: SCAN_WIDTH, height: SCAN_HEIGHT },
  overlayBottom: {
    flex: 1,
    backgroundColor: OVERLAY,
    alignItems: 'center',
    paddingTop: 28,
  },
  hint: { fontSize: 14, color: 'rgba(255,255,255,0.85)', textAlign: 'center' },

  // L-shaped corner markers
  corner: { position: 'absolute', width: CORNER_SIZE, height: CORNER_SIZE },
  cornerTL: {
    top: 0, left: 0,
    borderTopWidth: CORNER_BORDER, borderLeftWidth: CORNER_BORDER,
    borderTopColor: Brand.orange, borderLeftColor: Brand.orange,
  },
  cornerTR: {
    top: 0, right: 0,
    borderTopWidth: CORNER_BORDER, borderRightWidth: CORNER_BORDER,
    borderTopColor: Brand.orange, borderRightColor: Brand.orange,
  },
  cornerBL: {
    bottom: 0, left: 0,
    borderBottomWidth: CORNER_BORDER, borderLeftWidth: CORNER_BORDER,
    borderBottomColor: Brand.orange, borderLeftColor: Brand.orange,
  },
  cornerBR: {
    bottom: 0, right: 0,
    borderBottomWidth: CORNER_BORDER, borderRightWidth: CORNER_BORDER,
    borderBottomColor: Brand.orange, borderRightColor: Brand.orange,
  },
});
