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