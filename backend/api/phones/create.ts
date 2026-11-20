import type { VercelRequest, VercelResponse } from "@vercel/node";
import { api, listAllPhones, method, sendError, validInstallId } from "../_lib/morelogin.js";

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (!method(req, res, "POST")) return;
  const installId = req.body?.installId;
  if (!validInstallId(installId)) return res.status(400).json({ ok: false, error: "Invalid installation identifier" });
  try {
    const current = (await listAllPhones()).filter((p) => String(p.envRemark ?? "").startsWith(`MP:${installId}:`));
    if (current.length >= 10) return res.status(429).json({ ok: false, error: "This installation reached its 10-phone limit." });
    const used = new Set(current.map((p) => String(p.envRemark ?? "").split(":").slice(2).join(":")));
    let n = 1; while (used.has(`Android ${n}`)) n++;
    const name = `Android ${n}`;
    const created = await api("/cloudphone/create", {
      skuId: "10002", quantity: 1, envRemark: `MP:${installId}:${name}`,
      automaticGeo: true, automaticLanguage: true, automaticLocation: true,
    });
    const ids = created?.data;
    const id = Array.isArray(ids) ? String(ids[0] ?? "") : "";
    if (!id) throw new Error("Provider did not return the new phone ID");
    return res.status(202).json({ ok: true, phone: { id, name, status: 0 }, message: "Creation accepted; provisioning is asynchronous." });
  } catch (error) { return sendError(res, error); }
}
