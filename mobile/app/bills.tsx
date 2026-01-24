import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  Image,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { formatCurrency } from '@/utils/currency';

interface Biller {
  id: string;
  name: string;
  category: string;
  logo: string;
  color: string;
}

interface BillerCategory {
  id: string;
  name: string;
  icon: string;
  billers: Biller[];
}

const BILLER_CATEGORIES: BillerCategory[] = [
  {
    id: 'electricity',
    name: 'Electricity',
    icon: '⚡',
    billers: [
      { id: 'pln', name: 'PLN', category: 'electricity', logo: '🏭', color: '#fbbf24' },
    ],
  },
  {
    id: 'water',
    name: 'Water',
    icon: '💧',
    billers: [
      { id: 'pdam', name: 'PDAM', category: 'water', logo: '🚿', color: '#3b82f6' },
    ],
  },
  {
    id: 'internet',
    name: 'Internet',
    icon: '📶',
    billers: [
      { id: 'indihome', name: 'IndiHome', category: 'internet', logo: '🏠', color: '#ef4444' },
      { id: 'first-media', name: 'First Media', category: 'internet', logo: '📺', color: '#8b5cf6' },
    ],
  },
  {
    id: 'mobile',
    name: 'Mobile',
    icon: '📱',
    billers: [
      { id: 'telkomsel', name: 'Telkomsel', category: 'mobile', logo: '🔴', color: '#ef4444' },
      { id: 'indosat', name: 'Indosat', category: 'mobile', logo: '🟡', color: '#fbbf24' },
      { id: 'xl', name: 'XL Axiata', category: 'mobile', logo: '🔵', color: '#3b82f6' },
    ],
  },
  {
    id: 'tv-cable',
    name: 'TV Cable',
    icon: '📺',
    billers: [
      { id: 'transvision', name: 'Transvision', category: 'tv-cable', logo: '🎬', color: '#10b981' },
      { id: 'k-vision', name: 'K-Vision', category: 'tv-cable', logo: '📡', color: '#f97316' },
    ],
  },
  {
    id: 'insurance',
    name: 'Insurance',
    icon: '🛡️',
    billers: [
      { id: 'bpjs', name: 'BPJS', category: 'insurance', logo: '🏥', color: '#10b981' },
    ],
  },
];

export default function BillsScreen() {
  const router = useRouter();
  const { colors } = useTheme();

  const [selectedCategory, setSelectedCategory] = useState<BillerCategory | null>(null);
  const [selectedBiller, setSelectedBiller] = useState<Biller | null>(null);
  const [customerId, setCustomerId] = useState('');
  const [amount, setAmount] = useState('');
  const [showInquiry, setShowInquiry] = useState(false);
  const [inquiryResult, setInquiryResult] = useState<any>(null);

  const handleCategoryPress = (category: BillerCategory) => {
    setSelectedCategory(category);
  };

  const handleBillerPress = (biller: Biller) => {
    setSelectedBiller(biller);
  };

  const handleInquiry = async () => {
    if (!selectedBiller || !customerId) {
      Alert.alert('Error', 'Please select a biller and enter customer ID');
      return;
    }

    // Simulate bill inquiry
    setInquiryResult({
      biller: selectedBiller.name,
      customerName: 'John Doe',
      customerId,
      amount: 150000,
      adminFee: 5000,
      period: 'January 2026',
      dueDate: '2026-01-25',
    });
    setShowInquiry(true);
  };

  const handlePayment = async () => {
    if (!inquiryResult) return;

    Alert.alert(
      'Confirm Payment',
      `Pay ${formatCurrency(inquiryResult.amount + inquiryResult.adminFee)} for ${inquiryResult.biller}?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Pay',
          onPress: () => {
            // Process payment
            Alert.alert('Success', 'Bill payment successful!');
            setShowInquiry(false);
            setSelectedBiller(null);
            setCustomerId('');
            setAmount('');
            setInquiryResult(null);
          },
        },
      ]
    );
  };

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
    >
      <Text style={[styles.title, { color: colors.text }]}>Bill Payments</Text>

      {/* Biller Categories */}
      {!selectedBiller && (
        <>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>
            Select Category
          </Text>
          <View style={styles.categoryGrid}>
            {BILLER_CATEGORIES.map((category) => (
              <TouchableOpacity
                key={category.id}
                style={[styles.categoryCard, { backgroundColor: colors.card }]}
                onPress={() => handleCategoryPress(category)}
                activeOpacity={0.7}
              >
                <Text style={styles.categoryIcon}>{category.icon}</Text>
                <Text style={[styles.categoryName, { color: colors.text }]}>
                  {category.name}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          {/* Selected Category Billers */}
          {selectedCategory && (
            <>
              <Text style={[styles.sectionTitle, { color: colors.text }]}>
                {selectedCategory.name} Billers
              </Text>
              {selectedCategory.billers.map((biller) => (
                <TouchableOpacity
                  key={biller.id}
                  style={[styles.billerCard, { backgroundColor: colors.card }]}
                  onPress={() => handleBillerPress(biller)}
                  activeOpacity={0.7}
                >
                  <View style={[styles.billerLogo, { backgroundColor: biller.color }]}>
                    <Text style={styles.billerLogoText}>{biller.logo}</Text>
                  </View>
                  <View style={styles.billerInfo}>
                    <Text style={[styles.billerName, { color: colors.text }]}>
                      {biller.name}
                    </Text>
                    <Text style={[styles.billerCategory, { color: colors.textSecondary }]}>
                      {selectedCategory.name}
                    </Text>
                  </View>
                  <Text style={[styles.billerArrow, { color: colors.textSecondary }]}>
                    ›
                  </Text>
                </TouchableOpacity>
              ))}
            </>
          )}
        </>
      )}

      {/* Bill Payment Form */}
      {selectedBiller && !showInquiry && (
        <Card padding="lg">
          <View style={styles.selectedBillerHeader}>
            <View style={[styles.selectedBillerLogo, { backgroundColor: selectedBiller.color }]}>
              <Text style={styles.billerLogoText}>{selectedBiller.logo}</Text>
            </View>
            <View style={styles.selectedBillerInfo}>
              <Text style={[styles.billerName, { color: colors.text }]}>
                {selectedBiller.name}
              </Text>
              <TouchableOpacity onPress={() => setSelectedBiller(null)}>
                <Text style={styles.changeText}>Change</Text>
              </TouchableOpacity>
            </View>
          </View>

          <Input
            label="Customer ID"
            value={customerId}
            onChangeText={setCustomerId}
            placeholder="Enter customer ID"
            keyboardType="default"
            autoCapitalize="characters"
          />

          <Button
            title="Check Bill"
            onPress={handleInquiry}
            fullWidth
            style={styles.checkButton}
          />
        </Card>
      )}

      {/* Inquiry Result */}
      {showInquiry && inquiryResult && (
        <Card padding="lg">
          <View style={styles.inquiryHeader}>
            <Text style={[styles.inquiryTitle, { color: colors.text }]}>
              Bill Details
            </Text>
            <TouchableOpacity onPress={() => setShowInquiry(false)}>
              <Text style={styles.changeText}>Edit</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.inquiryDetails}>
            <View style={styles.detailRow}>
              <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
                Biller
              </Text>
              <Text style={[styles.detailValue, { color: colors.text }]}>
                {inquiryResult.biller}
              </Text>
            </View>

            <View style={styles.detailRow}>
              <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
                Customer Name
              </Text>
              <Text style={[styles.detailValue, { color: colors.text }]}>
                {inquiryResult.customerName}
              </Text>
            </View>

            <View style={styles.detailRow}>
              <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
                Customer ID
              </Text>
              <Text style={[styles.detailValue, { color: colors.text }]}>
                {inquiryResult.customerId}
              </Text>
            </View>

            <View style={styles.detailRow}>
              <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
                Period
              </Text>
              <Text style={[styles.detailValue, { color: colors.text }]}>
                {inquiryResult.period}
              </Text>
            </View>

            <View style={styles.detailRow}>
              <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
                Due Date
              </Text>
              <Text style={[styles.detailValue, { color: colors.text }]}>
                {inquiryResult.dueDate}
              </Text>
            </View>

            <View style={[styles.divider, { borderBottomColor: colors.border }]} />

            <View style={styles.detailRow}>
              <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
                Bill Amount
              </Text>
              <Text style={[styles.detailValue, { color: colors.text }]}>
                {formatCurrency(inquiryResult.amount)}
              </Text>
            </View>

            <View style={styles.detailRow}>
              <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
                Admin Fee
              </Text>
              <Text style={[styles.detailValue, { color: colors.text }]}>
                {formatCurrency(inquiryResult.adminFee)}
              </Text>
            </View>

            <View style={[styles.divider, { borderBottomColor: colors.border }]} />

            <View style={styles.detailRow}>
              <Text style={[styles.detailLabel, { color: colors.textSecondary }]}>
                Total
              </Text>
              <Text style={[styles.totalValue, { color: '#10b981' }]}>
                {formatCurrency(inquiryResult.amount + inquiryResult.adminFee)}
              </Text>
            </View>
          </View>

          <Button
            title="Pay Now"
            onPress={handlePayment}
            fullWidth
            style={styles.payButton}
          />
        </Card>
      )}
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
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 16,
  },
  categoryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginHorizontal: -8,
    marginBottom: 24,
  },
  categoryCard: {
    width: (100 - 4) / 3 + '%',
    aspectRatio: 1,
    marginHorizontal: 8,
    marginBottom: 16,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  categoryIcon: {
    fontSize: 32,
    marginBottom: 8,
  },
  categoryName: {
    fontSize: 12,
    fontWeight: '600',
    textAlign: 'center',
  },
  billerCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  billerLogo: {
    width: 48,
    height: 48,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  billerLogoText: {
    fontSize: 24,
  },
  billerInfo: {
    flex: 1,
  },
  billerName: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 2,
  },
  billerCategory: {
    fontSize: 12,
  },
  billerArrow: {
    fontSize: 20,
    fontWeight: '300',
  },
  selectedBillerHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
    paddingBottom: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  selectedBillerLogo: {
    width: 56,
    height: 56,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  selectedBillerInfo: {
    flex: 1,
  },
  changeText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#10b981',
    marginTop: 4,
  },
  checkButton: {
    marginTop: 16,
  },
  inquiryHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  inquiryTitle: {
    fontSize: 20,
    fontWeight: '700',
  },
  inquiryDetails: {
    marginBottom: 20,
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
    marginVertical: 8,
  },
  payButton: {
    marginTop: 8,
  },
});
