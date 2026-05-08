import http from "k6/http";
import { check, sleep } from "k6";
import { SharedArray } from "k6/data";

const required = ["BASE_URL", "PRACTICE_ID", "STRATEGY_ID", "SCENARIO_ID", "RUN_ID"];
for (const name of required) {
  if (!__ENV[name]) {
    throw new Error(`Missing required env var: ${name}`);
  }
}

const scenariosConfig = new SharedArray("scenarios", () => {
  const raw = open("./scenarios.json");
  return [JSON.parse(raw)];
});
const allScenarios = scenariosConfig[0];
const scenarioId = __ENV.SCENARIO_ID;
const scenario = allScenarios[scenarioId];

if (!scenario) {
  throw new Error(`Unknown SCENARIO_ID=${scenarioId}`);
}

const baseVus = Number(__ENV.K6_VUS || scenario.vus);
const burstVus = Math.max(1, Math.round(baseVus * Number(scenario.burst_multiplier || 1)));
const warmup = Number(scenario.warmup_seconds || 0);
const steady = Number(scenario.steady_seconds || 0);
const fault = Number(scenario.fault_seconds || 0);
const cooldown = Number(scenario.cooldown_seconds || 0);
const faultStartSec = warmup + steady;
const faultEndSec = faultStartSec + fault;
const testDurationSec = warmup + steady + fault + cooldown;
const duplicateMode = __ENV.DUPLICATE_MODE || "off";
const duplicateEveryN = Number(__ENV.DUPLICATE_EVERY_N || 5);

const authHeader = __ENV.AUTH_HEADER || "";
const authToken = __ENV.AUTH_TOKEN || "";

function nowSec() {
  return Math.floor(Date.now() / 1000);
}

const testStartEpochSec = nowSec();

export const options = {
  scenarios: {
    feedback_lab: {
      executor: "ramping-vus",
      startVUs: 1,
      stages: [
        { duration: `${warmup}s`, target: baseVus },
        { duration: `${steady}s`, target: baseVus },
        { duration: `${fault}s`, target: burstVus },
        { duration: `${cooldown}s`, target: baseVus }
      ]
    }
  },
  thresholds: {
    http_req_failed: ["rate<0.5"],
    http_req_duration: ["p(95)<20000"]
  },
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max", "count"]
};

function buildIdempotencyKey(vu, iteration) {
  const base = `rlab-${__ENV.RUN_ID}-${scenarioId}-vu${vu}-it${iteration}`;
  if (duplicateMode !== "periodic") {
    return base;
  }
  if (iteration > 0 && duplicateEveryN > 0 && iteration % duplicateEveryN === 0) {
    return `rlab-${__ENV.RUN_ID}-${scenarioId}-vu${vu}-it${iteration - 1}`;
  }
  return base;
}

function buildFaultHeaders() {
  return {
    "X-Lab-Run-Id": __ENV.RUN_ID,
    "X-Lab-Strategy-Id": __ENV.STRATEGY_ID,
    "X-Lab-Scenario-Id": scenarioId,
    "X-Lab-Fault-Mode": String(scenario.fault_mode || "none"),
    "X-Lab-Fault-Start-Sec": String(faultStartSec),
    "X-Lab-Fault-End-Sec": String(faultEndSec),
    "X-Lab-Test-Start-Epoch-Sec": String(testStartEpochSec),
    "Content-Type": "application/json"
  };
}

export default function () {
  const url = `${__ENV.BASE_URL}/api/v1/practices/${__ENV.PRACTICE_ID}/feedbacks`;
  const key = buildIdempotencyKey(__VU, __ITER);

  const headers = {
    "Idempotency-Key": key,
    ...buildFaultHeaders()
  };
  if (authHeader && authToken) {
    headers[authHeader] = authToken;
  }

  const res = http.post(url, "{}", { headers });
  check(res, {
    "response has status": (r) => r.status >= 200 && r.status < 600
  });
  sleep(0.1);
}

export function handleSummary(data) {
  return {
    stdout: JSON.stringify(
      {
        run_id: __ENV.RUN_ID,
        strategy_id: __ENV.STRATEGY_ID,
        scenario_id: scenarioId,
        duration_seconds: testDurationSec,
        metrics: data.metrics
      },
      null,
      2
    )
  };
}
