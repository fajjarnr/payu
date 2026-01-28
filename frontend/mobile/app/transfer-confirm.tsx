import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Alert,
  TouchableOpacity,
  Share,
} from 'react-native';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { useAuth } from '@/hooks/useAuth';
import { useBiometrics } from '@/hooks/useBiometrics';
import { useWallet } from '@/hooks/useWallet';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { formatCurrency } from '@/utils/currency';
import { transactionService } from '@/services/transaction.service';
import { useAnalytics } from '@/hooks/useAnalytics';

type TransferStep = 'review' | 'pin' | 'biometric' | 'processing' | 'success' | 'failed';

export default function TransferConfirmScreen() {
  const router = useRouter();
  const { colors } = useTheme();
  const params = useLocalSearchParams();
  const { user } = useAuth();
  const { authenticate } = useBiometrics();
  const { loadWallet } = useWallet();
  const { trackTransaction } = useAnalytics();

  const [step, setStep] = useState<TransferStep>('review');
  const [pin, setPin] = useState('');
  const [pinError, setPinError] = useState('');
  const [transactionResult, setTransactionResult] = useState<any>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  // Parse transfer data from params
  const transferData = params.data ? JSON.parse(params.data as string) : null;

  if (!transferData) {
    router.back();
    return null;
  }

  const transferTypes = [
    { id: 'bifast', name: 'BI-FAST', fee: 0, minAmount: 10000, maxAmount: 25000000 },
    { id: 'skn', name: 'SKN', fee: 5000, minAmount: 10000, maxAmount: 100000000 },
    { id: 'rtgs', name: 'RTGS', fee: 25000, minAmount: 10000001, maxAmount: 10000000000 },
  ];

  const [selectedTransferType, setSelectedTransferType] = useState(transferTypes[0]);

  const handleContinue = () => {
    setStep('pin');
  };

  const handlePinSubmit = async () => {
    if (pin.length !== 6) {
      setPinError('PIN must be 6 digits');
      return;
    }

    // Check if biometric is available
    const biometricAvailable = await authenticate('Confirm Transfer');

    if (biometricAvailable) {
      setStep('biometric');
    } else {
      await processTransfer();
    }
  };

  const handleBiometricConfirm = async () => {
    const success = await authenticate('Confirm Transfer');
    if (success) {
      await processTransfer();
    }
  };

  const processTransfer = async () => {
    setStep('processing');
    setIsProcessing(true);

    try {
      const result = await transactionService.transfer({
        ...transferData,
        transferType: selectedTransferType.id,
        pin,
      });

      setTransactionResult(result);
      setStep('success');

      // Track transaction
      trackTransaction(selectedTransferType.id, transferData.amount, 'success');

      // Refresh wallet balance
      await loadWallet();
    } catch (error: any) {
      setTransactionResult(error);
      setStep('failed');

      trackTransaction(selectedTransferType.id, transferData.amount, 'failed');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleShareReceipt = async () => {
    if (!transactionResult) return;

    const receiptText = `
PayU Transfer Receipt
---------------------
Transaction ID: ${transactionResult.id}
Date: ${new Date().toLocaleString('id-ID')}
Amount: ${formatCurrency(transferData.amount)}
Fee: ${formatCurrency(selectedTransferType.fee)}
Total: ${formatCurrency(transferData.amount + selectedTransferType.fee)}
To: ${transferData.recipientAccount} (${transferData.recipientBank})
Note: ${transferData.description}
Status: ${step === 'success' ? 'Success' : 'Failed'}
    `.trim();

    try {
      await Share.share({
        message: receiptText,
      });
    } catch (error) {
      console.error('Error sharing receipt:', error);
    }
  };

  const handleDone = () => {
    if (step === 'success') {
      router.replace('/(tabs)');
    } else {
      router.back();
    }
  };

  const renderReviewStep = () => (
    <View>
      <Text style={[styles.title, { color: colors.text }]}>Review Transfer</Text>

      {/* Transfer Type Selection */}
      <Card padding="lg" style={styles.card}>
        <Text style={[styles.sectionTitle, { color: colors.text }]}>Transfer Method</Text>
        {transferTypes.map((type) => (
          <TouchableOpacity
            key={type.id}
            style={[
              styles.transferTypeOption,
              selectedTransferType.id === type.id && styles.transferTypeOptionSelected,
              { borderColor: colors.border },
            ]}
            onPress={() => setSelectedTransferType(type)}
          >
            <View style={styles.transferTypeInfo}>
              <Text style={[styles.transferTypeName, { color: colors.text }]}>
                {type.name}
              </Text>
              <Text style={[styles.transferTypeFee, { color: colors.textSecondary }]}>
                Fee: {formatCurrency(type.fee)}
              </Text>
            </View>
            {selectedTransferType.id === type.id && (
              <View style={styles.checkmark}>
                <Text style={styles.checkmarkText}>✓</Text>
              </View>
            )}
          </TouchableOpacity>
        ))}
      </Card>

      {/* Transfer Details */}
      <Card padding="lg" style={styles.card}>
        <Text style={[styles.sectionTitle, { color: colors.text }]}>Transfer Details</Text>

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            From
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {user?.phoneNumber ? `•••• ${user.phoneNumber.slice(-4)}` : '•••• 1234'}
          </Text>
        </View>

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            To
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {transferData.recipientAccount}
          </Text>
        </View>

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Bank
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {transferData.recipientBank}
          </Text>
        </View>

        <View style={[styles.divider, { borderBottomColor: colors.border }]} />

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Amount
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {formatCurrency(transferData.amount)}
          </Text>
        </View>

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Fee
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {formatCurrency(selectedTransferType.fee)}
          </Text>
        </View>

        <View style={[styles.divider, { borderBottomColor: colors.border }]} />

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Total
          </Text>
          <Text style={[styles.totalValue, { color: '#10b981' }]}>
            {formatCurrency(transferData.amount + selectedTransferType.fee)}
          </Text>
        </View>

        <View style={[styles.divider, { borderBottomColor: colors.border }]} />

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Note
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {transferData.description}
          </Text>
        </View>
      </Card>

      <Button
        title="Continue"
        onPress={handleContinue}
        fullWidth
        style={styles.continueButton}
      />
    </View>
  );

  const renderPinStep = () => (
    <View style={styles.pinContainer}>
      <Text style={[styles.title, { color: colors.text }]}>Enter PIN</Text>
      <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
        Enter your 6-digit PIN to confirm this transfer
      </Text>

      <View style={styles.pinDisplay}>
        {[...Array(6)].map((_, index) => (
          <View
            key={index}
            style={[
              styles.pinDot,
              pin[index] ? styles.pinDotFilled : styles.pinDotEmpty,
              { borderColor: colors.border },
            ]}
          >
            <Text style={styles.pinDotText}>{pin[index] ? '•' : ''}</Text>
          </View>
        ))}
      </View>

      {pinError ? (
        <Text style={styles.pinError}>{pinError}</Text>
      ) : (
        <View style={styles.pinSpacer} />
      )}

      <View style={styles.numberPad}>
        {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((num) => (
          <TouchableOpacity
            key={num}
            style={styles.numberButton}
            onPress={() => {
              if (pin.length < 6) {
                setPin(pin + num.toString());
                setPinError('');
              }
            }}
          >
            <Text style={[styles.numberButtonText, { color: colors.text }]}>
              {num}
            </Text>
          </TouchableOpacity>
        ))}
        <View style={styles.numberButton} />
        <TouchableOpacity
          style={styles.numberButton}
          onPress={() => {
            if (pin.length < 6) {
              setPin(pin + '0');
              setPinError('');
            }
          }}
        >
          <Text style={[styles.numberButtonText, { color: colors.text }]}>
            0
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.numberButton}
          onPress={() => {
            setPin(pin.slice(0, -1));
            setPinError('');
          }}
        >
          <Text style={[styles.numberButtonText, { color: '#ef4444' }]}>
            ⌫
          </Text>
        </TouchableOpacity>
      </View>

      <TouchableOpacity style={styles.cancelButton} onPress={() => setStep('review')}>
        <Text style={styles.cancelButtonText}>Cancel</Text>
      </TouchableOpacity>
    </View>
  );

  const renderProcessingStep = () => (
    <View style={styles.processingContainer}>
      <View style={styles.spinner} />
      <Text style={[styles.processingText, { color: colors.text }]}>
        Processing your transfer...
      </Text>
    </View>
  );

  const renderSuccessStep = () => (
    <View style={styles.resultContainer}>
      <View style={styles.successIcon}>
        <Text style={styles.successIconText}>✓</Text>
      </View>
      <Text style={[styles.resultTitle, { color: colors.text }]}>
        Transfer Successful!
      </Text>
      <Text style={[styles.resultSubtitle, { color: colors.textSecondary }]}>
        {formatCurrency(transferData.amount + selectedTransferType.fee)}
      </Text>

      <Card padding="lg" style={styles.receiptCard}>
        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Transaction ID
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {transactionResult?.id || 'TXN' + Date.now()}
          </Text>
        </View>

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Date & Time
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {new Date().toLocaleString('id-ID')}
          </Text>
        </View>

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Recipient
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {transferData.recipientAccount}
          </Text>
        </View>

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Bank
          </Text>
          <Text style={[styles.detailValue, { color: colors.text }]}>
            {transferData.recipientBank}
          </Text>
        </View>

        <View style={[styles.divider, { borderBottomColor: colors.border }]} />

        <View style={styles.detailRow}>
          <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
            Total Amount
          </Text>
          <Text style={[styles.totalValue, { color: '#10b981' }]}>
            {formatCurrency(transferData.amount + selectedTransferType.fee)}
          </Text>
        </View>
      </Card>

      <View style={styles.actionButtons}>
        <Button
          title="Share Receipt"
          onPress={handleShareReceipt}
          variant="outline"
          style={styles.shareButton}
        />
        <Button
          title="Done"
          onPress={handleDone}
          style={styles.doneButton}
        />
      </View>
    </View>
  );

  const renderFailedStep = () => (
    <View style={styles.resultContainer}>
      <View style={[styles.failedIcon]}>
        <Text style={styles.failedIconText}>✕</Text>
      </View>
      <Text style={[styles.resultTitle, { color: colors.text }]}>
        Transfer Failed
      </Text>
      <Text style={[styles.resultSubtitle, { color: colors.textSecondary }]}>
        {transactionResult?.message || 'Something went wrong'}
      </Text>

      <Button
        title="Try Again"
        onPress={() => router.back()}
        fullWidth
        style={styles.retryButton}
      />
      <TouchableOpacity onPress={handleDone}>
        <Text style={styles.backToHomeText}>Back to Home</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
    >
      {step === 'review' && renderReviewStep()}
      {step === 'pin' && renderPinStep()}
      {step === 'biometric' && (
        <View style={styles.biometricContainer}>
          <Text style={[styles.title, { color: colors.text }]}>
            Biometric Authentication
          </Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            Use your biometric to confirm this transfer
          </Text>
          <Button
            title="Authenticate"
            onPress={handleBiometricConfirm}
            fullWidth
            style={styles.authenticateButton}
          />
        </View>
      )}
      {step === 'processing' && renderProcessingStep()}
      {step === 'success' && renderSuccessStep()}
      {step === 'failed' && renderFailedStep()}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  content: {
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: '900',
    marginBottom: 8,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 14,
    textAlign: 'center',
    marginBottom: 24,
  },
  card: {
    marginBottom: 16,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 16,
  },
  transferTypeOption: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderRadius: 12,
    borderWidth: 2,
    marginBottom: 12,
  },
  transferTypeOptionSelected: {
    borderColor: '#10b981',
    backgroundColor: '#f0fdf4',
  },
  transferTypeInfo: {
    flex: 1,
  },
  transferTypeName: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 4,
  },
  transferTypeFee: {
    fontSize: 12,
  },
  checkmark: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#10b981',
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkmarkText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 12,
  },
  detailLabel: {
    fontSize: 14,
  },
  detailValue: {
    fontSize: 14,
    fontWeight: '600',
  },
  totalValue: {
    fontSize: 18,
    fontWeight: '700',
  },
  divider: {
    borderBottomWidth: 1,
    marginVertical: 4,
  },
  continueButton: {
    marginTop: 8,
  },
  pinContainer: {
    flex: 1,
  },
  pinDisplay: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 12,
    marginVertical: 32,
  },
  pinDot: {
    width: 48,
    height: 56,
    borderRadius: 12,
    borderWidth: 2,
    justifyContent: 'center',
    alignItems: 'center',
  },
  pinDotEmpty: {
    backgroundColor: '#ffffff',
  },
  pinDotFilled: {
    backgroundColor: '#10b981',
    borderColor: '#10b981',
  },
  pinDotText: {
    fontSize: 24,
    color: '#ffffff',
    fontWeight: '700',
  },
  pinError: {
    fontSize: 14,
    color: '#ef4444',
    textAlign: 'center',
    marginBottom: 16,
  },
  pinSpacer: {
    height: 24,
  },
  numberPad: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 16,
    marginBottom: 24,
  },
  numberButton: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  numberButtonText: {
    fontSize: 24,
    fontWeight: '600',
  },
  cancelButton: {
    paddingVertical: 16,
    alignItems: 'center',
  },
  cancelButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#6b7280',
  },
  biometricContainer: {
    flex: 1,
    justifyContent: 'center',
  },
  authenticateButton: {
    marginTop: 24,
  },
  processingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  spinner: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 4,
    borderColor: '#10b981',
    borderTopColor: 'transparent',
    marginBottom: 24,
  },
  processingText: {
    fontSize: 16,
    fontWeight: '600',
  },
  resultContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  successIcon: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#10b981',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
  },
  successIconText: {
    fontSize: 48,
    color: '#ffffff',
    fontWeight: '700',
  },
  failedIcon: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#ef4444',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
  },
  failedIconText: {
    fontSize: 48,
    color: '#ffffff',
    fontWeight: '700',
  },
  resultTitle: {
    fontSize: 24,
    fontWeight: '900',
    marginBottom: 8,
  },
  resultSubtitle: {
    fontSize: 14,
    marginBottom: 32,
  },
  receiptCard: {
    width: '100%',
    marginBottom: 24,
  },
  actionButtons: {
    flexDirection: 'row',
    gap: 12,
    width: '100%',
  },
  shareButton: {
    flex: 1,
  },
  doneButton: {
    flex: 1,
  },
  retryButton: {
    width: '100%',
    marginBottom: 16,
  },
  backToHomeText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#10b981',
  },
});
