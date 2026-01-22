'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BackofficeService, FraudCaseStatus } from '@/services';
import { useRouter, useParams } from 'next/navigation';

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
    return <div>Loading...</div>;
  }

  if (!fraudCase) {
    return <div>Case not found</div>;
  }

  return (
    <div className="space-y-6">
      <div className="bg-white shadow overflow-hidden sm:rounded-lg">
        <div className="px-4 py-5 sm:px-6">
          <h3 className="text-lg leading-6 font-medium text-gray-900">Fraud Case Details</h3>
          <p className="mt-1 max-w-2xl text-sm text-gray-500">
            Case ID: {fraudCase.id}
          </p>
        </div>
        <div className="border-t border-gray-200">
          <dl>
            <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">User ID</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{fraudCase.userId}</dd>
            </div>
            <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Account Number</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{fraudCase.accountNumber}</dd>
            </div>
            <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Transaction ID</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{fraudCase.transactionId}</dd>
            </div>
             <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Transaction Amount</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{fraudCase.amount}</dd>
            </div>
            <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Risk Level</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{fraudCase.riskLevel}</dd>
            </div>
            <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Fraud Type</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{fraudCase.fraudType}</dd>
            </div>
             <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Description</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{fraudCase.description}</dd>
            </div>
             <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Current Status</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{fraudCase.status}</dd>
            </div>
          </dl>
        </div>
      </div>

      {(fraudCase.status === FraudCaseStatus.OPEN || fraudCase.status === FraudCaseStatus.UNDER_INVESTIGATION) && (
        <div className="bg-white shadow sm:rounded-lg p-6">
          <h4 className="text-lg font-medium text-gray-900 mb-4">Resolve Case</h4>
          <div className="mb-4">
            <label htmlFor="notes" className="block text-sm font-medium text-gray-700">
              Notes
            </label>
            <textarea
              id="notes"
              rows={3}
              className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              value={decisionNotes}
              onChange={(e) => setDecisionNotes(e.target.value)}
            />
          </div>
          <div className="flex space-x-3">
             <button
              onClick={() => mutation.mutate(FraudCaseStatus.UNDER_INVESTIGATION)}
              disabled={mutation.isPending}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Investigate
            </button>
            <button
              onClick={() => mutation.mutate(FraudCaseStatus.RESOLVED)}
              disabled={mutation.isPending}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
            >
              Resolve (Confirmed Fraud)
            </button>
            <button
              onClick={() => mutation.mutate(FraudCaseStatus.FALSE_POSITIVE)}
              disabled={mutation.isPending}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-gray-700 bg-gray-200 hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500"
            >
              False Positive
            </button>
             <button
              onClick={() => mutation.mutate(FraudCaseStatus.CLOSED)}
              disabled={mutation.isPending}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-gray-600 hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
