"use client"

import { useState, useEffect, useMemo } from "react"
import { motion, useMotionValue, AnimatePresence } from "framer-motion"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { countries, agriculturalProducts, currencies } from "@/lib/tariff-data"
import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid, ScatterChart, Scatter, ZAxis, PieChart, Pie, Cell } from "recharts"
import { BarChart3, Sparkles, Database, Globe, Plus, Trash2 } from "lucide-react"
import { getCurrencyCode, getCurrencyName, countryToCurrency } from "@/lib/fx"
import { fetchFxQuote } from "@/lib/fxApi"
import { ChevronDown } from "lucide-react"

type MetricValue = string | number

// Define the possible API response shape
interface ExchangeApiResponse {
  conversionRates?: Record<string, number>
  rates?: Record<string, number>
  rate?: number
}

type GeminiApiResponse = {
  summary?: string
  insights?: string[]
  metrics?: Record<string, MetricValue>
  recommendations?: string[]
  confidence?: string
} | string | null

type ProductRow = {
  id: string
  productCode: string
  value: string
  tariffRate: number | null
  tariffAmount: number | null
  dataSource: 'database' | 'wto' | null
  savedToDatabase: boolean
  status: 'idle' | 'loading' | 'success' | 'error'
  errorMessage?: string
  fromCountry: string
  toCountry: string
  year: string
}

// Derive country list as string[] from currency converter
const countryNames = Object.keys(countryToCurrency as Record<string, unknown>) as string[]

export default function CalculatorSection() {
  const calculatorY = useMotionValue(0)
  const [fromCountry, setFromCountry] = useState("004")
  const [toCountry, setToCountry] = useState("840")
  const [year, setYear] = useState("2024")

  // Changed to array of products
  const [products, setProducts] = useState<ProductRow[]>([
    { id: '1', productCode: '100630', value: '100', tariffRate: null, tariffAmount: null, dataSource: null, savedToDatabase: false, status: 'idle', fromCountry: '004', toCountry: '840', year: '2024' }
  ])

  const [isCalculatingTariff, setIsCalculatingTariff] = useState(false)
  const [inputError, setInputError] = useState<string | null>(null)
  const [apiError, setApiError] = useState<string | null>(null)
  const [calculationFinished, setCalculationFinished] = useState(false)
  const [showCharts, setShowCharts] = useState(false)
  // No per-product selection needed for pie; we'll show overall composition

  // === Currency Conversion ===
  const selectedCurrency = toCountry ? currencies[toCountry as keyof typeof currencies] || "USD" : "USD"
  const [displayCurrency, setDisplayCurrency] = useState<string>(selectedCurrency)
  const [conversionRate, setConversionRate] = useState<number>(1)
  const [currencyLoading, setCurrencyLoading] = useState(false)
  const [currencyError, setCurrencyError] = useState<string | null>(null)
  // Map of baseCurrency -> rate to displayCurrency (1 base = rate display)
  const [fxRates, setFxRates] = useState<Record<string, number>>({})

  // Backend API base URL, configurable via environment for Amplify and local dev
  const API_BASE = process.env.NEXT_PUBLIC_BACKEND_URL || process.env.BACKEND_URL || 'http://localhost:8080'

  // Helper: get currency code for a given importer country code
  // 1) Try code→currency map from tariff-data
  // 2) Fallback to country name→currency map from fx
  // 3) Default to USD
  const getImporterCurrency = (importerCountryCode: string) => {
    const direct = currencies[importerCountryCode as keyof typeof currencies] as string | undefined
    if (direct) return direct
    const countryName = countries.find(c => c.code === importerCountryCode)?.name
    if (countryName) {
      const byName = (countryToCurrency as Record<string, string | undefined>)[countryName]
      if (byName) return byName
    }
    return "USD"
  }

  // Fetch per-base conversion rates to the current display currency
  useEffect(() => {
    let cancelled = false
    setCurrencyError(null)

    // Unique base currencies from current products (their importer currencies)
    const basesSet = new Set<string>()
    products.forEach(p => basesSet.add(getImporterCurrency(p.toCountry)))
    // Always include display currency with a self-rate of 1
    basesSet.add(displayCurrency)
    const bases = Array.from(basesSet)

    // IMPORTANT: Rates depend on the current displayCurrency. If displayCurrency changes, previously
    // cached fxRates are no longer valid. Always re-fetch every base (except the displayCurrency itself)
    // to avoid stale 1:1 mappings.
    const toFetch = bases.filter(b => b !== displayCurrency)
    // Always ensure display currency self-rate is set; we'll still continue to fetch others.
    setFxRates(prev => ({ ...prev, [displayCurrency]: 1 }))

    setCurrencyLoading(true)

    const fetchRateForBase = async (base: string) => {
      try {
        const { rate } = await fetchFxQuote(base, displayCurrency)
        return { base, rate }
      } catch (err) {
        console.error('Currency conversion error (per-product):', err)
        setCurrencyError(`Failed to fetch ${base}→${displayCurrency} rate`)
        return { base, rate: NaN }
      }
    }

    Promise.all(toFetch.map(fetchRateForBase))
      .then(results => {
        if (cancelled) return
        setFxRates(prev => {
          const next = { [displayCurrency]: 1 } // rebuild fresh map for current display currency
          results.forEach(r => {
            if (r && Number.isFinite(r.rate)) {
              next[r.base] = r.rate as number
            }
          })
          return next
        })
      })
      .catch(err => {
        console.error('Currency conversion batch error:', err)
        setCurrencyError('Failed to fetch some exchange rates')
      })
      .finally(() => {
        if (!cancelled) setCurrencyLoading(false)
      })

    return () => { cancelled = true }
  }, [products, displayCurrency])

  // Update display currency when importer country changes
  useEffect(() => {
    const newCurrency = toCountry ? currencies[toCountry as keyof typeof currencies] || "USD" : "USD"
    setDisplayCurrency(newCurrency)
  }, [toCountry])

  // Keep legacy single-rate in sync for UI fallback (based on global importer)
  useEffect(() => {
    const singleBase = selectedCurrency
    if (singleBase === displayCurrency) {
      setConversionRate(1)
      return
    }
    // Prefer the fetched fxRates if available
    if (fxRates[singleBase] !== undefined) {
      setConversionRate(fxRates[singleBase])
      return
    }
    // Otherwise fetch once
    const fetchLegacy = async () => {
      try {
        setCurrencyLoading(true)
        const { rate } = await fetchFxQuote(singleBase, displayCurrency)
        setConversionRate(rate)
        setFxRates(prev => ({ ...prev, [singleBase]: rate, [displayCurrency]: 1 }))
      } catch (err) {
        console.error('Currency conversion error (legacy):', err)
        setCurrencyError(`Failed to fetch ${singleBase}→${displayCurrency} rate`)
      } finally {
        setCurrencyLoading(false)
      }
    }
    fetchLegacy()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [displayCurrency, selectedCurrency])

  // (Removed pie product selection effect – pie now aggregates all products.)

  // Unique importer currencies across all products (for UI hints)
  const importerCurrencies = useMemo(() => {
    const set = new Set<string>()
    products.forEach(p => set.add(getImporterCurrency(p.toCountry)))
    return Array.from(set)
  }, [products])

  // Helper functions to convert/format amounts from a given base currency to the display currency
  const convertAmountFrom = (amount: number, baseCurrency: string) => {
    if (!Number.isFinite(amount)) return 0
    if (baseCurrency === displayCurrency) return amount
    const rate = fxRates[baseCurrency]
    return Number.isFinite(rate) ? amount * rate : amount
  }

  const formatCurrencyFrom = (amount: number, baseCurrency: string) => {
    return convertAmountFrom(amount, baseCurrency).toLocaleString(undefined, { maximumFractionDigits: 2 })
  }

  const addProduct = () => {
    const newId = String(Date.now())
    setProducts([...products, {
      id: newId,
      productCode: '100630',
      value: '',
      tariffRate: null,
      tariffAmount: null,
      dataSource: null,
      savedToDatabase: false,
      status: 'idle',
      fromCountry,
      toCountry,
      year
    }])
  }

  const removeProduct = (id: string) => {
    if (products.length > 1) {
      setProducts(products.filter(p => p.id !== id))
    }
  }

  const updateProduct = (
    id: string,
    field: 'productCode' | 'value' | 'fromCountry' | 'toCountry' | 'year',
    value: string
  ) => {
    setProducts(products.map(p =>
      p.id === id ? { ...p, [field]: value } : p
    ))
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

  const queryTariffForProduct = async (
    productCode: string,
    importerCode: string,
    exporterCode: string,
    yearVal: string
  ): Promise<{
    rate: number | null
    source: 'database' | 'wto' | null
    savedToDatabase: boolean
  }> => {
    let parsedPercentage: number | null = null
    let foundInDatabase = false

    // STEP 1: Query your database first
    console.log(`\n📊 STEP 1: Querying database for product ${productCode}...`)
    const startDb = performance.now()
    const productInt = parseInt(productCode, 10)
    const dbUrl = `${API_BASE}/api/v1/tariffs?reporter=${encodeURIComponent(importerCode)}&partner=${encodeURIComponent(exporterCode)}&product=${productInt}&year=${encodeURIComponent(yearVal)}&tariffTypeId=1`
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
                return { rate: parsedPercentage, source: 'database', savedToDatabase: false }
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
      const wtoUrl = `${API_BASE}/api/v1/indicators/HS_P_0070/observations?i=HS_P_0070&r=${importerCode}&p=${exporterCode}&pc=${productCode}&ps=${yearVal}&fmt=json`
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

              type WTORecord = { Year?: number; Value?: string | number;[key: string]: unknown }
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

                    // Save to database if found in WTO API
                    const saveSuccess = await saveTariffToDatabase(
                      importerCode,     // reporter (importer)
                      exporterCode,     // partner (exporter)
                      productInt,       // product code
                      yearVal,          // year
                      parsedPercentage, // rate
                      "percent"         // unit
                    )

                    return { rate: parsedPercentage, source: 'wto', savedToDatabase: saveSuccess }
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

    return { rate: null, source: null, savedToDatabase: false }
  }

  const calculateTariff = async () => {
    // Validate all products have data
    const hasEmptyProducts = products.some(p => !p.productCode || !p.value)
    if (!fromCountry || !toCountry || !year || hasEmptyProducts) {
      setInputError("Please fill in all required fields for all products.")
      return
    }

    console.log('═══════════════════════════════════════════')
    console.log('🚀 STARTING TARIFF CALCULATION')
    console.log('═══════════════════════════════════════════')
    console.log('Parameters:', { fromCountry, toCountry, year, productCount: products.length })

    setInputError(null)
    setApiError(null)
    setIsCalculatingTariff(true)
    setCalculationFinished(false)
    setShowCharts(false)

    // Reset all products to loading state
    setProducts(products.map(p => ({
      ...p,
      status: 'loading' as const,
      tariffRate: null,
      tariffAmount: null,
      dataSource: null,
      savedToDatabase: false
    })))

    try {
      // Query tariffs for all products concurrently
      console.log(`\n🚀 Processing ${products.length} products concurrently...`)
      const productPromises = products.map(async (product) => {
        console.log(`\n🔍 Processing product ${product.productCode}...`)

        try {
          const result = await queryTariffForProduct(
            product.productCode,
            product.toCountry,
            product.fromCountry,
            product.year
          )
          const productValue = parseFloat(product.value)

          if (result.rate !== null && !isNaN(productValue)) {
            const tariffAmount = (result.rate / 100) * productValue
            console.log(`   ✅ Tariff calculated: ${result.rate}% = ${tariffAmount}`)
            return {
              ...product,
              tariffRate: result.rate,
              tariffAmount: tariffAmount,
              dataSource: result.source,
              savedToDatabase: result.savedToDatabase,
              status: 'success' as const
            }
          } else {
            console.log(`   ⚠️ No tariff data found`)
            return {
              ...product,
              tariffRate: null,
              tariffAmount: null,
              dataSource: null,
              savedToDatabase: false,
              status: 'error' as const,
              errorMessage: 'No tariff data found (MFN may apply)'
            }
          }
        } catch (err) {
          console.error(`   ❌ Error processing product:`, err)
          return {
            ...product,
            status: 'error' as const,
            errorMessage: err instanceof Error ? err.message : 'Unknown error'
          }
        }
      })

      // Wait for all product queries to complete concurrently
      const updatedProducts = await Promise.all(productPromises)
      console.log(`\n✅ All ${products.length} products processed concurrently`)

      setProducts(updatedProducts)
      setIsCalculatingTariff(false)
      setCalculationFinished(true)
      console.log('   ✅ Tariff results displayed to user')

      console.log('\n═══════════════════════════════════════════')
      console.log('✅ TARIFF CALCULATION COMPLETE')
      console.log('═══════════════════════════════════════════\n')

    } catch (err) {
      console.error('\n❌❌❌ ERROR in calculateTariff:', err)
      setApiError(err instanceof Error ? err.message : "Unknown error occurred")
      setCalculationFinished(true)
      console.error('   Stack trace:', err instanceof Error ? err.stack : 'N/A')
    } finally {
      setIsCalculatingTariff(false)
    }
  }

  const getCountryName = (code: string) => {
    const country = countries.find(c => c.code === code)
    return country ? country.name : code
  }

  // Calculate totals (converted to display currency using each product's importer currency)
  const totals = useMemo(() => {
    let value = 0
    let tariff = 0
    products.forEach(p => {
      const base = getImporterCurrency(p.toCountry)
      const v = parseFloat(p.value) || 0
      const t = p.tariffAmount || 0
      value += convertAmountFrom(v, base)
      tariff += convertAmountFrom(t, base)
    })
    return { value, tariff, cost: value + tariff }
  }, [products, fxRates, displayCurrency])

  const dummyChartData = [
    { product: "Wheat", tariff: 5.2 },
    { product: "Rice", tariff: 12.5 },
    { product: "Corn", tariff: 8.1 },
    { product: "Soybeans", tariff: 10.0 },
    { product: "Barley", tariff: 6.3 },
  ]

  // ===== Chart Data (A: Direct, B: Derived) =====
  const successfulProducts = useMemo(() => (
    products.filter(p => p.status === 'success' && p.tariffRate !== null)
  ), [products])

  const productName = (code: string) => (
    agriculturalProducts.find(p => p.hs_code === code)?.name || code
  )

  // Generate a short country code (acronym) from numeric code
  const shortCountry = (code: string) => {
    const name = getCountryName(code)
    if (!name) return code
    // Take first letter of up to first 3 words (to avoid very long names)
    const parts = name.replace(/['’]/g, '').split(/\s+/).filter(Boolean).slice(0, 3)
    const acronym = parts.map(p => p[0].toUpperCase()).join('')
    return acronym || code
  }

  // A1: Comparative bar (HS product vs tariff %)
  const tariffRateBarData = useMemo(() => (
    successfulProducts.map(p => ({
      // Include exporter→importer short country acronyms for differentiation
      name: `${productName(p.productCode)} (${shortCountry(p.fromCountry)}→${shortCountry(p.toCountry)})`,
      tariffRate: Number(p.tariffRate || 0)
    }))
  ), [successfulProducts])

  // A2: Stacked bar (Goods Value vs Tariff Amount) per product (in display currency)
  const costStackedData = useMemo(() => (
    successfulProducts.map(p => {
      const base = getImporterCurrency(p.toCountry)
      return {
        name: `${productName(p.productCode)} (${shortCountry(p.fromCountry)}→${shortCountry(p.toCountry)})`,
        goodsValue: convertAmountFrom(parseFloat(p.value) || 0, base),
        tariffAmount: convertAmountFrom(p.tariffAmount || 0, base)
      }
    })
  ), [successfulProducts, fxRates, displayCurrency])

  // B1: Scatter (Goods Value vs Tariff Rate)
  const valueVsRateScatter = useMemo(() => (
    successfulProducts.map(p => ({
      x: convertAmountFrom(parseFloat(p.value) || 0, getImporterCurrency(p.toCountry)),
      y: Number(p.tariffRate || 0),
      name: productName(p.productCode)
    }))
  ), [successfulProducts, fxRates, displayCurrency])

  // B2: Waterfall (Import Value -> +Tariff -> Total Cost)
  const waterfallData = useMemo(() => {
    const importVal = totals.value
    const tariffVal = totals.tariff
    const total = totals.cost
    // For enhanced visualization: keep steps plus a final split bar
    return [
      { name: 'Import Value', base: 0, change: importVal },
      { name: 'Tariff Added', base: importVal, change: tariffVal },
      // Final stacked representation for total cost (two components)
      { name: 'Total Cost (Split)', goodsValue: importVal, tariffAmount: tariffVal }
    ]
  }, [totals])

  // Aggregated pie data: each product contributes two slices (Product Value, Product Tariff)
  const pieData = useMemo(() => {
    const slices: { name: string; value: number }[] = []
    products.forEach((p, idx) => {
      if (p.status === 'success' && p.tariffRate !== null) {
        const base = getImporterCurrency(p.toCountry)
        const goodsVal = convertAmountFrom(parseFloat(p.value) || 0, base)
        const tariffVal = convertAmountFrom(p.tariffAmount || 0, base)
        const route = `${shortCountry(p.fromCountry)}→${shortCountry(p.toCountry)}`
        slices.push({ name: `P${idx + 1} ${route}`, value: goodsVal })
        slices.push({ name: `P${idx + 1} ${route} Tariff`, value: tariffVal })
      }
    })
    return slices
  }, [products, fxRates, displayCurrency])

  return (
    <motion.section style={{ y: calculatorY }} className="calculator-section py-20">
      <div className="max-w-4xl mx-auto px-4">
        {/* Standard header to match News page */}
        <div className="text-center mb-6 bg-black/30 backdrop-blur-md p-6 rounded-2xl">
          <h1 className="text-3xl md:text-4xl font-extrabold text-white">
            Agricultural Tariff Calculator
          </h1>
        </div>

        <Card className="calculator-card shadow-lg rounded-2xl bg-black/40 backdrop-blur-xl text-white">
          <CardContent className="space-y-6">
            {/* Top row: Display Currency selector (per-product currencies fetched) */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <Label htmlFor="display-currency" className="flex items-center gap-2">
                  <span>Display Currency</span>
                  {currencyLoading && <span className="text-xs text-blue-600">(updating...)</span>}
                </Label>
                <Select value={displayCurrency} onValueChange={setDisplayCurrency}>
                  <SelectTrigger>
                    <SelectValue>{displayCurrency}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {countryNames.map(name => {
                      const code = getCurrencyCode(name)
                      return (
                        <SelectItem key={name} value={code}>
                          {name} — {getCurrencyName(code)} ({code})
                        </SelectItem>
                      )
                    })}
                  </SelectContent>
                </Select>
                {/* Per-product FX rates are shown in results section now */}
                {currencyError && (
                  <p className="text-xs text-red-600">{currencyError}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label className="text-sm">Add Products (each with its own exporter/importer/year):</Label>
                <p className="text-xs text-muted-foreground">Use the list below to configure multiple trade routes before calculating tariffs.</p>
              </div>
            </div>

            {/* Products Section */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <Label>Products</Label>
                <Button
                  type="button"
                  onClick={addProduct}
                  disabled={isCalculatingTariff}
                  className="flex items-center bg-black hover:bg-neutral-800 text-white font-medium rounded-lg px-4 py-2 shadow-md transition-all duration-150 disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  <Plus className="w-4 h-4 mr-1" />
                  <span>Add Product</span>
                </Button>
              </div>

              {products.map((product, index) => {
                const importerCurrency = getImporterCurrency(product.toCountry)

                return (
                  <div key={product.id} className="p-4 rounded-lg border border-white/20 bg-black/50 relative space-y-4">
                    {products.length > 1 && !isCalculatingTariff && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="absolute top-2 right-2"
                        onClick={() => removeProduct(product.id)}
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    )}

                    {/* First Row: Product, Exporter, Importer, Year */}
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor={`product-${product.id}`}>Product {index + 1}</Label>
                        <Select
                          value={product.productCode}
                          onValueChange={(val) => updateProduct(product.id, 'productCode', val)}
                          disabled={isCalculatingTariff}
                        >
                          <SelectTrigger>
                            <SelectValue>{agriculturalProducts.find(p => p.hs_code === product.productCode)?.name}</SelectValue>
                          </SelectTrigger>
                          <SelectContent>
                            {agriculturalProducts.map(p => (
                              <SelectItem key={p.hs_code} value={p.hs_code}>{p.name} ({p.hs_code})</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>

                      <div className="space-y-2">
                        <Label>Exporter</Label>
                        <Select
                          value={product.fromCountry}
                          onValueChange={(val) => updateProduct(product.id, 'fromCountry', val)}
                          disabled={isCalculatingTariff}
                        >
                          <SelectTrigger>
                            <SelectValue>{getCountryName(product.fromCountry)}</SelectValue>
                          </SelectTrigger>
                          <SelectContent>
                            {countries.map(c => (
                              <SelectItem key={c.code} value={c.code}>{c.name} ({c.code})</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>

                      <div className="space-y-2">
                        <Label>Importer</Label>
                        <Select
                          value={product.toCountry}
                          onValueChange={(val) => updateProduct(product.id, 'toCountry', val)}
                          disabled={isCalculatingTariff}
                        >
                          <SelectTrigger>
                            <SelectValue>{getCountryName(product.toCountry)}</SelectValue>
                          </SelectTrigger>
                          <SelectContent>
                            {countries.map(c => (
                              <SelectItem key={c.code} value={c.code}>{c.name} ({c.code})</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>

                      <div className="space-y-2">
                        <Label>Year</Label>
                        <Select
                          value={product.year}
                          onValueChange={(val) => updateProduct(product.id, 'year', val)}
                          disabled={isCalculatingTariff}
                        >
                          <SelectTrigger>
                            <SelectValue>{product.year}</SelectValue>
                          </SelectTrigger>
                          <SelectContent>
                            {[2024, 2023, 2022, 2021, 2020, 2019].map(y => (
                              <SelectItem key={y} value={String(y)}>{y}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>

                    {/* Second Row: Value of Goods */}
                    <div className="space-y-2">
                      <Label htmlFor={`value-${product.id}`} className="flex items-center gap-2">
                        <span>Value of Goods</span>
                        <span className="text-xs font-normal text-muted-foreground">
                          (Importer Currency: {importerCurrency})
                        </span>
                      </Label>
                      <Input
                        type="number"
                        id={`value-${product.id}`}
                        value={product.value}
                        onChange={e => updateProduct(product.id, 'value', e.target.value)}
                        placeholder={`Enter value in ${importerCurrency}`}
                        disabled={isCalculatingTariff}
                        className="max-w-md"
                      />
                    </div>

                    {product.status === 'loading' && (
                      <div className="text-center text-sm text-gray-500">
                        <div className="inline-block animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900 mr-2"></div>
                        Loading tariff data...
                      </div>
                    )}
                  </div>
                )
              })}
            </div>

            <Button
              onClick={calculateTariff}
              disabled={!fromCountry || !toCountry || isCalculatingTariff}
              className="w-full py-4"
            >
              {isCalculatingTariff ? "Calculating..." : "Calculate Tariff"}
            </Button>

            {inputError && <div className="bg-red-600 text-white p-4 rounded-lg">{inputError}</div>}
            {apiError && !inputError && (
              <div className="bg-yellow-600 text-white p-4 rounded-lg">{apiError}</div>
            )}
          </CardContent>
        </Card>

        {calculationFinished && (
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="mt-8 space-y-6 rounded-2xl bg-black/40 backdrop-blur-xl border border-white/30 p-6">
            <h3 className="text-2xl font-bold text-white mb-4">Tariff Calculation Results</h3>

            {/* Individual Product Results */}
            <div className="space-y-4">
              {products.map((product, index) => {
                const productName = agriculturalProducts.find(p => p.hs_code === product.productCode)?.name || product.productCode
                const baseCurrency = getImporterCurrency(product.toCountry)

                return (
                  <div key={product.id} className="rounded-lg border border-white/20 p-4 bg-black/50">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-semibold text-white">Product {index + 1}: {productName}</h4>
                      {product.dataSource && (
                        <div className="flex items-center gap-2">
                          <div className="flex items-center gap-2 bg-blue-600/20 border border-blue-600 rounded-lg px-3 py-1 text-blue-200">
                            {product.dataSource === "database" ? (
                              <>
                                <Database className="w-4 h-4" />
                                <span className="text-sm">Database</span>
                              </>
                            ) : (
                              <>
                                <Globe className="w-4 h-4" />
                                <span className="text-sm">WTO API</span>
                              </>
                            )}
                          </div>
                          {product.savedToDatabase && (
                            <div className="flex items-center gap-2 bg-green-600/20 border border-green-600 rounded-lg px-3 py-1 text-green-200">
                              <Database className="w-4 h-4" />
                              <span className="text-sm">Saved to Database</span>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                    <div className="text-xs md:text-sm text-gray-300 mb-2">
                      Route: <span className="font-medium text-white">{getCountryName(product.fromCountry)}</span>
                      <span className="mx-1">→</span>
                      <span className="font-medium text-white">{getCountryName(product.toCountry)}</span>
                      <span className="mx-2">•</span>
                      Year: <span className="font-medium text-white">{product.year}</span>
                    </div>
                    {/* Show FX rate for this product's importer currency to the current display currency */}
                    <div className="mb-3">
                      {baseCurrency === displayCurrency ? (
                        <span className="inline-flex items-center gap-2 rounded-md border border-gray-500/40 bg-gray-500/10 px-3 py-1.5 text-xs md:text-sm text-gray-200">
                          <span className="font-semibold">FX:</span>
                          <span>Same currency</span>
                          <span className="opacity-70">•</span>
                          <span className="font-mono">{displayCurrency}</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-2 rounded-md border border-amber-500/40 bg-amber-500/10 px-3 py-1.5 text-xs md:text-sm text-amber-200">
                          <span className="font-semibold">FX:</span>
                          <span>1 {baseCurrency}</span>
                          <span className="opacity-70">≈</span>
                          <span className="font-mono">{Number.isFinite(fxRates[baseCurrency]) ? fxRates[baseCurrency].toFixed(4) : '...'}</span>
                          <span>{displayCurrency}</span>
                        </span>
                      )}
                    </div>

                    {product.status === 'success' && product.tariffRate !== null ? (
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-white">
                        <div>
                          <p className="text-gray-300 text-sm">Goods Value ({baseCurrency} → {displayCurrency}):</p>
                          <p className="font-semibold">{displayCurrency} {formatCurrencyFrom(Number.parseFloat(product.value), baseCurrency)}</p>
                        </div>
                        <div>
                          <p className="text-gray-300 text-sm">Tariff Rate:</p>
                          <p className="font-semibold">{product.tariffRate.toFixed(2)}%</p>
                        </div>
                        <div>
                          <p className="text-gray-300 text-sm">Tariff Amount ({baseCurrency} → {displayCurrency}):</p>
                          <p className="font-semibold text-red-300">
                            {displayCurrency} {formatCurrencyFrom(product.tariffAmount || 0, baseCurrency)}
                          </p>
                        </div>
                        <div>
                          <p className="text-gray-300 text-sm">Total Cost ({baseCurrency} → {displayCurrency}):</p>
                          <p className="font-semibold text-green-300">
                            {displayCurrency} {formatCurrencyFrom((parseFloat(product.value) || 0) + (product.tariffAmount || 0), baseCurrency)}
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div className="text-yellow-300">
                        {product.errorMessage || "Data Not Available - MFN rates may apply"}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>

            {/* Overall Totals */}
            <div className="rounded-lg border border-white/20 p-6 bg-black/50">
              <h4 className="text-xl font-bold text-white mb-4">Overall Totals</h4>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-white">
                <div>
                  <p className="text-gray-300">Total Import Value:</p>
                  <p className="text-2xl font-bold">{displayCurrency} {totals.value.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
                </div>
                <div>
                  <p className="text-gray-300">Total Tariff:</p>
                  <p className="text-2xl font-bold text-red-300">{displayCurrency} {totals.tariff.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
                </div>
                <div>
                  <p className="text-gray-300">Total Cost:</p>
                  <p className="text-2xl font-bold text-green-300">{displayCurrency} {totals.cost.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
                </div>
              </div>
            </div>

            {/* Charts Button */}
            <div className="mt-6">
              <Button
                onClick={() => setShowCharts(!showCharts)}
                size="lg"
                className={`w-full py-6 border-2 transition-all ${showCharts ? 'bg-primary text-white border-primary hover:bg-primary/90' : 'bg-white text-black border-white hover:bg-gray-100'
                  }`}
              >
                <BarChart3 className="w-5 h-5 mr-2" />
                <div className="text-left">
                  <div className="font-semibold">Charts & Diagrams</div>
                  <div className="text-xs opacity-80">Visualize tariff comparisons</div>
                </div>
              </Button>
            </div>


            {showCharts && (
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-6 mt-4">
                {/* A1: Tariff rate comparative bar */}
                <Card className="shadow-lg p-4 bg-black/40 backdrop-blur-xl border border-white/30 text-white">
                  <h4 className="text-lg font-semibold mb-2">Tariff Rate by Product (%)</h4>
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={tariffRateBarData} margin={{ top: 10, right: 20, left: 0, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" interval={0} angle={-20} textAnchor="end" height={60} />
                      <YAxis unit="%" />
                      <Tooltip formatter={(v: number) => `${v.toFixed(2)}%`} />
                      <Legend />
                      <Bar dataKey="tariffRate" name="Tariff %" fill="#6366F1" />
                    </BarChart>
                  </ResponsiveContainer>
                </Card>

                {/* A2: Stacked bar cost decomposition */}
                <Card className="shadow-lg p-4 bg-black/40 backdrop-blur-xl border border-white/30 text-white">
                  <h4 className="text-lg font-semibold mb-2">Cost Decomposition per Product ({displayCurrency})</h4>
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={costStackedData} margin={{ top: 10, right: 20, left: 0, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" interval={0} angle={-20} textAnchor="end" height={60} />
                      <YAxis />
                      <Tooltip formatter={(v: number) => `${displayCurrency} ${v.toLocaleString(undefined, { maximumFractionDigits: 2 })}`} />
                      <Legend />
                      <Bar dataKey="goodsValue" stackId="a" name="Goods Value" fill="#22C55E" />
                      <Bar dataKey="tariffAmount" stackId="a" name="Tariff Amount" fill="#EF4444" />
                    </BarChart>
                  </ResponsiveContainer>
                </Card>

                {/* B1: Scatter (Value vs Rate) */}
                <Card className="shadow-lg p-4 bg-black/40 backdrop-blur-xl border border-white/30 text-white">
                  <h4 className="text-lg font-semibold mb-2">Value vs Tariff Rate ({displayCurrency})</h4>
                  <ResponsiveContainer width="100%" height={300}>
                    <ScatterChart margin={{ top: 10, right: 20, bottom: 20, left: 0 }}>
                      <CartesianGrid />
                      <XAxis type="number" dataKey="x" name="Goods Value" tickFormatter={(v) => `${(v as number).toLocaleString()}`} />
                      <YAxis type="number" dataKey="y" name="Tariff %" unit="%" />
                      <ZAxis range={[60, 120]} />
                      <Tooltip cursor={{ strokeDasharray: '3 3' }} formatter={(v: number, n: string) => n === 'y' ? `${v.toFixed(2)}%` : `${displayCurrency} ${(v).toLocaleString(undefined, { maximumFractionDigits: 2 })}`} />
                      <Legend />
                      <Scatter data={valueVsRateScatter} name="Products" fill="#06B6D4" />
                    </ScatterChart>
                  </ResponsiveContainer>
                </Card>

                {/* B2: Waterfall (approximate) */}
                <Card className="shadow-lg p-4 bg-black/40 backdrop-blur-xl border border-white/30 text-white xl:col-span-2">
                  <h4 className="text-lg font-semibold mb-2">Landed Cost Waterfall & Split ({displayCurrency})</h4>
                  <ResponsiveContainer width="100%" height={340}>
                    <BarChart data={waterfallData} margin={{ top: 10, right: 30, left: 0, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" />
                      <YAxis />
                      <Tooltip formatter={(v: number, n: string) => `${displayCurrency} ${Number(v).toLocaleString(undefined, { maximumFractionDigits: 2 })}${n === 'tariffAmount' || n === 'change' ? ' (Tariff)' : ''}`} />
                      <Legend />
                      {/* Step bars */}
                      <Bar dataKey="base" stackId="wf" fill="transparent" isAnimationActive={false} />
                      <Bar dataKey="change" stackId="wf" name="Step Change" fill="#6366F1" />
                      {/* Final split stacked bar */}
                      <Bar dataKey="goodsValue" stackId="totalSplit" name="Import Value Portion" fill="#22C55E" />
                      <Bar dataKey="tariffAmount" stackId="totalSplit" name="Tariff Portion" fill="#EF4444" />
                    </BarChart>
                  </ResponsiveContainer>
                </Card>

                {/* Total Cost Composition Pie (route + product, multi-color) */}
                <Card className="shadow-lg p-4 bg-black/40 backdrop-blur-xl border border-white/30 text-white xl:col-span-2">
                  <div className="flex items-center justify-between mb-2">
                    <h4 className="text-lg font-semibold">Total Cost Composition ({displayCurrency})</h4>
                  </div>
                  {pieData.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No successful products to summarize yet.</p>
                  ) : (
                    <ResponsiveContainer width="100%" height={320}>
                      <PieChart>
                        <Pie
                          data={pieData}
                          dataKey="value"
                          nameKey="name"
                          cx="50%"
                          cy="50%"
                          innerRadius={60}
                          outerRadius={110}
                          paddingAngle={2}
                          label={(entry) => `${entry.name}: ${displayCurrency} ${entry.value.toLocaleString(undefined, { maximumFractionDigits: 2 })}`}
                        >
                          {pieData.map((entry, index) => {
                            const palette = [
                              '#22C55E', '#EF4444', '#6366F1', '#F59E0B', '#06B6D4',
                              '#8B5CF6', '#10B981', '#F97316', '#EC4899', '#3B82F6',
                              '#84CC16', '#D946EF', '#16A34A', '#DC2626', '#1D4ED8'
                            ]
                            // Differentiate tariff slices by darkening color or using next palette item
                            const baseColor = palette[index % palette.length]
                            const isTariff = /Tariff$/i.test(entry.name)
                            const color = isTariff ? palette[(index + 1) % palette.length] : baseColor
                            return <Cell key={`cell-${index}`} fill={color} />
                          })}
                        </Pie>
                        <Tooltip formatter={(v: number, n: string) => [`${displayCurrency} ${v.toLocaleString(undefined, { maximumFractionDigits: 2 })}`, n]} />
                        <Legend />
                      </PieChart>
                    </ResponsiveContainer>
                  )}
                  {pieData.length > 0 && (
                    <p className="text-xs mt-2 text-muted-foreground">
                      Pie total equals Overall Total Cost: {displayCurrency} {totals.cost.toLocaleString(undefined, { maximumFractionDigits: 2 })}
                    </p>
                  )}
                </Card>
              </div>
            )}
          </motion.div>
        )}
      </div>
    </motion.section>
  )
}