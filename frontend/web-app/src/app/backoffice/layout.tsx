'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

export default function BackofficeLayout({
 children,
}: {
 children: React.ReactNode;
}) {
 const pathname = usePathname();

 const navigation = [
  { name: 'Dashboard', href: '/backoffice' },
  { name: 'KYC Reviews', href: '/backoffice/kyc' },
  { name: 'Fraud Monitoring', href: '/backoffice/fraud' },
  { name: 'Customer Operations', href: '/backoffice/customers' },
 ];

 return (
  <div className="min-h-screen bg-gray-100 flex">
   <div className="w-64 bg-white shadow-md flex flex-col">
    <div className="p-4 border-b">
     <h1 className="text-xl font-bold text-gray-800">Backoffice</h1>
    </div>
    <nav className="flex-1 p-4 space-y-1">
     {navigation.map((item) => {
      const isActive = pathname === item.href;
      return (
       <Link
        key={item.name}
        href={item.href}
        className={`flex items-center px-4 py-2 text-sm font-medium rounded-md ${
         isActive
          ? 'bg-blue-50 text-blue-700'
          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
        }`}
       >
        {item.name}
       </Link>
      );
     })}
    </nav>
   </div>
   <div className="flex-1 overflow-auto">
    <header className="bg-white shadow-sm">
     <div className="px-6 py-4">
      <h2 className="text-lg font-medium text-gray-800">
       {navigation.find((item) => item.href === pathname)?.name || 'Dashboard'}
      </h2>
     </div>
    </header>
    <main className="p-6">{children}</main>
   </div>
  </div>
 );
}
