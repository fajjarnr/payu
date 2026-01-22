export default function BillsPage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
      <div className="max-w-2xl mx-auto bg-white dark:bg-gray-800 rounded-xl shadow p-8">
        <h1 className="text-3xl font-bold mb-6 text-gray-900 dark:text-white">Bill Payment</h1>
        
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          {['Pulsa', 'Listrik', 'PDAM', 'Internet'].map((item) => (
             <div key={item} className="flex flex-col items-center justify-center p-4 border rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer transition-colors dark:border-gray-700">
               <span className="font-medium text-gray-900 dark:text-gray-200">{item}</span>
             </div>
          ))}
        </div>

        <div className="border-t pt-6 dark:border-gray-700">
          <h2 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Recent Transactions</h2>
          <div className="text-sm text-gray-500 dark:text-gray-400 text-center py-8">
            No recent bill payments found.
          </div>
        </div>
      </div>
    </div>
  );
}
