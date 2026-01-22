import Link from "next/link";

export default function Home() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
      <main className="max-w-4xl mx-auto">
        <header className="mb-12 text-center">
          <h1 className="text-4xl font-bold text-blue-600 mb-4">PayU</h1>
          <p className="text-xl text-gray-600 dark:text-gray-300">
            Your Digital Banking Partner
          </p>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Link
            href="/onboarding"
            className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow-sm hover:shadow-md transition-shadow border border-gray-100 dark:border-gray-700"
          >
            <h2 className="text-2xl font-semibold mb-2 text-gray-900 dark:text-white">
              Open Account
            </h2>
            <p className="text-gray-500 dark:text-gray-400">
              Start your journey with eKYC registration
            </p>
          </Link>

          <Link
            href="/transfer"
            className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow-sm hover:shadow-md transition-shadow border border-gray-100 dark:border-gray-700"
          >
            <h2 className="text-2xl font-semibold mb-2 text-gray-900 dark:text-white">
              Transfer
            </h2>
            <p className="text-gray-500 dark:text-gray-400">
              Send money to PayU or other banks
            </p>
          </Link>

          <Link
            href="/bills"
            className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow-sm hover:shadow-md transition-shadow border border-gray-100 dark:border-gray-700"
          >
            <h2 className="text-2xl font-semibold mb-2 text-gray-900 dark:text-white">
              Pay Bills
            </h2>
            <p className="text-gray-500 dark:text-gray-400">
              Top up data, pay electricity and more
            </p>
          </Link>

          <Link
            href="/pockets"
            className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow-sm hover:shadow-md transition-shadow border border-gray-100 dark:border-gray-700"
          >
            <h2 className="text-2xl font-semibold mb-2 text-gray-900 dark:text-white">
              Pockets
            </h2>
            <p className="text-gray-500 dark:text-gray-400">
              Manage your savings and goals
            </p>
          </Link>
        </div>
      </main>
    </div>
  );
}
