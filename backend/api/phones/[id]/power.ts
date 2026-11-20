import type { VercelRequest, VercelResponse } from "@vercel/node";
import { api, listAllPhones, method, phoneForInstall, sendError, validInstallId } from "../../../_lib/morelogin.js";

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (!method(req, res, "POST")) return;
  const installId = req.body?.installId;
  const id = String(req.query.id ?? "");
  if (!validInstallId(installId) || !id) return res.status(400).json({ ok: false, error: "Invalid request" });
  try {
    const rows = await listAllPhones();
    if (!phoneForInstall(rows, installId, id)) return res.status(404).json({ ok: false, error: "Phone not found for this installation" });
    const on = req.body?.on === true;
    await api(on ? "/cloudphone/powerOn" : "/cloudphone/powerOff", { id });
    return res.status(202).json({ ok: true, requestedState: on ? "on" : "off" });
  } catch (error) { return sendError(res, error); }
}
