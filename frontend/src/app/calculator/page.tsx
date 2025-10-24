"use client"

import { useState } from "react"
import { motion, useMotionValue } from "framer-motion"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { countries, agriculturalProducts, currencies } from "@/lib/tariff-data"
import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from "recharts"
import { BarChart3, Sparkles } from "lucide-react"

type MetricValue = string | number

type GeminiApiResponse = {
  summary?: string
  insights?: string[]
  metrics?: Record<string, MetricValue>
  recommendations?: string[]
  confidence?: string
} | string | null

export default function CalculatorSection() {
  const calculatorY = useMotionValue(0)
  const [fromCountry, setFromCountry] = useState("004")
  const [toCountry, setToCountry] = useState("840")
  const [product, setProduct] = useState("100630")
  const [value, setValue] = useState("100")
  const [year, setYear] = useState("2025")
  const [calculatedTariff, setCalculatedTariff] = useState<number | null>(null)

  const [apiResponse, setApiResponse] = useState<GeminiApiResponse | string | null>(null)
  const [isCalculatingTariff, setIsCalculatingTariff] = useState(false)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [inputError, setInputError] = useState<string | null>(null)
  const [apiError, setApiError] = useState<string | null>(null)
  const [tariffPercentage, setTariffPercentage] = useState<string | null>(null)
  const [aiFinished, setAiFinished] = useState(false)

  const [showAIAnalysis, setShowAIAnalysis] = useState(false)
  const [showCharts, setShowCharts] = useState(false)

  const callGeminiApi = async (data: string, prompt?: string) => {
    try {
      setIsAnalyzing(true)
      setApiError(null)

      const url = 'https://teamcmiggwp.duckdns.org/api/v1/gemini/analyses'
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({ data, prompt })
      })
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)

      const result = await response.json()
      if (result?.success && result?.analysis) setApiResponse(result.analysis)
      else setApiResponse("No analysis data returned from API.")
    } catch (err: unknown) {
      setApiError(err instanceof Error ? err.message : "Unknown error occurred")
      console.error("API Error:", err)
    } finally {
      setIsAnalyzing(false)
    }
  }

  const calculateTariff = async () => {
  if (!fromCountry || !toCountry || !product || !value || !year) {
    setInputError("Please fill in all required fields.")
    return
  }

  setInputError(null)
  setApiError(null)
  setTariffPercentage(null)
  setCalculatedTariff(null)
  setApiResponse(null)
  setIsCalculatingTariff(true)
  setIsAnalyzing(true)
  setAiFinished(false)
  setShowAIAnalysis(false)
  setShowCharts(false)

  try {
    // Call WTO API with HSP0070 indicator
    // i = indicator, r = reporting economy (importer), p = partner economy (exporter), pc = product code, ps = period
    const apiUrl = `https://teamcmiggwp.duckdns.org/api/v1/indicators/HS_P_0070/observations?i=HS_P_0070&r=${toCountry}&p=${fromCountry}&pc=${product}&ps=${year}&fmt=json`
    console.log('Calling WTO API:', apiUrl)
    const response = await fetch(apiUrl, { credentials: 'include' })
    console.log('Response status:', response.status)

    let parsedPercentage: number | null = null

    if (response.ok) {
      const text = await response.text()
      console.log('Raw response:', text)
      
      if (!text || text.trim() === '') {
        console.log('Empty response - treating as MFN')
        parsedPercentage = null
      } else {
        try {
          const data = JSON.parse(text)
          console.log('WTO API Response:', JSON.stringify(data, null, 2))
          
          // Parse WTO API response - Dataset is an array of records
          if (data.Dataset && Array.isArray(data.Dataset) && data.Dataset.length > 0) {
            // Get the most recent record (usually the last one, or we can filter by Year)
            const records = data.Dataset.sort((a: any, b: any) => (b.Year || 0) - (a.Year || 0))
            const latestRecord = records[0]
            
            if (latestRecord && latestRecord.Value !== undefined) {
              parsedPercentage = parseFloat(latestRecord.Value)
              console.log('Parsed tariff rate:', parsedPercentage, '%')
            }
          } else if (data.Dataset && Array.isArray(data.Dataset) && data.Dataset.length === 0) {
            // Empty dataset - no tariff data found, treat as MFN
            console.log('Empty dataset returned - treating as MFN')
            parsedPercentage = null
          }
          
          // If still no data found, log the structure and treat as MFN
          if (parsedPercentage === null && (!data.Dataset || !Array.isArray(data.Dataset))) {
            console.warn('Could not parse tariff rate from response structure. Full response:', data)
          }
        } catch (parseError) {
          console.error('JSON parse error:', parseError)
          console.log('Failed to parse response as JSON - treating as MFN')
          parsedPercentage = null
        }
      }
    } else if (response.status === 404 || response.status === 422) {
      // If API returns 404 or 422 (no data), treat as MFN
      parsedPercentage = null
    } else {
      throw new Error(`API call failed with status ${response.status}`)
    }

    if (parsedPercentage !== null && !isNaN(parsedPercentage)) {
      setTariffPercentage(`${parsedPercentage.toFixed(2)}%`)
      const goodsValue = parseFloat(value)
      setCalculatedTariff((parsedPercentage / 100) * goodsValue)
    } else {
      setTariffPercentage("MFN")
      setCalculatedTariff(null)
    }

    // Prepare AI data
    const apiData = `Trade analysis: Export from ${fromCountry} to ${toCountry}. Product: ${product}, Value: $${value}, Year: ${year || 'N/A'}`
    const prompt = "Analyze this agricultural trade data and provide insights on tariff implications, trade relationships, and economic factors, 000 is world"
    //await callGeminiApi(apiData, prompt)
    setAiFinished(true)

  } catch (err) {
    console.error('Error in calculateTariff:', err)
    setApiError(err instanceof Error ? err.message : "Unknown error occurred")
    // Still show MFN in case of unexpected error
    if (!tariffPercentage) setTariffPercentage("MFN")
    setCalculatedTariff(null)
    setAiFinished(true) // Show results even on error
  } finally {
    setIsCalculatingTariff(false)
    setIsAnalyzing(false)
  }
}

  const selectedCurrency = toCountry ? currencies[toCountry as keyof typeof currencies] || "USD" : "USD"
  const getCountryName = (code: string) => {
    const country = countries.find(c => c.code === code)
    return country ? country.name : code
  }

  const dummyChartData = [
    { product: "Wheat", tariff: 5.2 },
    { product: "Rice", tariff: 12.5 },
    { product: "Corn", tariff: 8.1 },
    { product: "Soybeans", tariff: 10.0 },
    { product: "Barley", tariff: 6.3 },
  ]

  return (
    <motion.section style={{ y: calculatorY }} className="calculator-section py-20">
      <div className="max-w-4xl mx-auto px-4">
        {/* Calculator Card */}
        <Card className="calculator-card shadow-lg">
          <CardHeader>
            <CardTitle className="calculator-title">Agricultural Tariff Calculator</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* From Country */}
              <div className="space-y-2">
                <Label htmlFor="from-country">From Country (Exporter)</Label>
                <Select value={fromCountry} onValueChange={setFromCountry}>
                  <SelectTrigger>
                    <SelectValue>{getCountryName(fromCountry)}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {countries.map(c => (
                      <SelectItem key={c.code} value={c.code}>{c.name} ({c.code})</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* To Country */}
              <div className="space-y-2">
                <Label htmlFor="to-country">To Country (Importer)</Label>
                <Select value={toCountry} onValueChange={setToCountry}>
                  <SelectTrigger>
                    <SelectValue>{getCountryName(toCountry)}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {countries.map(c => (
                      <SelectItem key={c.code} value={c.code}>{c.name} ({c.code})</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Product */}
              <div className="space-y-2">
                <Label htmlFor="product">Agricultural Product</Label>
                <Select value={product} onValueChange={setProduct}>
                  <SelectTrigger>
                    <SelectValue>{agriculturalProducts.find(p => p.hs_code === product)?.name}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {agriculturalProducts.map(p => (
                      <SelectItem key={p.hs_code} value={p.hs_code}>{p.name} ({p.hs_code})</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Value */}
              <div className="space-y-2">
                <Label htmlFor="value">Value of Goods ({selectedCurrency})</Label>
                <Input type="number" value={value} onChange={e => setValue(e.target.value)} placeholder="Enter value" />
              </div>

              {/* Year */}
              <div className="space-y-2">
                <Label htmlFor="year">Year</Label>
                <Select value={year} onValueChange={setYear}>
                  <SelectTrigger>
                    <SelectValue>{year}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {[2025, 2024, 2023, 2022, 2021, 2020].map(y => (
                      <SelectItem key={y} value={String(y)}>{y}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <Button
              onClick={calculateTariff}
              disabled={!fromCountry || !toCountry || !product || !value || isCalculatingTariff || isAnalyzing}
              className="w-full py-4"
            >
              {isCalculatingTariff ? "Calculating..." : isAnalyzing ? "Analyzing..." : "Calculate Tariff"}
            </Button>

            {inputError && <div className="bg-red-600 text-white p-4 rounded-lg">{inputError}</div>}
          </CardContent>
        </Card>

        {/* Only show results after AI analysis finishes */}
        {aiFinished && tariffPercentage && (
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="calculator-results mt-8 space-y-6">
            <h3 className="text-2xl font-bold text-white mb-4">Tariff Calculation Results</h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-white">
              <div>
                <p className="text-gray-300">Trade Route:</p>
                <p className="font-semibold">{getCountryName(fromCountry)} → {getCountryName(toCountry)}</p>
              </div>
              <div>
                <p className="text-gray-300">Product:</p>
                <p className="font-semibold">{agriculturalProducts.find(p => p.hs_code === product)?.name || product}</p>
              </div>
              <div>
                <p className="text-gray-300">Goods Value:</p>
                <p className="font-semibold">{selectedCurrency} {Number.parseFloat(value).toLocaleString()}</p>
              </div>
              <div>
                <p className="text-gray-300">Tariff Percentage:</p>
                <p className="font-semibold">{tariffPercentage}</p>
              </div>
              <div className="md:col-span-2">
                <p className="text-gray-300">Estimated Tariff:</p>
                <p className="font-semibold">
                  {calculatedTariff !== null
                    ? `${selectedCurrency} ${calculatedTariff.toLocaleString(undefined, { maximumFractionDigits: 2 })}`
                    : "MFN"}
                </p>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="grid gap-4 md:grid-cols-2 mt-6">
              <Button
                onClick={() => { setShowAIAnalysis(!showAIAnalysis); setShowCharts(false); }}
                size="lg"
                className={`py-6 border-2 transition-all ${
                  showAIAnalysis ? 'bg-primary text-white border-primary hover:bg-primary/90' : 'bg-white text-black border-white hover:bg-gray-100'
                }`}
              >
                <Sparkles className="w-5 h-5 mr-2" />
                <div className="text-left">
                  <div className="font-semibold">AI Analysis</div>
                  <div className="text-xs opacity-80">View intelligent insights and recommendations</div>
                </div>
              </Button>

              <Button
                onClick={() => { setShowCharts(!showCharts); setShowAIAnalysis(false); }}
                size="lg"
                className={`py-6 border-2 transition-all ${
                  showCharts ? 'bg-primary text-white border-primary hover:bg-primary/90' : 'bg-white text-black border-white hover:bg-gray-100'
                }`}
              >
                <BarChart3 className="w-5 h-5 mr-2" />
                <div className="text-left">
                  <div className="font-semibold">Charts & Diagrams</div>
                  <div className="text-xs opacity-80">Visualize tariff comparisons</div>
                </div>
              </Button>
            </div>

            {showAIAnalysis && apiResponse && typeof apiResponse === "object" && !Array.isArray(apiResponse) && (
  <Card className="shadow-lg animate-in fade-in slide-in-from-bottom-4 duration-500 border-primary">
    <CardHeader>
      <CardTitle className="flex items-center gap-2">
        <Sparkles className="w-5 h-5" />
        Gemini API Analysis
      </CardTitle>

    </CardHeader>
    <CardContent className="space-y-4">
      <div className="prose prose-sm dark:prose-invert max-w-none">

        {/* Summary */}
        {apiResponse.summary && (
          <>
            <h4 className="font-semibold text-lg mb-2">Summary</h4>
            <p>{apiResponse.summary}</p>
          </>
        )}

        {/* Key Metrics as a clean list */}
        {apiResponse.metrics && (
          <>
            <h4 className="font-semibold text-lg mt-4 mb-2">Key Metrics</h4>
            <ul className="space-y-1 list-disc pl-4">
              {Object.entries(apiResponse.metrics).map(([key, value], idx) => (
                <li key={idx}>
                  <strong>{key.replace(/_/g, " ")}:</strong> {value}
                </li>
              ))}
            </ul>
          </>
        )}

        {/* Insights */}
        {apiResponse.insights && apiResponse.insights.length > 0 && (
          <>
            <h4 className="font-semibold text-lg mt-4 mb-2">Insights</h4>
            <ul className="space-y-2 list-disc pl-4">
              {apiResponse.insights.map((insight, idx) => (
                <li key={idx}>
                  {insight.split("**").map((part, i) =>
                    i % 2 === 1 ? <strong key={i}>{part}</strong> : part
                  )}
                </li>
              ))}
            </ul>
          </>
        )}

        {/* Recommendations */}
        {apiResponse.recommendations && apiResponse.recommendations.length > 0 && (
          <>
            <h4 className="font-semibold text-lg mt-4 mb-2">Recommendations</h4>
            <ul className="space-y-2 list-disc pl-4">
              {apiResponse.recommendations.map((rec, idx) => (
                <li key={idx}>
                  {rec.split("**").map((part, i) =>
                    i % 2 === 1 ? <strong key={i}>{part}</strong> : part
                  )}
                </li>
              ))}
            </ul>
          </>
        )}

        {/* Confidence */}
        {apiResponse.confidence && (
          <>
            <h4 className="font-semibold text-lg mt-4 mb-2">Confidence</h4>
            <p>{apiResponse.confidence}</p>
          </>
        )}

      </div>
    </CardContent>
  </Card>
)}


            {/* Charts */}
            {showCharts && (
              <Card className="shadow-lg border-primary p-4 mt-4 bg-white dark:bg-gray-900">
                <h4 className="text-lg font-semibold mb-2">Tariff Comparisons</h4>
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={dummyChartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="product" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="tariff" fill="hsl(var(--chart-1))" />
                  </BarChart>
                </ResponsiveContainer>
                <p className="text-center text-sm text-muted-foreground mt-2">
                  This bar chart compares tariffs for different agricultural products.
                </p>
              </Card>
            )}
          </motion.div>
        )}
      </div>
    </motion.section>
  )
}
