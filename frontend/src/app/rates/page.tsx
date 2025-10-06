"use client"

import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Label } from "@/components/ui/label"
import { countries, agriculturalProducts } from "@/lib/tariff-data"

export default function RatesPage() {
  const [fromCountry, setFromCountry] = useState("")
  const [toCountry, setToCountry] = useState("")
  const [product, setProduct] = useState("")
  const [year, setYear] = useState("")
  const [retrievedRate, setRetrievedRate] = useState<string | null>(null)

  const [isFetching, setIsFetching] = useState(false)
  const [errorMessage, setErrorMessage] = useState("")

  const parseCountryCode = (code: string): string | null => {
    return code ? code : null
  }

  const parseHsCode = (hsCode: string): number | null => {
    if (!hsCode) return null
    const parsed = parseInt(hsCode, 10)
    return isNaN(parsed) ? null : parsed
  }

  const handleRetrieve = async () => {
    try {
      if (!fromCountry || !toCountry || !product || !year) {
        throw new Error("Please fill in all fields.")
      }

      const fromCountryIso = parseCountryCode(fromCountry)
      const toCountryIso = parseCountryCode(toCountry)
      const hsCode = parseHsCode(product)

      if (fromCountryIso === null || toCountryIso === null) {
        throw new Error("Invalid country selection.")
      }

      if (!hsCode) {
        throw new Error("Invalid product selection.")
      }

      setIsFetching(true)
      setErrorMessage("")
      setRetrievedRate(null)

      const query = new URLSearchParams({
        reporter: toCountryIso,
        partner: fromCountryIso,
        product: hsCode.toString(),
        year: year,
      })

      const response = await fetch(`https://teamcmiggwp.duckdns.org/api/database/retrieve?${query.toString()}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      })

      if (!response.ok) {
        let errorText = "Failed to retrieve tariff rate"
        try {
          const errorData = await response.json()
          errorText = errorData.message || errorText
        } catch (_) {}
        throw new Error(errorText)
      }

      const data = await response.json()
      setRetrievedRate(`${data.rate}%`)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "An unknown error occurred")
    } finally {
      setIsFetching(false)
    }
  }

  return (
    <section className="calculator-section py-20">
      <div className="max-w-4xl mx-auto px-4">
        <Card className="calculator-card">
          <CardHeader>
            <CardTitle className="calculator-title">Retrieve Tariff Rate</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* From Country */}
              <div className="space-y-2">
                <Label className="calculator-label">From Country (Exporter)</Label>
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
                      <SelectItem key={country.code} value={country.code} className="calculator-select-item">
                        <div className="flex flex-col">
                          <span className="font-medium">{country.name}</span>
                          <span className="text-xs text-gray-600 mt-0.5">code: {country.code}</span>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* To Country */}
              <div className="space-y-2">
                <Label className="calculator-label">To Country (Importer)</Label>
                <Select value={toCountry} onValueChange={setToCountry}>
                  <SelectTrigger className="calculator-select">
                    <SelectValue placeholder="Select importing country">
                      {toCountry
                        ? countries.find((c) => c.code === toCountry)?.name
                        : "Select importing country"}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent className="calculator-select-content">
                    {countries.map((country) => (
                      <SelectItem key={country.code} value={country.code} className="calculator-select-item">
                        <div className="flex flex-col">
                          <span className="font-medium">{country.name}</span>
                          <span className="text-xs text-gray-600 mt-0.5">code: {country.code}</span>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Product */}
              <div className="space-y-2">
                <Label className="calculator-label">Agricultural Product</Label>
                <Select value={product} onValueChange={setProduct}>
                  <SelectTrigger className="calculator-select">
                    <SelectValue placeholder="Select product type">
                      {product
                        ? agriculturalProducts.find((p) => p.hs_code === product)?.name
                        : "Select product type"}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent className="calculator-select-content">
                    {agriculturalProducts.map((product) => (
                      <SelectItem key={product.hs_code} value={product.hs_code} className="calculator-select-item">
                        <div className="flex flex-col">
                          <span className="font-medium">{product.name}</span>
                          <span className="text-xs text-gray-600 mt-0.5">code: {product.hs_code}</span>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Year */}
              <div className="space-y-2">
                <Label className="calculator-label">Year</Label>
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

            {/* Submit Button */}
            <div className="flex justify-center pt-6">
              <Button onClick={handleRetrieve} disabled={isFetching} className="calculator-button">
                {isFetching ? "Retrieving..." : "Retrieve Tariff Rate"}
              </Button>
            </div>

            {/* Retrieved Rate */}
            {retrievedRate && (
              <p className="text-green-500 text-center font-semibold">
                Tariff Rate: {retrievedRate}
              </p>
            )}

            {/* Error Message */}
            {errorMessage && (
              <p className="text-red-500 text-center font-semibold">{errorMessage}</p>
            )}
          </CardContent>
        </Card>
      </div>
    </section>
  )
}
