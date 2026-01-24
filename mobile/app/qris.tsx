import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { CameraView, Camera } from 'expo-camera';
import { useTheme } from '@react-navigation/native';
import { useQRScanner } from '@/hooks/useCamera';
import { transactionService } from '@/services/transaction.service';
import { Button } from '@/components/ui/Button';
import { formatCurrency } from '@/utils/currency';

export default function QRISscreen() {
  const router = useRouter();
  const { colors } = useTheme();
  const { hasPermission, facing, toggleCameraFacing } = useCamera();
  const { scanned, handleBarCodeScanned, resetScanner } = useQRScanner();

  const [qrData, setQrData] = useState<any>(null);
  const [amount, setAmount] = useState('');
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    if (scanned) {
      // QR data would be parsed here
      // For demo, show payment confirmation
    }
  }, [scanned]);

  const handleBarCodeScannedWrapper = ({ data, type }: { data: string; type: string }) => {
    if (scanned) return;

    try {
      // Parse QRIS data
      const parsed = JSON.parse(data);
      setQrData(parsed);
    } catch {
      // If not JSON, treat as raw data
      setQrData({ raw: data });
    }
  };

  const handlePayment = async () => {
    if (!qrData || !amount) {
      Alert.alert('Error', 'Please enter amount');
      return;
    }

    setProcessing(true);

    try {
      const transaction = await transactionService.payQRIS({
        ...qrData,
        amount: parseFloat(amount),
      });

      Alert.alert('Success', 'Payment successful!', [
        {
          text: 'OK',
          onPress: () => {
            resetScanner();
            router.back();
          },
        },
      ]);
    } catch (error: any) {
      Alert.alert(
        'Payment Failed',
        error.response?.data?.message || 'Something went wrong'
      );
    } finally {
      setProcessing(false);
    }
  };

  if (hasPermission === null) {
    return (
      <View style={styles.container}>
        <Text>Requesting camera permission...</Text>
      </View>
    );
  }

  if (hasPermission === false) {
    return (
      <View style={styles.container}>
        <Text style={styles.errorText}>No camera permission</Text>
        <Button title="Go Back" onPress={() => router.back()} />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <CameraView
        style={styles.camera}
        facing={facing}
        onBarcodeScanned={scanned ? undefined : handleBarCodeScannedWrapper}
        barcodeScannerSettings={{
          barcodeTypes: ['qr'],
        }}
      >
        <View style={styles.overlay}>
          {/* Scan Frame */}
          <View style={styles.scanFrame}>
            <View style={[styles.corner, styles.topLeft]} />
            <View style={[styles.corner, styles.topRight]} />
            <View style={[styles.corner, styles.bottomLeft]} />
            <View style={[styles.corner, styles.bottomRight]} />
          </View>

          <Text style={styles.instruction}>
            {scanned ? 'QR Code detected!' : 'Align QR code within frame'}
          </Text>

          {scanned && (
            <View style={styles.actionButtons}>
              <TouchableOpacity
                style={styles.button}
                onPress={resetScanner}
              >
                <Text style={styles.buttonText}>Scan Again</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.button, styles.primaryButton]}
                onPress={() => router.back()}
              >
                <Text style={styles.primaryButtonText}>Done</Text>
              </TouchableOpacity>
            </View>
          )}

          <TouchableOpacity
            style={styles.flipButton}
            onPress={toggleCameraFacing}
          >
            <Text style={styles.flipButtonText}>🔄 Flip Camera</Text>
          </TouchableOpacity>
        </View>
      </CameraView>

      {/* Payment Modal */}
      {qrData && scanned && (
        <View style={styles.paymentModal}>
          <View style={[styles.paymentContent, { backgroundColor: colors.background }]}>
            <Text style={[styles.paymentTitle, { color: colors.text }]}>
              QRIS Payment
            </Text>

            {qrData.merchantName && (
              <Text style={[styles.merchantName, { color: colors.textSecondary }]}>
                {qrData.merchantName}
              </Text>
            )}

            <Text style={[styles.amountLabel, { color: colors.textSecondary }]}>
              Amount
            </Text>
            <Text style={[styles.amount, { color: colors.text }]}>
              {amount ? formatCurrency(parseFloat(amount)) : 'Rp 0'}
            </Text>

            <TouchableOpacity
              style={[styles.enterAmountButton, { borderColor: colors.border }]}
            >
              <Text style={[styles.enterAmountText, { color: colors.text }]}>
                Enter Amount
              </Text>
            </TouchableOpacity>

            <View style={styles.paymentActions}>
              <Button
                title="Cancel"
                onPress={resetScanner}
                variant="outline"
                style={{ flex: 1, marginRight: 8 }}
              />
              <Button
                title="Pay"
                onPress={handlePayment}
                loading={processing}
                style={{ flex: 1 }}
              />
            </View>
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  camera: {
    flex: 1,
  },
  overlay: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
  },
  scanFrame: {
    width: 280,
    height: 280,
    borderWidth: 2,
    borderColor: 'rgba(255, 255, 255, 0.3)',
    borderRadius: 20,
    position: 'relative',
  },
  corner: {
    position: 'absolute',
    width: 40,
    height: 40,
    borderColor: '#10b981',
  },
  topLeft: {
    top: -2,
    left: -2,
    borderTopWidth: 4,
    borderLeftWidth: 4,
    borderTopLeftRadius: 20,
  },
  topRight: {
    top: -2,
    right: -2,
    borderTopWidth: 4,
    borderRightWidth: 4,
    borderTopRightRadius: 20,
  },
  bottomLeft: {
    bottom: -2,
    left: -2,
    borderBottomWidth: 4,
    borderLeftWidth: 4,
    borderBottomLeftRadius: 20,
  },
  bottomRight: {
    bottom: -2,
    right: -2,
    borderBottomWidth: 4,
    borderRightWidth: 4,
    borderBottomRightRadius: 20,
  },
  instruction: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
    marginTop: 24,
    textAlign: 'center',
  },
  actionButtons: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 24,
  },
  button: {
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderRadius: 12,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  primaryButton: {
    backgroundColor: '#10b981',
  },
  primaryButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
  },
  flipButton: {
    position: 'absolute',
    bottom: 40,
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 24,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
  },
  flipButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  paymentModal: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
  },
  paymentContent: {
    padding: 24,
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
  },
  paymentTitle: {
    fontSize: 24,
    fontWeight: '900',
    marginBottom: 8,
  },
  merchantName: {
    fontSize: 14,
    marginBottom: 24,
  },
  amountLabel: {
    fontSize: 14,
    marginBottom: 8,
  },
  amount: {
    fontSize: 36,
    fontWeight: '900',
    marginBottom: 24,
  },
  enterAmountButton: {
    paddingVertical: 16,
    borderRadius: 12,
    borderWidth: 1,
    alignItems: 'center',
    marginBottom: 24,
  },
  enterAmountText: {
    fontSize: 16,
    fontWeight: '600',
  },
  paymentActions: {
    flexDirection: 'row',
    gap: 12,
  },
  errorText: {
    fontSize: 16,
    color: '#ffffff',
    marginBottom: 24,
  },
});
