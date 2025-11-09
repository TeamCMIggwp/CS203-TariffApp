import { NextResponse } from 'next/server';

/**
 * Fetches live agricultural and trade news from NewsData.io API
 * GET /api/external/news
 */
export async function GET() {
  try {
    // Using NewsData.io free tier API
    // Search for agricultural trade, tariff, and WTO related news
    const apiKey = process.env.NEWSDATA_API_KEY || 'pub_61247c05cf2cba8a3d8c6cf9bb1095823a6d4';
    const query = 'agriculture trade OR tariff OR WTO OR agricultural exports OR trade policy';
    const url = `https://newsdata.io/api/1/news?apikey=${apiKey}&q=${encodeURIComponent(query)}&language=en&category=business,politics&size=3`;

    const response = await fetch(url, {
      headers: {
        'Accept': 'application/json',
      },
      // Cache for 1 hour to avoid hitting API rate limits
      next: { revalidate: 3600 }
    });

    if (!response.ok) {
      console.error('NewsData API error:', response.status);
      // Return fallback news if API fails
      return NextResponse.json({
        articles: getFallbackNews()
      });
    }

    const data = await response.json();

    // Transform NewsData.io response to our format
    const articles = (data.results || []).slice(0, 3).map((article: NewsDataArticle) => ({
      title: article.title,
      description: article.description || article.content || 'Read more about this agricultural trade development',
      url: article.link,
      publishedAt: article.pubDate,
      source: article.source_id || 'News'
    }));

    return NextResponse.json({ articles });
  } catch (error) {
    console.error('Error fetching external news:', error);
    // Return fallback news on error
    return NextResponse.json({
      articles: getFallbackNews()
    });
  }
}

interface NewsDataArticle {
  title: string;
  description?: string;
  content?: string;
  link: string;
  pubDate: string;
  source_id?: string;
}

// Fallback news articles if external API fails
function getFallbackNews() {
  return [
    {
      title: "WTO Agricultural Trade Monitoring Update",
      description: "The World Trade Organization releases its latest report on agricultural trade policies and market access conditions across member countries.",
      url: "https://www.wto.org/english/tratop_e/agric_e/agric_e.htm",
      publishedAt: new Date().toISOString(),
      source: "WTO"
    },
    {
      title: "Global Food Security and Trade Policy Developments",
      description: "Recent developments in international agricultural trade agreements and their impact on global food security and farmer livelihoods.",
      url: "https://www.fao.org/trade/en/",
      publishedAt: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
      source: "FAO"
    },
    {
      title: "Agricultural Tariff Trends and Market Analysis",
      description: "Analysis of current agricultural tariff trends, including changes in MFN rates and preferential trade agreements affecting global markets.",
      url: "https://www.trade.gov/agriculture",
      publishedAt: new Date(Date.now() - 172800000).toISOString(), // 2 days ago
      source: "Trade.gov"
    }
  ];
}
