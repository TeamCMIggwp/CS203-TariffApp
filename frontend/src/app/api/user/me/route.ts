import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  try {
    const accessToken = request.cookies.get('access_token')?.value;

    if (!accessToken) {
      return NextResponse.json({ authenticated: false, isAdmin: false }, { status: 200 });
    }

    // Decode JWT to get role
    try {
      const payload = JSON.parse(Buffer.from(accessToken.split('.')[1], 'base64').toString());
      const role = payload.role || 'user';
      const isAdmin = role.toLowerCase() === 'admin' ||
                     role.toLowerCase() === 'administrator' ||
                     role.toUpperCase() === 'ROLE_ADMIN';

      return NextResponse.json({
        authenticated: true,
        isAdmin,
        role,
        userId: payload.userId
      });
    } catch (decodeError) {
      console.error('Error decoding JWT:', decodeError);
      return NextResponse.json({ authenticated: false, isAdmin: false }, { status: 200 });
    }
  } catch (error) {
    console.error('Error in /api/user/me:', error);
    return NextResponse.json({ authenticated: false, isAdmin: false }, { status: 200 });
  }
}
