export default function OnboardingPage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
      <div className="max-w-2xl mx-auto bg-white dark:bg-gray-800 rounded-xl shadow p-8">
        <h1 className="text-3xl font-bold mb-6 text-gray-900 dark:text-white">Account Opening</h1>
        
        <div className="space-y-6">
          <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg">
            <h3 className="font-semibold text-blue-700 dark:text-blue-300 mb-2">Step 1: eKYC Verification</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">Please prepare your KTP and ensure good lighting for selfie.</p>
          </div>

          <form className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">NIK</label>
              <input 
                type="text" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
                placeholder="Enter 16 digit NIK"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Full Name</label>
              <input 
                type="text" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
                placeholder="As shown on KTP"
              />
            </div>

            <button className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors">
              Continue
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
