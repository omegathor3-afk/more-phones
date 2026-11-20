import type { VercelRequest, VercelResponse } from "@vercel/node";
import { api, listAllPhones, method, sendError, validInstallId } from "../_lib/morelogin.js";

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (!method(req, res, "GET")) return;
  const installId = req.query.installId;
  if (!validInstallId(installId)) return res.status(400).json({ ok: false, error: "Invalid installation identifier" });
  try {
    const rows = await listAllPhones();
    const phones = rows.filter((p) => String(p.envRemark ?? "").startsWith(`MP:${installId}:`)).map((p) => ({
      id: String(p.id), name: String(p.envRemark).replace(`MP:${installId}:`, ""),
      status: Number(p.envStatus ?? -1), ip: String(p.proxy?.exportIp ?? ""),
    }));
    return res.status(200).json({ ok: true, phones });
  } catch (error) { return sendError(res, error); }
}
