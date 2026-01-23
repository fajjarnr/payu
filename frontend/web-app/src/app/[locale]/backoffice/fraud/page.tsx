'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, FraudCaseStatus, FraudRiskLevel } from '@/services';
import Link from 'next/link';

export default function FraudCasesPage() {
 const [status, setStatus] = useState<string>('');
 const [riskLevel, setRiskLevel] = useState<string>('');
 const [page, setPage] = useState(0);

 const { data: cases, isLoading } = useQuery({
  queryKey: ['fraud-cases', status, riskLevel, page],
  queryFn: () => BackofficeService.getFraudCases(status || undefined, riskLevel || undefined, page),
 });

 return (
  <div className="space-y-6">
   <div className="flex justify-between items-center">
    <h2 className="text-2xl font-bold text-gray-800">Fraud Cases</h2>
    <div className="flex space-x-4">
     <select
      value={riskLevel}
      onChange={(e) => setRiskLevel(e.target.value)}
      className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
     >
      <option value="">All Risks</option>
       {Object.values(FraudRiskLevel).map((s) => (
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
      {Object.values(FraudCaseStatus).map((s) => (
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
        Risk
       </th>
       <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 tracking-wider">
        Type
       </th>
       <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 tracking-wider">
        Amount
       </th>
        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 tracking-wider">
        Status
       </th>
        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 tracking-wider">
        Created At
       </th>
       <th scope="col" className="relative px-6 py-3">
        <span className="sr-only">Details</span>
       </th>
      </tr>
     </thead>
     <tbody className="bg-white divide-y divide-gray-200">
      {isLoading ? (
       <tr>
        <td colSpan={6} className="px-6 py-4 text-center text-sm text-gray-500">
         Loading...
        </td>
       </tr>
      ) : cases?.length === 0 ? (
       <tr>
        <td colSpan={6} className="px-6 py-4 text-center text-sm text-gray-500">
         No cases found
        </td>
       </tr>
      ) : (
       cases?.map((c) => (
        <tr key={c.id}>
         <td className="px-6 py-4 whitespace-nowrap">
           <span
           className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
            c.riskLevel === FraudRiskLevel.CRITICAL
             ? 'bg-red-100 text-red-800'
             : c.riskLevel === FraudRiskLevel.HIGH
             ? 'bg-orange-100 text-orange-800'
             : c.riskLevel === FraudRiskLevel.MEDIUM
             ? 'bg-yellow-100 text-yellow-800'
             : 'bg-green-100 text-green-800'
           }`}
          >
           {c.riskLevel}
          </span>
         </td>
         <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
          {c.fraudType}
         </td>
         <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
          {c.amount}
         </td>
          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
          {c.status}
         </td>
         <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
          {new Date(c.createdAt).toLocaleDateString()}
         </td>
         <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
          <Link href={`/backoffice/fraud/${c.id}`} className="text-blue-600 hover:text-blue-900">
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
