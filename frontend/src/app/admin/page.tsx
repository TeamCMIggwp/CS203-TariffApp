export default function AdminPage() {
  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      {/* Header */}
      <header className="bg-blue-600 text-white p-4">
        <h1 className="text-xl font-bold">Admin Dashboard</h1>
      </header>

      <div className="flex flex-1">
        {/* Sidebar */}
        <aside className="w-64 bg-white shadow-md p-4">
          <nav className="space-y-2">
            <a href="#" className="block p-2 rounded hover:bg-gray-200">
              Users
            </a>
            <a href="#" className="block p-2 rounded hover:bg-gray-200">
              Settings
            </a>
            <a href="#" className="block p-2 rounded hover:bg-gray-200">
              Reports
            </a>
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 p-6">
          <div className="bg-white rounded shadow p-6">
            <h2 className="text-lg font-semibold mb-4">Welcome, Admin</h2>
            <p>This is a placeholder for your admin tools and analytics.</p>
          </div>
        </main>
      </div>
    </div>
  );
}