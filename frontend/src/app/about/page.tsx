export default function AboutPage() {
  const title = "About";
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500 text-white p-4">
      <div className="text-center max-w-md">
        <h1 className="text-6xl font-extrabold mb-4 animate-pulse">🚀</h1>
        <h2 className="text-4xl font-bold mb-2">{title}</h2>
        <p className="text-lg text-white/80">
          We&apos;re working hard to bring this feature to you. Stay tuned!
        </p>
        <div className="mt-6">
          <div className="inline-block rounded-full bg-white/20 px-6 py-3 text-lg font-semibold hover:bg-white/30 transition">
            Coming Soon
          </div>
        </div>
      </div>
    </div>
  );
}