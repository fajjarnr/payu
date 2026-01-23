'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BackofficeService, CustomerCaseStatus } from '@/services';
import { useParams } from 'next/navigation';

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
  return <div>Loading...</div>;
 }

 if (!customerCase) {
  return <div>Case not found</div>;
 }

 return (
  <div className="space-y-6">
   <div className="bg-white shadow overflow-hidden sm:rounded-lg">
    <div className="px-4 py-5 sm:px-6">
     <h3 className="text-lg leading-6 font-medium text-gray-900">Customer Case: {customerCase.caseNumber}</h3>
     <p className="mt-1 max-w-2xl text-sm text-gray-500">
      {customerCase.subject}
     </p>
    </div>
    <div className="border-t border-gray-200">
     <dl>
      <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-gray-500">User ID</dt>
       <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{customerCase.userId}</dd>
      </div>
       <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-gray-500">Case Type</dt>
       <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{customerCase.caseType}</dd>
      </div>
       <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-gray-500">Priority</dt>
       <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{customerCase.priority}</dd>
      </div>
       <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-gray-500">Description</dt>
       <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2 whitespace-pre-wrap">{customerCase.description}</dd>
      </div>
      <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-gray-500">Current Status</dt>
       <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{customerCase.status}</dd>
      </div>
       <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-gray-500">Latest Notes</dt>
       <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{customerCase.notes}</dd>
      </div>
     </dl>
    </div>
   </div>

   <div className="bg-white shadow sm:rounded-lg p-6">
    <h4 className="text-lg font-medium text-gray-900 mb-4">Update Case</h4>
     <div className="mb-4">
      <label htmlFor="status" className="block text-sm font-medium text-gray-700">
       New Status
      </label>
      <select
       id="status"
       className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
       value={newStatus}
       onChange={(e) => setNewStatus(e.target.value as CustomerCaseStatus)}
      >
       <option value="">Select Status</option>
       {Object.values(CustomerCaseStatus).map((s) => (
        <option key={s} value={s}>
         {s}
        </option>
       ))}
      </select>
     </div>
    <div className="mb-4">
     <label htmlFor="notes" className="block text-sm font-medium text-gray-700">
      Notes
     </label>
     <textarea
      id="notes"
      rows={3}
      className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
      value={updateNotes}
      onChange={(e) => setUpdateNotes(e.target.value)}
     />
    </div>
    <button
     onClick={() => {
      if (newStatus) {
        mutation.mutate(newStatus as CustomerCaseStatus)
      }
     }}
     disabled={mutation.isPending || !newStatus}
     className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-blue-300"
    >
     Update Case
    </button>
   </div>
  </div>
 );
}
