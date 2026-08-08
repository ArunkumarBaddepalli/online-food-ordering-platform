import React, { useState } from "react";
import { saveHours } from "../../../services/api";

const DAY_LABELS = {
    MONDAY: "Mon", TUESDAY: "Tue", WEDNESDAY: "Wed", THURSDAY: "Thu",
    FRIDAY: "Fri", SATURDAY: "Sat", SUNDAY: "Sun",
};

const OnboardStep3Hours = ({ hours, onHoursChange, settings, onSettingsChange, onboardingId, onNext, onBack }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const updateDay = (idx, field, value) => {
        const updated = hours.map((h, i) => i === idx ? { ...h, [field]: value } : h);
        onHoursChange(updated);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError("");
        try {
            await saveHours(onboardingId, {
                hours,
                acceptsScheduledOrders: settings.acceptsScheduledOrders,
                slotDurationMinutes: settings.slotDurationMinutes,
            });
            onNext();
        } catch (err) {
            setError(err.response?.data || "Failed to save hours.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card p-4">
            <h5 className="mb-3">Step 3: Operating Hours</h5>
            {error && <div className="alert alert-danger">{error}</div>}
            <form onSubmit={handleSubmit}>
                <div className="table-responsive mb-3">
                    <table className="table table-sm align-middle">
                        <thead>
                            <tr>
                                <th>Day</th>
                                <th>Open</th>
                                <th>Opening Time</th>
                                <th>Closing Time</th>
                            </tr>
                        </thead>
                        <tbody>
                            {hours.map((h, idx) => (
                                <tr key={h.dayOfWeek}>
                                    <td><strong>{DAY_LABELS[h.dayOfWeek]}</strong></td>
                                    <td>
                                        <div className="form-check form-switch mb-0">
                                            <input className="form-check-input" type="checkbox"
                                                checked={h.isOpen}
                                                onChange={(e) => updateDay(idx, "isOpen", e.target.checked)} />
                                        </div>
                                    </td>
                                    <td>
                                        <input type="time" className="form-control form-control-sm"
                                            value={h.openTime} disabled={!h.isOpen}
                                            onChange={(e) => updateDay(idx, "openTime", e.target.value)} />
                                    </td>
                                    <td>
                                        <input type="time" className="form-control form-control-sm"
                                            value={h.closeTime} disabled={!h.isOpen}
                                            onChange={(e) => updateDay(idx, "closeTime", e.target.value)} />
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                <div className="row mb-3">
                    <div className="col-md-6">
                        <div className="form-check">
                            <input className="form-check-input" type="checkbox" id="scheduledOrders"
                                checked={settings.acceptsScheduledOrders}
                                onChange={(e) => onSettingsChange({ ...settings, acceptsScheduledOrders: e.target.checked })} />
                            <label className="form-check-label" htmlFor="scheduledOrders">
                                Accept scheduled / pre-orders
                            </label>
                        </div>
                    </div>
                    {settings.acceptsScheduledOrders && (
                        <div className="col-md-6">
                            <label className="form-label mb-1">Slot Duration</label>
                            <select className="form-select form-select-sm"
                                value={settings.slotDurationMinutes}
                                onChange={(e) => onSettingsChange({ ...settings, slotDurationMinutes: Number(e.target.value) })}>
                                {[15, 20, 30, 45, 60].map((m) => (
                                    <option key={m} value={m}>{m} minutes</option>
                                ))}
                            </select>
                        </div>
                    )}
                </div>

                <div className="d-flex justify-content-between">
                    <button type="button" className="btn btn-outline-secondary" onClick={onBack}>← Back</button>
                    <button type="submit" className="btn btn-primary" disabled={loading}>
                        {loading ? "Saving..." : "Next →"}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default OnboardStep3Hours;
