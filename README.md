# CS203-TariffApp

A comprehensive web application for calculating and analyzing international tariff rates, integrating data from WTO and WITS APIs with AI-powered analysis capabilities.

## Features

- Interactive tariff rate calculator
- Real-time data integration with WTO and WITS APIs
- Admin dashboard for tariff management
- AI-powered trade analysis using Gemini
- Interactive global trade visualization
- User authentication and history tracking

## Tech Stack

- **Frontend:**
  - Next.js
  - TypeScript
  - Tailwind CSS
  - Framer Motion

- **Backend:**
  - Java Spring Boot
  - Maven
  - MySQL Database

## Prerequisites

- Node.js (v16 or higher)
- Java JDK 17 or higher
- Maven
- MySQL

## Installation

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

### Backend Setup

1. Navigate to the backend directory:
```bash
cd backend/app
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

## Production Deployment

To run the backend service in production:
```bash
nohup java -jar target/app-name.jar &
```

To stop the service:
```bash
ps aux | grep java
kill <PID>
```

## API Documentation

The API documentation is available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Password reset (SES)

The backend supports a secure forgot/reset password flow via AWS SES (or any SMTP):

- POST `/auth/forgot` body: `{ "email": "user@example.com" }`
  - Always returns 200; if the email exists, a reset link is sent.
- POST `/auth/reset` body: `{ "token": "<from email>", "newPassword": "..." }`
  - Returns `{ "updated": 1 }` when successful.

Frontend pages:
- `/forgot-password` — email form
- `/reset-password?token=...` — set new password

Configure mail for production (example for AWS SES SMTP):

```
spring.mail.host=email-smtp.<region>.amazonaws.com
spring.mail.port=587
spring.mail.username=<SMTP_USERNAME>
spring.mail.password=<SMTP_PASSWORD>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.from=noreply@yourdomain.tld
app.frontend.url=https://www.teamcmiggwpholidaymood.fun
```

If mail isn’t configured, the backend logs the reset link instead of sending an email (no-op), so flows can be tested locally.