'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, CustomerCaseStatus, CustomerCasePriority } from '@/services';
import Link from 'next/link';

export default function CustomerCasesPage() {
 const [status, setStatus] = useState<string>('');
 const [priority, setPriority] = useState<string>('');
 const [page, setPage] = useState(0);

 const { data: cases, isLoading } = useQuery({
  queryKey: ['customer-cases', status, priority, page],
  queryFn: () => BackofficeService.getCustomerCases(status || undefined, priority || undefined, page),
 });

 return (
  <div className="space-y-6">
   <div className="flex justify-between items-center">
    <h2 className="text-2xl font-bold text-gray-800">Customer Operations</h2>
    <div className="flex space-x-4">
      <select
      value={priority}
      onChange={(e) => setPriority(e.target.value)}
      className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
     >
      <option value="">All Priorities</option>
      {Object.values(CustomerCasePriority).map((s) => (
       <option key={s} value={s}>
        {s}
       </option>
      ))}
     </select>
     <select
      value={status}
      onChange={(e) => setStatus(e.target.value)}
      className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
     >
      <option value="">All Statuses</option>
      {Object.values(CustomerCaseStatus).map((s) => (
       <option key={s} value={s}>
        {s}
       </option>
      ))}
     </select>
    </div>
   </div>

   <div className="bg-white shadow overflow-hidden sm:rounded-lg">
    <table className="min-w-full divide-y divide-gray-200">
     <thead className="bg-gray-50">
      <tr>
       <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 tracking-wider">
        Case #
       </th>
        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 tracking-wider">
        Subject
       </th>
        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 tracking-wider">
        Priority
       </th>
       <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 tracking-wider">
        Status
       </th>
       <th scope="col" className="relative px-6 py-3">
        <span className="sr-only">Details</span>
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
      ) : cases?.length === 0 ? (
       <tr>
        <td colSpan={5} className="px-6 py-4 text-center text-sm text-gray-500">
         No cases found
        </td>
       </tr>
      ) : (
       cases?.map((c) => (
        <tr key={c.id}>
         <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
          {c.caseNumber}
         </td>
         <td className="px-6 py-4 whitespace-nowrap">
          <div className="text-sm font-medium text-gray-900">{c.subject}</div>
          <div className="text-sm text-gray-500">{c.userId}</div>
         </td>
          <td className="px-6 py-4 whitespace-nowrap">
           <span
           className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
            c.priority === CustomerCasePriority.URGENT
             ? 'bg-red-100 text-red-800'
             : c.priority === CustomerCasePriority.HIGH
             ? 'bg-orange-100 text-orange-800'
             : 'bg-green-100 text-green-800'
           }`}
          >
           {c.priority}
          </span>
         </td>
         <td className="px-6 py-4 whitespace-nowrap">
          <span
           className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
            c.status === CustomerCaseStatus.OPEN
             ? 'bg-blue-100 text-blue-800'
             : c.status === CustomerCaseStatus.RESOLVED
             ? 'bg-green-100 text-green-800'
             : 'bg-gray-100 text-gray-800'
           }`}
          >
           {c.status}
          </span>
         </td>
         <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
          <Link href={`/backoffice/customers/${c.id}`} className="text-blue-600 hover:text-blue-900">
           View
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
       disabled={cases && cases.length < 20}
       className="px-3 py-1 border rounded text-sm disabled:opacity-50"
      >
       Next
      </button>
    </div>
   </div>
  </div>
 );
}
