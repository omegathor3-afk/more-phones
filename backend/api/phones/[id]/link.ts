import type { VercelRequest, VercelResponse } from "@vercel/node";
import { api, listAllPhones, method, phoneForInstall, sendError, validInstallId } from "../../../_lib/morelogin.js";

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (!method(req, res, "POST")) return;
  const installId = req.body?.installId;
  const id = String(req.query.id ?? "");
  if (!validInstallId(installId) || !id) return res.status(400).json({ ok: false, error: "Invalid request" });
  try {
    const rows = await listAllPhones();
    const phone = phoneForInstall(rows, installId, id);
    if (!phone) return res.status(404).json({ ok: false, error: "Phone not found for this installation" });
    if (Number(phone.envStatus) !== 4) return res.status(409).json({ ok: false, error: "Phone is not running yet" });
    const result = await api("/cloudphone/link", { id, lang: "en-US" });
    const url = result?.data?.url;
    if (!url) throw new Error("Provider did not return a control URL");
    return res.status(200).json({ ok: true, url });
  } catch (error) { return sendError(res, error); }
}
