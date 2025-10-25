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
import { BarChart3, Sparkles, Database, Globe } from "lucide-react"

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
  const [year, setYear] = useState("2024")
  const [calculatedTariff, setCalculatedTariff] = useState<number | null>(null)

  const [apiResponse, setApiResponse] = useState<GeminiApiResponse | string | null>(null)
  const [isCalculatingTariff, setIsCalculatingTariff] = useState(false)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [inputError, setInputError] = useState<string | null>(null)
  const [apiError, setApiError] = useState<string | null>(null)
  const [tariffPercentage, setTariffPercentage] = useState<string | null>(null)
  const [aiFinished, setAiFinished] = useState(false)
  const [dataSource, setDataSource] = useState<"database" | "wto" | null>(null)

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
    setDataSource(null)
    setIsCalculatingTariff(true)
    setIsAnalyzing(true)
    setAiFinished(false)
    setShowAIAnalysis(false)
    setShowCharts(false)

    try {
      let parsedPercentage: number | null = null
      let foundInDatabase = false

      // STEP 1: Query your database first
      console.log('Step 1: Querying database...')
      // Convert product to integer for the API call
      const productInt = parseInt(product, 10)
      const dbUrl = `https://teamcmiggwp.duckdns.org/api/v1/tariffs?reporter=${encodeURIComponent(toCountry)}&partner=${encodeURIComponent(fromCountry)}&product=${productInt}&year=${encodeURIComponent(year)}&tariffTypeId=1`
      console.log('Database API URL:', dbUrl)

      try {
        const dbResponse = await fetch(dbUrl, {
          method: 'GET',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          credentials: 'include'
        })

        console.log('Database response status:', dbResponse.status)

        if (dbResponse.status === 204) {
          console.log('✗ Database returned 204 No Content - tariff not found')
        } else if (dbResponse.ok) {
          const responseText = await dbResponse.text()
          console.log('Database raw response:', responseText)

          // Check if response is empty
          if (!responseText || responseText.trim() === '') {
            console.log('✗ Database returned empty response')
          } else {
            try {
              const dbData = JSON.parse(responseText)
              console.log('Database parsed response:', JSON.stringify(dbData, null, 2))

              // Check if we got valid tariff data from database
              if (dbData && dbData.rate !== undefined && dbData.rate !== null) {
                parsedPercentage = typeof dbData.rate === 'number' ? dbData.rate : parseFloat(String(dbData.rate))

                if (Number.isFinite(parsedPercentage)) {
                  console.log('✓ Found tariff in database:', parsedPercentage, '%')
                  foundInDatabase = true
                  setDataSource("database")
                }
              } else {
                console.log('✗ Database response missing rate field')
              }
            } catch (parseErr) {
              console.error('Failed to parse database JSON:', parseErr)
            }
          }
        } else if (dbResponse.status === 404) {
          console.log('✗ Tariff not found in database (404)')
        } else if (dbResponse.status === 502) {
          // Get error details for 502
          const errorText = await dbResponse.text()
          console.error('Database 502 error response:', errorText)
        } else {
          console.warn('Database query returned status:', dbResponse.status)
          const errorText = await dbResponse.text()
          console.warn('Error response:', errorText)
        }
      } catch (dbErr) {
        console.error('Database query exception:', dbErr)
        if (dbErr instanceof Error) {
          console.error('Error details:', dbErr.message, dbErr.stack)
        }
      }

      // STEP 2: If not found in database, query WTO API
      if (!foundInDatabase) {
        console.log('Step 2: Querying WTO API as fallback...')
        const wtoUrl = `https://teamcmiggwp.duckdns.org/api/v1/indicators/HS_P_0070/observations?i=HS_P_0070&r=${toCountry}&p=${fromCountry}&pc=${product}&ps=${year}&fmt=json`
        console.log('WTO API URL:', wtoUrl)

        try {
          const wtoResponse = await fetch(wtoUrl, {
            method: 'GET',
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json'
            },
            mode: 'cors',
            credentials: 'omit'
          })
          console.log('WTO API response status:', wtoResponse.status)

          if (wtoResponse.status === 204) {
            console.log('✗ WTO API returned 204 No Content - tariff not found')
          } else if (wtoResponse.ok) {
            const responseText = await wtoResponse.text()
            console.log('WTO API raw response:', responseText)

            // Check if response is empty
            if (!responseText || responseText.trim() === '') {
              console.log('✗ WTO API returned empty response')
            } else {
              try {
                const wtoData = JSON.parse(responseText)
                console.log('WTO API parsed data:', JSON.stringify(wtoData, null, 2))

                // Parse WTO API response
                type WTORecord = { Year?: number; Value?: string | number;[key: string]: unknown }
                if (wtoData.Dataset && Array.isArray(wtoData.Dataset) && wtoData.Dataset.length > 0) {
                  const records = (wtoData.Dataset as WTORecord[]).sort((a: WTORecord, b: WTORecord) => ((b.Year ?? 0) - (a.Year ?? 0)))
                  const latestRecord = records[0]

                  if (latestRecord && latestRecord.Value !== undefined) {
                    const v = latestRecord.Value
                    const num = typeof v === 'number' ? v : parseFloat(String(v))
                    parsedPercentage = Number.isFinite(num) ? num : null

                    if (parsedPercentage !== null) {
                      console.log('✓ Found tariff in WTO API:', parsedPercentage, '%')
                      setDataSource("wto")
                    }
                  }
                } else {
                  console.log('✗ WTO API returned no dataset or empty dataset')
                }
              } catch (parseErr) {
                console.error('Failed to parse WTO API JSON:', parseErr)
                console.log('Response text was:', responseText.substring(0, 500))
              }
            }

            if (parsedPercentage === null) {
              console.warn('Could not parse tariff rate from WTO API response')
            }
          } else if (wtoResponse.status === 404 || wtoResponse.status === 422) {
            console.log('✗ Tariff not found in WTO API (404/422)')
          } else if (wtoResponse.status === 502) {
            const errorText = await wtoResponse.text()
            console.error('WTO API 502 error:', errorText)
          } else {
            console.warn('WTO API returned status:', wtoResponse.status)
          }
        } catch (wtoErr) {
          console.error('WTO API fetch failed:', wtoErr)
          if (wtoErr instanceof Error) {
            setApiError(`WTO API error: ${wtoErr.message}`)
          }
        }
      }

      // STEP 3: Set results
      if (parsedPercentage !== null && !isNaN(parsedPercentage)) {
        setTariffPercentage(`${parsedPercentage.toFixed(2)}%`)
        const goodsValue = parseFloat(value)
        setCalculatedTariff((parsedPercentage / 100) * goodsValue)
      } else {
        console.log('No tariff data found in either source, showing MFN')
        setTariffPercentage("MFN")
        setCalculatedTariff(null)
        setDataSource(null)
        // Add a helpful message
        setApiError('No tariff data found in database or WTO API for this combination. This may indicate MFN (Most Favored Nation) rates apply, or data is not available for the selected year.')
      }

      // STEP 4: Call Gemini AI for analysis
      const apiData = `Trade analysis: Export from ${fromCountry} to ${toCountry}. Product: ${product}, Value: $${value}, Year: ${year}. Tariff Rate: ${parsedPercentage !== null ? `${parsedPercentage}%` : 'MFN'}. Data source: ${foundInDatabase ? 'Internal Database' : 'WTO API'}`
      const prompt = "Analyze this agricultural trade data and provide insights on tariff implications, trade relationships, and economic factors. Note: 000 represents 'World' in country codes."
      //await callGeminiApi(apiData, prompt)
      setAiFinished(true)

    } catch (err) {
      console.error('Error in calculateTariff:', err)
      setApiError(err instanceof Error ? err.message : "Unknown error occurred")
      if (!tariffPercentage) setTariffPercentage("MFN")
      setCalculatedTariff(null)
      setAiFinished(true)
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
        <Card className="calculator-card shadow-lg">
          <CardHeader>
            <CardTitle className="calculator-title">Agricultural Tariff Calculator</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
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

              <div className="space-y-2">
                <Label htmlFor="value">Value of Goods ({selectedCurrency})</Label>
                <Input type="number" value={value} onChange={e => setValue(e.target.value)} placeholder="Enter value" />
              </div>

              <div className="space-y-2">
                <Label htmlFor="year">Year</Label>
                <Select value={year} onValueChange={setYear}>
                  <SelectTrigger>
                    <SelectValue>{year}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {[2024, 2023, 2022, 2021, 2020, 2019].map(y => (
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
            {apiError && !inputError && (
              <div className="bg-yellow-600 text-white p-4 rounded-lg">{apiError}</div>
            )}
          </CardContent>
        </Card>

        {aiFinished && tariffPercentage && (
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="calculator-results mt-8 space-y-6">
            <h3 className="text-2xl font-bold text-white mb-4">Tariff Calculation Results</h3>

            {/* Data Source Badge */}
            {dataSource && (
              <div className="flex items-center gap-2 bg-blue-600/20 border border-blue-600 rounded-lg p-3 text-blue-200">
                {dataSource === "database" ? (
                  <>
                    <Database className="w-5 h-5" />
                    <span className="font-semibold">Data Source: Internal Database</span>
                  </>
                ) : (
                  <>
                    <Globe className="w-5 h-5" />
                    <span className="font-semibold">Data Source: WTO API</span>
                  </>
                )}
              </div>
            )}

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
                    : "Data Not Available"}
                </p>
                {calculatedTariff === null && (
                  <p className="text-sm text-yellow-300 mt-1">
                    No tariff data found in our database or WTO API. This could mean MFN rates apply or data is unavailable for this combination.
                  </p>
                )}
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2 mt-6">
              <Button
                onClick={() => { setShowAIAnalysis(!showAIAnalysis); setShowCharts(false); }}
                size="lg"
                className={`py-6 border-2 transition-all ${showAIAnalysis ? 'bg-primary text-white border-primary hover:bg-primary/90' : 'bg-white text-black border-white hover:bg-gray-100'
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
                className={`py-6 border-2 transition-all ${showCharts ? 'bg-primary text-white border-primary hover:bg-primary/90' : 'bg-white text-black border-white hover:bg-gray-100'
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
                    {apiResponse.summary && (
                      <>
                        <h4 className="font-semibold text-lg mb-2">Summary</h4>
                        <p>{apiResponse.summary}</p>
                      </>
                    )}

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