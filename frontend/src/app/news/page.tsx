'use client';

import { IconNews, IconDatabase, IconAlertCircle, IconSearch, IconCalendar, IconListNumbers, IconTrash, IconGlobe, IconPackage, IconCalendarEvent, IconPercentage } from "@tabler/icons-react";
import { useState, useEffect } from "react";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { countries, agriculturalProducts } from "@/lib/tariff-data";

// TypeScript interfaces matching the new API response structure

interface ScrapedArticle {
  url: string;
  title: string;
  sourceDomain: string;
  relevantText: string[];
  exporter: string | null;
  importer: string | null;
  product: string | null;
  year: string | null;
  tariffRate: string | null;
  publishDate: string | null;
}

interface GeminiAnalysis {
  exporterCountry: string | null;
  importerCountry: string | null;
  product: string | null;
  year: string | null;
  tariffRate: string | null;
}

interface MetaData {
  minYear: number;
  maxResults: number;
}

interface ScrapeResponse {
  query: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  startTime: string;
  endTime: string;
  totalSourcesFound: number;
  sourcesScraped: number;
  articles: ScrapedArticle[];
  errors: { [key: string]: string };
  meta: MetaData;
}

interface NewsFromDB {
  newsLink: string;
  remarks: string | null;
  isHidden: boolean;
  timestamp: string;
}

interface EnrichedArticle extends ScrapedArticle {
  isInDatabase: boolean;
  isHidden: boolean;
  remarks: string | null;
  geminiAnalysis: GeminiAnalysis | null;
  analyzingWithGemini: boolean;
}

export default function NewsPage() {
  const [query, setQuery] = useState("rice tariff");
  const [maxResults, setMaxResults] = useState(10);
  const [minYear, setMinYear] = useState(2024);
  const [newsData, setNewsData] = useState<ScrapeResponse | null>(null);
  const [enrichedArticles, setEnrichedArticles] = useState<EnrichedArticle[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [editingRemarks, setEditingRemarks] = useState<{ [key: number]: string }>({});
  const [updatingRemarks, setUpdatingRemarks] = useState<{ [key: number]: boolean }>({});
  const [addingToDatabase, setAddingToDatabase] = useState<{ [key: number]: boolean }>({});
  const [deletingFromDatabase, setDeletingFromDatabase] = useState<{ [key: number]: boolean }>({});
  const [runningGeminiAnalysis, setRunningGeminiAnalysis] = useState<{ [key: number]: boolean }>({});
  const [hidingSource, setHidingSource] = useState<{ [key: number]: boolean }>({});
  const [showHiddenPanel, setShowHiddenPanel] = useState(false);
  const [hiddenSources, setHiddenSources] = useState<NewsFromDB[]>([]);
  const [loadingHiddenSources, setLoadingHiddenSources] = useState(false);
  const [unhidingSource, setUnhidingSource] = useState<{ [key: string]: boolean }>({});
  const [isAdmin, setIsAdmin] = useState<boolean>(false);

  // Tariff Rate Modal State
  const [showTariffModal, setShowTariffModal] = useState(false);
  const [selectedArticleForTariff, setSelectedArticleForTariff] = useState<EnrichedArticle | null>(null);
  const [tariffFormData, setTariffFormData] = useState({
    countryId: '',
    partnerCountryId: '',
    productId: '',
    tariffTypeId: '',
    year: '',
    rate: '',
    unit: ''
  });
  const [savingTariffRate, setSavingTariffRate] = useState(false);

  // Backend API base URL for non-auth endpoints
  const API_BASE = process.env.NEXT_PUBLIC_BACKEND_URL || process.env.BACKEND_URL || 'http://localhost:8080'
  // Admin News API - for checking if news exists and admin operations
  const ADMIN_NEWS_API = '/api/database/admin/news' // Proxied through Next.js to /api/v1/admin/news
  // User Hidden News API - for regular users to hide sources personally
  const USER_HIDDEN_API = '/api/database/user/hidden-news' // Proxied through Next.js to /api/v1/user/hidden-news
  const GEMINI_API = `${API_BASE}/api/v1/gemini/analyses`

  // Note: Authorization headers are now automatically injected by Next.js middleware
  // for all /api/database/* requests, so we don't need getAuthHeaders anymore

  /**
   * Check user role by calling server-side API (can read HttpOnly cookie)
   */
  const checkUserRole = async () => {
    try {
      const response = await fetch('/api/user/me', {
        method: 'GET',
        cache: 'no-store'
      });

      if (response.ok) {
        const data = await response.json();
        setIsAdmin(data.isAdmin || false);
        console.log('User role check - isAdmin:', data.isAdmin, 'role:', data.role);
      } else {
        setIsAdmin(false);
      }
    } catch (error) {
      console.error('Error checking user role:', error);
      setIsAdmin(false);
    }
  };

  // Check user role on component mount
  useEffect(() => {
    checkUserRole();
  }, []);

  // Fetch hidden sources count when isAdmin is determined
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    fetchHiddenSources();
  }, [isAdmin]);

  /**
   * Call Gemini API to analyze a tariff article
   */
  const analyzeWithGemini = async (article: EnrichedArticle, searchQuery: string): Promise<GeminiAnalysis | null> => {
    try {
      // Enhanced prompt with context and clear instructions
      const prompt = `You are analyzing a trade policy document to extract tariff information.

DOCUMENT CONTEXT:
- Title: "${article.title}"
- Source: ${article.sourceDomain}
- Search Query: "${searchQuery}"

EXTRACTION RULES:
1. EXPORTER COUNTRY: The country IMPOSING/CHARGING the tariff
   - For "Trade Policy Review of [Country]", the reviewed country is the EXPORTER
   - For "[Country A] imposes tariff on [Country B]", Country A is EXPORTER

2. IMPORTER COUNTRY: The country RECEIVING goods and PAYING the tariff
   - May be "N/A" for general trade policy reviews
   - Specific country if bilateral tariff mentioned

3. PRODUCT: Specific goods/category (e.g., "Rice", "Steel", "Automobiles")
   - Use "Various" or "General" only if multiple products discussed

4. YEAR: Year the tariff applies or document published
   - Extract from title, date, or content

5. TARIFF RATE: The percentage or amount (e.g., "50%", "10.5%", "$5 per kg")
   - Include average rates if specific rate not available
   - Use "N/A" only if truly not mentioned

ARTICLE CONTENT:
${article.relevantText.slice(0, 3).join('\n\n')}

Return ONLY valid JSON (no markdown, no explanation):
{"exporterCountry":"","importerCountry":"","product":"","year":"","tariffRate":""}`;

      const response = await fetch(GEMINI_API, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          data: article.url,
          prompt: prompt
        }),
        cache: 'no-store'
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error(`Gemini API failed for ${article.url}: ${response.status}`, errorText);
        console.error('Full error details:', {
          status: response.status,
          statusText: response.statusText,
          url: article.url,
          title: article.title,
          errorBody: errorText
        });
        return null;
      }

      const result = await response.json();
      console.log('Gemini API raw response:', result);

      // Check if the analysis failed
      if (result.success === false) {
        console.error('Gemini analysis failed:', result.error);
        alert(`Gemini analysis failed: ${result.error || 'Unknown error'}`);
        return null;
      }

      // Parse the Gemini response - it should return JSON
      // Handle different possible response formats
      let analysis: GeminiAnalysis;

      // Check if it's an AnalysisResponse object with success field
      if (result.success !== undefined && result.analysis !== undefined) {
        console.log('Detected AnalysisResponse format');
        const analysisData = result.analysis;

        // If analysis is a string, try to parse JSON from it
        if (typeof analysisData === 'string') {
          const jsonMatch = analysisData.match(/\{[\s\S]*\}/);
          if (jsonMatch) {
            analysis = JSON.parse(jsonMatch[0]);
          } else {
            console.error('Could not extract JSON from analysis string:', analysisData);
            return null;
          }
        } else if (typeof analysisData === 'object') {
          // If it's already an object, use it directly
          analysis = analysisData;
        } else {
          console.error('Unexpected analysis data type:', typeof analysisData);
          return null;
        }
      } else if (typeof result === 'string') {
        // If response is a string, try to parse it
        const jsonMatch = result.match(/\{[\s\S]*\}/);
        if (jsonMatch) {
          analysis = JSON.parse(jsonMatch[0]);
        } else {
          console.error('Could not extract JSON from string response');
          return null;
        }
      } else if (result.exporterCountry !== undefined) {
        // If response is already an object with the right structure
        analysis = result;
      } else if (result.response || result.result) {
        // If response is wrapped in another object
        const innerData = result.response || result.result;
        if (typeof innerData === 'string') {
          const jsonMatch = innerData.match(/\{[\s\S]*\}/);
          if (jsonMatch) {
            analysis = JSON.parse(jsonMatch[0]);
          } else {
            console.error('Could not extract JSON from wrapped response');
            return null;
          }
        } else {
          analysis = innerData;
        }
      } else {
        console.error('Unknown response format. Expected fields not found:', {
          result,
          hasSuccess: 'success' in result,
          hasAnalysis: 'analysis' in result,
          hasExporterCountry: 'exporterCountry' in result
        });
        return null;
      }

      console.log('Parsed analysis:', analysis);

      return {
        exporterCountry: analysis.exporterCountry || null,
        importerCountry: analysis.importerCountry || null,
        product: analysis.product || null,
        year: analysis.year || null,
        tariffRate: analysis.tariffRate || null
      };

    } catch (error) {
      console.error('Error calling Gemini API:', error);
      return null;
    }
  };

  const fetchNewsData = async (searchQuery: string, max: number, year: number) => {
    setLoading(true);
    setError(false);
    setErrorMessage('');

    try {
      // Step 1: Scrape news using GET endpoint with query parameters
      const queryParams = new URLSearchParams({
        query: searchQuery,
        maxResults: max.toString(),
        minYear: year.toString()
      });

      const response = await fetch(`${API_BASE}/api/v1/scrape?${queryParams.toString()}`, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
        },
        cache: 'no-store'
      });

      if (!response.ok) {
        // Handle error response
        try {
          const errorData = await response.json();
          setErrorMessage(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
        } catch {
          setErrorMessage(`HTTP ${response.status}: ${response.statusText}`);
        }
        setError(true);
        setLoading(false);
        return;
      }

      const scrapedData: ScrapeResponse = await response.json();

      // Check if job failed
      if (scrapedData.status === 'FAILED') {
        setErrorMessage('Scraping job failed');
        setError(true);
        setLoading(false);
        return;
      }

      setNewsData(scrapedData);

      // Step 2: Enrich articles with database information
      const enriched: EnrichedArticle[] = await Promise.all(
        scrapedData.articles.map(async (article) => {
          try {
            const encodedUrl = encodeURIComponent(article.url);
            console.log(`[DB Check] Checking if article exists: ${article.url}`);
            console.log(`[DB Check] Encoded URL: ${encodedUrl}`);
            console.log(`[DB Check] API endpoint: ${ADMIN_NEWS_API}?newsLink=${encodedUrl}`);

            const checkResponse = await fetch(
              `${ADMIN_NEWS_API}?newsLink=${encodedUrl}`,
              {
                method: 'GET',
                cache: 'no-store'
              }
            );

            console.log(`[DB Check] Response status for ${article.url}: ${checkResponse.status}`);

            if (checkResponse.ok) {
              const dbEntry: NewsFromDB = await checkResponse.json();
              console.log(`[DB Check] ✅ Found in database:`, dbEntry);
              return {
                ...article,
                isInDatabase: true,
                isHidden: dbEntry.isHidden,
                remarks: dbEntry.remarks,
                geminiAnalysis: null,
                analyzingWithGemini: false
              };
            } else if (checkResponse.status === 404) {
              console.log(`[DB Check] ❌ Not found in database (404): ${article.url}`);
              return {
                ...article,
                isInDatabase: false,
                isHidden: false,
                remarks: null,
                geminiAnalysis: null,
                analyzingWithGemini: false
              };
            } else {
              // Other error status codes
              console.warn(`[DB Check] ⚠️ Unexpected status ${checkResponse.status} for ${article.url}`);
              const errorText = await checkResponse.text();
              console.warn(`[DB Check] Error response:`, errorText);
            }
          } catch (dbError) {
            console.error(`[DB Check] 💥 Exception checking database for ${article.url}:`, dbError);
          }

          return {
            ...article,
            isInDatabase: false,
            isHidden: false,
            remarks: null,
            geminiAnalysis: null,
            analyzingWithGemini: false
          };
        })
      );

      // Step 3: Filter out hidden sources
      const visibleArticles = enriched.filter(article => !article.isHidden);

      setEnrichedArticles(visibleArticles);

    } catch (err) {
      console.error('Error fetching news:', err);
      setErrorMessage(err instanceof Error ? err.message : 'An unexpected error occurred');
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

  /**
   * Merge Gemini analysis with scraper data - use scraper as fallback for N/A fields
   */
  const mergeWithScraperData = (geminiResult: GeminiAnalysis | null, article: EnrichedArticle): GeminiAnalysis => {
    // If Gemini completely failed, use all scraper data
    if (!geminiResult) {
      console.log('Gemini failed, using scraper data as fallback');
      return {
        exporterCountry: article.exporter || 'N/A',
        importerCountry: article.importer || 'N/A',
        product: article.product || 'N/A',
        year: article.year || 'N/A',
        tariffRate: article.tariffRate || 'N/A'
      };
    }

    // Hybrid: Use Gemini data, but fall back to scraper for N/A fields
    const hybrid: GeminiAnalysis = {
      exporterCountry: (geminiResult.exporterCountry && geminiResult.exporterCountry !== 'N/A')
        ? geminiResult.exporterCountry
        : (article.exporter || 'N/A'),
      importerCountry: (geminiResult.importerCountry && geminiResult.importerCountry !== 'N/A')
        ? geminiResult.importerCountry
        : (article.importer || 'N/A'),
      product: (geminiResult.product && geminiResult.product !== 'N/A')
        ? geminiResult.product
        : (article.product || 'N/A'),
      year: (geminiResult.year && geminiResult.year !== 'N/A')
        ? geminiResult.year
        : (article.year || 'N/A'),
      tariffRate: (geminiResult.tariffRate && geminiResult.tariffRate !== 'N/A')
        ? geminiResult.tariffRate
        : (article.tariffRate || 'N/A')
    };

    // Log which fields came from which source
    console.log('Hybrid Analysis Result:', {
      exporterCountry: hybrid.exporterCountry === geminiResult.exporterCountry ? '🤖 Gemini' : '🔍 Scraper',
      importerCountry: hybrid.importerCountry === geminiResult.importerCountry ? '🤖 Gemini' : '🔍 Scraper',
      product: hybrid.product === geminiResult.product ? '🤖 Gemini' : '🔍 Scraper',
      year: hybrid.year === geminiResult.year ? '🤖 Gemini' : '🔍 Scraper',
      tariffRate: hybrid.tariffRate === geminiResult.tariffRate ? '🤖 Gemini' : '🔍 Scraper'
    });

    return hybrid;
  };

  const runGeminiAnalysisForArticle = async (index: number, article: EnrichedArticle) => {
    setRunningGeminiAnalysis(prev => ({ ...prev, [index]: true }));

    // Mark as analyzing
    setEnrichedArticles(prev => {
      const updated = [...prev];
      updated[index] = { ...updated[index], analyzingWithGemini: true };
      return updated;
    });

    const geminiAnalysis = await analyzeWithGemini(article, query);

    // Merge Gemini results with scraper data
    const hybridAnalysis = mergeWithScraperData(geminiAnalysis, article);

    // Check if final hybrid analysis still has all N/A
    const allNA = Object.values(hybridAnalysis).every(val => val === 'N/A' || val === '' || val === null);
    if (allNA) {
      alert(`⚠️ Analysis Warning\n\nBoth Gemini AI and web scraper could not extract tariff information.\n\nThis article may not contain structured tariff data.\n\nTry checking the article manually.`);
    } else if (!geminiAnalysis) {
      // Gemini failed but scraper provided some data
      alert(`ℹ️ Using Scraper Data\n\nGemini AI analysis failed, but the web scraper extracted some information.\n\nCheck the results carefully as scraper data may be less accurate.`);
    }

    // Update with hybrid analysis results
    setEnrichedArticles(prev => {
      const updated = [...prev];
      updated[index] = {
        ...updated[index],
        geminiAnalysis: hybridAnalysis,
        analyzingWithGemini: false
      };
      return updated;
    });

    setRunningGeminiAnalysis(prev => ({ ...prev, [index]: false }));
  };

  /**
   * Open tariff rate modal and pre-fill form if Gemini analysis exists
   * Try to match Gemini data to dropdown options
   */
  const openTariffModal = (article: EnrichedArticle, prefillWithGemini: boolean = true) => {
    setSelectedArticleForTariff(article);

    // Pre-fill form with Gemini AI extracted data (try to match dropdown options)
    if (prefillWithGemini && article.geminiAnalysis) {
      // Try to match country name to country code - handles various formats
      const matchCountry = (countryName: string | null) => {
        if (!countryName) return '';
        const normalized = countryName.trim().toLowerCase();

        // Try exact match first
        let country = countries.find(c =>
          c.name.toLowerCase() === normalized ||
          c.code === countryName.toUpperCase()
        );

        // Try partial match if exact match fails
        if (!country) {
          country = countries.find(c =>
            c.name.toLowerCase().includes(normalized) ||
            normalized.includes(c.name.toLowerCase())
          );
        }

        const result = country?.code || '';
        if (result && country) {
          console.log(`Matched "${countryName}" to "${country.name}" (code: ${result})`);
        } else {
          console.warn(`Could not match country: "${countryName}"`);
        }
        return result;
      };

      // Try to match product name to HS code
      const matchProduct = (productName: string | null) => {
        if (!productName) return '';
        const normalized = productName.trim().toLowerCase();

        // Try to find product that contains the search term or vice versa
        const product = agriculturalProducts.find(p =>
          p.name.toLowerCase().includes(normalized) ||
          normalized.includes(p.name.toLowerCase()) ||
          p.hs_code === productName
        );

        const result = product?.hs_code || '';
        if (result && product) {
          console.log(`Matched "${productName}" to "${product.name}" (HS code: ${result})`);
        } else {
          console.warn(`Could not match product: "${productName}"`);
        }
        return result;
      };

      setTariffFormData({
        countryId: matchCountry(article.geminiAnalysis.exporterCountry),
        partnerCountryId: matchCountry(article.geminiAnalysis.importerCountry),
        productId: matchProduct(article.geminiAnalysis.product),
        tariffTypeId: '',
        year: article.geminiAnalysis.year || '',
        rate: article.geminiAnalysis.tariffRate?.replace('%', '').trim() || '',
        unit: article.geminiAnalysis.tariffRate?.includes('%') ? '%' : ''
      });
    } else {
      // Empty form for manual entry
      setTariffFormData({
        countryId: '',
        partnerCountryId: '',
        productId: '',
        tariffTypeId: '',
        year: '',
        rate: '',
        unit: ''
      });
    }

    setShowTariffModal(true);
  };

  const closeTariffModal = () => {
    setShowTariffModal(false);
    setSelectedArticleForTariff(null);
  };

  const handleTariffFormChange = (field: string, value: string) => {
    setTariffFormData(prev => ({ ...prev, [field]: value }));
  };

  const saveTariffRateToDatabase = async () => {
    if (!selectedArticleForTariff) return;

    // Validation
    if (!tariffFormData.countryId || !tariffFormData.productId || !tariffFormData.year || !tariffFormData.rate) {
      alert('Please fill in all required fields: From Country, Product, Year, and Rate');
      return;
    }

    // Validate productId is a valid integer
    const productIdNum = parseInt(tariffFormData.productId);
    if (isNaN(productIdNum)) {
      alert('Invalid product selection');
      return;
    }

    // Validate year is a valid integer
    const yearNum = parseInt(tariffFormData.year);
    if (isNaN(yearNum) || yearNum < 1900 || yearNum > 2100) {
      alert('Invalid year selection');
      return;
    }

    // Validate rate is a valid number
    const rateNum = parseFloat(tariffFormData.rate);
    if (isNaN(rateNum) || rateNum < 0) {
      alert('Tariff Rate must be a valid non-negative number');
      return;
    }

    setSavingTariffRate(true);

    try {
      // Prepare request body matching CreateNewsTariffRateRequest
      const requestBody = {
        newsLink: selectedArticleForTariff.url,
        countryId: tariffFormData.countryId,
        partnerCountryId: tariffFormData.partnerCountryId || null,
        productId: productIdNum,
        tariffTypeId: null, // Removed tariffTypeId field
        year: yearNum,
        rate: rateNum,
        unit: '%' // Always use %
      };

      console.log('Saving tariff rate:', requestBody);

      // Use proxied endpoint so middleware can inject Authorization header
      const response = await fetch('/api/database/news/rates', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
        cache: 'no-store'
      });

      if (response.ok) {
        const result = await response.json();
        console.log('Tariff rate saved:', result);
        alert('✅ Tariff rate saved to database successfully!');
        closeTariffModal();
      } else {
        let errorMessage = `HTTP ${response.status}`;
        try {
          const contentType = response.headers.get('content-type');
          if (contentType && contentType.includes('application/json')) {
            const errorData = await response.json();
            errorMessage = errorData.message || JSON.stringify(errorData);
          } else {
            errorMessage = await response.text();
          }
        } catch (e) {
          console.error('Error parsing error response:', e);
        }
        alert(`Failed to save tariff rate: ${errorMessage}`);
      }
    } catch (error) {
      console.error('Error saving tariff rate:', error);
      alert(`Failed to save tariff rate: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setSavingTariffRate(false);
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
      const response = await fetch(`${ADMIN_NEWS_API}`, {
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
        const updatedArticles = [...enrichedArticles];
        updatedArticles[index] = {
          ...updatedArticles[index],
          remarks: remarksToUpdate
        };
        setEnrichedArticles(updatedArticles);

        const newEditingRemarks = { ...editingRemarks };
        delete newEditingRemarks[index];
        setEditingRemarks(newEditingRemarks);

        alert('Remarks updated successfully!');
      } else {
        let msg = `HTTP ${response.status}`;
        try {
          const errorData = await response.json();
          msg = errorData.message || msg;
        } catch { }
        if (response.status === 401 || response.status === 403) {
          alert(`Failed to update remarks: ${msg}. Please login as an admin user.`);
        } else {
          alert(`Failed to update remarks: ${msg}`);
        }
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
      const response = await fetch(`${ADMIN_NEWS_API}`, {
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
        const updatedArticles = [...enrichedArticles];
        updatedArticles[index] = {
          ...updatedArticles[index],
          isInDatabase: true,
          remarks: remarksToAdd
        };
        setEnrichedArticles(updatedArticles);

        const newEditingRemarks = { ...editingRemarks };
        delete newEditingRemarks[index];
        setEditingRemarks(newEditingRemarks);

        alert('Source added to database successfully!');
      } else {
        let msg = `HTTP ${response.status}`;
        try {
          const errorData = await response.json();
          msg = errorData.message || msg;
        } catch { }
        alert(`Failed to add source: ${msg}`);
      }
    } catch (error) {
      console.error('Error adding source to database:', error);
      alert('Failed to add source. Please try again.');
    } finally {
      setAddingToDatabase(prev => ({ ...prev, [index]: false }));
    }
  };

  const deleteSourceFromDatabase = async (index: number, article: EnrichedArticle) => {
    if (!confirm(`Are you sure you want to delete this source from the database?\n\n"${article.title}"`)) {
      return;
    }

    setDeletingFromDatabase(prev => ({ ...prev, [index]: true }));

    try {
      const response = await fetch(`${ADMIN_NEWS_API}?newsLink=${encodeURIComponent(article.url)}`, {
        method: 'DELETE',
        cache: 'no-store'
      });

      if (response.ok) {
        const updatedArticles = [...enrichedArticles];
        updatedArticles[index] = {
          ...updatedArticles[index],
          isInDatabase: false,
          remarks: null
        };
        setEnrichedArticles(updatedArticles);

        const newEditingRemarks = { ...editingRemarks };
        delete newEditingRemarks[index];
        setEditingRemarks(newEditingRemarks);

        alert('Source deleted from database successfully!');
      } else {
        let msg = `HTTP ${response.status}`;
        try {
          const errorData = await response.json();
          msg = errorData.message || msg;
        } catch { }
        if (response.status === 401 || response.status === 403) {
          alert(`Failed to delete source: ${msg}. Please login as an admin user.`);
        } else {
          alert(`Failed to delete source: ${msg}`);
        }
      }
    } catch (error) {
      console.error('Error deleting source from database:', error);
      alert('Failed to delete source. Please try again.');
    } finally {
      setDeletingFromDatabase(prev => ({ ...prev, [index]: false }));
    }
  };

  const fetchHiddenSources = async () => {
    setLoadingHiddenSources(true);
    try {
      if (isAdmin) {
        // Admin: Fetch from News table (shared hidden sources)
        const response = await fetch(`${ADMIN_NEWS_API}/all`, {
          method: 'GET',
          cache: 'no-store'
        });

        if (response.ok) {
          const allNews: NewsFromDB[] = await response.json();
          // Filter only hidden sources
          const hidden = allNews.filter(news => news.isHidden);
          setHiddenSources(hidden);
        } else {
          console.error('Failed to fetch admin hidden sources:', response.status);
        }
      } else {
        // Regular user: Fetch from UserHiddenSources table (proxied through Next.js)
        const response = await fetch(USER_HIDDEN_API, {
          method: 'GET',
          cache: 'no-store'
        });

        if (response.ok) {
          type UserHidden = { newsLink: string; hiddenAt: string };

          const userHidden: UserHidden[] = await response.json();

          const hidden: NewsFromDB[] = userHidden.map(({ newsLink, hiddenAt }) => ({
            newsLink,
            remarks: null,
            isHidden: true,
            timestamp: hiddenAt
          }));

          setHiddenSources(hidden);
        }
        else {
          console.error('Failed to fetch user hidden sources:', response.status);
        }
      }
    } catch (error) {
      console.error('Error fetching hidden sources:', error);
    } finally {
      setLoadingHiddenSources(false);
    }
  };

  const openHiddenPanel = () => {
    setShowHiddenPanel(true);
    fetchHiddenSources();
  };

  const hideSource = async (index: number, article: EnrichedArticle) => {
    if (!confirm(`Are you sure you want to hide this source? You won't see it in future searches.\n\n"${article.title}"`)) {
      return;
    }

    setHidingSource(prev => ({ ...prev, [index]: true }));

    try {
      let response;

      if (isAdmin) {
        // Admin: Use News table visibility endpoint (RESTful - shared for all admins)
        response = await fetch(`${ADMIN_NEWS_API}/visibility?newsLink=${encodeURIComponent(article.url)}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ hidden: true }),
          cache: 'no-store'
        });
      } else {
        // Regular user: Use UserHiddenSources endpoint (proxied through Next.js)
        response = await fetch(`${USER_HIDDEN_API}?newsLink=${encodeURIComponent(article.url)}`, {
          method: 'POST',
          cache: 'no-store'
        });
      }

      if (response.ok) {
        // Remove the article from the displayed list
        const updatedArticles = enrichedArticles.filter((_, i) => i !== index);
        setEnrichedArticles(updatedArticles);

        // Update hidden sources count by adding this source to the list
        setHiddenSources(prev => [...prev, {
          newsLink: article.url,
          remarks: null,
          isHidden: true,
          timestamp: new Date().toISOString()
        }]);

        alert('Source hidden successfully! It will no longer appear in search results.');
      } else {
        let msg = `HTTP ${response.status}`;
        try {
          const errorData = await response.json();
          msg = errorData.message || msg;
        } catch { }
        alert(`Failed to hide source: ${msg}`);
      }
    } catch (error) {
      console.error('Error hiding source:', error);
      alert('Failed to hide source. Please try again.');
    } finally {
      setHidingSource(prev => ({ ...prev, [index]: false }));
    }
  };

  const unhideSource = async (newsLink: string) => {
    if (!confirm(`Are you sure you want to unhide this source?\n\n"${newsLink}"`)) {
      return;
    }

    setUnhidingSource(prev => ({ ...prev, [newsLink]: true }));

    try {
      let response;

      if (isAdmin) {
        // Admin: Use News table visibility endpoint (RESTful)
        response = await fetch(`${ADMIN_NEWS_API}/visibility?newsLink=${encodeURIComponent(newsLink)}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ hidden: false }),
          cache: 'no-store'
        });
      } else {
        // Regular user: Use UserHiddenSources delete endpoint (proxied through Next.js)
        response = await fetch(`${USER_HIDDEN_API}?newsLink=${encodeURIComponent(newsLink)}`, {
          method: 'DELETE',
          cache: 'no-store'
        });
      }

      if (response.ok) {
        // Remove from hidden sources list
        const updatedHiddenSources = hiddenSources.filter(source => source.newsLink !== newsLink);
        setHiddenSources(updatedHiddenSources);

        alert('Source unhidden successfully! It will now appear in search results.');

        // Add back to current search results if it was in the original scraped data
        if (newsData) {
          const article = newsData.articles.find(a => a.url === newsLink);
          if (article) {
            // Check if not already in enrichedArticles
            const exists = enrichedArticles.some(a => a.url === newsLink);
            if (!exists) {
              // Check database status
              try {
                const encodedUrl = encodeURIComponent(newsLink);
                const checkResponse = await fetch(
                  `${ADMIN_NEWS_API}?newsLink=${encodedUrl}`,
                  {
                    method: 'GET',
                    cache: 'no-store'
                  }
                );

                let enrichedArticle: EnrichedArticle;
                if (checkResponse.ok) {
                  const dbEntry: NewsFromDB = await checkResponse.json();
                  enrichedArticle = {
                    ...article,
                    isInDatabase: true,
                    isHidden: false,
                    remarks: dbEntry.remarks,
                    geminiAnalysis: null,
                    analyzingWithGemini: false
                  };
                } else {
                  enrichedArticle = {
                    ...article,
                    isInDatabase: false,
                    isHidden: false,
                    remarks: null,
                    geminiAnalysis: null,
                    analyzingWithGemini: false
                  };
                }

                // Add to enrichedArticles
                setEnrichedArticles(prev => [...prev, enrichedArticle]);
              } catch (error) {
                console.error('Error checking database for unhidden article:', error);
              }
            }
          }
        }
      } else {
        let msg = `HTTP ${response.status}`;
        try {
          const errorData = await response.json();
          msg = errorData.message || msg;
        } catch { }
        alert(`Failed to unhide source: ${msg}`);
      }
    } catch (error) {
      console.error('Error unhiding source:', error);
      alert('Failed to unhide source. Please try again.');
    } finally {
      setUnhidingSource(prev => ({ ...prev, [newsLink]: false }));
    }
  };

  const unhideAllSources = async () => {
    if (hiddenSources.length === 0) {
      alert('No hidden sources to unhide.');
      return;
    }

    if (!confirm(`Are you sure you want to unhide all ${hiddenSources.length} sources? They will all appear in future searches.`)) {
      return;
    }

    setLoadingHiddenSources(true);

    try {
      if (isAdmin) {
        // Admin: Unhide all sources in parallel from News table (RESTful)
        const unhidePromises = hiddenSources.map(source =>
          fetch(`${ADMIN_NEWS_API}/visibility?newsLink=${encodeURIComponent(source.newsLink)}`, {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({ hidden: false }),
            cache: 'no-store'
          })
        );

        const results = await Promise.allSettled(unhidePromises);

        // Count successes and failures
        const successCount = results.filter(r => r.status === 'fulfilled' && r.value.ok).length;
        const failCount = results.length - successCount;

        // Get URLs of successfully unhidden sources
        const unhiddenUrls = hiddenSources
          .filter((_, index) => results[index].status === 'fulfilled' && (results[index] as PromiseFulfilledResult<Response>).value.ok)
          .map(source => source.newsLink);

        // Clear all hidden sources
        setHiddenSources([]);

        if (failCount === 0) {
          alert(`Successfully unhidden all ${successCount} sources!`);
        } else {
          alert(`Unhidden ${successCount} sources. ${failCount} failed.`);
        }

        // Refresh current search results if we have news data
        await refreshUnhiddenArticles(unhiddenUrls);

      } else {
        // Regular user: Use single DELETE endpoint to unhide all (proxied through Next.js)
        const response = await fetch(`${USER_HIDDEN_API}?all=true`, {
          method: 'DELETE',
          cache: 'no-store'
        });

        if (response.ok) {
          const result = await response.text();
          setHiddenSources([]);
          alert(result || 'All sources unhidden successfully!');

          // Refresh current search results
          const unhiddenUrls = hiddenSources.map(s => s.newsLink);
          await refreshUnhiddenArticles(unhiddenUrls);
        } else {
          alert('Failed to unhide all sources.');
        }
      }
    } catch (error) {
      console.error('Error unhiding all sources:', error);
      alert('Failed to unhide all sources. Please try again.');
    } finally {
      setLoadingHiddenSources(false);
    }
  };

  const refreshUnhiddenArticles = async (unhiddenUrls: string[]) => {
    // Refresh current search results if we have news data
    if (newsData && unhiddenUrls.length > 0) {
      // Re-enrich articles to include the newly unhidden sources
      const currentArticles = [...enrichedArticles];

      // Check if any unhidden URLs match articles in the original scraped data
      const articlesToAdd: EnrichedArticle[] = [];

      for (const article of newsData.articles) {
        if (unhiddenUrls.includes(article.url)) {
          // Check if this article is not already in enrichedArticles
          const exists = currentArticles.some(a => a.url === article.url);
          if (!exists) {
            // Check database status for this article (only for admins)
            try {
              if (isAdmin) {
                const encodedUrl = encodeURIComponent(article.url);
                const checkResponse = await fetch(
                  `${ADMIN_NEWS_API}?newsLink=${encodedUrl}`,
                  {
                    method: 'GET',
                    cache: 'no-store'
                  }
                );

                if (checkResponse.ok) {
                  const dbEntry: NewsFromDB = await checkResponse.json();
                  articlesToAdd.push({
                    ...article,
                    isInDatabase: true,
                    isHidden: false, // Just unhidden
                    remarks: dbEntry.remarks,
                    geminiAnalysis: null,
                    analyzingWithGemini: false
                  });
                } else {
                  articlesToAdd.push({
                    ...article,
                    isInDatabase: false,
                    isHidden: false,
                    remarks: null,
                    geminiAnalysis: null,
                    analyzingWithGemini: false
                  });
                }
              } else {
                // Regular users don't have access to News database
                articlesToAdd.push({
                  ...article,
                  isInDatabase: false,
                  isHidden: false,
                  remarks: null,
                  geminiAnalysis: null,
                  analyzingWithGemini: false
                });
              }
            } catch (error) {
              console.error('Error checking database for unhidden article:', article.url, error);
            }
          }
        }
      }

      // Add the newly unhidden articles to the display
      if (articlesToAdd.length > 0) {
        setEnrichedArticles([...currentArticles, ...articlesToAdd]);
      }
    }
  };

  // Error state
  if (error) {
    return (
      <section className="py-20 min-h-screen relative z-10">
        <div className="flex items-center justify-center min-h-[60vh]">
          <div className="text-center bg-black/40 backdrop-blur-lg p-8 rounded-2xl border border-white/30 max-w-2xl">
            <IconAlertCircle className="w-16 h-16 text-red-400 mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-white mb-2">Failed to Load News</h2>
            <p className="text-white/90 mb-4">{errorMessage || 'Unable to fetch tariff news at this time'}</p>
            <button
              onClick={() => {
                setError(false);
                setErrorMessage('');
              }}
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
  if (newsData && enrichedArticles.length === 0 && !loading) {
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
      {/* Hidden Sources Panel - Sliding from left */}
      <div
        className={`fixed top-0 left-0 h-full w-96 bg-black/95 backdrop-blur-xl border-r-2 border-cyan-400/50 shadow-2xl transform transition-transform duration-300 ease-in-out z-50 ${showHiddenPanel ? 'translate-x-0' : '-translate-x-full'
          }`}
      >
        {/* Panel Header */}
        <div className="p-6 border-b-2 border-white/20">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-2xl font-bold text-white flex items-center gap-2">
              <IconAlertCircle className="w-6 h-6 text-cyan-300" />
              Hidden Sources
            </h2>
            <button
              onClick={() => setShowHiddenPanel(false)}
              className="text-white/70 hover:text-white transition-colors p-2 hover:bg-white/10 rounded-lg"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Unhide All Button */}
          {hiddenSources.length > 0 && (
            <button
              onClick={unhideAllSources}
              disabled={loadingHiddenSources}
              className="w-full bg-gradient-to-r from-orange-500/20 to-red-500/20 hover:from-orange-500/30 hover:to-red-500/30 disabled:from-gray-500/20 disabled:to-gray-500/20 text-orange-300 hover:text-orange-100 disabled:text-gray-400 font-bold px-4 py-2 rounded-lg border border-orange-400/40 hover:border-orange-300 disabled:border-gray-400/40 transition-all text-sm disabled:cursor-not-allowed"
            >
              {loadingHiddenSources ? (
                <>
                  <div className="inline-block w-4 h-4 border-2 border-orange-300/30 border-t-orange-300 rounded-full animate-spin mr-2"></div>
                  Unhiding all...
                </>
              ) : (
                `Unhide All (${hiddenSources.length})`
              )}
            </button>
          )}
        </div>

        {/* Panel Content - Scrollable */}
        <div className="h-[calc(100%-140px)] overflow-y-auto p-6">
          {loadingHiddenSources ? (
            <div className="flex items-center justify-center py-20">
              <div className="text-center">
                <div className="w-12 h-12 border-4 border-cyan-300/30 border-t-cyan-300 rounded-full animate-spin mx-auto mb-4"></div>
                <p className="text-white/70">Loading hidden sources...</p>
              </div>
            </div>
          ) : hiddenSources.length === 0 ? (
            <div className="text-center py-20">
              <IconAlertCircle className="w-16 h-16 text-white/30 mx-auto mb-4" />
              <p className="text-white/70 text-lg">No hidden sources</p>
              <p className="text-white/50 text-sm mt-2">Sources you hide will appear here</p>
            </div>
          ) : (
            <div className="space-y-4">
              {hiddenSources.map((source) => (
                <div
                  key={source.newsLink}
                  className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/20 hover:border-cyan-400/50 transition-all"
                >
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <a
                      href={source.newsLink}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-cyan-300 hover:text-cyan-100 text-sm font-medium underline break-all flex-1"
                    >
                      {source.newsLink}
                    </a>
                  </div>

                  {source.remarks && (
                    <p className="text-white/70 text-xs mb-3 italic">
                      &quot;{source.remarks}&quot;
                    </p>
                  )}

                  <button
                    onClick={() => unhideSource(source.newsLink)}
                    disabled={unhidingSource[source.newsLink]}
                    className="w-full bg-cyan-500/20 hover:bg-cyan-500/30 disabled:bg-gray-500/20 text-cyan-300 hover:text-cyan-100 disabled:text-gray-400 font-medium px-4 py-2 rounded-lg border border-cyan-400/40 hover:border-cyan-300 disabled:border-gray-400/40 transition-all text-sm disabled:cursor-not-allowed"
                  >
                    {unhidingSource[source.newsLink] ? (
                      <>
                        <div className="inline-block w-4 h-4 border-2 border-cyan-300/30 border-t-cyan-300 rounded-full animate-spin mr-2"></div>
                        Unhiding...
                      </>
                    ) : (
                      'Unhide this source'
                    )}
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Overlay when panel is open */}
      {showHiddenPanel && (
        <div
          className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40"
          onClick={() => setShowHiddenPanel(false)}
        />
      )}

      {/* Hidden Sources Button - Fixed on left side */}
      <button
        onClick={openHiddenPanel}
        className="fixed left-4 top-24 bg-gradient-to-r from-gray-500/30 to-gray-600/30 hover:from-gray-500/40 hover:to-gray-600/40 text-gray-200 hover:text-white font-bold px-4 py-3 rounded-lg border-2 border-gray-400/40 hover:border-gray-300 transition-all duration-200 shadow-lg hover:shadow-gray-500/30 z-30 flex items-center gap-2"
      >
        <IconAlertCircle className="w-5 h-5" />
        <span className="text-sm">Hidden Sources ({hiddenSources.length})</span>
      </button>

      <div className="max-w-6xl mx-auto px-4">

        {/* Header with Search */}
        <div className="text-center mb-10 bg-black/30 backdrop-blur-md p-6 rounded-2xl border border-white/30">
          <h1 className="text-5xl font-extrabold text-white mb-6 drop-shadow-lg">
            Latest Tariff News
          </h1>

          {/* Search Form */}
          <form onSubmit={handleSearch} className="max-w-4xl mx-auto mb-6">
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
          {newsData && !loading && (
            <>
              <p className="text-cyan-300 text-sm font-semibold">
                Query: &quot;{newsData.query}&quot;
              </p>
              <p className="text-white/90 text-lg font-medium mt-2">
                Status: {newsData.status} • {newsData.sourcesScraped} articles found
                {newsData.meta.minYear && <span className="text-cyan-300"> • From {newsData.meta.minYear} onwards</span>}
              </p>
              <div className="mt-4 bg-black/40 backdrop-blur-md p-3 rounded-xl border border-white/30">
                <p className="text-white/90 font-medium">
                  Scraped {newsData.sourcesScraped} of {newsData.totalSourcesFound} sources
                  {isAdmin && enrichedArticles.length > 0 && (
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
                        <h2 className="text-3xl font-bold text-white leading-tight flex-1">
                          {article.title}
                        </h2>
                        <div className="flex items-center gap-3 flex-shrink-0">
                          {isAdmin && article.isInDatabase && (
                            <span className="bg-green-500/20 text-green-300 px-3 py-1 rounded-full text-xs font-semibold border border-green-400/40">
                              ✓ In Database
                            </span>
                          )}
                          {isAdmin && article.isInDatabase && !article.geminiAnalysis && !article.analyzingWithGemini && (
                            <>
                              <button
                                onClick={() => runGeminiAnalysisForArticle(index, article)}
                                disabled={runningGeminiAnalysis[index]}
                                className="bg-gradient-to-r from-purple-500/30 to-pink-500/30 hover:from-purple-500/40 hover:to-pink-500/40 disabled:from-gray-500/30 disabled:to-gray-500/30 text-purple-200 hover:text-purple-100 disabled:text-gray-400 font-bold px-4 py-2 rounded-lg border border-purple-400/40 hover:border-purple-300 disabled:border-gray-400/40 transition-all duration-200 disabled:cursor-not-allowed text-sm flex items-center gap-2"
                              >
                                {runningGeminiAnalysis[index] ? (
                                  <>
                                    <div className="w-4 h-4 border-2 border-purple-300/30 border-t-purple-300 rounded-full animate-spin"></div>
                                    Running...
                                  </>
                                ) : (
                                  <>
                                    <IconDatabase className="w-4 h-4" />
                                    Run Gemini Analysis
                                  </>
                                )}
                              </button>
                              <button
                                onClick={() => openTariffModal(article, false)}
                                className="bg-gradient-to-r from-green-500/30 to-emerald-500/30 hover:from-green-500/40 hover:to-emerald-500/40 text-green-200 hover:text-green-100 font-bold px-4 py-2 rounded-lg border border-green-400/40 hover:border-green-300 transition-all duration-200 text-sm flex items-center gap-2"
                              >
                                <IconDatabase className="w-4 h-4" />
                                Save Tariff to Database
                              </button>
                            </>
                          )}
                        </div>
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

                  {/* Gemini Analysis Results - Admin Only */}
                  {isAdmin && article.analyzingWithGemini && (
                    <div className="bg-gradient-to-r from-purple-500/20 to-pink-500/20 border-2 border-purple-400/40 rounded-xl p-6 mb-6 shadow-lg">
                      <div className="flex items-center gap-3 mb-2">
                        <div className="w-6 h-6 border-3 border-purple-300/30 border-t-purple-300 rounded-full animate-spin"></div>
                        <span className="text-purple-100 font-bold text-lg">Analyzing with Gemini AI...</span>
                      </div>
                    </div>
                  )}

                  {isAdmin && article.geminiAnalysis && !article.analyzingWithGemini && (
                    <div className="bg-gradient-to-r from-cyan-500/20 to-blue-500/20 border-2 border-cyan-400/50 rounded-xl p-6 mb-6 shadow-lg">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-3">
                          <div className="bg-cyan-400/30 p-2 rounded-lg">
                            <IconDatabase className="w-6 h-6 text-cyan-200" />
                          </div>
                          <span className="text-cyan-100 font-bold text-lg">Tariff Analysis (AI-Extracted)</span>
                        </div>

                        {/* Save Tariff Rate Button - Only if in database */}
                        {article.isInDatabase && (
                          <button
                            onClick={() => openTariffModal(article)}
                            className="bg-gradient-to-r from-green-500/30 to-emerald-500/30 hover:from-green-500/40 hover:to-emerald-500/40 text-green-200 hover:text-green-100 font-bold px-4 py-2 rounded-lg border border-green-400/40 hover:border-green-300 transition-all duration-200 text-sm flex items-center gap-2"
                          >
                            <IconDatabase className="w-4 h-4" />
                            Save Tariff Rate to Database
                          </button>
                        )}
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
                        {/* Exporter Country */}
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                          <div className="flex items-center gap-2 mb-2">
                            <IconGlobe className="w-5 h-5 text-cyan-300" />
                            <span className="text-cyan-300 text-xs font-semibold uppercase">From Country</span>
                          </div>
                          <p className="text-white text-lg font-bold">
                            {article.geminiAnalysis.exporterCountry || 'N/A'}
                          </p>
                        </div>

                        {/* Importer Country */}
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                          <div className="flex items-center gap-2 mb-2">
                            <IconGlobe className="w-5 h-5 text-green-300" />
                            <span className="text-green-300 text-xs font-semibold uppercase">To Country</span>
                          </div>
                          <p className="text-white text-lg font-bold">
                            {article.geminiAnalysis.importerCountry || 'N/A'}
                          </p>
                        </div>

                        {/* Product */}
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                          <div className="flex items-center gap-2 mb-2">
                            <IconPackage className="w-5 h-5 text-yellow-300" />
                            <span className="text-yellow-300 text-xs font-semibold uppercase">Product</span>
                          </div>
                          <p className="text-white text-lg font-bold">
                            {article.geminiAnalysis.product || 'N/A'}
                          </p>
                        </div>

                        {/* Year */}
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                          <div className="flex items-center gap-2 mb-2">
                            <IconCalendarEvent className="w-5 h-5 text-purple-300" />
                            <span className="text-purple-300 text-xs font-semibold uppercase">Year</span>
                          </div>
                          <p className="text-white text-lg font-bold">
                            {article.geminiAnalysis.year || 'N/A'}
                          </p>
                        </div>

                        {/* Tariff Rate */}
                        <div className="bg-gradient-to-br from-orange-500/30 to-red-500/30 backdrop-blur-sm rounded-lg p-4 border-2 border-orange-400/50">
                          <div className="flex items-center gap-2 mb-2">
                            <IconPercentage className="w-5 h-5 text-orange-200" />
                            <span className="text-orange-200 text-xs font-semibold uppercase">Tariff Rate</span>
                          </div>
                          <p className="text-white text-2xl font-extrabold">
                            {article.geminiAnalysis.tariffRate || 'N/A'}
                          </p>
                        </div>
                      </div>
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
                    <div className="flex flex-col gap-2">
                      <a
                        href={article.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-2 bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 hover:text-cyan-100 font-bold px-6 py-3 rounded-lg border border-cyan-400/40 hover:border-cyan-300 transition-all duration-200 shadow-lg hover:shadow-cyan-500/30 w-fit"
                      >
                        Read Full Article →
                      </a>

                      {/* Hide this source button */}
                      <button
                        onClick={() => hideSource(index, article)}
                        disabled={hidingSource[index]}
                        className="inline-flex items-center gap-2 bg-gray-500/20 hover:bg-gray-500/30 disabled:bg-gray-500/10 text-gray-300 hover:text-gray-100 disabled:text-gray-500 font-medium px-6 py-2 rounded-lg border border-gray-400/40 hover:border-gray-300 disabled:border-gray-400/20 transition-all duration-200 shadow-lg hover:shadow-gray-500/30 w-fit text-sm disabled:cursor-not-allowed"
                      >
                        {hidingSource[index] ? (
                          <>
                            <div className="inline-block w-4 h-4 border-2 border-gray-300/30 border-t-gray-300 rounded-full animate-spin"></div>
                            Hiding...
                          </>
                        ) : (
                          <>
                            <IconAlertCircle className="w-4 h-4" />
                            Hide this source
                          </>
                        )}
                      </button>
                    </div>

                    {/* Remarks Section - Admin Only */}
                    {isAdmin && (
                      <div className="flex-1 min-w-0 flex flex-col gap-2">
                        {article.isInDatabase ? (
                          <>
                            <textarea
                              value={editingRemarks[index] !== undefined ? editingRemarks[index] : (article.remarks || '')}
                              onChange={(e) => handleRemarksChange(index, e.target.value)}
                              placeholder="No remarks added yet"
                              className="flex-1 bg-yellow-500/10 border border-yellow-400/30 rounded-lg p-4 text-yellow-100 text-sm placeholder-yellow-300/50 focus:outline-none focus:border-yellow-400/60 transition-all resize-none min-h-[100px]"
                            />
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
                            <textarea
                              value={editingRemarks[index] || ''}
                              onChange={(e) => handleRemarksChange(index, e.target.value)}
                              placeholder="Add remarks (optional)"
                              className="flex-1 bg-white/5 border border-white/20 rounded-lg p-4 text-white/90 text-sm placeholder-white/40 focus:outline-none focus:border-cyan-400/60 transition-all resize-none min-h-[100px]"
                            />
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
                    )}
                  </div>
                </div>
              ))}
            </div>

            {/* Stats Footer */}
            <div className="mt-10 text-center bg-black/30 backdrop-blur-md p-4 rounded-xl border border-white/30">
              <p className="text-white/90 font-medium">
                Scraped {newsData.sourcesScraped} of {newsData.totalSourcesFound} sources
                {newsData.meta.minYear && <span className="text-cyan-300"> • Filtered from {newsData.meta.minYear}</span>}
                {isAdmin && (
                  <>
                    {' • '}
                    <span className="text-green-300">
                      {enrichedArticles.filter(a => a.isInDatabase).length} already in database
                    </span>
                  </>
                )}
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

      {/* Tariff Rate Modal */}
      {showTariffModal && selectedArticleForTariff && (
        <>
          {/* Modal Overlay */}
          <div
            className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50"
            onClick={closeTariffModal}
          />

          {/* Modal Content */}
          <div className="fixed inset-0 flex items-center justify-center z-50 p-4">
            <div
              className="bg-white rounded-2xl shadow-2xl max-w-3xl w-full max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Modal Header */}
              <div className="pt-8 pb-4 px-6 border-b border-gray-200">
                <div className="flex items-center justify-between mb-2">
                  <h2 className="text-center text-2xl font-semibold text-gray-900 flex-1">Save Tariff Rate to Database</h2>
                  <button
                    onClick={closeTariffModal}
                    className="text-gray-400 hover:text-gray-600 transition-colors p-2 hover:bg-gray-100 rounded-lg"
                  >
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
                <p className="text-center text-sm text-gray-500 mt-2">
                  {selectedArticleForTariff.title.length > 100
                    ? selectedArticleForTariff.title.substring(0, 100) + '...'
                    : selectedArticleForTariff.title}
                </p>
              </div>

              {/* Modal Body */}
              <div className="px-6 py-6 space-y-6">
                {/* Instruction Text */}
                <p className="text-center text-sm text-gray-600">
                  Fill the fields and click Save to add this tariff rate to the database.
                </p>

                {/* Show Gemini extracted data as reference */}
                {selectedArticleForTariff.geminiAnalysis && (
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                    <p className="text-blue-900 text-sm font-semibold mb-2">✨ AI-Extracted Data (Pre-filled below):</p>
                    <div className="text-blue-800 text-xs space-y-1">
                      <p>• Country: {selectedArticleForTariff.geminiAnalysis.exporterCountry}</p>
                      <p>• Partner: {selectedArticleForTariff.geminiAnalysis.importerCountry}</p>
                      <p>• Product: {selectedArticleForTariff.geminiAnalysis.product}</p>
                      <p>• Year: {selectedArticleForTariff.geminiAnalysis.year}</p>
                      <p>• Rate: {selectedArticleForTariff.geminiAnalysis.tariffRate}</p>
                    </div>
                    <p className="text-amber-700 text-xs mt-2 italic font-semibold">⚠️ Please verify and convert to ISO/HS codes as needed.</p>
                  </div>
                )}

                {/* Form Fields in Grid Layout (matching admin dashboard) */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* From Country (Exporter) */}
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-gray-700">
                      From Country (Exporter) <span className="text-red-500">*</span>
                    </label>
                    <Select value={tariffFormData.countryId} onValueChange={(value) => handleTariffFormChange('countryId', value)}>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select exporting country" />
                      </SelectTrigger>
                      <SelectContent className="max-h-[300px]">
                        {countries.map((country) => (
                          <SelectItem key={country.code} value={country.code}>
                            {country.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {/* To Country (Importer) */}
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-gray-700">
                      To Country (Importer)
                    </label>
                    <Select value={tariffFormData.partnerCountryId} onValueChange={(value) => handleTariffFormChange('partnerCountryId', value)}>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select importing country" />
                      </SelectTrigger>
                      <SelectContent className="max-h-[300px]">
                        {countries.map((country) => (
                          <SelectItem key={country.code} value={country.code}>
                            {country.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Agricultural Product */}
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-gray-700">
                      Agricultural Product <span className="text-red-500">*</span>
                    </label>
                    <Select value={tariffFormData.productId} onValueChange={(value) => handleTariffFormChange('productId', value)}>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select product type" />
                      </SelectTrigger>
                      <SelectContent>
                        {agriculturalProducts.map((product) => (
                          <SelectItem key={product.hs_code} value={product.hs_code}>
                            {product.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Year */}
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-gray-700">
                      Year <span className="text-red-500">*</span>
                    </label>
                    <Select value={tariffFormData.year} onValueChange={(value) => handleTariffFormChange('year', value)}>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select year" />
                      </SelectTrigger>
                      <SelectContent>
                        {[2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017, 2016, 2015, 2010, 2005, 2000, 1998, 1995, 1990].map((yr) => (
                          <SelectItem key={yr} value={String(yr)}>
                            {yr}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Tariff Rate */}
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-gray-700">
                      Tariff Rate (%) <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="number"
                      step="any"
                      value={tariffFormData.rate}
                      onChange={(e) => handleTariffFormChange('rate', e.target.value)}
                      placeholder="e.g., 11.12345"
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-gray-900 placeholder-gray-400"
                    />
                  </div>
                </div>
              </div>

              {/* Modal Footer */}
              <div className="border-t border-gray-200 px-6 py-6 flex gap-4 justify-center">
                <button
                  onClick={closeTariffModal}
                  disabled={savingTariffRate}
                  className="px-6 py-2 border border-gray-300 rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={saveTariffRateToDatabase}
                  disabled={savingTariffRate}
                  className="px-6 py-2 border border-transparent rounded-md shadow-sm text-white bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                >
                  {savingTariffRate ? 'Saving...' : 'Save to Database'}
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </section>
  );
}