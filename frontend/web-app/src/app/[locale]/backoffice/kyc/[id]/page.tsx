'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BackofficeService, BackofficeKycStatus } from '@/services';
import { useParams } from 'next/navigation';
import { useRouter } from '@/lib/navigation';

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
   <div className="bg-surface shadow overflow-hidden sm:rounded-lg">
    <div className="px-4 py-5 sm:px-6">
     <h3 className="text-lg leading-6 font-medium text-text-primary">KYC Application Details</h3>
     <p className="mt-1 max-w-2xl text-sm text-text-secondary">
      {review.fullName} - {review.userId}
     </p>
    </div>
    <div className="border-t border-border">
     <dl>
      <div className="bg-surface-dim px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-text-secondary">Document Type</dt>
       <dd className="mt-1 text-sm text-text-primary sm:mt-0 sm:col-span-2">{review.documentType}</dd>
      </div>
      <div className="bg-surface px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-text-secondary">Document Number</dt>
       <dd className="mt-1 text-sm text-text-primary sm:mt-0 sm:col-span-2">{review.documentNumber}</dd>
      </div>
      <div className="bg-surface-dim px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-text-secondary">Address</dt>
       <dd className="mt-1 text-sm text-text-primary sm:mt-0 sm:col-span-2">{review.address}</dd>
      </div>
      <div className="bg-surface px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-text-secondary">Phone Number</dt>
       <dd className="mt-1 text-sm text-text-primary sm:mt-0 sm:col-span-2">{review.phoneNumber}</dd>
      </div>
       <div className="bg-surface-dim px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-text-secondary">Document Image</dt>
       <dd className="mt-1 text-sm text-text-primary sm:mt-0 sm:col-span-2">
         {/* In a real app, use next/image and handle auth/signed URLs */}
         <a href={review.documentUrl} target="_blank" rel="noopener noreferrer" className="text-primary-dark hover:text-primary">
          View Document
         </a>
       </dd>
      </div>
      <div className="bg-surface px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
       <dt className="text-sm font-medium text-text-secondary">Current Status</dt>
       <dd className="mt-1 text-sm text-text-primary sm:mt-0 sm:col-span-2">{review.status}</dd>
      </div>
     </dl>
    </div>
   </div>

   {review.status === BackofficeKycStatus.PENDING && (
    <div className="bg-surface shadow sm:rounded-lg p-6">
     <h4 className="text-lg font-medium text-text-primary mb-4">Make Decision</h4>
     <div className="mb-4">
      <label htmlFor="notes" className="block text-sm font-medium text-text-primary">
       Notes
      </label>
      <textarea
       id="notes"
       rows={3}
       className="mt-1 block w-full rounded-md border-border shadow-sm focus:border-primary focus:ring-primary sm:text-sm"
       value={decisionNotes}
       onChange={(e) => setDecisionNotes(e.target.value)}
      />
     </div>
     <div className="flex space-x-3">
      <button
       onClick={() => mutation.mutate(BackofficeKycStatus.APPROVED)}
       disabled={mutation.isPending}
       className="inline-flex items-center min-h-[44px] px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-surface bg-success hover:bg-success focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-success"
      >
       Approve
      </button>
      <button
       onClick={() => mutation.mutate(BackofficeKycStatus.REJECTED)}
       disabled={mutation.isPending}
       className="inline-flex items-center min-h-[44px] px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-surface bg-error hover:bg-error focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-error"
      >
       Reject
      </button>
       <button
       onClick={() => mutation.mutate(BackofficeKycStatus.REQUIRES_ADDITIONAL_INFO)}
       disabled={mutation.isPending}
       className="inline-flex items-center min-h-[44px] px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-surface bg-warning hover:bg-warning focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-warning"
      >
       Request Info
      </button>
     </div>
    </div>
   )}
  </div>
 );
}
