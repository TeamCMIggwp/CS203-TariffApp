import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

/**
 * Proxy for News Tariff Rates API
 * POST /api/database/news/rates -> POST /api/v1/admin/news/rates (backend)
 */
export async function POST(request: NextRequest) {
  try {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get('access_token')?.value;

    if (!accessToken) {
      return NextResponse.json(
        { error: 'Unauthorized - No access token found' },
        { status: 401 }
      );
    }

    // Get request body
    const body = await request.json();

    // Forward to backend
    const backendResponse = await fetch(`${BACKEND_URL}/api/v1/admin/news/rates`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`,
      },
      body: JSON.stringify(body),
    });

    // Forward backend response
    if (backendResponse.ok) {
      const data = await backendResponse.json();
      return NextResponse.json(data, { status: backendResponse.status });
    } else {
      // Forward error from backend
      const errorText = await backendResponse.text();
      return new NextResponse(errorText, {
        status: backendResponse.status,
        headers: { 'Content-Type': 'text/plain' }
      });
    }
  } catch (error) {
    console.error('Error in news tariff rates proxy:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

/**
 * Proxy for News Tariff Rates API
 * GET /api/database/news/rates?newsLink=... -> GET /api/v1/admin/news/rates?newsLink=... (backend)
 */
export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const newsLink = searchParams.get('newsLink');
    const checkExistence = searchParams.get('checkExistence');

    if (!newsLink) {
      return NextResponse.json(
        { error: 'Missing newsLink parameter' },
        { status: 400 }
      );
    }

    // Build backend URL
    let backendUrl = `${BACKEND_URL}/api/v1/admin/news/rates`;
    if (checkExistence === 'true') {
      backendUrl += `/existence?newsLink=${encodeURIComponent(newsLink)}`;
    } else {
      backendUrl += `?newsLink=${encodeURIComponent(newsLink)}`;
    }

    // Forward to backend (GET endpoints are public, no auth needed)
    const backendResponse = await fetch(backendUrl, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
    });

    // Forward backend response
    if (backendResponse.ok) {
      const data = await backendResponse.json();
      return NextResponse.json(data, { status: backendResponse.status });
    } else {
      const errorText = await backendResponse.text();
      return new NextResponse(errorText, {
        status: backendResponse.status,
        headers: { 'Content-Type': 'text/plain' }
      });
    }
  } catch (error) {
    console.error('Error in news tariff rates GET proxy:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
