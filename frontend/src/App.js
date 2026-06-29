import { useState, useEffect } from "react";
import axios from "axios";

const API = "/api/payments";

const CURRENCIES = ["USD", "EUR", "GBP", "INR", "JPY"];

function RiskBar({ score }) {
  const color = score <= 30 ? "#22c55e" : score <= 60 ? "#f59e0b" : "#ef4444";
  return (
    <div style={{ marginTop: 4 }}>
      <div style={{ height: 6, borderRadius: 999, background: "#1e293b", overflow: "hidden" }}>
        <div style={{ width: `${score}%`, height: "100%", background: color, borderRadius: 999, transition: "width 0.6s ease" }} />
      </div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: "#64748b", marginTop: 3 }}>
        <span>Low</span>
        <span style={{ color, fontWeight: 600 }}>{score}/100</span>
        <span>High</span>
      </div>
    </div>
  );
}

function Badge({ label, type }) {
  const styles = {
    STANDARD: { bg: "#0f172a", color: "#38bdf8", border: "#1e40af" },
    HIGH_VALUE: { bg: "#1c1003", color: "#fbbf24", border: "#92400e" },
    SUSPICIOUS: { bg: "#1a0505", color: "#f87171", border: "#7f1d1d" },
    INTERNAL: { bg: "#071a0e", color: "#4ade80", border: "#14532d" },
    VALIDATED: { bg: "#071a0e", color: "#4ade80", border: "#14532d" },
    REJECTED: { bg: "#1a0505", color: "#f87171", border: "#7f1d1d" },
    anomaly: { bg: "#1a0505", color: "#f87171", border: "#7f1d1d" },
    safe: { bg: "#071a0e", color: "#4ade80", border: "#14532d" },
  };
  const s = styles[type] || styles.STANDARD;
  return (
    <span style={{ display: "inline-block", padding: "2px 10px", borderRadius: 4, fontSize: 11, fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", background: s.bg, color: s.color, border: `1px solid ${s.border}` }}>
      {label}
    </span>
  );
}

function AIInsightsPanel({ payment }) {
  if (!payment.aiSummary && !payment.aiCategory) return null;
  return (
    <div style={{ marginTop: 20, background: "#0b1120", border: "1px solid #1e293b", borderRadius: 10, padding: "18px 20px" }}>
      <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: "0.12em", color: "#38bdf8", textTransform: "uppercase", marginBottom: 14 }}>AI Analysis</div>
      {payment.aiSummary && <p style={{ color: "#cbd5e1", fontSize: 13, lineHeight: 1.6, margin: "0 0 16px" }}>{payment.aiSummary}</p>}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
        {payment.aiCategory && (
          <div>
            <div style={{ fontSize: 11, color: "#475569", marginBottom: 6, textTransform: "uppercase", letterSpacing: "0.08em" }}>Category</div>
            <Badge label={payment.aiCategory} type={payment.aiCategory} />
          </div>
        )}
        {payment.aiAnomalyFlag !== null && payment.aiAnomalyFlag !== undefined && (
          <div>
            <div style={{ fontSize: 11, color: "#475569", marginBottom: 6, textTransform: "uppercase", letterSpacing: "0.08em" }}>Anomaly</div>
            <Badge label={payment.aiAnomalyFlag ? "Flagged" : "Clean"} type={payment.aiAnomalyFlag ? "anomaly" : "safe"} />
          </div>
        )}
      </div>
      {payment.aiRiskScore !== null && payment.aiRiskScore !== undefined && (
        <div style={{ marginTop: 16 }}>
          <div style={{ fontSize: 11, color: "#475569", marginBottom: 6, textTransform: "uppercase", letterSpacing: "0.08em" }}>Risk Score</div>
          <RiskBar score={payment.aiRiskScore} />
        </div>
      )}
    </div>
  );
}

function PaymentForm({ onSubmit, loading }) {
  const [form, setForm] = useState({ senderAccount: "", receiverAccount: "", amount: "", currency: "USD" });
  const [error, setError] = useState("");
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");
    if (!form.senderAccount || !form.receiverAccount || !form.amount) { setError("All fields are required."); return; }
    if (isNaN(form.amount) || Number(form.amount) <= 0) { setError("Amount must be a positive number."); return; }
    onSubmit({ ...form, amount: parseFloat(form.amount) });
  };
  const inputStyle = { width: "100%", background: "#0b1120", border: "1px solid #1e293b", borderRadius: 7, color: "#f1f5f9", fontSize: 14, padding: "10px 12px", outline: "none", boxSizing: "border-box" };
  const labelStyle = { display: "block", fontSize: 11, fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "#475569", marginBottom: 6 };
  return (
    <form onSubmit={handleSubmit}>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14, marginBottom: 14 }}>
        <div><label style={labelStyle}>Sender Account</label><input style={inputStyle} placeholder="e.g. ACC001" value={form.senderAccount} onChange={set("senderAccount")} /></div>
        <div><label style={labelStyle}>Receiver Account</label><input style={inputStyle} placeholder="e.g. ACC002" value={form.receiverAccount} onChange={set("receiverAccount")} /></div>
        <div><label style={labelStyle}>Amount</label><input style={inputStyle} placeholder="0.00" type="number" min="0.01" step="0.01" value={form.amount} onChange={set("amount")} /></div>
        <div><label style={labelStyle}>Currency</label><select style={{ ...inputStyle, cursor: "pointer" }} value={form.currency} onChange={set("currency")}>{CURRENCIES.map((c) => <option key={c}>{c}</option>)}</select></div>
      </div>
      {error && <div style={{ color: "#f87171", fontSize: 12, marginBottom: 12 }}>{error}</div>}
      <button type="submit" disabled={loading} style={{ width: "100%", padding: "12px", background: loading ? "#1e293b" : "#2563eb", color: loading ? "#475569" : "#fff", border: "none", borderRadius: 7, fontSize: 14, fontWeight: 600, cursor: loading ? "not-allowed" : "pointer" }}>
        {loading ? "Processing…" : "Submit Payment"}
      </button>
    </form>
  );
}

function PaymentRow({ p, onClick, active }) {
  return (
    <tr onClick={() => onClick(p)} style={{ cursor: "pointer", background: active ? "#0f172a" : "transparent", borderBottom: "1px solid #1e293b" }}>
      <td style={{ padding: "10px 14px", fontSize: 12, color: "#64748b", fontFamily: "monospace" }}>{p.id.slice(0, 8)}…</td>
      <td style={{ padding: "10px 14px", fontSize: 13, color: "#cbd5e1" }}>{p.senderAccount} → {p.receiverAccount}</td>
      <td style={{ padding: "10px 14px", fontSize: 13, color: "#f1f5f9", fontWeight: 600 }}>{p.currency} {Number(p.amount).toFixed(2)}</td>
      <td style={{ padding: "10px 14px" }}><Badge label={p.status} type={p.status} /></td>
      <td style={{ padding: "10px 14px" }}>{p.aiCategory ? <Badge label={p.aiCategory} type={p.aiCategory} /> : <span style={{ color: "#334155", fontSize: 12 }}>—</span>}</td>
      <td style={{ padding: "10px 14px", fontSize: 13, color: "#64748b" }}>{p.aiRiskScore ?? "—"}</td>
    </tr>
  );
}

export default function App() {
  const [payments, setPayments] = useState([]);
  const [latest, setLatest] = useState(null);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState(null);
  const [fetchError, setFetchError] = useState("");

  const fetchPayments = async () => {
    try {
      const res = await axios.get(API);
      setPayments(res.data.slice().reverse());
      setFetchError("");
    } catch {
      setFetchError("Could not reach the payment service. Is it running on port 8080?");
    }
  };

  useEffect(() => { fetchPayments(); }, []);

  const handleSubmit = async (data) => {
    setLoading(true);
    setLatest(null);
    try {
      const res = await axios.post(API, data);
      setLatest(res.data);
      setSelected(res.data);
      fetchPayments();
    } catch (err) {
      setLatest({ error: err.response?.data?.message || "Submission failed." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: "#060d1a", color: "#f1f5f9", fontFamily: "'Inter', system-ui, sans-serif" }}>
      <div style={{ borderBottom: "1px solid #1e293b", padding: "18px 40px", display: "flex", alignItems: "center", gap: 12 }}>
        <div style={{ width: 8, height: 8, borderRadius: "50%", background: "#2563eb", boxShadow: "0 0 8px #2563eb" }} />
        <span style={{ fontSize: 15, fontWeight: 700, letterSpacing: "0.04em", color: "#f1f5f9" }}>Payment Flow</span>
        <span style={{ fontSize: 13, color: "#334155", marginLeft: 4 }}>/ Simulator</span>
      </div>
      <div style={{ maxWidth: 1100, margin: "0 auto", padding: "36px 40px", display: "grid", gridTemplateColumns: "380px 1fr", gap: 32, alignItems: "start" }}>
        <div>
          <div style={{ background: "#0b1120", border: "1px solid #1e293b", borderRadius: 12, padding: 24 }}>
            <h2 style={{ margin: "0 0 20px", fontSize: 15, fontWeight: 700, color: "#f1f5f9" }}>New Payment</h2>
            <PaymentForm onSubmit={handleSubmit} loading={loading} />
          </div>
          {latest && !latest.error && (
            <div style={{ marginTop: 16, background: "#0b1120", border: "1px solid #1e293b", borderRadius: 12, padding: 24 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                <span style={{ fontSize: 13, color: "#94a3b8" }}>Payment created</span>
                <Badge label={latest.status} type={latest.status} />
              </div>
              <div style={{ fontFamily: "monospace", fontSize: 11, color: "#334155", marginBottom: 12 }}>{latest.id}</div>
              <AIInsightsPanel payment={latest} />
            </div>
          )}
          {latest?.error && (
            <div style={{ marginTop: 16, background: "#1a0505", border: "1px solid #7f1d1d", borderRadius: 10, padding: 16, color: "#f87171", fontSize: 13 }}>{latest.error}</div>
          )}
        </div>
        <div>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
            <h2 style={{ margin: 0, fontSize: 15, fontWeight: 700, color: "#f1f5f9" }}>Payment History</h2>
            <button onClick={fetchPayments} style={{ background: "none", border: "1px solid #1e293b", color: "#64748b", borderRadius: 6, padding: "5px 12px", fontSize: 12, cursor: "pointer" }}>Refresh</button>
          </div>
          {fetchError && <div style={{ color: "#f87171", fontSize: 13, padding: 16, background: "#1a0505", borderRadius: 8, border: "1px solid #7f1d1d", marginBottom: 16 }}>{fetchError}</div>}
          <div style={{ background: "#0b1120", border: "1px solid #1e293b", borderRadius: 12, overflow: "hidden" }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid #1e293b" }}>
                  {["ID", "Route", "Amount", "Status", "Category", "Risk"].map((h) => (
                    <th key={h} style={{ padding: "10px 14px", textAlign: "left", fontSize: 11, fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "#334155" }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {payments.length === 0 ? (
                  <tr><td colSpan={6} style={{ padding: 32, textAlign: "center", color: "#334155", fontSize: 13 }}>No payments yet. Submit one to get started.</td></tr>
                ) : (
                  payments.map((p) => <PaymentRow key={p.id} p={p} onClick={setSelected} active={selected?.id === p.id} />)
                )}
              </tbody>
            </table>
          </div>
          {selected && (
            <div style={{ marginTop: 16, background: "#0b1120", border: "1px solid #1e293b", borderRadius: 12, padding: 24 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                <span style={{ fontSize: 13, fontWeight: 600, color: "#94a3b8" }}>Payment Detail</span>
                <button onClick={() => setSelected(null)} style={{ background: "none", border: "none", color: "#475569", cursor: "pointer", fontSize: 18, lineHeight: 1 }}>×</button>
              </div>
              <div style={{ fontFamily: "monospace", fontSize: 11, color: "#334155", marginBottom: 12 }}>{selected.id}</div>
              <AIInsightsPanel payment={selected} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
