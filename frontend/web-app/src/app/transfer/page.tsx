export default function TransferPage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
      <div className="max-w-2xl mx-auto bg-white dark:bg-gray-800 rounded-xl shadow p-8">
        <h1 className="text-3xl font-bold mb-6 text-gray-900 dark:text-white">Transfer</h1>
        
        <div className="space-y-6">
          <div className="flex gap-4 mb-8">
             <button className="flex-1 py-2 px-4 rounded-md bg-blue-100 text-blue-700 font-medium">PayU Transfer</button>
             <button className="flex-1 py-2 px-4 rounded-md text-gray-600 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700">Bank Transfer</button>
          </div>

          <form className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Recipient Account</label>
              <input 
                type="text" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
                placeholder="Account number / Phone"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Amount</label>
              <input 
                type="number" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
                placeholder="Rp 0"
              />
            </div>

            <div>
               <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Note (Optional)</label>
               <input 
                type="text" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
              />
            </div>

            <button className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors">
              Review Transfer
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
