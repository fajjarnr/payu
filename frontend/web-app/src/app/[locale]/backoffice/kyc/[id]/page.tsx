'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BackofficeService, BackofficeKycStatus } from '@/services';
import { useParams } from 'next/navigation';
import { useRouter } from '@/lib/navigation';
import { Button, Card, Descriptions, Flex, Input, Space, Spin, Typography } from 'antd';

export default function KycReviewDetailPage() {
 const { id } = useParams() as { id: string };
 const router = useRouter();
 const queryClient = useQueryClient();
 const [decisionNotes, setDecisionNotes] = useState('');

 const { data: review, isLoading } = useQuery({
  queryKey: ['kyc-review', id],
  queryFn: () => BackofficeService.getKycReview(id),
 });

 const mutation = useMutation({
  mutationFn: (status: BackofficeKycStatus) =>
   BackofficeService.reviewKyc(id, { status, notes: decisionNotes }),
  onSuccess: () => {
   queryClient.invalidateQueries({ queryKey: ['kyc-reviews'] });
   queryClient.invalidateQueries({ queryKey: ['kyc-review', id] });
   router.push('/backoffice/kyc');
  },
 });

 if (isLoading) {
  return <Spin data-testid="loading" />;
 }

 if (!review) {
  return <Typography.Text data-testid="not-found">Review not found</Typography.Text>;
 }

 return (
  <Space direction="vertical" size={24} style={{ width: '100%' }}>
   <Card
     title={<Typography.Title level={4} style={{ margin: 0 }}>KYC Application Details</Typography.Title>}
     extra={<Typography.Text type="secondary">{review.fullName} - {review.userId}</Typography.Text>}
   >
    <Descriptions column={1} bordered>
     <Descriptions.Item label="Document Type">{review.documentType}</Descriptions.Item>
     <Descriptions.Item label="Document Number">{review.documentNumber}</Descriptions.Item>
     <Descriptions.Item label="Address">{review.address}</Descriptions.Item>
     <Descriptions.Item label="Phone Number">{review.phoneNumber}</Descriptions.Item>
     <Descriptions.Item label="Document Image">
       {/* In a real app, use next/image and handle auth/signed URLs */}
       <a href={review.documentUrl} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--ant-color-primary)' }}>
        View Document
       </a>
     </Descriptions.Item>
     <Descriptions.Item label="Current Status">{review.status}</Descriptions.Item>
    </Descriptions>
   </Card>

   {review.status === BackofficeKycStatus.PENDING && (
    <Card title={<Typography.Title level={4} style={{ margin: 0 }}>Make Decision</Typography.Title>}>
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
        onClick={() => mutation.mutate(BackofficeKycStatus.APPROVED)}
        disabled={mutation.isPending}
       >
        Approve
       </Button>
       <Button
        danger
        onClick={() => mutation.mutate(BackofficeKycStatus.REJECTED)}
        disabled={mutation.isPending}
       >
        Reject
       </Button>
       <Button
        onClick={() => mutation.mutate(BackofficeKycStatus.REQUIRES_ADDITIONAL_INFO)}
        disabled={mutation.isPending}
       >
        Request Info
       </Button>
      </Flex>
     </Space>
    </Card>
   )}
  </Space>
 );
}
