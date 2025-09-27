"use client"

import { useState } from "react"
import { motion, useMotionValue } from "framer-motion"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { countries, agriculturalProducts, currencies } from "@/lib/tariff-data"

type GeminiApiResponse = {
  analysis?: string;
  success?: boolean;
  [key: string]: unknown;
};

//test
export default function CalculatorSection() {
  const calculatorY = useMotionValue(0)
  const [fromCountry, setFromCountry] = useState("")
  const [toCountry, setToCountry] = useState("")
  const [product, setProduct] = useState("")
  const [value, setValue] = useState("")
  const [year, setYear] = useState("")
  const [calculatedTariff, setCalculatedTariff] = useState<number | null>(null)

  // States for API Integration
  const [apiResponse, setApiResponse] = useState<GeminiApiResponse | string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [inputError, setInputError] = useState<string | null>(null)
  const [apiError, setApiError] = useState<string | null>(null)

  const [tariffPercentage, setTariffPercentage] = useState<string | null>(null)

  // Function to call API
  const callGeminiApi = async (data: string, prompt?: string) => {
    try {
      setIsLoading(true)
      setApiError(null)

      const baseUrl = 'https://teamcmiggwp.duckdns.org/gemini/analyze'
      const params = new URLSearchParams()
      params.append('data', data)
      if (prompt) params.append('prompt', prompt)

      const url = `${baseUrl}?${params.toString()}`

      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
        }
      })

      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)

      const result = await response.json()

      // Save raw text or structured result to display
      if (result?.success && result?.analysis) {
        setApiResponse(
          typeof result.analysis === 'string'
            ? result.analysis
            : JSON.stringify(result.analysis, null, 2)
        )
      } else {
        setApiResponse("No analysis data returned from API.")
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Unknown error occurred'
      setApiError(errorMessage)
      console.error('API Error:', errorMessage)
    } finally {
      setIsLoading(false)
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

    try {
      const dummyApiUrl = `https://teamcmiggwp.duckdns.org/api/wits/tariffs/min-rate?reporter=${toCountry}&partner=${fromCountry}&product=${product}&year=${year}`;

      const dummyResponse = await fetch(dummyApiUrl);

      if (!dummyResponse.ok) {
        throw new Error("Dummy API call failed");
      }

      const dummyText = await dummyResponse.text();

      const match = dummyText.match(/([\d.]+)%?/)
      const parsedPercentage = match ? parseFloat(match[1]) : null

      if (parsedPercentage !== null && !isNaN(parsedPercentage)) {
        setTariffPercentage(`${parsedPercentage.toFixed(2)}%`)
        const goodsValue = parseFloat(value)
        const tariffAmount = (parsedPercentage / 100) * goodsValue
        setCalculatedTariff(tariffAmount)
      } else {
        setTariffPercentage("MFN")
        setCalculatedTariff(null)
      }

      //reverting
    } catch (err) {
      setTariffPercentage("MFN")
      setCalculatedTariff(null)
    }

    const apiData = `Trade analysis: Export from ${fromCountry} to ${toCountry}. Product: ${product}, Value: $${value}, Year: ${year || 'N/A'}`
    const prompt = "Analyze this agricultural trade data and provide insights on tariff implications, trade relationships, and economic factors"

    await callGeminiApi(apiData, prompt)
  }

  const selectedCurrency = toCountry ? currencies[toCountry as keyof typeof currencies] || "USD" : "USD"

  return (
    <motion.section style={{ y: calculatorY }} className="calculator-section py-20">
      <div className="max-w-4xl mx-auto px-4">
        <Card className="calculator-card">
          <CardHeader>
            <CardTitle className="calculator-title">Agricultural Tariff Calculator</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">

              {/* From Country */}
              <div className="space-y-2">
                <Label htmlFor="from-country" className="calculator-label">
                  From Country (Exporter)
                </Label>
                <Select value={fromCountry} onValueChange={setFromCountry}>
                  <SelectTrigger className="calculator-select">
                    <SelectValue placeholder="Select exporting country">
                      {fromCountry
                        ? countries.find((c) => c.code === fromCountry)?.name
                        : "Select exporting country"}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent className="calculator-select-content">
                    {countries.map((country) => (
                      <SelectItem
                        key={country.code}
                        value={country.code}
                        className="calculator-select-item"
                      >
                        <div className="flex flex-col">
                          <span className="font-medium">{country.name}</span>
                          <span className="text-xs text-gray-600 mt-0.5">
                            code: {country.code}
                          </span>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* To Country */}
              <div className="space-y-2">
                <Label htmlFor="to-country" className="calculator-label">
                  To Country (Importer)
                </Label>
                <Select value={toCountry} onValueChange={setToCountry}>
                  <SelectTrigger className="calculator-select">
                    {/* Show ONLY the country name in the trigger */}
                    <SelectValue placeholder="Select importing country">
                      {toCountry
                        ? countries.find((c) => c.code === toCountry)?.name
                        : "Select importing country"}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent className="calculator-select-content">
                    {countries.map((country) => (
                      <SelectItem
                        key={country.code}
                        value={country.code}
                        className="calculator-select-item"
                      >
                        <div className="flex flex-col">
                          <span className="font-medium">{country.name}</span>
                          <span className="text-xs text-gray-600 mt-0.5">
                            code: {country.code}
                          </span>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Product Type */}
              <div className="space-y-2">
                <Label htmlFor="product" className="calculator-label">
                  Agricultural Product
                </Label>
                <Select value={product} onValueChange={setProduct}>
                  <SelectTrigger className="calculator-select">
                    {/* Show ONLY the product name in the trigger */}
                    <SelectValue placeholder="Select product type">
                      {product
                        ? agriculturalProducts.find((p) => p.hs_code === product)?.name
                        : "Select product type"}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent className="calculator-select-content">
                    {agriculturalProducts.map((prod) => (
                      <SelectItem
                        key={prod.hs_code}
                        value={prod.hs_code}
                        className="calculator-select-item"
                      >
                        <div className="flex flex-col">
                          <span className="font-medium">{prod.name}</span>
                          <span className="text-xs text-gray-600 mt-0.5">
                            code: {prod.hs_code}
                          </span>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Value of Goods */}
              <div className="space-y-2">
                <Label htmlFor="value" className="calculator-label">
                  Value of Goods ({selectedCurrency})
                </Label>
                <Input
                  id="value"
                  type="number"
                  placeholder="Enter value"
                  value={value}
                  onChange={(e) => setValue(e.target.value)}
                  className="calculator-input"
                />
              </div>

              {/* Year */}
              <div className="space-y-2">
                <Label htmlFor="year" className="calculator-label">
                  Year
                </Label>
                <Select value={year} onValueChange={setYear}>
                  <SelectTrigger className="calculator-select">
                    <SelectValue placeholder="Select year" />
                  </SelectTrigger>
                  <SelectContent className="calculator-select-content">
                    {[2025, 2024, 2023, 2022, 2021, 2020].map((yr) => (
                      <SelectItem key={yr} value={String(yr)} className="calculator-select-item">
                        {yr}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Calculate Button */}
            <div className="flex justify-center pt-6">
              <Button
                onClick={calculateTariff}
                disabled={!fromCountry || !toCountry || !product || !value || isLoading}
                className="calculator-button"
              >
                {isLoading ? "Calculating..." : "Calculate Tariff"}
              </Button>
            </div>

            {/* Input Error Message */}
            {inputError && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-red-600 p-4 rounded-lg text-white"
              >
                <strong>Error:</strong> {inputError}
              </motion.div>
            )}

            {/* Results */}
            {tariffPercentage && (
              <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="calculator-results">
                <h3 className="text-xl font-bold text-white mb-4">Tariff Calculation Results</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-white">
                  <div>
                    <p className="text-gray-300">Trade Route:</p>
                    <p className="font-semibold">
                      {fromCountry} → {toCountry}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-300">Product:</p>
                    <p className="font-semibold">{product}</p>
                  </div>
                  <div>
                    <p className="text-gray-300">Goods Value:</p>
                    <p className="font-semibold">
                      {selectedCurrency} {Number.parseFloat(value).toLocaleString()}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-300">Tariff Percentage:</p>
                    <p className="font-semibold">
                      {tariffPercentage}
                    </p>
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

                {/* Gemini AI Analysis (Plain Text) */}
                {apiResponse && (
                  <div className="mt-8 bg-blue-600 p-4 rounded-lg">
                    <h4 className="text-white font-semibold mb-2">Gemini AI Analysis</h4>
                    <p className="text-white whitespace-pre-wrap">
                      {typeof apiResponse === 'object' && apiResponse !== null && 'analysis' in apiResponse
                        ? apiResponse.analysis
                        : typeof apiResponse === 'string'
                          ? apiResponse
                          : 'No analysis available.'}
                    </p>
                  </div>
                )}

                {/* API Error Message */}
                {apiError && (
                  <div className="mt-6 bg-red-600 p-4 rounded-lg">
                    <h4 className="text-white font-semibold mb-2">API Error</h4>
                    <p className="text-white">{apiError}</p>
                  </div>
                )}

              </motion.div>
            )}
          </CardContent>
        </Card>
      </div>
    </motion.section>
  )
}