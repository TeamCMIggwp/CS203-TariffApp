"use client"
import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  BookOpen,
  Globe,
  TrendingUp,
  Shield,
  Users,
  BarChart3,
  Leaf,
  Percent,
  TrendingDown,
  Scale,
  Lock,
  CheckCircle,
  Calendar,
  ArrowRight,
  Activity,
} from "lucide-react"

const tariffFeatures = [
  {
    id: 1,
    title: "Real-Time Calculations",
    description: "Instant tariff calculations for agricultural products across 195+ countries with up-to-date rates",
    image: "/pic1.jpg",
    number: "01",
  },
  {
    id: 2,
    title: "Global Database Access",
    description:
      "Comprehensive WTO tariff data covering MFN rates, preferential agreements, and bound tariff commitments",
    image: "/pic2.jpg",
    number: "02",
  },
  {
    id: 3,
    title: "Trade Compliance Tools",
    description: "Navigate complex trade regulations with confidence using our compliance verification system",
    image: "/pic3.jpg",
    number: "03",
  },
]

function DynamicRates() {
  const countries = [
    { name: "USA", code: "840" },
    { name: "Singapore", code: "702" },
    { name: "China", code: "156" },
    { name: "India", code: "356" },
    { name: "Australia", code: "036" },
  ]

  const [currentCountry, setCurrentCountry] = useState(0)
  const [isAnimating, setIsAnimating] = useState(false)
  const [currentRate, setCurrentRate] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [cachedRates, setCachedRates] = useState<Map<string, number>>(new Map())

  const fetchTariffData = async (countryCode: string): Promise<number | null> => {
    try {
      const API_BASE = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
      const indicator = 'TP_A_0160'
      const currentYear = (new Date().getFullYear() - 1).toString()
      const previousYear = (new Date().getFullYear() - 2).toString()

      const url = new URL(`${API_BASE}/api/v1/indicators/${encodeURIComponent(indicator)}/observations`)
      url.searchParams.set('r', countryCode)
      url.searchParams.set('ps', currentYear)
      url.searchParams.set('fmt', 'json')
      url.searchParams.set('mode', 'full')
      url.searchParams.set('echo', 'false')

      const res = await fetch(url.toString(), { credentials: 'include' })
      const text = await res.text()

      if (!res.ok) {
        url.searchParams.set('ps', previousYear)
        const retryRes = await fetch(url.toString(), { credentials: 'include' })
        const retryText = await retryRes.text()

        if (!retryRes.ok || !retryText || retryText.trim() === '') {
          return null
        }

        try {
          const json = JSON.parse(retryText)
          const value = extractValueFromObj(json, previousYear)
          return value
        } catch (parseError) {
          return null
        }
      }

      if (!text || text.trim() === '') {
        return null
      }

      try {
        const json = JSON.parse(text)
        const value = extractValueFromObj(json, currentYear)
        return value
      } catch (parseError) {
        return null
      }
    } catch (error) {
      return null
    }
  }

  const extractValueFromObj = (obj: unknown, preferYear?: string): number | null => {
    if (obj == null) return null

    if (Array.isArray(obj)) {
      for (const el of obj) {
        if (el && typeof el === "object" && "Value" in el && el.Value != null && !isNaN(Number(el.Value))) {
          const yearVal = el.Year ?? null
          if (preferYear && yearVal != null && String(yearVal) === String(preferYear)) return Number(el.Value)
        }
      }
      for (const el of obj) {
        const v = extractValueFromObj(el, preferYear)
        if (v !== null) return v
      }
      return null
    }

    if (typeof obj === "object") {
      const rec = obj as Record<string, unknown>
      if ("Value" in rec && rec.Value != null && !isNaN(Number(rec.Value))) {
        const hasYearField = Object.prototype.hasOwnProperty.call(rec, "Year")
        const yearVal = rec["Year"] ?? null
        if (hasYearField && yearVal != null && preferYear && String(yearVal) === String(preferYear)) {
          return Number(rec.Value)
        }
        if (!hasYearField || yearVal == null) {
          return Number(rec.Value)
        }
      }
      for (const k of Object.keys(rec)) {
        const v = extractValueFromObj(rec[k], preferYear)
        if (v !== null) return v
      }
    }

    return null
  }

  useEffect(() => {
    let isMounted = true

    const loadAllData = async () => {
      const ratesMap = new Map<string, number>()

      for (const country of countries) {
        const rate = await fetchTariffData(country.code)
        if (rate !== null) {
          ratesMap.set(country.code, rate)
        }
      }

      if (isMounted) {
        setCachedRates(ratesMap)
        const firstCountryWithData = countries.find(c => ratesMap.has(c.code))
        if (firstCountryWithData) {
          const rate = ratesMap.get(firstCountryWithData.code)!
          setCurrentRate(rate)
          setCurrentCountry(countries.indexOf(firstCountryWithData))
        }
        setLoading(false)
      }
    }

    loadAllData()

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    if (loading || cachedRates.size === 0) return

    const interval = setInterval(() => {
      setIsAnimating(true)
      setTimeout(() => {
        let nextIndex = (currentCountry + 1) % countries.length
        let attempts = 0

        while (!cachedRates.has(countries[nextIndex].code) && attempts < countries.length) {
          nextIndex = (nextIndex + 1) % countries.length
          attempts++
        }

        if (cachedRates.has(countries[nextIndex].code)) {
          const country = countries[nextIndex]
          const rate = cachedRates.get(country.code)!
          setCurrentCountry(nextIndex)
          setCurrentRate(rate)
        }

        setTimeout(() => setIsAnimating(false), 50)
      }, 300)
    }, 3000)

    return () => {
      clearInterval(interval)
    }
  }, [currentCountry, loading, cachedRates])

  return (
    <div className="relative">
      <div className="relative bg-black/80 backdrop-blur-xl border-2 border-white/30 rounded-2xl p-12 shadow-2xl">
        <div className="max-w-4xl mx-auto">
          <div className="flex items-center justify-center gap-3 mb-8">
            <Activity className="w-6 h-6 text-cyan-300 animate-pulse" />
            <h3 className="text-2xl font-bold text-white">Live Tariff Updates</h3>
            <Activity className="w-6 h-6 text-cyan-300 animate-pulse" />
          </div>

          <div className="flex flex-col md:flex-row items-center justify-center gap-8 md:gap-16">
            <div className="text-center min-w-[200px]">
              <p className="text-sm font-semibold tracking-wider text-white/80 mb-2">COUNTRY</p>
              <p
                className={`text-3xl font-bold text-white transition-all duration-300 ${
                  isAnimating ? "scale-110 opacity-0 blur-sm" : "scale-100 opacity-100 blur-0"
                }`}
              >
                {countries[currentCountry].name}
              </p>
            </div>

            <div className="text-center min-w-[250px]">
              <p className="text-sm font-semibold tracking-wider text-white/80 mb-2">CURRENT RATE</p>
              <div
                className={`text-7xl font-bold tabular-nums text-white transition-all duration-300 ${
                  isAnimating ? "scale-110 opacity-0 blur-sm" : "scale-100 opacity-100 blur-0"
                }`}
              >
                {loading ? (
                  <span className="text-4xl">Loading...</span>
                ) : currentRate !== null ? (
                  <>
                    {currentRate.toFixed(1)}
                    <span className="text-4xl text-white/80">%</span>
                  </>
                ) : (
                  <span className="text-3xl">N/A</span>
                )}
              </div>
            </div>
          </div>

          <div className="mt-8 text-center">
            <p className="text-sm text-white/90">
              Agricultural tariff rates from WTO Database (TP_A_0160 - Simple Average MFN Applied Tariff)
            </p>
            {cachedRates.size > 0 && (
              <p className="text-xs text-white/70 mt-2">
                Displaying {cachedRates.size} of {countries.length} countries with available data
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default function AboutPage() {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null)
  const [newsArticles, setNewsArticles] = useState<Array<{
    title: string
    description: string
    url: string
    publishedAt: string
    source: string
  }>>([])
  const [loadingNews, setLoadingNews] = useState(true)

  useEffect(() => {
    const fetchNews = async () => {
      try {
        const response = await fetch('/api/external/news')
        if (response.ok) {
          const data = await response.json()
          setNewsArticles(data.articles || [])
        }
      } catch (error) {
        console.error('Error fetching news:', error)
      } finally {
        setLoadingNews(false)
      }
    }

    fetchNews()
  }, [])

  const indicators = [
    {
      code: "HSP_0070",
      icon: TrendingDown,
      title: "Lowest Preferential Tariff",
      description: "Simple average ad valorem tariff at HS 6-digit level",
      details:
        "Measures the lowest tariff rate applied to imports from preferential trading partners. This indicator helps identify the most favorable market access conditions available through trade agreements.",
      color: "bg-blue-500",
    },
    {
      code: "TP_A_0160",
      icon: Percent,
      title: "Simple Average MFN Applied Tariff",
      description: "Agricultural products",
      details:
        "The arithmetic mean of applied Most Favored Nation (MFN) tariff rates for agricultural products. This represents the standard tariff rate applied to WTO members without preferential agreements.",
      color: "bg-green-500",
    },
    {
      code: "TP_A_0170",
      icon: Scale,
      title: "Trade-Weighted MFN Applied Tariff",
      description: "Agricultural products",
      details:
        "Weighted average of applied MFN tariffs, where weights are based on import values. This provides a more accurate picture of the actual tariff burden on agricultural trade.",
      color: "bg-amber-500",
    },
    {
      code: "TP_B_0180",
      icon: Lock,
      title: "Simple Average Bound Tariff",
      description: "Agricultural products",
      details:
        "The average of maximum tariff rates that WTO members have committed not to exceed. Bound tariffs provide predictability and stability in international trade relations.",
      color: "bg-purple-500",
    },
    {
      code: "TP_B_0190",
      icon: CheckCircle,
      title: "Percentage of Bound Tariff Lines",
      description: "Agricultural products",
      details:
        "The proportion of agricultural tariff lines that have binding commitments. Higher percentages indicate greater trade policy predictability and market access security.",
      color: "bg-rose-500",
    },
  ]

  const reasons = [
    {
      icon: Shield,
      title: "Food Security",
      description:
        "Tariffs help protect domestic agricultural production, ensuring stable food supplies and reducing dependency on imports during global crises.",
    },
    {
      icon: Users,
      title: "Farmer Protection",
      description:
        "Agricultural tariffs shield local farmers from unfair competition and price volatility in international markets, supporting rural livelihoods.",
    },
    {
      icon: BarChart3,
      title: "Market Transparency",
      description:
        "Standardized tariff data enables businesses, policymakers, and researchers to analyze trade patterns and make evidence-based decisions.",
    },
    {
      icon: Leaf,
      title: "Sustainable Development",
      description:
        "Well-designed tariff policies can promote sustainable agricultural practices and support developing countries' economic growth.",
    },
  ]

  const formatDate = (dateString: string): string => {
    try {
      const date = new Date(dateString)
      const now = new Date()
      const diffTime = Math.abs(now.getTime() - date.getTime())
      const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))

      if (diffDays === 0) return 'Today'
      if (diffDays === 1) return 'Yesterday'
      if (diffDays < 7) return `${diffDays} days ago`

      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    } catch {
      return 'Recent'
    }
  }

  const truncateDescription = (text: string, maxLength: number = 200): string => {
    if (text.length <= maxLength) return text
    return text.substring(0, maxLength).trim() + '...'
  }

  return (
    <main className="relative min-h-screen">
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          {/* Hero Content */}
          <div className="text-center mb-16 bg-black/60 backdrop-blur-xl p-8 rounded-2xl border-2 border-white/30">
            <p className="text-sm font-semibold tracking-[0.3em] text-white/80 mb-6">ABOUT</p>
            <h1 className="text-5xl md:text-6xl lg:text-7xl font-bold mb-6 text-balance leading-tight text-white">
              AgriTariff
            </h1>
            <p className="text-lg md:text-xl text-white/90 max-w-2xl mx-auto">
              Calculate tariffs with confidence and clarity.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8 max-w-6xl mx-auto">
            {tariffFeatures.map((feature) => (
              <div key={feature.id} className="relative">
                <div className="relative group">
                  <div className="relative overflow-hidden rounded-3xl bg-black/60 aspect-[3/4] mb-6 border-2 border-white/30">
                    <img
                      src={feature.image || "/placeholder.svg"}
                      alt={feature.title}
                      className="w-full h-full object-cover opacity-70"
                    />
                  </div>

                  <div className="bg-black/80 backdrop-blur-xl border-2 border-white/30 rounded-2xl p-6">
                    <div className="flex items-start gap-4">
                      <span className="text-4xl font-bold text-white">{feature.number}</span>
                      <div className="flex-1">
                        <h3 className="text-xl font-bold text-white mb-2">{feature.title}</h3>
                        <p className="text-sm text-white/90 leading-relaxed">{feature.description}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* What Are Tariffs Section */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="mb-12 max-w-3xl bg-black/60 backdrop-blur-xl p-8 rounded-2xl border-2 border-white/30">
            <h2 className="text-4xl font-bold mb-4 text-white">What Are Tariffs?</h2>
            <p className="text-lg text-white/90 leading-relaxed">
              Tariffs are taxes imposed by governments on imported goods. In agricultural trade, they serve as crucial
              policy instruments that affect food security, farmer livelihoods, and international trade relationships.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-6">
            <Card className="bg-black/80 backdrop-blur-xl border-2 border-white/30 hover:border-cyan-400/50 transition-colors">
              <CardContent className="pt-6">
                <div className="w-12 h-12 rounded-lg bg-cyan-500/20 border-2 border-cyan-400/40 flex items-center justify-center mb-4">
                  <BookOpen className="w-6 h-6 text-cyan-300" />
                </div>
                <h3 className="text-xl font-semibold mb-3 text-white">Historical Context</h3>
                <p className="text-white/90 leading-relaxed">
                  Agricultural tariffs have evolved since the GATT Uruguay Round (1986-1994), which transformed
                  non-tariff barriers into transparent tariff measures.
                </p>
              </CardContent>
            </Card>

            <Card className="bg-black/80 backdrop-blur-xl border-2 border-white/30 hover:border-cyan-400/50 transition-colors">
              <CardContent className="pt-6">
                <div className="w-12 h-12 rounded-lg bg-cyan-500/20 border-2 border-cyan-400/40 flex items-center justify-center mb-4">
                  <Globe className="w-6 h-6 text-cyan-300" />
                </div>
                <h3 className="text-xl font-semibold mb-3 text-white">Global Impact</h3>
                <p className="text-white/90 leading-relaxed">
                  Tariffs directly influence food prices, market access for farmers, and the competitiveness of
                  agricultural products in international markets.
                </p>
              </CardContent>
            </Card>

            <Card className="bg-black/80 backdrop-blur-xl border-2 border-white/30 hover:border-cyan-400/50 transition-colors">
              <CardContent className="pt-6">
                <div className="w-12 h-12 rounded-lg bg-cyan-500/20 border-2 border-cyan-400/40 flex items-center justify-center mb-4">
                  <TrendingUp className="w-6 h-6 text-cyan-300" />
                </div>
                <h3 className="text-xl font-semibold mb-3 text-white">Trade Dynamics</h3>
                <p className="text-white/90 leading-relaxed">
                  Understanding tariff structures helps stakeholders navigate complex trade agreements and make informed
                  decisions about market entry strategies.
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      {/* Why It Matters Section */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16 bg-black/60 backdrop-blur-xl p-8 rounded-2xl border-2 border-white/30">
            <h2 className="text-4xl font-bold mb-4 text-white">Why Tariffs Matter</h2>
            <p className="text-lg text-white/90 max-w-3xl mx-auto leading-relaxed">
              Agricultural tariffs play a critical role in shaping global food systems, economic development, and
              international trade relationships
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-8">
            {reasons.map((reason, index) => {
              const Icon = reason.icon
              return (
                <Card key={index} className="bg-black/80 backdrop-blur-xl border-2 border-white/30 hover:border-cyan-400/50 transition-colors">
                  <CardContent className="pt-6">
                    <div className="flex gap-4">
                      <div className="flex-shrink-0">
                        <div className="w-14 h-14 rounded-xl bg-cyan-500/20 border-2 border-cyan-400/40 text-cyan-300 flex items-center justify-center">
                          <Icon className="w-7 h-7" />
                        </div>
                      </div>
                      <div>
                        <h3 className="text-xl font-semibold mb-2 text-white">{reason.title}</h3>
                        <p className="text-white/90 leading-relaxed">{reason.description}</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )
            })}
          </div>
        </div>
      </section>

      {/* Tariff Indicators Section */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16 bg-black/60 backdrop-blur-xl p-8 rounded-2xl border-2 border-white/30">
            <h2 className="text-4xl font-bold mb-4 text-white">Key Tariff Indicators</h2>
            <p className="text-lg text-white/90 max-w-3xl mx-auto leading-relaxed">
              Understanding WTO tariff indicators is essential for analyzing agricultural trade policies and market
              access conditions across countries. Hover over each card to see detailed information.
            </p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
            {indicators.map((indicator, index) => {
              const Icon = indicator.icon
              const isHovered = hoveredIndex === index

              return (
                <Card
                  key={index}
                  className="bg-black/80 backdrop-blur-xl border-2 border-white/30 hover:border-cyan-400/50 hover:shadow-xl transition-all duration-300 cursor-pointer relative overflow-hidden aspect-square flex flex-col"
                  onMouseEnter={() => setHoveredIndex(index)}
                  onMouseLeave={() => setHoveredIndex(null)}
                >
                  <div
                    className={`absolute inset-0 p-4 flex flex-col items-center justify-center text-center transition-opacity duration-300 ${
                      isHovered ? "opacity-0" : "opacity-100"
                    }`}
                  >
                    <div
                      className={`w-12 h-12 rounded-lg ${indicator.color} text-white flex items-center justify-center mb-3`}
                    >
                      <Icon className="w-6 h-6" />
                    </div>
                    <h3 className="font-bold text-sm mb-2 leading-tight text-white">{indicator.title}</h3>
                    <p className="text-xs font-mono text-white/80">{indicator.code}</p>
                  </div>

                  <div
                    className={`absolute inset-0 p-4 flex flex-col transition-opacity duration-300 ${
                      isHovered ? "opacity-100" : "opacity-0"
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-2">
                      <div
                        className={`w-6 h-6 rounded ${indicator.color} text-white flex items-center justify-center flex-shrink-0`}
                      >
                        <Icon className="w-3 h-3" />
                      </div>
                      <p className="text-xs font-mono text-white/80">{indicator.code}</p>
                    </div>
                    <h3 className="font-bold text-xs mb-2 leading-tight text-white">{indicator.title}</h3>
                    <p className="text-xs text-white/90 mb-2 leading-relaxed">{indicator.description}</p>
                    <p className="text-xs text-white/90 leading-relaxed flex-1 overflow-y-auto">{indicator.details}</p>
                  </div>
                </Card>
              )
            })}
          </div>
        </div>
      </section>

      {/* Live Tariff Updates Section */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <DynamicRates />
        </div>
      </section>

      {/* News Section */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="flex items-center justify-between mb-12 bg-black/60 backdrop-blur-xl p-8 rounded-2xl border-2 border-white/30">
            <div>
              <h2 className="text-4xl font-bold mb-2 text-white">Latest News</h2>
              <p className="text-lg text-white/90">Stay updated on agricultural trade developments</p>
            </div>
          </div>

          <div className="grid md:grid-cols-3 gap-6">
            {loadingNews ? (
              Array.from({ length: 3 }).map((_, index) => (
                <Card key={index} className="flex flex-col bg-black/80 backdrop-blur-xl border-2 border-white/30">
                  <CardHeader>
                    <div className="h-6 bg-white/10 rounded animate-pulse mb-3 w-24"></div>
                    <div className="h-8 bg-white/10 rounded animate-pulse"></div>
                  </CardHeader>
                  <CardContent className="flex-1">
                    <div className="space-y-2">
                      <div className="h-4 bg-white/10 rounded animate-pulse"></div>
                      <div className="h-4 bg-white/10 rounded animate-pulse"></div>
                      <div className="h-4 bg-white/10 rounded animate-pulse w-3/4"></div>
                    </div>
                  </CardContent>
                </Card>
              ))
            ) : newsArticles.length > 0 ? (
              newsArticles.map((article, index) => (
                <Card key={index} className="flex flex-col bg-black/80 backdrop-blur-xl border-2 border-white/30 hover:border-cyan-400/50 hover:shadow-lg transition-all">
                  <CardHeader>
                    <div className="flex items-center gap-2 mb-3">
                      <Badge variant="secondary" className="bg-cyan-500/20 text-cyan-300 border-2 border-cyan-400/40">{article.source}</Badge>
                      <div className="flex items-center gap-1 text-sm text-white/80">
                        <Calendar className="w-4 h-4" />
                        <span>{formatDate(article.publishedAt)}</span>
                      </div>
                    </div>
                    <CardTitle className="text-xl leading-tight text-balance text-white">
                      {article.title}
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="flex-1 flex flex-col">
                    <CardDescription className="text-base text-white/90 leading-relaxed mb-4 flex-1 line-clamp-4">
                      {truncateDescription(article.description)}
                    </CardDescription>
                    <Button
                      variant="ghost"
                      className="w-fit px-0 group text-cyan-300 hover:text-cyan-100 hover:bg-transparent"
                      onClick={() => window.open(article.url, '_blank')}
                    >
                      Read more
                      <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
                    </Button>
                  </CardContent>
                </Card>
              ))
            ) : (
              <div className="col-span-3 text-center py-12">
                <p className="text-white/80 text-lg">No news articles available at the moment</p>
              </div>
            )}
          </div>
        </div>
      </section>
    </main>
  )
}