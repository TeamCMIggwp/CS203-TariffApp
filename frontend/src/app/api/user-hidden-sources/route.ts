import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080';

export async function GET(request: NextRequest) {
  try {
    const accessToken = request.cookies.get('access_token')?.value;

    if (!accessToken) {
      return NextResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const response = await fetch(`${BACKEND_URL}/api/v1/user/hidden-sources`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    });

    const data = await response.json();
    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    console.error('Error fetching user hidden sources:', error);
    return NextResponse.json({ message: 'Internal server error' }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    const accessToken = request.cookies.get('access_token')?.value;

    if (!accessToken) {
      return NextResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const url = new URL(request.url);
    const newsLink = url.searchParams.get('newsLink');
    const action = url.searchParams.get('action'); // 'hide' or 'unhide'

    if (!newsLink) {
      return NextResponse.json({ message: 'newsLink parameter required' }, { status: 400 });
    }

    let backendUrl: string;
    let method: string;

    if (action === 'unhide') {
      backendUrl = `${BACKEND_URL}/api/v1/user/hidden-sources/unhide?newsLink=${encodeURIComponent(newsLink)}`;
      method = 'DELETE';
    } else {
      backendUrl = `${BACKEND_URL}/api/v1/user/hidden-sources/hide?newsLink=${encodeURIComponent(newsLink)}`;
      method = 'POST';
    }

    const response = await fetch(backendUrl, {
      method,
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    });

    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const data = await response.json();
    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    console.error('Error in user hidden sources operation:', error);
    return NextResponse.json({ message: 'Internal server error' }, { status: 500 });
  }
}

export async function DELETE(request: NextRequest) {
  try {
    const accessToken = request.cookies.get('access_token')?.value;

    if (!accessToken) {
      return NextResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const url = new URL(request.url);
    const unhideAll = url.searchParams.get('unhideAll') === 'true';

    let backendUrl: string;

    if (unhideAll) {
      backendUrl = `${BACKEND_URL}/api/v1/user/hidden-sources/unhide-all`;
    } else {
      const newsLink = url.searchParams.get('newsLink');
      if (!newsLink) {
        return NextResponse.json({ message: 'newsLink parameter required' }, { status: 400 });
      }
      backendUrl = `${BACKEND_URL}/api/v1/user/hidden-sources/unhide?newsLink=${encodeURIComponent(newsLink)}`;
    }

    const response = await fetch(backendUrl, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    });

    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      const data = await response.json();
      return NextResponse.json(data, { status: response.status });
    } else {
      const text = await response.text();
      return new NextResponse(text, { status: response.status });
    }
  } catch (error) {
    console.error('Error in user hidden sources delete:', error);
    return NextResponse.json({ message: 'Internal server error' }, { status: 500 });
  }
}
