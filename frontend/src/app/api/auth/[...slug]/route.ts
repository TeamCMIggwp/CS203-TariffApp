import { NextResponse } from "next/server";

function backendBase(): string {
  const base = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
  return base.replace(/\/$/, "");
}

async function proxy(req: Request, ctx: { params: { slug?: string[] } }) {
  const { slug = [] } = ctx.params || {};
  const urlIn = new URL(req.url);
  const target = new URL(`${backendBase()}/auth/${slug.join("/")}`);
  if (urlIn.search) target.search = urlIn.search;

  // Clone headers, forward cookies; drop hop-by-hop headers
  const inHeaders = new Headers(req.headers);
  inHeaders.delete("host");
  inHeaders.delete("connection");
  inHeaders.delete("content-length");
  // Build the upstream request init
  const init: RequestInit = {
    method: req.method,
    headers: inHeaders,
    // Forward body for non-GET/HEAD
    body: req.method === "GET" || req.method === "HEAD" ? undefined : await req.clone().arrayBuffer(),
    // credentials not used in server-side fetch; cookies are in headers
    redirect: "manual",
  };

  const upstream = await fetch(target, init);
  const body = await upstream.arrayBuffer();

  // Copy headers; ensure Set-Cookie is preserved (single cookie expected)
  const outHeaders = new Headers();
  // Pass through common headers
  const contentType = upstream.headers.get("content-type");
  if (contentType) outHeaders.set("content-type", contentType);
  const setCookie = upstream.headers.get("set-cookie");
  if (setCookie) outHeaders.append("set-cookie", setCookie);

  // Allow JSON by default
  return new NextResponse(body, { status: upstream.status, headers: outHeaders });
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const DELETE = proxy;
