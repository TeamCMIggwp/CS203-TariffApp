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
    { name: "China", rates: [12.5, 11.8, 13.2, 12.1, 11.9, 12.7], trend: "up" },
    { name: "Singapore", rates: [0.0, 0.0, 0.5, 0.2, 0.0, 0.3], trend: "stable" },
    { name: "USA", rates: [5.3, 5.1, 5.5, 5.2, 5.4, 5.0], trend: "down" },
    { name: "EU", rates: [8.7, 8.9, 8.5, 8.8, 8.6, 8.7], trend: "stable" },
    { name: "India", rates: [33.5, 34.2, 33.8, 34.0, 33.6, 34.1], trend: "up" },
    { name: "Brazil", rates: [10.2, 10.5, 10.1, 10.3, 10.4, 10.2], trend: "down" },
  ]

  const [currentIndex, setCurrentIndex] = useState(0)
  const [currentCountry, setCurrentCountry] = useState(0)
  const [isAnimating, setIsAnimating] = useState(false)

  useEffect(() => {
    const interval = setInterval(() => {
      setIsAnimating(true)
      setTimeout(() => {
        let nextCountry = currentCountry
        do {
          nextCountry = Math.floor(Math.random() * countries.length)
        } while (nextCountry === currentCountry)

        setCurrentCountry(nextCountry)
        setCurrentIndex(Math.floor(Math.random() * countries[nextCountry].rates.length))

        setTimeout(() => setIsAnimating(false), 50)
      }, 300)
    }, 2000)

    return () => clearInterval(interval)
  }, [currentCountry])

  const currentRate = countries[currentCountry].rates[currentIndex]
  const currentTrend = countries[currentCountry].trend

  const getTrendIcon = () => {
    if (currentTrend === "up") return <TrendingUp className="w-10 h-10 text-green-500" />
    if (currentTrend === "down") return <TrendingDown className="w-10 h-10 text-red-500" />
    return <Scale className="w-10 h-10 text-gray-500" />
  }

  return (
    <div className="relative">
      <div className="absolute inset-0 bg-gradient-to-r from-primary/5 via-primary/10 to-primary/5 rounded-2xl" />
      <div className="relative bg-background/80 backdrop-blur-sm border-2 border-primary/20 rounded-2xl p-12 shadow-2xl">
        <div className="max-w-4xl mx-auto">
          <div className="flex items-center justify-center gap-3 mb-8">
            <Activity className="w-6 h-6 text-primary animate-pulse" />
            <h3 className="text-2xl font-bold text-foreground">Live Tariff Updates</h3>
            <Activity className="w-6 h-6 text-primary animate-pulse" />
          </div>

          <div className="flex flex-col md:flex-row items-center justify-center gap-8 md:gap-16">
            {/* Country */}
            <div className="text-center">
              <p className="text-sm font-semibold tracking-wider text-muted-foreground mb-2">COUNTRY</p>
              <p
                className={`text-3xl font-bold text-foreground transition-all duration-300 ${
                  isAnimating ? "scale-110 opacity-0 blur-sm" : "scale-100 opacity-100 blur-0"
                }`}
              >
                {countries[currentCountry].name}
              </p>
            </div>

            {/* Current Rate */}
            <div className="flex items-center gap-6">
              <div className="text-center">
                <p className="text-sm font-semibold tracking-wider text-muted-foreground mb-2">CURRENT RATE</p>
                <div
                  className={`text-7xl font-bold tabular-nums text-black transition-all duration-300 ${
                    isAnimating ? "scale-110 opacity-0 blur-sm" : "scale-100 opacity-100 blur-0"
                  }`}
                >
                  {currentRate.toFixed(1)}
                  <span className="text-4xl text-muted-foreground">%</span>
                </div>
              </div>

              <div
                className={`transition-all duration-300 ${
                  isAnimating ? "opacity-0 scale-50" : "opacity-100 scale-100"
                }`}
              >
                {getTrendIcon()}
              </div>
            </div>

            {/* Trend */}
            <div className="text-center">
              <p className="text-sm font-semibold tracking-wider text-muted-foreground mb-2">TREND</p>
              <p className="text-2xl font-semibold capitalize text-foreground">{currentTrend}</p>
            </div>
          </div>

          <div className="mt-8 text-center">
            <p className="text-sm text-gray-700">
              Agricultural tariff rates updating every 2 seconds • Data from WTO Database
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
export default function AboutPage() {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null)

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

  const news = [
    {
      title: "WTO Members Agree on New Agricultural Trade Framework",
      description:
        "Major breakthrough in negotiations as 164 member countries reach consensus on reducing agricultural subsidies and tariff barriers.",
      date: "March 15, 2024",
      category: "Policy",
      link: "#",
    },
    {
      title: "Global Food Prices Stabilize Amid Tariff Reforms",
      description:
        "Recent tariff reductions in key agricultural markets have contributed to more stable commodity prices worldwide.",
      date: "March 10, 2024",
      category: "Market Analysis",
      link: "#",
    },
    {
      title: "New Database Release: 2024 Tariff Indicators Now Available",
      description:
        "The latest comprehensive dataset covering agricultural tariff rates for 195 countries is now accessible through AgriTariff.",
      date: "March 5, 2024",
      category: "Data Release",
      link: "#",
    },
  ]

  return (
    <main className="relative min-h-screen bg-white/45 backdrop-blur-lg">
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          {/* Hero Content */}
          <div className="text-center mb-16">
            <p className="text-sm font-semibold tracking-[0.3em] text-gray-400 mb-6">ABOUT</p>
            <h1 className="text-5xl md:text-6xl lg:text-7xl font-bold mb-6 text-balance leading-tight text-gray-900">
              AgriTariff
            </h1>
            <p className="text-lg md:text-xl text-gray-600 max-w-2xl mx-auto">
              Calculate tariffs with confidence and clarity.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8 max-w-6xl mx-auto">
            {tariffFeatures.map((feature) => (
              <div key={feature.id} className="relative">
                <div className="relative group">
                  {/* Image Container */}
                  <div className="relative overflow-hidden rounded-3xl bg-gray-100 aspect-[3/4] mb-6">
                    <img
                      src={feature.image || "/placeholder.svg"}
                      alt={feature.title}
                      className="w-full h-full object-cover"
                    />
                  </div>

                  {/* Content Card */}
                  <div className="bg-[#E8E5D5] rounded-2xl p-6">
                    <div className="flex items-start gap-4">
                      <span className="text-4xl font-bold text-gray-900">{feature.number}</span>
                      <div className="flex-1">
                        <h3 className="text-xl font-bold text-gray-900 mb-2">{feature.title}</h3>
                        <p className="text-sm text-gray-700 leading-relaxed">{feature.description}</p>
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
          <div className="mb-12 max-w-3xl">
            <h2 className="text-4xl font-bold mb-4 text-gray-900">What Are Tariffs?</h2>
            <p className="text-lg text-gray-700 leading-relaxed">
              Tariffs are taxes imposed by governments on imported goods. In agricultural trade, they serve as crucial
              policy instruments that affect food security, farmer livelihoods, and international trade relationships.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-6">
            <Card className="bg-white border-2 hover:border-blue-500 transition-colors">
              <CardContent className="pt-6">
                <div className="w-12 h-12 rounded-lg bg-blue-500/10 flex items-center justify-center mb-4">
                  <BookOpen className="w-6 h-6 text-blue-600" />
                </div>
                <h3 className="text-xl font-semibold mb-3 text-gray-900">Historical Context</h3>
                <p className="text-gray-700 leading-relaxed">
                  Agricultural tariffs have evolved since the GATT Uruguay Round (1986-1994), which transformed
                  non-tariff barriers into transparent tariff measures.
                </p>
              </CardContent>
            </Card>

            <Card className="bg-white border-2 hover:border-blue-500 transition-colors">
              <CardContent className="pt-6">
                <div className="w-12 h-12 rounded-lg bg-blue-500/10 flex items-center justify-center mb-4">
                  <Globe className="w-6 h-6 text-blue-600" />
                </div>
                <h3 className="text-xl font-semibold mb-3 text-gray-900">Global Impact</h3>
                <p className="text-gray-700 leading-relaxed">
                  Tariffs directly influence food prices, market access for farmers, and the competitiveness of
                  agricultural products in international markets.
                </p>
              </CardContent>
            </Card>

            <Card className="bg-white border-2 hover:border-blue-500 transition-colors">
              <CardContent className="pt-6">
                <div className="w-12 h-12 rounded-lg bg-blue-500/10 flex items-center justify-center mb-4">
                  <TrendingUp className="w-6 h-6 text-blue-600" />
                </div>
                <h3 className="text-xl font-semibold mb-3 text-gray-900">Trade Dynamics</h3>
                <p className="text-gray-700 leading-relaxed">
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
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold mb-4 text-gray-900">Why Tariffs Matter</h2>
            <p className="text-lg text-gray-700 max-w-3xl mx-auto leading-relaxed">
              Agricultural tariffs play a critical role in shaping global food systems, economic development, and
              international trade relationships
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-8">
            {reasons.map((reason, index) => {
              const Icon = reason.icon
              return (
                <Card key={index} className="bg-white border-2">
                  <CardContent className="pt-6">
                    <div className="flex gap-4">
                      <div className="flex-shrink-0">
                        <div className="w-14 h-14 rounded-xl bg-blue-600 text-white flex items-center justify-center">
                          <Icon className="w-7 h-7" />
                        </div>
                      </div>
                      <div>
                        <h3 className="text-xl font-semibold mb-2 text-gray-900">{reason.title}</h3>
                        <p className="text-gray-700 leading-relaxed">{reason.description}</p>
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
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold mb-4 text-gray-900">Key Tariff Indicators</h2>
            <p className="text-lg text-gray-700 max-w-3xl mx-auto leading-relaxed">
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
                  className="bg-white border-2 hover:shadow-xl transition-all duration-300 cursor-pointer relative overflow-hidden aspect-square flex flex-col"
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
                    <h3 className="font-bold text-sm mb-2 leading-tight text-gray-900">{indicator.title}</h3>
                    <p className="text-xs font-mono text-gray-600">{indicator.code}</p>
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
                      <p className="text-xs font-mono text-gray-600">{indicator.code}</p>
                    </div>
                    <h3 className="font-bold text-xs mb-2 leading-tight text-gray-900">{indicator.title}</h3>
                    <p className="text-xs text-gray-700 mb-2 leading-relaxed">{indicator.description}</p>
                    <p className="text-xs text-gray-700 leading-relaxed flex-1 overflow-y-auto">{indicator.details}</p>
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
          <div className="flex items-center justify-between mb-12">
            <div>
              <h2 className="text-4xl font-bold mb-2 text-gray-900">Latest News</h2>
              <p className="text-lg text-gray-700">Stay updated on agricultural trade developments</p>
            </div>
          </div>

          <div className="grid md:grid-cols-3 gap-6">
            {news.map((item, index) => (
              <Card key={index} className="flex flex-col bg-white hover:shadow-lg transition-shadow">
                <CardHeader>
                  <div className="flex items-center gap-2 mb-3">
                    <Badge variant="secondary">{item.category}</Badge>
                    <div className="flex items-center gap-1 text-sm text-gray-600">
                      <Calendar className="w-4 h-4" />
                      <span>{item.date}</span>
                    </div>
                  </div>
                  <CardTitle className="text-xl leading-tight text-balance text-gray-900">{item.title}</CardTitle>
                </CardHeader>
                <CardContent className="flex-1 flex flex-col">
                  <CardDescription className="text-base text-gray-700 leading-relaxed mb-4 flex-1">
                    {item.description}
                  </CardDescription>
                  <Button variant="ghost" className="w-fit px-0 group text-blue-600 hover:text-blue-700">
                    Read more
                    <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
                  </Button>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>
    </main>
  )
}
