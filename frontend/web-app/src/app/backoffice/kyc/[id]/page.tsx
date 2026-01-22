'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BackofficeService, BackofficeKycStatus } from '@/services';
import { useRouter, useParams } from 'next/navigation';

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
    return <div>Loading...</div>;
  }

  if (!review) {
    return <div>Review not found</div>;
  }

  return (
    <div className="space-y-6">
      <div className="bg-white shadow overflow-hidden sm:rounded-lg">
        <div className="px-4 py-5 sm:px-6">
          <h3 className="text-lg leading-6 font-medium text-gray-900">KYC Application Details</h3>
          <p className="mt-1 max-w-2xl text-sm text-gray-500">
            {review.fullName} - {review.userId}
          </p>
        </div>
        <div className="border-t border-gray-200">
          <dl>
            <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Document Type</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{review.documentType}</dd>
            </div>
            <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Document Number</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{review.documentNumber}</dd>
            </div>
            <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Address</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{review.address}</dd>
            </div>
            <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Phone Number</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{review.phoneNumber}</dd>
            </div>
             <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Document Image</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                 {/* In a real app, use next/image and handle auth/signed URLs */}
                 <a href={review.documentUrl} target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:text-blue-500">
                   View Document
                 </a>
              </dd>
            </div>
            <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Current Status</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{review.status}</dd>
            </div>
          </dl>
        </div>
      </div>

      {review.status === BackofficeKycStatus.PENDING && (
        <div className="bg-white shadow sm:rounded-lg p-6">
          <h4 className="text-lg font-medium text-gray-900 mb-4">Make Decision</h4>
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
              onClick={() => mutation.mutate(BackofficeKycStatus.APPROVED)}
              disabled={mutation.isPending}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
            >
              Approve
            </button>
            <button
              onClick={() => mutation.mutate(BackofficeKycStatus.REJECTED)}
              disabled={mutation.isPending}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
            >
              Reject
            </button>
             <button
              onClick={() => mutation.mutate(BackofficeKycStatus.REQUIRES_ADDITIONAL_INFO)}
              disabled={mutation.isPending}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-yellow-600 hover:bg-yellow-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500"
            >
              Request Info
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
