import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

/**
 * Proxy for News Tariff Rates API
 * POST /api/database/news/rates -> POST /api/v1/admin/news/rates (backend)
 */
export async function POST(request: NextRequest) {
  try {
    console.log('[API Route] POST /api/database/news/rates called');
    console.log('[API Route] BACKEND_URL:', BACKEND_URL);

    const cookieStore = await cookies();
    const accessToken = cookieStore.get('access_token')?.value;

    if (!accessToken) {
      console.error('[API Route] No access token found in cookies');
      return NextResponse.json(
        { error: 'Unauthorized - No access token found' },
        { status: 401 }
      );
    }

    // Get request body
    const body = await request.json();
    const targetUrl = `${BACKEND_URL}/api/v1/admin/news/rates`;
    console.log('[API Route] Forwarding request to:', targetUrl);
    console.log('[API Route] Request body:', body);
    console.log('[API Route] Access token present:', !!accessToken);

    // Forward to backend
    const backendResponse = await fetch(targetUrl, {
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
    console.error('[API Route] Error in news tariff rates proxy:', error);
    console.error('[API Route] Error details:', error instanceof Error ? error.message : String(error));
    console.error('[API Route] Error stack:', error instanceof Error ? error.stack : 'No stack trace');
    return NextResponse.json(
      {
        error: 'Internal server error',
        details: error instanceof Error ? error.message : String(error),
        backendUrl: BACKEND_URL
      },
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
