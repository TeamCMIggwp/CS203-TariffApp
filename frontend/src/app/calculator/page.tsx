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

  // Backend API base URL, configurable via environment for Amplify and local dev
  const API_BASE = process.env.NEXT_PUBLIC_BACKEND_URL || process.env.BACKEND_URL || 'http://localhost:8080'

  const callGeminiApi = async (data: string, prompt?: string) => {
    try {
      console.log('   📤 Sending request to Gemini API...')
      console.log('   📝 Data:', data)
      console.log('   📝 Prompt:', prompt)
      const url = `${API_BASE}/api/v1/gemini/analyses`
      const startTime = performance.now()
      
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({ data, prompt })
      })
      
      const responseTime = performance.now() - startTime
      console.log(`   ⏱️  Gemini API response time: ${responseTime.toFixed(2)}ms`)
      console.log('   📥 Response status:', response.status)
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      console.log('   📊 Gemini API result:', result)
      
      if (result?.success && result?.analysis) {
        setApiResponse(result.analysis)
        console.log('   ✅ Analysis set successfully')
      } else {
        setApiResponse("No analysis data returned from API.")
        console.log('   ⚠️  No analysis data in response')
      }
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : "Unknown error occurred"
      setApiError(errorMsg)
      console.error('   ❌ Gemini API Error:', errorMsg)
      if (err instanceof Error && err.stack) {
        console.error('   ❌ Stack:', err.stack)
      }
    }
  }

  // NEW: Function to save tariff to database
  const saveTariffToDatabase = async (
    reporter: string,
    partner: string,
    productCode: number,
    yearVal: string,
    rate: number,
    unit: string = "percent"
  ) => {
    try {
      console.log('\n💾 SAVING TO DATABASE...')
      const saveUrl = `${API_BASE}/api/v1/tariffs`
      const startTime = performance.now()
      
      const payload = {
        reporter: reporter,
        partner: partner,
        product: productCode,
        year: yearVal,
        tariff_type_id: 1, // Default to tariff type 1
        rate: rate,
        unit: unit
      }
      
      console.log('   📤 Payload:', JSON.stringify(payload, null, 2))
      
      const response = await fetch(saveUrl, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(payload)
      })
      
      const saveTime = performance.now() - startTime
      console.log(`   ⏱️  Save response time: ${saveTime.toFixed(2)}ms`)
      console.log('   📥 Save response status:', response.status)
      
      if (response.status === 201) {
        const result = await response.json()
        console.log('   ✅ Successfully saved tariff to database!')
        console.log('   📊 Saved data:', result)
        return true
      } else if (response.status === 409) {
        console.log('   ⚠️  Tariff already exists in database (409 Conflict)')
        // Not an error - it just already exists
        return true
      } else {
        const errorText = await response.text()
        console.error('   ❌ Failed to save tariff:', response.status, errorText)
        return false
      }
    } catch (err) {
      console.error('   ❌ Exception while saving tariff:', err)
      if (err instanceof Error) {
        console.error('   ❌ Error:', err.message)
      }
      return false
    }
  }

  const calculateTariff = async () => {
    if (!fromCountry || !toCountry || !product || !value || !year) {
      setInputError("Please fill in all required fields.")
      return
    }

    console.log('═══════════════════════════════════════════')
    console.log('🚀 STARTING TARIFF CALCULATION')
    console.log('═══════════════════════════════════════════')
    console.log('Parameters:', { fromCountry, toCountry, product, year, value })

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
      console.log('\n📊 STEP 1: Querying database...')
      const startDb = performance.now()
      const productInt = parseInt(product, 10)
      const dbUrl = `${API_BASE}/api/v1/tariffs?reporter=${encodeURIComponent(toCountry)}&partner=${encodeURIComponent(fromCountry)}&product=${productInt}&year=${encodeURIComponent(year)}&tariffTypeId=1`
      console.log('   📍 Database API URL:', dbUrl)

      try {
        const dbResponse = await fetch(dbUrl, { 
          method: 'GET',
          headers: { 
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          credentials: 'include'
        })
        
        const dbTime = performance.now() - startDb
        console.log(`   ⏱️  Database response time: ${dbTime.toFixed(2)}ms`)
        console.log('   📥 Database response status:', dbResponse.status)

        if (dbResponse.status === 204) {
          console.log('   ❌ Database returned 204 No Content - tariff not found')
        } else if (dbResponse.ok) {
          const responseText = await dbResponse.text()
          console.log('   📄 Database raw response length:', responseText.length, 'chars')
          
          if (!responseText || responseText.trim() === '') {
            console.log('   ❌ Database returned empty response')
          } else {
            try {
              const dbData = JSON.parse(responseText)
              console.log('   ✅ Database parsed response:', JSON.stringify(dbData, null, 2))
              
              if (dbData && dbData.rate !== undefined && dbData.rate !== null) {
                parsedPercentage = typeof dbData.rate === 'number' ? dbData.rate : parseFloat(String(dbData.rate))
                
                if (Number.isFinite(parsedPercentage)) {
                  console.log('   ✅ ✅ SUCCESS! Found tariff in database:', parsedPercentage, '%')
                  foundInDatabase = true
                  setDataSource("database")
                }
              } else {
                console.log('   ❌ Database response missing rate field')
              }
            } catch (parseErr) {
              console.error('   ❌ Failed to parse database JSON:', parseErr)
            }
          }
        } else if (dbResponse.status === 404) {
          console.log('   ❌ Tariff not found in database (404)')
        } else if (dbResponse.status === 502) {
          const errorText = await dbResponse.text()
          console.error('   ❌ Database 502 error response:', errorText)
        } else {
          console.warn('   ⚠️  Database query returned status:', dbResponse.status)
          const errorText = await dbResponse.text()
          console.warn('   ⚠️  Error response:', errorText)
        }
      } catch (dbErr) {
        console.error('   ❌ Database query exception:', dbErr)
        if (dbErr instanceof Error) {
          console.error('   ❌ Error details:', dbErr.message, dbErr.stack)
        }
      }

      // STEP 2: If not found in database, query WTO API
      if (!foundInDatabase) {
        console.log('\n🌐 STEP 2: Querying WTO API as fallback...')
        const startWto = performance.now()
        const wtoUrl = `${API_BASE}/api/v1/indicators/HS_P_0070/observations?i=HS_P_0070&r=${toCountry}&p=${fromCountry}&pc=${product}&ps=${year}&fmt=json`
        console.log('   📍 WTO API URL:', wtoUrl)
        
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
          const wtoTime = performance.now() - startWto
          console.log(`   ⏱️  WTO API response time: ${wtoTime.toFixed(2)}ms`)
          console.log('   📥 WTO API response status:', wtoResponse.status)

          if (wtoResponse.status === 204) {
            console.log('   ❌ WTO API returned 204 No Content - tariff not found')
          } else if (wtoResponse.ok) {
            const responseText = await wtoResponse.text()
            console.log('   📄 WTO API raw response length:', responseText.length, 'chars')
            
            if (!responseText || responseText.trim() === '') {
              console.log('   ❌ WTO API returned empty response')
            } else {
              try {
                const wtoData = JSON.parse(responseText)
                console.log('   ✅ WTO API parsed data:', JSON.stringify(wtoData, null, 2))
                
                type WTORecord = { Year?: number; Value?: string | number; [key: string]: unknown }
                if (wtoData.Dataset && Array.isArray(wtoData.Dataset) && wtoData.Dataset.length > 0) {
                  const records = (wtoData.Dataset as WTORecord[]).sort((a: WTORecord, b: WTORecord) => ((b.Year ?? 0) - (a.Year ?? 0)))
                  const latestRecord = records[0]
                  console.log('   📊 Latest WTO record:', latestRecord)
                  
                  if (latestRecord && latestRecord.Value !== undefined) {
                    const v = latestRecord.Value
                    const num = typeof v === 'number' ? v : parseFloat(String(v))
                    parsedPercentage = Number.isFinite(num) ? num : null
                    
                    if (parsedPercentage !== null) {
                      console.log('   ✅ ✅ SUCCESS! Found tariff in WTO API:', parsedPercentage, '%')
                      setDataSource("wto")
                    }
                  }
                } else {
                  console.log('   ❌ WTO API returned no dataset or empty dataset')
                }
              } catch (parseErr) {
                console.error('   ❌ Failed to parse WTO API JSON:', parseErr)
                console.log('   📄 Response text was:', responseText.substring(0, 500))
              }
            }
            
            if (parsedPercentage === null) {
              console.warn('   ⚠️  Could not parse tariff rate from WTO API response')
            }
          } else if (wtoResponse.status === 404 || wtoResponse.status === 422) {
            console.log('   ❌ Tariff not found in WTO API (404/422)')
          } else if (wtoResponse.status === 502) {
            const errorText = await wtoResponse.text()
            console.error('   ❌ WTO API 502 error:', errorText)
          } else {
            console.warn('   ⚠️  WTO API returned status:', wtoResponse.status)
          }
        } catch (wtoErr) {
          console.error('   ❌ WTO API fetch failed:', wtoErr)
          if (wtoErr instanceof Error) {
            console.error('   ❌ Error:', wtoErr.message)
          }
        }
      } else {
        console.log('\n⏭️  STEP 2: Skipping WTO API (data found in database)')
      }

      // STEP 3: Set tariff results and show to user immediately
      console.log('\n💰 STEP 3: Setting tariff results...')
      // STEP 3: Save to database if we got data from WTO API
      // if (!foundInDatabase && parsedPercentage !== null) {
      //   console.log('\n💾 STEP 3: Saving WTO data to database for future use...')
      //   await saveTariffToDatabase(
      //     toCountry,     // reporter (importer)
      //     fromCountry,   // partner (exporter)
      //     productInt,    // product code
      //     year,          // year
      //     parsedPercentage, // rate
      //     "percent"      // unit
      //   )
      // } else if (foundInDatabase) {
      //   console.log('\n⏭️  STEP 3: Skipping database save (data already in database)')
      // } else {
      //   console.log('\n⏭️  STEP 3: Skipping database save (no tariff data found)')
      // }

      // STEP 4: Set tariff results and show to user immediately
      console.log('\n💰 STEP 4: Setting tariff results...')
      if (parsedPercentage !== null && !isNaN(parsedPercentage)) {
        setTariffPercentage(`${parsedPercentage.toFixed(2)}%`)
        const goodsValue = parseFloat(value)
        const calculatedValue = (parsedPercentage / 100) * goodsValue
        setCalculatedTariff(calculatedValue)
        console.log('   ✅ Tariff Percentage:', `${parsedPercentage.toFixed(2)}%`)
        console.log('   ✅ Calculated Tariff:', `${selectedCurrency} ${calculatedValue.toFixed(2)}`)
      } else {
        console.log('   ⚠️  No tariff data found in either source')
        setTariffPercentage("MFN")
        setCalculatedTariff(null)
        setDataSource(null)
        setApiError('No tariff data found in database or WTO API for this combination. This may indicate MFN (Most Favored Nation) rates apply, or data is not available for the selected year.')
        console.log('   ⚠️  Showing MFN (data not available)')
      }

      // Show results immediately (don't wait for AI)
      setIsCalculatingTariff(false)
      setAiFinished(true)
      console.log('   ✅ Tariff results displayed to user')

      // STEP 4: Start AI analysis in parallel (non-blocking)
      console.log('\n🤖 STEP 4: Starting Gemini AI analysis (parallel)...')
      // STEP 5: Start AI analysis in parallel (non-blocking)
      console.log('\n🤖 STEP 5: Starting Gemini AI analysis (parallel)...')
      const startAi = performance.now()
      const apiData = `Trade analysis: Export from ${fromCountry} to ${toCountry}. Product: ${product}, Value: $${value}, Year: ${year}. Tariff Rate: ${parsedPercentage !== null ? `${parsedPercentage}%` : 'MFN'}. Data source: ${foundInDatabase ? 'Internal Database' : 'WTO API'}`
      const prompt = "Analyze this agricultural trade data and provide insights on tariff implications, trade relationships, and economic factors. Note: 000 represents 'World' in country codes."
      
      // Call AI without await - let it run in background
      callGeminiApi(apiData, prompt).then(() => {
        const aiTime = performance.now() - startAi
        console.log(`   ✅ Gemini AI analysis completed in ${aiTime.toFixed(2)}ms`)
        console.log('   ✅ AI analysis ready for display')
      }).catch((err) => {
        console.error('   ❌ Gemini AI analysis failed:', err)
      }).finally(() => {
        setIsAnalyzing(false)
      })

      console.log('   🚀 AI analysis started (running in background)')
      console.log('\n═══════════════════════════════════════════')
      console.log('✅ TARIFF CALCULATION COMPLETE')
      console.log('═══════════════════════════════════════════\n')

    } catch (err) {
      console.error('\n❌❌❌ ERROR in calculateTariff:', err)
      setApiError(err instanceof Error ? err.message : "Unknown error occurred")
      if (!tariffPercentage) setTariffPercentage("MFN")
      setCalculatedTariff(null)
      setAiFinished(true)
      console.error('   Stack trace:', err instanceof Error ? err.stack : 'N/A')
    } finally {
      setIsCalculatingTariff(false)
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
                    <span className="font-semibold">Data Source: WTO API (Saved to Database)</span>
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
                disabled={isAnalyzing}
                className={`py-6 border-2 transition-all ${
                  showAIAnalysis ? 'bg-primary text-white border-primary hover:bg-primary/90' : 'bg-white text-black border-white hover:bg-gray-100'
                }`}
              >
                <Sparkles className={`w-5 h-5 mr-2 ${isAnalyzing ? 'animate-pulse' : ''}`} />
                <div className="text-left">
                  <div className="font-semibold">
                    {isAnalyzing ? 'AI Analyzing...' : 'AI Analysis'}
                  </div>
                  <div className="text-xs opacity-80">
                    {isAnalyzing ? 'Please wait, analyzing data' : 'View intelligent insights and recommendations'}
                  </div>
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

            {showAIAnalysis && (
              <>
                {isAnalyzing ? (
                  <Card className="shadow-lg border-primary animate-pulse">
                    <CardContent className="p-8 text-center">
                      <Sparkles className="w-12 h-12 mx-auto mb-4 animate-spin text-primary" />
                      <h3 className="text-xl font-semibold mb-2">AI Analysis in Progress...</h3>
                      <p className="text-muted-foreground">Our AI is analyzing the trade data and generating insights</p>
                    </CardContent>
                  </Card>
                ) : apiResponse && typeof apiResponse === "object" && !Array.isArray(apiResponse) ? (
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
                ) : (
                  <Card className="shadow-lg border-yellow-500">
                    <CardContent className="p-6">
                      <p className="text-muted-foreground">No AI analysis available. Please try again.</p>
                    </CardContent>
                  </Card>
                )}
              </>
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