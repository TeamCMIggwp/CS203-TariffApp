"use client"

import { useState } from "react"
import { motion, type MotionValue } from "framer-motion"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { countries, agriculturalProducts, currencies } from "@/lib/tariff-data"

interface CalculatorSectionProps {
  calculatorY: MotionValue<number>
}

export default function CalculatorSection({ calculatorY }: CalculatorSectionProps) {
  const [fromCountry, setFromCountry] = useState("")
  const [toCountry, setToCountry] = useState("")
  const [product, setProduct] = useState("")
  const [value, setValue] = useState("")
  const [units, setUnits] = useState("")
  const [calculatedTariff, setCalculatedTariff] = useState<number | null>(null)

  const calculateTariff = () => {
    if (!fromCountry || !toCountry || !product || !value) return

    // Simple tariff calculation logic
    const baseRate = Math.random() * 0.25 + 0.05 // 5-30% tariff rate
    const productMultiplier = agriculturalProducts.indexOf(product) * 0.01 + 1
    const tariffAmount = Number.parseFloat(value) * baseRate * productMultiplier

    setCalculatedTariff(tariffAmount)
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
                    <SelectValue placeholder="Select exporting country" />
                  </SelectTrigger>
                  <SelectContent className="calculator-select-content">
                    {countries.map((country) => (
                      <SelectItem key={country} value={country} className="calculator-select-item">
                        {country}
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
                    <SelectValue placeholder="Select importing country" />
                  </SelectTrigger>
                  <SelectContent className="calculator-select-content">
                    {countries.map((country) => (
                      <SelectItem key={country} value={country} className="calculator-select-item">
                        {country}
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
                    <SelectValue placeholder="Select product type" />
                  </SelectTrigger>
                  <SelectContent className="calculator-select-content">
                    {agriculturalProducts.map((prod) => (
                      <SelectItem key={prod} value={prod} className="calculator-select-item">
                        {prod}
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

              {/* Units */}
              <div className="space-y-2">
                <Label htmlFor="units" className="calculator-label">
                  Units (kg/tons)
                </Label>
                <Input
                  id="units"
                  type="number"
                  placeholder="Enter quantity"
                  value={units}
                  onChange={(e) => setUnits(e.target.value)}
                  className="calculator-input"
                />
              </div>
            </div>

            {/* Calculate Button */}
            <div className="flex justify-center pt-6">
              <Button
                onClick={calculateTariff}
                disabled={!fromCountry || !toCountry || !product || !value}
                className="calculator-button"
              >
                Calculate Tariff
              </Button>
            </div>

            {/* Results */}
            {calculatedTariff !== null && (
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
                    <p className="text-gray-300">Estimated Tariff:</p>
                    <p className="font-semibold text-green-400">
                      {selectedCurrency} {calculatedTariff.toLocaleString(undefined, { maximumFractionDigits: 2 })}
                    </p>
                  </div>
                </div>
              </motion.div>
            )}
          </CardContent>
        </Card>
      </div>
    </motion.section>
  )
}