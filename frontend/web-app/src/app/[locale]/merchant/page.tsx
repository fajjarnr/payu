'use client';

import { useEffect, useState } from 'react';
import { PartnerService, Partner } from '@/services/PartnerService';
import Link from 'next/link';

export default function MerchantDashboard() {
 const [partner, setPartner] = useState<Partner | null>(null);
 const [loading, setLoading] = useState(true);

 useEffect(() => {
  // In a real app, we'd get the partner ID from the logged-in user context
  // For demo, we'll try to fetch partner with ID 1
  const fetchPartner = async () => {
   try {
    const data = await PartnerService.getProfile(1);
    setPartner(data);
   } catch (error) {
    console.error('Failed to fetch partner', error);
   } finally {
    setLoading(false);
   }
  };

  fetchPartner();
 }, []);

 if (loading) return <div className="p-8">Loading...</div>;

 if (!partner) {
  return (
   <div className="p-8">
    <h1 className="text-2xl font-bold mb-4">Merchant Portal</h1>
    <p className="mb-4">You are not registered as a merchant yet (or ID 1 not found).</p>
    <Link 
     href="/merchant/register" 
     className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
    >
     Register as Merchant
    </Link>
   </div>
  );
 }

 return (
  <div className="p-8">
   <h1 className="text-2xl font-bold mb-6">Merchant Dashboard</h1>
   
   <div className="bg-white shadow rounded-lg p-6 mb-6">
    <h2 className="text-xl font-semibold mb-4">Profile</h2>
    <div className="grid grid-cols-2 gap-4">
     <div>
      <label className="block text-gray-500 text-sm">Merchant Name</label>
      <div className="font-medium">{partner.name}</div>
     </div>
     <div>
      <label className="block text-gray-500 text-sm">Email</label>
      <div className="font-medium">{partner.email}</div>
     </div>
     <div>
      <label className="block text-gray-500 text-sm">Type</label>
      <div className="font-medium">{partner.type}</div>
     </div>
     <div>
      <label className="block text-gray-500 text-sm">Status</label>
      <span className={`px-2 py-1 rounded text-sm ${partner.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
       {partner.active ? 'Active' : 'Inactive'}
      </span>
     </div>
    </div>
   </div>

   <div className="bg-white shadow rounded-lg p-6">
    <h2 className="text-xl font-semibold mb-4">API Credentials (SNAP BI)</h2>
    <div className="space-y-4">
     <div>
      <label className="block text-gray-500 text-sm">Client ID</label>
      <code className="block bg-gray-100 p-2 rounded mt-1">{partner.clientId || 'N/A'}</code>
     </div>
     {partner.clientSecret && (
       <div>
        <label className="block text-gray-500 text-sm">Client Secret</label>
        <code className="block bg-gray-100 p-2 rounded mt-1">{partner.clientSecret}</code>
        <p className="text-xs text-red-500 mt-1">Keep this secret safe! It won&apos;t be shown again.</p>
       </div>
     )}
     <div>
      <label className="block text-gray-500 text-sm">Public Key</label>
      <textarea 
       readOnly 
       className="w-full bg-gray-100 p-2 rounded mt-1 h-24 font-mono text-sm"
       value={partner.publicKey || 'No public key set'}
      />
     </div>
    </div>
   </div>
  </div>
 );
}
