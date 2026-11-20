import type { VercelRequest, VercelResponse } from "@vercel/node";

const BASE = "https://api.morelogin.com";
let cached: { token: string; expiresAt: number } | null = null;

function env(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error("Service is not configured: " + name);
  return value;
}

async function request(path: string, body?: unknown, bearer?: string) {
  const response = await fetch(BASE + path, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...(bearer ? { Authorization: `Bearer ${bearer}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(25_000),
  });
  const data = await response.json().catch(() => ({})) as any;
  if (!response.ok || (typeof data.code === "number" && data.code !== 0)) {
    throw new Error(data.msg || `MoreLogin request failed (${response.status})`);
  }
  return data;
}

async function token() {
  if (cached && cached.expiresAt > Date.now() + 60_000) return cached.token;
  const data = await request("/oauth2/token", {
    client_id: env("MORELOGIN_CLIENT_ID"),
    client_secret: env("MORELOGIN_CLIENT_SECRET"),
    grant_type: "client_credentials",
  });
  const value = data?.data?.access_token;
  if (!value) throw new Error("MoreLogin did not return an access token");
  cached = { token: value, expiresAt: Date.now() + 50 * 60_000 };
  return value as string;
}

export async function api(path: string, body: unknown) {
  return request(path, body, await token());
}

export async function listAllPhones() {
  const result = await api("/cloudphone/page", { pageNo: 1, pageSize: 100 });
  return (result?.data?.dataList ?? []) as Array<Record<string, any>>;
}

export function phoneForInstall(rows: Array<Record<string, any>>, installId: string, phoneId: string) {
  return rows.find((p) => String(p.id) === phoneId && String(p.envRemark ?? "").startsWith(`MP:${installId}:`));
}

export function validInstallId(value: unknown): value is string {
  return typeof value === "string" && /^[a-f0-9-]{36}$/i.test(value);
}

export function sendError(res: VercelResponse, error: unknown) {
  const message = error instanceof Error ? error.message : "Unexpected service error";
  const status = /not configured/i.test(message) ? 503 : 502;
  res.status(status).json({ ok: false, error: message });
}

export function method(req: VercelRequest, res: VercelResponse, expected: string) {
  if (req.method !== expected) {
    res.setHeader("Allow", expected);
    res.status(405).json({ ok: false, error: "Method not allowed" });
    return false;
  }
  return true;
}
