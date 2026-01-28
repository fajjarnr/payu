import { NextResponse } from 'next/server';

/**
 * Health check endpoint for container orchestration
 * Used by Docker HEALTHCHECK and Kubernetes readiness probes
 */
export async function GET() {
  try {
    // Basic health check - always return 200 if the server is running
    return NextResponse.json(
      {
        status: 'healthy',
        timestamp: new Date().toISOString(),
        service: 'payu-web-app',
        version: '1.0.0',
      },
      {
        status: 200,
        headers: {
          'Cache-Control': 'no-store, no-cache, must-revalidate',
          'Content-Type': 'application/json',
        },
      }
    );
  } catch (error) {
    return NextResponse.json(
      {
        status: 'unhealthy',
        error: 'Service unavailable',
        timestamp: new Date().toISOString(),
      },
      { status: 503 }
    );
  }
}
