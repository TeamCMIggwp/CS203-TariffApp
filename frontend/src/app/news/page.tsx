'use client';

import { IconNews, IconDatabase, IconAlertCircle, IconSearch, IconCalendar, IconListNumbers } from "@tabler/icons-react";
import { useState } from "react";

// TypeScript interfaces based on your backend
interface ScrapedData {
  url: string;
  title: string;
  sourceDomain: string;
  relevantText: string[];
  extractedRate: number | null;
  publishDate: string | null;
}

interface ScrapeResult {
  query: string;
  startTime: string;
  endTime: string;
  completed: boolean;
  totalSourcesFound: number;
  sourcesScraped: number;
  data: ScrapedData[];
  errors: { [key: string]: string };
}

export default function NewsPage() {
  const [query, setQuery] = useState("rice tariff");
  const [maxResults, setMaxResults] = useState(10);
  const [minYear, setMinYear] = useState(2024);
  const [newsData, setNewsData] = useState<ScrapeResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  const fetchNewsData = async (searchQuery: string, max: number, year: number) => {
    setLoading(true);
    setError(false);

    try {
      const response = await fetch('https://teamcmiggwp.duckdns.org/api/scraper/search-and-scrape', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          query: searchQuery,
          maxResults: max,
          minYear: year
        }),
        cache: 'no-store'
      });

      if (!response.ok) {
        console.error('Failed to fetch news:', response.status);
        setError(true);
        setLoading(false);
        return;
      }

      const data = await response.json();
      setNewsData(data);
    } catch (err) {
      console.error('Error fetching news:', err);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      fetchNewsData(query.trim(), maxResults, minYear);
    }
  };

  // Error state
  if (error || (newsData && !newsData.completed)) {
    return (
      <section className="py-20 min-h-screen relative z-10">
        <div className="flex items-center justify-center min-h-[60vh]">
          <div className="text-center bg-black/40 backdrop-blur-lg p-8 rounded-2xl border border-white/30">
            <IconAlertCircle className="w-16 h-16 text-red-400 mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-white mb-2">Failed to Load News</h2>
            <p className="text-white/90">Unable to fetch tariff news at this time</p>
            <button
              onClick={() => setError(false)}
              className="mt-4 bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 font-bold px-6 py-2 rounded-lg border border-cyan-400/40"
            >
              Try Again
            </button>
          </div>
        </div>
      </section>
    );
  }

  // No data state
  if (newsData && newsData.data.length === 0) {
    return (
      <section className="py-20 min-h-screen relative z-10">
        <div className="flex items-center justify-center min-h-[60vh]">
          <div className="text-center bg-black/40 backdrop-blur-lg p-8 rounded-2xl border border-white/30">
            <IconNews className="w-16 h-16 text-white/70 mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-white mb-2">No News Found</h2>
            <p className="text-white/90">No tariff news articles available for this query</p>
          </div>
        </div>
      </section>
    );
  }

  // Main page with search
  return (
    <section className="py-20 min-h-screen relative z-10">
      <div className="max-w-6xl mx-auto px-4">

        {/* Header with Search */}
        <div className="text-center mb-10 bg-black/30 backdrop-blur-md p-6 rounded-2xl border border-white/30">
          <h1 className="text-5xl font-extrabold text-white mb-6 drop-shadow-lg">
            Latest Tariff News
          </h1>

          {/* Search Form */}
          <form onSubmit={handleSearch} className="max-w-4xl mx-auto mb-6">
            {/* Query Input */}
            <div className="flex gap-3 mb-4">
              <div className="flex-1 relative">
                <input
                  type="text"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Enter search query (e.g., rice tariff)"
                  className="w-full px-5 py-3 bg-black/50 border-2 border-white/30 rounded-xl text-white placeholder-white/50 focus:outline-none focus:border-cyan-400/60 transition-all"
                />
              </div>
              <button
                type="submit"
                disabled={loading}
                className="bg-cyan-500/30 hover:bg-cyan-500/40 disabled:bg-gray-500/30 text-cyan-100 font-bold px-8 py-3 rounded-xl border-2 border-cyan-400/40 hover:border-cyan-300 disabled:border-gray-400/40 transition-all duration-200 flex items-center gap-2 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <>
                    <div className="w-5 h-5 border-2 border-cyan-300/30 border-t-cyan-300 rounded-full animate-spin"></div>
                    Searching...
                  </>
                ) : (
                  <>
                    <IconSearch className="w-5 h-5" />
                    Search
                  </>
                )}
              </button>
            </div>

            {/* Filters Row */}
            <div className="flex gap-4 justify-center">
              {/* Max Results */}
              <div className="flex items-center gap-2 bg-black/50 px-4 py-2 rounded-lg border border-white/20">
                <IconListNumbers className="w-5 h-5 text-cyan-300" />
                <label className="text-white/90 text-sm font-medium">Max Results:</label>
                <input
                  type="number"
                  min="1"
                  max="50"
                  value={maxResults}
                  onChange={(e) => setMaxResults(parseInt(e.target.value) || 10)}
                  className="w-16 px-2 py-1 bg-black/70 border border-white/20 rounded text-white text-center focus:outline-none focus:border-cyan-400/60"
                />
              </div>

              {/* Min Year */}
              <div className="flex items-center gap-2 bg-black/50 px-4 py-2 rounded-lg border border-white/20">
                <IconCalendar className="w-5 h-5 text-cyan-300" />
                <label className="text-white/90 text-sm font-medium">From Year:</label>
                <input
                  type="number"
                  min="2000"
                  max="2030"
                  value={minYear}
                  onChange={(e) => setMinYear(parseInt(e.target.value) || 2024)}
                  className="w-20 px-2 py-1 bg-black/70 border border-white/20 rounded text-white text-center focus:outline-none focus:border-cyan-400/60"
                />
              </div>
            </div>
          </form>

          {/* Results Info */}
          {newsData && (
            <>
              <p className="text-cyan-300 text-sm font-semibold">
                Query: {newsData.query}
              </p>
              <p className="text-white/90 text-lg font-medium mt-2">
                Scraped from official sources • {newsData.sourcesScraped} articles found
                {minYear && <span className="text-cyan-300"> • From {minYear} onwards</span>}
              </p>
            </>
          )}
        </div>

        {/* Loading State */}
        {loading && (
          <div className="flex items-center justify-center py-20">
            <div className="text-center">
              <div className="w-16 h-16 border-4 border-cyan-300/30 border-t-cyan-300 rounded-full animate-spin mx-auto mb-4"></div>
              <p className="text-white/90 text-lg">Searching for tariff news...</p>
              <p className="text-white/70 text-sm mt-2">
                Filtering results from {minYear} onwards
              </p>
            </div>
          </div>
        )}

        {/* News Articles */}
        {newsData && !loading && (
          <>
            <div className="space-y-6">
              {newsData.data.map((article, index) => (
                <div
                  key={index}
                  className="bg-black/50 backdrop-blur-xl rounded-2xl p-8 border-2 border-white/30 shadow-2xl hover:shadow-cyan-500/20 hover:border-cyan-400/50 transition-all duration-300"
                >
                  {/* Article Header */}
                  <div className="flex items-start gap-4 mb-6">
                    <div className="bg-cyan-500/20 p-3 rounded-lg border border-cyan-400/40">
                      <IconNews className="w-7 h-7 text-cyan-300 flex-shrink-0" />
                    </div>
                    <div className="flex-1">
                      <h2 className="text-3xl font-bold text-white mb-3 leading-tight">
                        {article.title}
                      </h2>
                      <div className="flex items-center gap-3 text-sm">
                        <span className="bg-cyan-500/30 text-cyan-100 px-4 py-1.5 rounded-full font-semibold border border-cyan-400/40">
                          {article.sourceDomain}
                        </span>
                        {article.publishDate && (
                          <span className="text-white/80 font-medium">
                            {new Date(article.publishDate).toLocaleDateString()}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Extracted Rate */}
                  {article.extractedRate !== null && (
                    <div className="bg-gradient-to-r from-cyan-500/30 to-blue-500/30 border-2 border-cyan-400/60 rounded-xl p-6 mb-6 shadow-lg">
                      <div className="flex items-center gap-3 mb-2">
                        <div className="bg-cyan-400/30 p-2 rounded-lg">
                          <IconDatabase className="w-6 h-6 text-cyan-200" />
                        </div>
                        <span className="text-cyan-100 font-bold text-lg">Extracted Tariff Rate</span>
                      </div>
                      <p className="text-5xl font-extrabold text-cyan-300 drop-shadow-glow">
                        {article.extractedRate}%
                      </p>
                    </div>
                  )}

                  {/* Relevant Text Excerpts */}
                  {article.relevantText && article.relevantText.length > 0 && (
                    <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 mb-6 border border-white/20">
                      <h3 className="text-white font-bold mb-3 text-base flex items-center gap-2">
                        <span className="w-2 h-2 bg-cyan-400 rounded-full"></span>
                        Key Excerpts:
                      </h3>
                      <div className="space-y-3">
                        {article.relevantText.slice(0, 2).map((text, idx) => (
                          <p key={idx} className="text-white/90 text-sm leading-relaxed pl-4 border-l-2 border-cyan-400/50">
                            {text.substring(0, 200)}...
                          </p>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Source Link */}

                  <a
                  href={article.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 hover:text-cyan-100 font-bold px-6 py-3 rounded-lg border border-cyan-400/40 hover:border-cyan-300 transition-all duration-200 shadow-lg hover:shadow-cyan-500/30"
                  
                  >
                  Read Full Article →
                </a>
                </div>
              ))}
          </div>

        {/* Stats Footer */}
        <div className="mt-10 text-center bg-black/30 backdrop-blur-md p-4 rounded-xl border border-white/30">
          <p className="text-white/90 font-medium">
            Scraped {newsData.sourcesScraped} of {newsData.totalSourcesFound} sources
            {minYear && <span className="text-cyan-300"> • Filtered from {minYear}</span>}
          </p>
          {Object.keys(newsData.errors).length > 0 && (
            <p className="text-yellow-300 mt-2 font-semibold">
              ⚠️ {Object.keys(newsData.errors).length} sources failed to scrape
            </p>
          )}
        </div>
      </>
        )}
    </div>
    </section >
  );
}