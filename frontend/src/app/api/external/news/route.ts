import { NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080';

/**
 * Fetches live agricultural and trade news using the same scrape endpoint as the News page
 * GET /api/external/news
 */
export async function GET() {
  try {
    // Use the same query and parameters as the News page
    const searchQuery = 'tariff';
    const maxResults = 10; // Request more to increase chances of getting at least 3
    const minYear = new Date().getFullYear() - 2; // Go back 2 years for more results

    const queryParams = new URLSearchParams({
      query: searchQuery,
      maxResults: maxResults.toString(),
      minYear: minYear.toString()
    });

    const response = await fetch(`${BACKEND_URL}/api/v1/scrape?${queryParams.toString()}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
      // Cache for 1 hour to improve performance
      next: { revalidate: 3600 }
    });

    if (!response.ok) {
      console.error('Scrape API error:', response.status);
      const errorText = await response.text();
      console.error('Error details:', errorText);

      // Return empty array if API fails - no fallback
      return NextResponse.json(
        {
          articles: [],
          error: `Failed to fetch news: ${response.status} ${response.statusText}`
        },
        { status: response.status }
      );
    }

    const data = await response.json();

    // Transform scrape API response to our format
    const articles = (data.articles || []).slice(0, 3).map((article: ScrapedArticle) => ({
      title: article.title,
      description: article.relevantText?.[0] || 'Click to read more about this agricultural trade development',
      url: article.url,
      publishedAt: new Date().toISOString(), // Scrape API doesn't return dates, use current time
      source: article.sourceDomain || 'News'
    }));

    return NextResponse.json({ articles });
  } catch (error) {
    console.error('Error fetching news:', error);

    // Return empty array on error - no fallback
    return NextResponse.json(
      {
        articles: [],
        error: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    );
  }
}

interface ScrapedArticle {
  url: string;
  title: string;
  sourceDomain: string;
  relevantText?: string[];
}
