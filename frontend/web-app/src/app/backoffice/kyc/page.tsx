'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, BackofficeKycStatus } from '@/services';
import Link from 'next/link';

export default function KycReviewsPage() {
  const [status, setStatus] = useState<string>('');
  const [page, setPage] = useState(0);

  const { data: reviews, isLoading } = useQuery({
    queryKey: ['kyc-reviews', status, page],
    queryFn: () => BackofficeService.getKycReviews(status || undefined, page),
  });

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-800">KYC Reviews</h2>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
        >
          <option value="">All Statuses</option>
          {Object.values(BackofficeKycStatus).map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      <div className="bg-white shadow overflow-hidden sm:rounded-lg">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                User
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Document
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Submitted At
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th scope="col" className="relative px-6 py-3">
                <span className="sr-only">Review</span>
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {isLoading ? (
              <tr>
                <td colSpan={5} className="px-6 py-4 text-center text-sm text-gray-500">
                  Loading...
                </td>
              </tr>
            ) : reviews?.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-6 py-4 text-center text-sm text-gray-500">
                  No reviews found
                </td>
              </tr>
            ) : (
              reviews?.map((review) => (
                <tr key={review.id}>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{review.fullName}</div>
                    <div className="text-sm text-gray-500">{review.userId}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{review.documentType}</div>
                    <div className="text-sm text-gray-500">{review.documentNumber}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {new Date(review.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        review.status === BackofficeKycStatus.APPROVED
                          ? 'bg-green-100 text-green-800'
                          : review.status === BackofficeKycStatus.REJECTED
                          ? 'bg-red-100 text-red-800'
                          : review.status === BackofficeKycStatus.PENDING
                          ? 'bg-yellow-100 text-yellow-800'
                          : 'bg-gray-100 text-gray-800'
                      }`}
                    >
                      {review.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <Link href={`/backoffice/kyc/${review.id}`} className="text-blue-600 hover:text-blue-900">
                      Review
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <div className="px-6 py-4 border-t border-gray-200 flex justify-between">
           <button 
             onClick={() => setPage(p => Math.max(0, p - 1))}
             disabled={page === 0}
             className="px-3 py-1 border rounded text-sm disabled:opacity-50"
           >
             Previous
           </button>
           <span className="text-sm text-gray-600 self-center">Page {page + 1}</span>
           <button 
             onClick={() => setPage(p => p + 1)}
             disabled={reviews && reviews.length < 20}
             className="px-3 py-1 border rounded text-sm disabled:opacity-50"
           >
             Next
           </button>
        </div>
      </div>
    </div>
  );
}
