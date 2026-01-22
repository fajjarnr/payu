export default function PocketsPage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">My Pockets</h1>
          <button className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors">
            + Create Pocket
          </button>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
           {/* Main Pocket */}
           <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white shadow-lg">
              <div className="text-blue-100 mb-1">Main Pocket</div>
              <div className="text-2xl font-bold mb-4">Rp 15.000.000</div>
              <div className="flex gap-2 text-sm">
                 <div className="bg-white/20 px-2 py-1 rounded">Daily</div>
                 <div className="bg-white/20 px-2 py-1 rounded">Active</div>
              </div>
           </div>

           {/* Saving Pocket Example */}
           <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow border border-gray-100 dark:border-gray-700">
              <div className="text-gray-500 dark:text-gray-400 mb-1">Holiday Trip</div>
              <div className="text-2xl font-bold text-gray-900 dark:text-white mb-4">Rp 2.500.000</div>
              <div className="w-full bg-gray-200 rounded-full h-2.5 dark:bg-gray-700 mb-2">
                <div className="bg-green-500 h-2.5 rounded-full" style={{ width: '25%' }}></div>
              </div>
              <div className="text-xs text-gray-500 dark:text-gray-400 text-right">Target: Rp 10.000.000</div>
           </div>

           {/* Emergency Fund Example */}
           <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow border border-gray-100 dark:border-gray-700">
              <div className="text-gray-500 dark:text-gray-400 mb-1">Emergency Fund</div>
              <div className="text-2xl font-bold text-gray-900 dark:text-white mb-4">Rp 50.000.000</div>
              <div className="flex gap-2">
                 <span className="text-xs bg-yellow-100 text-yellow-800 px-2 py-1 rounded">Locked</span>
              </div>
           </div>
        </div>
      </div>
    </div>
  );
}
