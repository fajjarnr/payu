import Link from 'next/link';

export default function BackofficeDashboard() {
 return (
  <div className="space-y-6">
   <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
     <h3 className="text-lg font-medium text-gray-900">KYC Reviews</h3>
     <p className="mt-2 text-sm text-gray-500">
      Review pending customer verifications and documents.
     </p>
     <Link
      href="/backoffice/kyc"
      className="mt-4 inline-flex items-center text-sm font-medium text-blue-600 hover:text-blue-500"
     >
      Go to KYC Reviews &rarr;
     </Link>
    </div>
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
     <h3 className="text-lg font-medium text-gray-900">Fraud Monitoring</h3>
     <p className="mt-2 text-sm text-gray-500">
      Investigate suspicious transactions and high-risk activities.
     </p>
     <Link
      href="/backoffice/fraud"
      className="mt-4 inline-flex items-center text-sm font-medium text-blue-600 hover:text-blue-500"
     >
      Go to Fraud Monitoring &rarr;
     </Link>
    </div>
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
     <h3 className="text-lg font-medium text-gray-900">Customer Operations</h3>
     <p className="mt-2 text-sm text-gray-500">
      Manage customer support cases and inquiries.
     </p>
     <Link
      href="/backoffice/customers"
      className="mt-4 inline-flex items-center text-sm font-medium text-blue-600 hover:text-blue-500"
     >
      Go to Customer Operations &rarr;
     </Link>
    </div>
   </div>
  </div>
 );
}
