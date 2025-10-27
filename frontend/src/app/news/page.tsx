'use client';

import { IconNews, IconDatabase, IconAlertCircle, IconSearch, IconCalendar, IconListNumbers, IconTrash } from "@tabler/icons-react";
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

interface NewsFromDB {
  newsLink: string;
  remarks: string | null;
  timestamp: string;
}

interface EnrichedArticle extends ScrapedData {
  isInDatabase: boolean;
  remarks: string | null;
}

export default function NewsPage() {
  const [query, setQuery] = useState("rice tariff");
  const [maxResults, setMaxResults] = useState(10);
  const [minYear, setMinYear] = useState(2024);
  const [newsData, setNewsData] = useState<ScrapeResult | null>(null);
  const [enrichedArticles, setEnrichedArticles] = useState<EnrichedArticle[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [editingRemarks, setEditingRemarks] = useState<{ [key: number]: string }>({});
  const [updatingRemarks, setUpdatingRemarks] = useState<{ [key: number]: boolean }>({});
  const [addingToDatabase, setAddingToDatabase] = useState<{ [key: number]: boolean }>({});
  const [deletingFromDatabase, setDeletingFromDatabase] = useState<{ [key: number]: boolean }>({});

  // Backend API base URL available across all handlers
  const API_BASE = process.env.NEXT_PUBLIC_BACKEND_URL || process.env.BACKEND_URL || 'http://localhost:8080'

  const fetchNewsData = async (searchQuery: string, max: number, year: number) => {
    setLoading(true);
    setError(false);
    
    try {
      // Step 1: Scrape news from official sources
  const response = await fetch(`${API_BASE}/api/scraper/search-and-scrape`, {
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

      const scrapedData: ScrapeResult = await response.json();
      setNewsData(scrapedData);

      // Step 2: For each scraped article, check if it exists in database
      const enriched: EnrichedArticle[] = await Promise.all(
        scrapedData.data.map(async (article) => {
          try {
            // Check if this link exists in database using GET with query parameter
            const encodedUrl = encodeURIComponent(article.url);
            const checkResponse = await fetch(
              `${API_BASE}/api/v1/database/news?newsLink=${encodedUrl}`,
              {
                method: 'GET',
                cache: 'no-store'
              }
            );

            if (checkResponse.ok) {
              // If exists, we got the full record with remarks
              const dbEntry: NewsFromDB = await checkResponse.json();
              return {
                ...article,
                isInDatabase: true,
                remarks: dbEntry.remarks
              };
            } else if (checkResponse.status === 404) {
              // Not found in database
              return {
                ...article,
                isInDatabase: false,
                remarks: null
              };
            }
          } catch (dbError) {
            console.error('Error checking database for:', article.url, dbError);
          }

          // Default: not in database
          return {
            ...article,
            isInDatabase: false,
            remarks: null
          };
        })
      );
      
      setEnrichedArticles(enriched);

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

  const handleRemarksChange = (index: number, value: string) => {
    setEditingRemarks(prev => ({ ...prev, [index]: value }));
  };

  const updateRemarksToDatabase = async (index: number, article: EnrichedArticle) => {
    const remarksToUpdate = editingRemarks[index] !== undefined 
      ? editingRemarks[index] 
      : (article.remarks || '');

    setUpdatingRemarks(prev => ({ ...prev, [index]: true }));

    try {
  const response = await fetch(`${API_BASE}/api/v1/news`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          newsLink: article.url,
          remarks: remarksToUpdate
        }),
        cache: 'no-store'
      });

      if (response.ok) {
        // Update local state to reflect the change
        const updatedArticles = [...enrichedArticles];
        updatedArticles[index] = {
          ...updatedArticles[index],
          remarks: remarksToUpdate
        };
        setEnrichedArticles(updatedArticles);
        
        // Clear editing state for this article
        const newEditingRemarks = { ...editingRemarks };
        delete newEditingRemarks[index];
        setEditingRemarks(newEditingRemarks);

        alert('Remarks updated successfully!');
      } else {
        const errorData = await response.json();
        alert(`Failed to update remarks: ${errorData.message || 'Unknown error'}`);
      }
    } catch (error) {
      console.error('Error updating remarks:', error);
      alert('Failed to update remarks. Please try again.');
    } finally {
      setUpdatingRemarks(prev => ({ ...prev, [index]: false }));
    }
  };

  const addSourceToDatabase = async (index: number, article: EnrichedArticle) => {
    const remarksToAdd = editingRemarks[index] || null;

    setAddingToDatabase(prev => ({ ...prev, [index]: true }));

    try {
  const response = await fetch(`${API_BASE}/api/v1/news`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          newsLink: article.url,
          remarks: remarksToAdd
        }),
        cache: 'no-store'
      });

      if (response.ok) {
        // Update local state to mark article as in database
        const updatedArticles = [...enrichedArticles];
        updatedArticles[index] = {
          ...updatedArticles[index],
          isInDatabase: true,
          remarks: remarksToAdd
        };
        setEnrichedArticles(updatedArticles);
        
        // Clear editing state for this article
        const newEditingRemarks = { ...editingRemarks };
        delete newEditingRemarks[index];
        setEditingRemarks(newEditingRemarks);

        alert('Source added to database successfully!');
      } else {
        const errorData = await response.json();
        alert(`Failed to add source: ${errorData.message || 'Unknown error'}`);
      }
    } catch (error) {
      console.error('Error adding source to database:', error);
      alert('Failed to add source. Please try again.');
    } finally {
      setAddingToDatabase(prev => ({ ...prev, [index]: false }));
    }
  };

  const deleteSourceFromDatabase = async (index: number, article: EnrichedArticle) => {
    // Confirm deletion
    if (!confirm(`Are you sure you want to delete this source from the database?\n\n"${article.title}"`)) {
      return;
    }

    setDeletingFromDatabase(prev => ({ ...prev, [index]: true }));

    try {
  const response = await fetch(`${API_BASE}/api/v1/news`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          newsLink: article.url
        }),
        cache: 'no-store'
      });

      if (response.ok) {
        // Update local state to mark article as not in database
        const updatedArticles = [...enrichedArticles];
        updatedArticles[index] = {
          ...updatedArticles[index],
          isInDatabase: false,
          remarks: null
        };
        setEnrichedArticles(updatedArticles);
        
        // Clear editing state for this article
        const newEditingRemarks = { ...editingRemarks };
        delete newEditingRemarks[index];
        setEditingRemarks(newEditingRemarks);

        alert('Source deleted from database successfully!');
      } else {
        const errorData = await response.json();
        alert(`Failed to delete source: ${errorData.message || 'Unknown error'}`);
      }
    } catch (error) {
      console.error('Error deleting source from database:', error);
      alert('Failed to delete source. Please try again.');
    } finally {
      setDeletingFromDatabase(prev => ({ ...prev, [index]: false }));
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
  if (newsData && enrichedArticles.length === 0) {
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
                Query: &quot;{newsData.query}&quot;
              </p>
              <p className="text-white/90 text-lg font-medium mt-2">
                Scraped from official sources • {newsData.sourcesScraped} articles found
                {minYear && <span className="text-cyan-300"> • From {minYear} onwards</span>}
              </p>
              {/* Stats moved to top */}
              <div className="mt-4 bg-black/40 backdrop-blur-md p-3 rounded-xl border border-white/30">
                <p className="text-white/90 font-medium">
                  Scraped {newsData.sourcesScraped} of {newsData.totalSourcesFound} sources
                  {minYear && <span className="text-cyan-300"> • Filtered from {minYear}</span>}
                  {enrichedArticles.length > 0 && (
                    <>
                      {' • '}
                      <span className="text-green-300">
                        {enrichedArticles.filter(a => a.isInDatabase).length} already in database
                      </span>
                    </>
                  )}
                </p>
                {Object.keys(newsData.errors).length > 0 && (
                  <p className="text-yellow-300 mt-2 font-semibold text-sm">
                    ⚠️ {Object.keys(newsData.errors).length} sources failed to scrape
                  </p>
                )}
              </div>
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
        {newsData && !loading && enrichedArticles.length > 0 && (
          <>
            <div className="space-y-6 mt-6">
              {enrichedArticles.map((article, index) => (
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
                      <div className="flex items-center gap-3 mb-3">
                        <h2 className="text-3xl font-bold text-white leading-tight">
                          {article.title}
                        </h2>
                        {article.isInDatabase && (
                          <span className="bg-green-500/20 text-green-300 px-3 py-1 rounded-full text-xs font-semibold border border-green-400/40 flex-shrink-0">
                            ✓ In Database
                          </span>
                        )}
                      </div>
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
                            &quot;{text.substring(0, 200)}...&quot;
                          </p>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Source Link and Remarks */}
                  <div className="flex flex-col lg:flex-row gap-4 items-stretch">
                    <a
                      href={article.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-2 bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 hover:text-cyan-100 font-bold px-6 py-3 rounded-lg border border-cyan-400/40 hover:border-cyan-300 transition-all duration-200 shadow-lg hover:shadow-cyan-500/30 w-fit"
                    >
                      Read Full Article →
                    </a>
                    
                    {/* Remarks Section */}
                    <div className="flex-1 min-w-0 flex flex-col gap-2">
                      {article.isInDatabase ? (
                        <>
                          {/* Editable Remarks Textarea - In Database */}
                          <textarea
                            value={editingRemarks[index] !== undefined ? editingRemarks[index] : (article.remarks || '')}
                            onChange={(e) => handleRemarksChange(index, e.target.value)}
                            placeholder="No remarks added yet"
                            className="flex-1 bg-yellow-500/10 border border-yellow-400/30 rounded-lg p-4 text-yellow-100 text-sm placeholder-yellow-300/50 focus:outline-none focus:border-yellow-400/60 transition-all resize-none min-h-[100px]"
                          />
                          {/* Update and Delete Buttons */}
                          <div className="flex gap-2 self-end">
                            <button
                              onClick={() => updateRemarksToDatabase(index, article)}
                              disabled={updatingRemarks[index]}
                              className="bg-yellow-500/20 hover:bg-yellow-500/30 disabled:bg-gray-500/20 text-yellow-300 hover:text-yellow-100 disabled:text-gray-400 font-bold px-6 py-2 rounded-lg border border-yellow-400/40 hover:border-yellow-300 disabled:border-gray-400/40 transition-all duration-200 disabled:cursor-not-allowed"
                            >
                              {updatingRemarks[index] ? (
                                <>
                                  <div className="inline-block w-4 h-4 border-2 border-yellow-300/30 border-t-yellow-300 rounded-full animate-spin mr-2"></div>
                                  Updating...
                                </>
                              ) : (
                                'Update remarks'
                              )}
                            </button>
                            <button
                              onClick={() => deleteSourceFromDatabase(index, article)}
                              disabled={deletingFromDatabase[index]}
                              className="bg-red-500/20 hover:bg-red-500/30 disabled:bg-gray-500/20 text-red-300 hover:text-red-100 disabled:text-gray-400 font-bold px-6 py-2 rounded-lg border border-red-400/40 hover:border-red-300 disabled:border-gray-400/40 transition-all duration-200 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                              {deletingFromDatabase[index] ? (
                                <>
                                  <div className="inline-block w-4 h-4 border-2 border-red-300/30 border-t-red-300 rounded-full animate-spin"></div>
                                  Deleting...
                                </>
                              ) : (
                                <>
                                  <IconTrash className="w-4 h-4" />
                                  Delete from database
                                </>
                              )}
                            </button>
                          </div>
                        </>
                      ) : (
                        <>
                          {/* Editable Remarks Textarea - Not in Database */}
                          <textarea
                            value={editingRemarks[index] || ''}
                            onChange={(e) => handleRemarksChange(index, e.target.value)}
                            placeholder="Add remarks (optional)"
                            className="flex-1 bg-white/5 border border-white/20 rounded-lg p-4 text-white/90 text-sm placeholder-white/40 focus:outline-none focus:border-cyan-400/60 transition-all resize-none min-h-[100px]"
                          />
                          {/* Add to Database Button */}
                          <button
                            onClick={() => addSourceToDatabase(index, article)}
                            disabled={addingToDatabase[index]}
                            className="self-end bg-cyan-500/20 hover:bg-cyan-500/30 disabled:bg-gray-500/20 text-cyan-300 hover:text-cyan-100 disabled:text-gray-400 font-bold px-6 py-2 rounded-lg border border-cyan-400/40 hover:border-cyan-300 disabled:border-gray-400/40 transition-all duration-200 disabled:cursor-not-allowed"
                          >
                            {addingToDatabase[index] ? (
                              <>
                                <div className="inline-block w-4 h-4 border-2 border-cyan-300/30 border-t-cyan-300 rounded-full animate-spin mr-2"></div>
                                Adding...
                              </>
                            ) : (
                              'Add source to database'
                            )}
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Stats Footer */}
            <div className="mt-10 text-center bg-black/30 backdrop-blur-md p-4 rounded-xl border border-white/30">
              <p className="text-white/90 font-medium">
                Scraped {newsData.sourcesScraped} of {newsData.totalSourcesFound} sources
                {minYear && <span className="text-cyan-300"> • Filtered from {minYear}</span>}
                {' • '}
                <span className="text-green-300">
                  {enrichedArticles.filter(a => a.isInDatabase).length} already in database
                </span>
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
    </section>
  );
}