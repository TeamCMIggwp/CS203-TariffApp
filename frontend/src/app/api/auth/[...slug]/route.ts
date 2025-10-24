import { NextResponse } from "next/server";

function backendBase(): string {
  const base = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
  return base.replace(/\/$/, "");
}

async function handle(req: Request, params: { slug: string[] }) {
  const slug = params?.slug ?? [];
  const urlIn = new URL(req.url);
  const target = new URL(`${backendBase()}/auth/${slug.join("/")}`);
  if (urlIn.search) target.search = urlIn.search;

  // Clone headers, forward cookies; drop hop-by-hop headers
  const inHeaders = new Headers(req.headers);
  inHeaders.delete("host");
  inHeaders.delete("connection");
  inHeaders.delete("content-length");

  const bodyNeeded = !(req.method === "GET" || req.method === "HEAD");
  const init: RequestInit = {
    method: req.method,
    headers: inHeaders,
    body: bodyNeeded ? await req.arrayBuffer() : undefined,
    redirect: "manual",
  };

  const upstream = await fetch(target, init);
  const body = await upstream.arrayBuffer();

  const outHeaders = new Headers();
  const contentType = upstream.headers.get("content-type");
  if (contentType) outHeaders.set("content-type", contentType);
  const setCookie = upstream.headers.get("set-cookie");
  if (setCookie) outHeaders.append("set-cookie", setCookie);

  return new NextResponse(body, { status: upstream.status, headers: outHeaders });
}

type RouteCtx = { params?: { slug?: string[] } } | undefined;
const getParams = (ctx: RouteCtx): { slug: string[] } => ({ slug: ctx?.params?.slug ?? [] });

export async function GET(req: Request, context: unknown) {
  return handle(req, getParams(context as RouteCtx));
}
export async function POST(req: Request, context: unknown) {
  return handle(req, getParams(context as RouteCtx));
}
export async function PUT(req: Request, context: unknown) {
  return handle(req, getParams(context as RouteCtx));
}
export async function DELETE(req: Request, context: unknown) {
  return handle(req, getParams(context as RouteCtx));
}
