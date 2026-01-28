import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Alert,
  TextInput,
  TouchableOpacity,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { Button } from '@/components/ui/Button';
import { formatCurrency } from '@/utils/currency';

export default function QRISscreen() {
  const router = useRouter();
  const { colors } = useTheme();

  const [amount, setAmount] = useState('');
  const [processing, setProcessing] = useState(false);
  const [qrData, setQrData] = useState<any>(null);
  const [showManualInput, setShowManualInput] = useState(false);

  const handlePayment = async () => {
    if (!amount || parseFloat(amount) <= 0) {
      Alert.alert('Error', 'Please enter a valid amount');
      return;
    }

    setProcessing(true);

    try {
      // Mock payment - in production, call actual payment service
      Alert.alert('Success', 'Payment successful!', [
        {
          text: 'OK',
          onPress: () => router.back(),
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

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.contentContainer}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.text }]}>QRIS Payment</Text>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={[styles.backButton, { color: colors.text }]}>← Back</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.card}>
        <Text style={[styles.label, { color: colors.textSecondary }]}>
          Scan QR Code or Enter Amount
        </Text>

        {!showManualInput ? (
          <>
            <View style={styles.qrPlaceholder}>
              <Text style={[styles.qrPlaceholderText, { color: colors.textSecondary }]}>
                QR scanner will appear here
              </Text>
              <Text style={[styles.qrPlaceholderHint, { color: colors.textSecondary }]}>
                (Camera functionality requires additional setup)
              </Text>
            </View>

            <TouchableOpacity
              style={[styles.manualInputButton, { borderColor: colors.border }]}
              onPress={() => setShowManualInput(true)}
            >
              <Text style={[styles.manualInputButtonText, { color: colors.text }]}>
                Or Enter Amount Manually
              </Text>
            </TouchableOpacity>
          </>
        ) : (
          <>
            <Text style={[styles.label, { color: colors.textSecondary }]}>
              Enter Amount (IDR)
            </Text>
            <TextInput
              value={amount}
              onChangeText={setAmount}
              keyboardType="decimal-pad"
              placeholder="0"
              style={[styles.input, {
                borderColor: colors.border,
                color: colors.text,
                backgroundColor: colors.card
              }]}
              editable={!processing}
            />

            <Text style={[styles.note, { color: colors.textSecondary }]}>
              Enter the amount you want to pay
            </Text>

            <TouchableOpacity
              style={[styles.manualInputButton, { borderColor: colors.border }]}
              onPress={() => setShowManualInput(false)}
            >
              <Text style={[styles.manualInputButtonText, { color: colors.text }]}>
                ← Back to QR Scanner
              </Text>
            </TouchableOpacity>
          </>
        )}

        <View style={styles.paymentSection}>
          <Button
            title={`Pay ${amount ? formatCurrency(parseFloat(amount)) : 'Rp 0'}`}
            onPress={handlePayment}
            loading={processing}
            disabled={!amount || parseFloat(amount) <= 0}
            style={{ marginTop: 16 }}
          />
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  contentContainer: {
    padding: 20,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
  },
  backButton: {
    fontSize: 16,
  },
  card: {
    backgroundColor: '#1e293b',
    borderRadius: 16,
    padding: 24,
  },
  label: {
    fontSize: 14,
    marginBottom: 16,
  },
  qrPlaceholder: {
    height: 200,
    backgroundColor: '#374151',
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
  },
  qrPlaceholderText: {
    fontSize: 16,
    marginBottom: 8,
  },
  qrPlaceholderHint: {
    fontSize: 12,
  },
  manualInputButton: {
    borderWidth: 1,
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  manualInputButtonText: {
    fontSize: 14,
  },
  input: {
    borderWidth: 1,
    borderRadius: 8,
    paddingVertical: 12,
    paddingHorizontal: 16,
    fontSize: 16,
    marginBottom: 12,
  },
  note: {
    fontSize: 12,
    marginBottom: 24,
  },
  paymentSection: {
    marginTop: 24,
  },
});
