import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Alert,
  TouchableOpacity,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { useWallet } from '@/hooks/useWallet';
import { useTransfer } from '@/hooks/useTransactions';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card } from '@/components/ui/Card';
import { BANKS } from '@/constants/config';
import { validateAmount } from '@/utils/validation';
import { TransferData } from '@/types';

export default function TransfersScreen() {
  const router = useRouter();
  const { colors } = useTheme();
  const { pockets, primaryWallet } = useWallet();
  const { transfer, isLoading } = useTransfer();

  const [formData, setFormData] = useState({
    amount: '',
    recipientAccount: '',
    recipientBank: BANKS[0].code,
    description: '',
    fromPocket: primaryWallet?.id || '',
  });

  const [errors, setErrors] = useState<{
    amount?: string;
    recipientAccount?: string;
    description?: string;
  }>({});

  const handleTransfer = async () => {
    const newErrors: typeof errors = {};

    const amountNum = parseFloat(formData.amount);
    if (!validateAmount(amountNum)) {
      newErrors.amount = 'Please enter a valid amount';
    } else if (amountNum < 10000) {
      newErrors.amount = 'Minimum transfer is Rp 10.000';
    } else if (primaryWallet && amountNum > primaryWallet.balance) {
      newErrors.amount = 'Insufficient balance';
    }

    if (!formData.recipientAccount.trim()) {
      newErrors.recipientAccount = 'Recipient account is required';
    }

    if (!formData.description.trim()) {
      newErrors.description = 'Description is required';
    }

    setErrors(newErrors);
    if (Object.keys(newErrors).length > 0) return;

    const transferData: TransferData = {
      amount: amountNum,
      recipientAccount: formData.recipientAccount,
      recipientBank: formData.recipientBank,
      description: formData.description,
      fromPocket: formData.fromPocket,
    };

    try {
      const transaction = await transfer(transferData);
      // @ts-ignore
      router.push({
        pathname: '/transfer-confirm',
        params: { data: JSON.stringify(transferData) },
      });
    } catch (error: any) {
      Alert.alert(
        'Transfer Failed',
        error.response?.data?.message || 'Something went wrong'
      );
    }
  };

  const selectedBank = BANKS.find((b) => b.code === formData.recipientBank);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
    >
      <Text style={[styles.title, { color: colors.text }]}>Transfer Money</Text>

      <Card padding="lg">
        {/* From Pocket */}
        <View style={styles.fieldGroup}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>
            From
          </Text>
          <View style={styles.pocketSelector}>
            <View style={styles.pocketInfo}>
              <Text style={styles.pocketName}>Primary Wallet</Text>
              <Text style={styles.pocketBalance}>
                Available: Rp {(primaryWallet?.balance || 0).toLocaleString('id-ID')}
              </Text>
            </View>
          </View>
        </View>

        {/* Amount */}
        <Input
          label="Amount"
          value={formData.amount}
          onChangeText={(text) =>
            setFormData({ ...formData, amount: text })
          }
          placeholder="0"
          keyboardType="numeric"
          error={errors.amount}
        />

        {/* Recipient Bank */}
        <View style={styles.fieldGroup}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>
            Recipient Bank
          </Text>
          <TouchableOpacity
            style={[styles.bankSelector, { borderColor: colors.border }]}
          >
            <Text style={[styles.bankName, { color: colors.text }]}>
              {selectedBank?.name}
            </Text>
            <Text style={styles.dropdownIcon}>▼</Text>
          </TouchableOpacity>
        </View>

        {/* Recipient Account */}
        <Input
          label="Recipient Account Number"
          value={formData.recipientAccount}
          onChangeText={(text) =>
            setFormData({ ...formData, recipientAccount: text })
          }
          placeholder="Enter account number"
          keyboardType="numeric"
          error={errors.recipientAccount}
        />

        {/* Description */}
        <Input
          label="Description"
          value={formData.description}
          onChangeText={(text) =>
            setFormData({ ...formData, description: text })
          }
          placeholder="What's this transfer for?"
          multiline
          numberOfLines={3}
          error={errors.description}
        />

        <Button
          title="Continue"
          onPress={handleTransfer}
          loading={isLoading}
          fullWidth
          style={styles.continueButton}
        />
      </Card>

      {/* Recent Recipients */}
      <View style={styles.recipientSection}>
        <Text style={[styles.sectionTitle, { color: colors.text }]}>
          Recent Recipients
        </Text>
        <View style={styles.recipientList}>
          <Text style={[styles.noRecipients, { color: colors.textSecondary }]}>
            No recent recipients
          </Text>
        </View>
      </View>
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
    fontSize: 28,
    fontWeight: '900',
    marginBottom: 24,
    letterSpacing: -1,
  },
  fieldGroup: {
    marginBottom: 20,
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
    marginLeft: 4,
  },
  pocketSelector: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#f9fafb',
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  pocketInfo: {
    flex: 1,
  },
  pocketName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#111827',
    marginBottom: 4,
  },
  pocketBalance: {
    fontSize: 14,
    color: '#6b7280',
  },
  bankSelector: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#f9fafb',
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
  },
  bankName: {
    fontSize: 16,
    fontWeight: '500',
  },
  dropdownIcon: {
    fontSize: 12,
    color: '#6b7280',
  },
  continueButton: {
    marginTop: 8,
  },
  recipientSection: {
    marginTop: 32,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 16,
  },
  recipientList: {
    flexDirection: 'row',
    gap: 12,
  },
  noRecipients: {
    fontSize: 14,
    fontStyle: 'italic',
  },
});
