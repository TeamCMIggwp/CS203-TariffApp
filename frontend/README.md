This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev

## Configure backend target

Create a `.env.local` in this folder to point the proxy rewrites and middleware to your hosted backend:

```
BACKEND_URL=https://teamcmiggwp.duckdns.org
```

Restart `npm run dev` after adding or changing env vars.

```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.

## Google Sign-In configuration

The login page renders a Google Sign-In button using Google Identity Services. To enable it:

1. In Google Cloud Console, create an OAuth 2.0 Client (Web application) and add your authorized JavaScript origins (e.g. http://localhost:3000 and your Amplify domain).
2. In your environment, set the client id for the frontend, and ensure the backend can validate the audience:

```
# frontend/.env.local
NEXT_PUBLIC_GOOGLE_CLIENT_ID=YOUR_GOOGLE_OAUTH_CLIENT_ID

# optional: point directly to a different backend in dev
# BACKEND_URL=http://localhost:8080

# backend environment (e.g., Docker env, Amplify vars)
GOOGLE_OAUTH_CLIENT_ID=YOUR_GOOGLE_OAUTH_CLIENT_ID
JWT_SECRET=change-me               # must match frontend/middleware
JWT_ISSUER=tariff
JWT_AUDIENCE=tariff-web
COOKIE_SECURE=false                # true in prod behind HTTPS
COOKIE_SAMESITE=Lax                # consider None in cross-site HTTPS
FRONTEND_ORIGIN=http://localhost:3000
```

3. Restart both backend and frontend after setting env vars.

How it works:
- The browser obtains a Google ID token via Google Identity Services.
- The frontend posts it to `/api/auth/google` (rewritten to your backend `/auth/google`).
- The backend verifies the ID token with Google, upserts the user, rotates a refresh cookie, and returns an access token.
- The frontend stores the access token as an HttpOnly cookie for route gating and redirects to `/` or `/admin` based on role.
