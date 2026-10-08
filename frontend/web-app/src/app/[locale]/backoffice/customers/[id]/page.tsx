'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BackofficeService, CustomerCaseStatus } from '@/services';
import { useParams } from 'next/navigation';
import { Button, Card, Descriptions, Flex, Input, Select, Space, Spin, Typography } from 'antd';

export default function CustomerCaseDetailPage() {
 const { id } = useParams() as { id: string };
 const queryClient = useQueryClient();
 const [updateNotes, setUpdateNotes] = useState('');
 const [newStatus, setNewStatus] = useState<CustomerCaseStatus | ''>('');

 const { data: customerCase, isLoading } = useQuery({
  queryKey: ['customer-case', id],
  queryFn: () => BackofficeService.getCustomerCase(id),
 });

 const mutation = useMutation({
  mutationFn: (status: CustomerCaseStatus) =>
   BackofficeService.updateCustomerCase(id, { status, notes: updateNotes }),
  onSuccess: () => {
   queryClient.invalidateQueries({ queryKey: ['customer-cases'] });
   queryClient.invalidateQueries({ queryKey: ['customer-case', id] });
   setUpdateNotes('');
   setNewStatus('');
   // Don't redirect, stay on page to see updates
  },
 });

 if (isLoading) {
  return <Spin data-testid="loading" />;
 }

 if (!customerCase) {
  return <Typography.Text data-testid="not-found">Case not found</Typography.Text>;
 }

 return (
  <Space direction="vertical" size={24} style={{ width: '100%' }}>
   <Card
     title={<Typography.Title level={4} style={{ margin: 0 }}>Customer Case: {customerCase.caseNumber}</Typography.Title>}
     extra={<Typography.Text type="secondary">{customerCase.subject}</Typography.Text>}
   >
    <Descriptions column={1} bordered>
     <Descriptions.Item label="User ID">{customerCase.userId}</Descriptions.Item>
     <Descriptions.Item label="Case Type">{customerCase.caseType}</Descriptions.Item>
     <Descriptions.Item label="Priority">{customerCase.priority}</Descriptions.Item>
     <Descriptions.Item label="Description"><Typography.Text style={{ whiteSpace: 'pre-wrap' }}>{customerCase.description}</Typography.Text></Descriptions.Item>
     <Descriptions.Item label="Current Status">{customerCase.status}</Descriptions.Item>
     <Descriptions.Item label="Latest Notes">{customerCase.notes}</Descriptions.Item>
    </Descriptions>
   </Card>

   <Card title={<Typography.Title level={4} style={{ margin: 0 }}>Update Case</Typography.Title>}>
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
     <Space direction="vertical" size={4} style={{ width: '100%' }}>
      <Typography.Text strong>New Status</Typography.Text>
      <Select
       id="status"
       style={{ width: '100%' }}
       value={newStatus || undefined}
       placeholder="Select Status"
       onChange={(value) => setNewStatus(value as CustomerCaseStatus)}
       options={Object.values(CustomerCaseStatus).map((s) => ({ value: s, label: s }))}
      />
     </Space>
     <Space direction="vertical" size={4} style={{ width: '100%' }}>
      <Typography.Text strong>Notes</Typography.Text>
      <Input.TextArea
       id="notes"
       rows={3}
       value={updateNotes}
       onChange={(e) => setUpdateNotes(e.target.value)}
      />
     </Space>
     <Flex>
      <Button
       type="primary"
       onClick={() => {
        if (newStatus) {
          mutation.mutate(newStatus as CustomerCaseStatus)
        }
       }}
       disabled={mutation.isPending || !newStatus}
      >
       Update Case
      </Button>
     </Flex>
    </Space>
   </Card>
  </Space>
 );
}
