'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BackofficeService, FraudCaseStatus } from '@/services';
import { useParams } from 'next/navigation';
import { useRouter } from '@/lib/navigation';
import { Button, Card, Descriptions, Flex, Input, Space, Spin, Typography } from 'antd';

export default function FraudCaseDetailPage() {
 const { id } = useParams() as { id: string };
 const router = useRouter();
 const queryClient = useQueryClient();
 const [decisionNotes, setDecisionNotes] = useState('');

 const { data: fraudCase, isLoading } = useQuery({
  queryKey: ['fraud-case', id],
  queryFn: () => BackofficeService.getFraudCase(id),
 });

 const mutation = useMutation({
  mutationFn: (status: FraudCaseStatus) =>
   BackofficeService.resolveFraudCase(id, { status, notes: decisionNotes }),
  onSuccess: () => {
   queryClient.invalidateQueries({ queryKey: ['fraud-cases'] });
   queryClient.invalidateQueries({ queryKey: ['fraud-case', id] });
   router.push('/backoffice/fraud');
  },
 });

 if (isLoading) {
  return <Spin data-testid="loading" />;
 }

 if (!fraudCase) {
  return <Typography.Text data-testid="not-found">Case not found</Typography.Text>;
 }

 return (
  <Space direction="vertical" size={24} style={{ width: '100%' }}>
   <Card
     title={<Typography.Title level={4} style={{ margin: 0 }}>Fraud Case Details</Typography.Title>}
     extra={<Typography.Text type="secondary">Case ID: {fraudCase.id}</Typography.Text>}
   >
    <Descriptions column={1} bordered>
     <Descriptions.Item label="User ID">{fraudCase.userId}</Descriptions.Item>
     <Descriptions.Item label="Account Number">{fraudCase.accountNumber}</Descriptions.Item>
     <Descriptions.Item label="Transaction ID">{fraudCase.transactionId}</Descriptions.Item>
     <Descriptions.Item label="Transaction Amount">{fraudCase.amount}</Descriptions.Item>
     <Descriptions.Item label="Risk Level">{fraudCase.riskLevel}</Descriptions.Item>
     <Descriptions.Item label="Fraud Type">{fraudCase.fraudType}</Descriptions.Item>
     <Descriptions.Item label="Description">{fraudCase.description}</Descriptions.Item>
     <Descriptions.Item label="Current Status">{fraudCase.status}</Descriptions.Item>
    </Descriptions>
   </Card>

   {(fraudCase.status === FraudCaseStatus.OPEN || fraudCase.status === FraudCaseStatus.UNDER_INVESTIGATION) && (
    <Card title={<Typography.Title level={4} style={{ margin: 0 }}>Resolve Case</Typography.Title>}>
     <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space direction="vertical" size={4} style={{ width: '100%' }}>
       <Typography.Text strong>Notes</Typography.Text>
       <Input.TextArea
        id="notes"
        rows={3}
        value={decisionNotes}
        onChange={(e) => setDecisionNotes(e.target.value)}
       />
      </Space>
      <Flex gap={12} wrap>
       <Button
        type="primary"
        onClick={() => mutation.mutate(FraudCaseStatus.UNDER_INVESTIGATION)}
        disabled={mutation.isPending}
       >
        Investigate
       </Button>
       <Button
        type="primary"
        onClick={() => mutation.mutate(FraudCaseStatus.RESOLVED)}
        disabled={mutation.isPending}
       >
        Resolve (Confirmed Fraud)
       </Button>
       <Button
        onClick={() => mutation.mutate(FraudCaseStatus.FALSE_POSITIVE)}
        disabled={mutation.isPending}
       >
        False Positive
       </Button>
       <Button
        onClick={() => mutation.mutate(FraudCaseStatus.CLOSED)}
        disabled={mutation.isPending}
       >
        Close
       </Button>
      </Flex>
     </Space>
    </Card>
   )}
  </Space>
 );
}
