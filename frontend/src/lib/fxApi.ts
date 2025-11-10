// Shared FX API logic used by currency converter and calculator

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

export function extractRateAndTo(json: unknown, wantTo: string): { rate: number; to: string } {
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

export async function fetchFxQuote(base: string, to: string): Promise<{ rate: number; to: string }> {
  const url = `${API_BASE}/api/v1/exchange?base=${encodeURIComponent(base)}`
  const res = await fetch(url, { method: "GET", headers: { Accept: "application/json" } })
  if (!res.ok) throw new Error(`FX API error: ${res.status} ${res.statusText}`)

  const data: unknown = await res.json()
  return extractRateAndTo(data, to)
}
