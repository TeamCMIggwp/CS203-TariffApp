"use client"

import { useMemo, useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { ArrowLeftRight } from "lucide-react"
import { countryToCurrency, getCurrencyCode, getCurrencyName } from "@/lib/fx"
import PageShell from "@/components/PageShell"

// ----- FX response shapes and type guards -----

type ConversionRatesShape = {
    conversionRates: Record<string, number>
}

type RatesShape = {
    rates: Record<string, number>
}

type RateToShape = {
    rate: number
    to: string
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null
}

function isConversionRatesShape(json: unknown): json is ConversionRatesShape {
    if (!isRecord(json)) return false
    const cr = json["conversionRates"]
    if (!isRecord(cr)) return false
    // spot check that values are numbers
    return Object.values(cr).every(v => typeof v === "number")
}

function isRatesShape(json: unknown): json is RatesShape {
    if (!isRecord(json)) return false
    const r = json["rates"]
    if (!isRecord(r)) return false
    return Object.values(r).every(v => typeof v === "number")
}

function isRateToShape(json: unknown): json is RateToShape {
    if (!isRecord(json)) return false
    return typeof json["rate"] === "number" && typeof json["to"] === "string"
}

function extractRateAndTo(json: unknown, wantTo: string): { rate: number; to: string } {
    const TO = wantTo.toUpperCase()

    if (isConversionRatesShape(json)) {
        const r = json.conversionRates[TO]
        if (typeof r === "number") return { rate: r, to: TO }
    }

    if (isRatesShape(json)) {
        const r = json.rates[TO]
        if (typeof r === "number") return { rate: r, to: TO }
    }

    if (isRateToShape(json)) {
        return { rate: json.rate, to: json.to.toUpperCase() }
    }

    throw new Error("Unrecognized FX response shape")
}

// ----- API base -----

const API_BASE =
    process.env.NEXT_PUBLIC_BACKEND_URL ||
    process.env.BACKEND_URL ||
    "http://localhost:8080"

// ----- Fetcher -----

async function fetchFxQuote(base: string, to: string): Promise<{ rate: number; to: string }> {
    const url = `${API_BASE}/api/v1/exchange?base=${encodeURIComponent(base)}`
    const res = await fetch(url, { method: "GET", headers: { Accept: "application/json" } })
    if (!res.ok) throw new Error(`FX API error: ${res.status} ${res.statusText}`)

    const data: unknown = await res.json()
    return extractRateAndTo(data, to)
}

// Derive country list as string[]
const countryNames = Object.keys(countryToCurrency as Record<string, unknown>) as string[]

export default function CurrencyConverterCard() {
    const [fromCountry, setFromCountry] = useState("Singapore")
    const [toCountry, setToCountry] = useState("United States")
    const [amount, setAmount] = useState("100")
    const [rate, setRate] = useState<number | null>(null)
    const [result, setResult] = useState<number | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const fromCcy = useMemo(() => getCurrencyCode(fromCountry), [fromCountry])
    const toCcy = useMemo(() => getCurrencyCode(toCountry), [toCountry])

    // keep a resolved target code that reflects what the API returns
    const [resolvedToCcy, setResolvedToCcy] = useState(toCcy)
    useEffect(() => {
        setResolvedToCcy(toCcy)
    }, [toCcy])

    const swapPairs = () => {
        setFromCountry(toCountry)
        setToCountry(fromCountry)
        setRate(null)
        setResult(null)
    }

    const handleConvert = async (): Promise<void> => {
        setError(null)
        setLoading(true)
        setResult(null)
        try {
            const amt = Number(amount)
            if (!Number.isFinite(amt) || amt < 0) throw new Error("Enter a valid amount.")
            const { rate: fx, to: apiTo } = await fetchFxQuote(fromCcy, toCcy)
            setRate(fx)
            setResolvedToCcy(apiTo || toCcy)
            setResult(amt * fx)
        } catch (e: unknown) {
            const msg = e instanceof Error ? e.message : "Conversion failed."
            setError(msg)
        } finally {
            setLoading(false)
        }
    }

    return (
        <PageShell title="Currency Converter" subtitle="Convert between importer/exporter currencies with live rates" maxWidthClassName="max-w-4xl">
            <Card className="w-full max-w-3xl rounded-2xl bg-black/40 backdrop-blur-xl border border-white/30 text-white mx-auto">
                <CardHeader className="pb-2">
                    <CardTitle className="text-3xl text-center text-white">Currency Converter</CardTitle>
                </CardHeader>

                <CardContent className="space-y-6">
                    {/* From / To country */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <Label>From Country ({fromCcy})</Label>
                            <Select value={fromCountry} onValueChange={setFromCountry}>
                                <SelectTrigger>
                                    <SelectValue>{fromCountry}</SelectValue>
                                </SelectTrigger>
                                <SelectContent>
                                    {countryNames.map(name => {
                                        const code = getCurrencyCode(name)
                                        return (
                                            <SelectItem key={name} value={name}>
                                                {name} — {getCurrencyName(code)} ({code})
                                            </SelectItem>
                                        )
                                    })}
                                </SelectContent>

                            </Select>
                        </div>

                        <div className="space-y-2">
                            <Label>To Country ({toCcy})</Label>
                            <Select value={toCountry} onValueChange={setToCountry}>
                                <SelectTrigger>
                                    <SelectValue>{toCountry}</SelectValue>
                                </SelectTrigger>
                                <SelectContent>
                                    {countryNames.map(name => {
                                        const code = getCurrencyCode(name)
                                        return (
                                            <SelectItem key={name} value={name}>
                                                {name} — {getCurrencyName(code)} ({code})
                                            </SelectItem>
                                        )
                                    })}
                                </SelectContent>

                            </Select>
                        </div>
                    </div>

                    {/* Amount + Swap + Convert */}
                    <div className="grid grid-cols-1 md:grid-cols-[1fr_auto_auto] gap-4 items-end">
                        <div className="space-y-2">
                            <Label>Amount</Label>
                            <div className="relative w-1/3 max-w-xs">
                                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                                    {fromCcy}
                                </span>
                                <Input
                                    type="text"
                                    inputMode="decimal"
                                    className="pl-14"
                                    placeholder={`Enter amount in ${fromCcy}`}
                                    value={amount}
                                    onChange={(e) => setAmount(e.target.value)}
                                />
                            </div>
                        </div>

                        <Button variant="secondary" onClick={swapPairs} className="h-10 md:h-11">
                            <ArrowLeftRight className="w-4 h-4 mr-2" />
                            Swap
                        </Button>

                        <Button onClick={handleConvert} className="h-10 md:h-11" disabled={loading}>
                            {loading ? "Converting…" : "Convert"}
                        </Button>
                    </div>

                    {/* Result */}
                    <div className="rounded-lg border border-white/20 p-4 bg-black/50">
                        {error ? (
                            <p className="text-sm text-red-300">{error}</p>
                        ) : rate !== null && result !== null ? (
                            <div className="space-y-1">
                                <span className="inline-flex items-center gap-2 rounded-md border border-amber-500/40 bg-amber-500/10 px-3 py-1.5 text-xs md:text-sm text-amber-200">
                                  <span className="font-semibold">FX:</span>
                                  <span>1 {fromCcy}</span>
                                  <span className="opacity-70">≈</span>
                                  <span className="font-mono">{rate.toFixed(6)}</span>
                                  <span>{resolvedToCcy}</span>
                                </span>
                                <p className="text-xl font-medium text-white">
                                    {Number(amount).toLocaleString()} {fromCcy} = {result.toLocaleString()} {resolvedToCcy}
                                </p>
                            </div>
                        ) : (
                            <p className="text-sm text-white/70">
                                Enter an amount and click Convert to see the result.
                            </p>
                        )}
                    </div>
                </CardContent>
            </Card>
        </PageShell>
    )
}