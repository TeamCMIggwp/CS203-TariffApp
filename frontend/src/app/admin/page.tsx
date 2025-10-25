"use client"

import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { countries, agriculturalProducts } from "@/lib/tariff-data"

export default function AdminPage() {
  const [fromCountry, setFromCountry] = useState("")
  const [toCountry, setToCountry] = useState("")
  const [product, setProduct] = useState("")
  const [year, setYear] = useState("")
  const [tariffRate, setTariffRate] = useState("")

  const [activeTab, setActiveTab] = useState<"update" | "delete">("update")

  const [isUpdating, setIsUpdating] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const [updateSuccessMessage, setUpdateSuccessMessage] = useState("")
  const [updateErrorMessage, setUpdateErrorMessage] = useState("")
  const [deleteSuccessMessage, setDeleteSuccessMessage] = useState("")
  const [deleteErrorMessage, setDeleteErrorMessage] = useState("")


  const parseCountryCode = (code: string): string | null => {
    return code ? code : null
  }

  const parseHsCode = (hsCode: string): number | null => {
    if (!hsCode) return null
    const parsed = parseInt(hsCode, 10)
    return isNaN(parsed) ? null : parsed
  }

  const handleSubmit = async () => {
    try {
        if (!fromCountry || !toCountry || !product || !year || !tariffRate) {
            throw new Error("Please fill in all fields.");
        }

        const fromCountryIso = parseCountryCode(fromCountry)
        const toCountryIso = parseCountryCode(toCountry)
        const hsCode = parseHsCode(product)

        if (fromCountryIso === null || toCountryIso === null) {
            throw new Error("Invalid country selection");
        }

        if (!hsCode) {
            throw new Error("Invalid product selection");
        }

        const rateValue = parseFloat(tariffRate);
        if (isNaN(rateValue) || rateValue < 0) {
            throw new Error("Please enter a valid tariff rate");
        }

        setIsUpdating(true);
        setUpdateErrorMessage("");
        setUpdateSuccessMessage("");

      let response = await fetch("/api/v1/tariffs", {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify({
          partnerIsoNumeric: fromCountryIso,   
          countryIsoNumeric: toCountryIso,     
          productHsCode: hsCode,              
          year: year,
          rate: rateValue,
          unit: "percent"
        })
      });

      // If tariff not found (404), fallback to POST
      if (response.status === 404) {
        response = await fetch("/api/v1/tariffs", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          credentials: "include",
          body: JSON.stringify({
            countryIsoNumeric: toCountryIso,
            partnerIsoNumeric: fromCountryIso,
            productHsCode: hsCode,
            year: year,
            rate: rateValue,
            unit: "percent"
          })
        });
      }

      if (!response.ok) {
          // Try to parse JSON error message, otherwise read raw text, else fallback to generic
          const ct = response.headers.get("content-type") || "";
          let errorText = `Failed to update tariff rate (HTTP ${response.status})`;
          try {
            if (ct.includes("application/json")) {
              const errorData = await response.json();
              errorText = errorData.message || errorText;
            } else {
              const text = await response.text();
              if (text) errorText = text.slice(0, 500);
            }
          } catch {
            // ignore parse errors
          }
          throw new Error(errorText);
      }

      // Ensure backend returned JSON; avoid JSON parse errors on unexpected HTML
      const contentType = response.headers.get("content-type") || "";
      if (!contentType.includes("application/json")) {
        const text = await response.text();
        throw new Error(text?.slice(0, 200) || "Unexpected non-JSON response from server");
      }
      const data = await response.json();
      setUpdateSuccessMessage(data.message || "Tariff rate updated successfully");
      
      // Clear form after successful update
      setFromCountry("");
      setToCountry("");
      setProduct("");
      setYear("");
      setTariffRate("");

    } catch (error) {
        setUpdateErrorMessage(error instanceof Error ? error.message : "An unknown error occurred");
    } finally {
        setIsUpdating(false);
    }
  };

  const handleDelete = async () => {
    try {
      if (!fromCountry || !toCountry || !product || !year) {
        throw new Error("Please fill in all fields for deletion.");
      }

      const hsCode = parseHsCode(product);
      if (!hsCode) throw new Error("Invalid product selection")

      setIsDeleting(true);
      setDeleteErrorMessage("");
      setDeleteSuccessMessage("");

      const params = new URLSearchParams({
        reporter: toCountry,
        partner: fromCountry,
        product: String(hsCode),
        year: year
      });

      const url = `/api/v1/tariffs?${params.toString()}`;

      const response = await fetch(url, {
        method: "DELETE",
        credentials: "include"
      });

      if (response.status === 204) {
        setDeleteSuccessMessage("Entry deleted successfully");
      } else {
        const ct = response.headers.get("content-type") || "";
        let errMsg = `Failed to delete tariff entry (HTTP ${response.status})`;
        try {
          if (ct.includes("application/json")) {
            const body = await response.json();
            errMsg = body.message || JSON.stringify(body) || errMsg;
          } else {
            const text = await response.text();
            if (text) errMsg = text.slice(0, 500);
          }
        } catch {
          // ignore parse errors
        }
        throw new Error(errMsg);
      }

      // Clear fields after successful delete
      setFromCountry("");
      setToCountry("");
      setProduct("");
      setYear("");
      setTariffRate("");
    } catch (err) {
      setDeleteErrorMessage(err instanceof Error ? err.message : "An unknown error occurred");
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <section className="calculator-section py-16">
      <div className="max-w-3xl mx-auto px-4">
        <Card className="calculator-card">
          <CardHeader className="pt-8 pb-4">
            <CardTitle className="text-center text-2xl font-semibold">Tariff Admin Dashboard</CardTitle>

            {/* Tabs centered below title */}
            <div className="mt-4 flex justify-center space-x-3">
              <Button
                variant={activeTab === "update" ? "default" : "ghost"}
                onClick={() => {
                  setActiveTab("update")
                  setDeleteErrorMessage("")
                  setDeleteSuccessMessage("")
                }}
                className="px-4"
              >
                Update / Add Tariff Rate
              </Button>
              <Button
                variant={activeTab === "delete" ? "destructive" : "ghost"}
                onClick={() => {
                  setActiveTab("delete")
                  setUpdateErrorMessage("")
                  setUpdateSuccessMessage("")
                }}
                className="px-4"
              >
                Delete Tariff Rate
              </Button>
            </div>
          </CardHeader>

          <CardContent className="space-y-6 pt-2 pb-8">
            <p className="text-center text-sm text-muted-foreground my-6">
              {activeTab === "update"
                ? "Fill the fields and click Update to add or update a tariff rate."
                : "Fill the identifying fields and click Delete to remove the tariff entry."}
            </p>

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

              {/* Tariff Rate - only shown for update/add */}
              {activeTab === "update" && (
                <div className="space-y-2">
                  <Label className="calculator-label">Tariff Rate (%)</Label>
                  <Input
                    type="number"
                    step="any"
                    placeholder="e.g. 11.12345"
                    value={tariffRate}
                    onChange={(e) => setTariffRate(e.target.value)}
                    className="calculator-input"
                  />
                </div>
              )}
            </div>

            {/* Submit Buttons */}
            <div className="flex justify-center pt-6">
              {activeTab === "update" ? (
                <Button onClick={handleSubmit} disabled={isUpdating} className="calculator-button">
                  {isUpdating ? "Updating..." : "Update Database"}
                </Button>
              ) : (
                <Button onClick={handleDelete} disabled={isDeleting} className="calculator-button">
                  {isDeleting ? "Deleting..." : "Delete Entry"}
                </Button>
              )}
            </div>

            {/* Messages */}
            <div className="mt-4 text-center">
              {activeTab === "update" && updateSuccessMessage && (
                <p className="text-green-500 text-center font-semibold">{updateSuccessMessage}</p>
              )}
              {activeTab === "update" && updateErrorMessage && (
                <p className="text-red-500 text-center font-semibold">{updateErrorMessage}</p>
              )}

              {activeTab === "delete" && deleteSuccessMessage && (
                <p className="text-green-500 text-center font-semibold">{deleteSuccessMessage}</p>
              )}
              {activeTab === "delete" && deleteErrorMessage && (
                <p className="text-red-500 text-center font-semibold">{deleteErrorMessage}</p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </section>
  )
}