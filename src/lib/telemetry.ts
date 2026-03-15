export type ProductEventName =
  | "workout_started"
  | "workout_completed"
  | "set_logged"
  | "exercise_created";

type ApiTimingPoint = {
  durationMs: number;
  status?: number;
  ok?: boolean;
  at: string;
};

const productEventCounts: Record<ProductEventName, number> = {
  workout_started: 0,
  workout_completed: 0,
  set_logged: 0,
  exercise_created: 0,
};

const apiTimings: Record<string, ApiTimingPoint[]> = {};

function percentile(values: number[], ratio: number) {
  if (values.length === 0) return 0;
  const sorted = [...values].sort((a, b) => a - b);
  const index = Math.min(
    sorted.length - 1,
    Math.max(0, Math.ceil(sorted.length * ratio) - 1),
  );
  return sorted[index];
}

export function trackProductEvent(name: ProductEventName, count = 1) {
  productEventCounts[name] += count;
  console.info(`[metrics:event] ${name} +${count} (total=${productEventCounts[name]})`);
}

export function trackApiTiming(
  endpoint: string,
  durationMs: number,
  status?: number,
) {
  const point: ApiTimingPoint = {
    durationMs,
    status,
    ok: typeof status === "number" ? status >= 200 && status < 300 : undefined,
    at: new Date().toISOString(),
  };

  if (!apiTimings[endpoint]) {
    apiTimings[endpoint] = [];
  }
  apiTimings[endpoint].push(point);

  const statusLabel = typeof status === "number" ? String(status) : "network_error";
  console.info(
    `[metrics:api] ${endpoint} status=${statusLabel} duration_ms=${durationMs.toFixed(1)}`,
  );
}

export function getTelemetrySnapshot() {
  const api = Object.entries(apiTimings).map(([endpoint, points]) => {
    const durations = points.map((point) => point.durationMs);
    return {
      endpoint,
      count: points.length,
      p50Ms: percentile(durations, 0.5),
      p95Ms: percentile(durations, 0.95),
      lastStatus: points[points.length - 1]?.status,
    };
  });

  return {
    productEvents: { ...productEventCounts },
    api,
  };
}

if (__DEV__) {
  // Expose quick metrics inspection from the RN debug console.
  (globalThis as { __caliTrackMetrics?: { getSnapshot: typeof getTelemetrySnapshot } }).__caliTrackMetrics =
    { getSnapshot: getTelemetrySnapshot };
}
