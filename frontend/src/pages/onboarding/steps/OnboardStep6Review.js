import React, { useState } from "react";
import { submitOnboarding } from "../../../services/api";

const DAY_LABELS = { MONDAY:"Mon",TUESDAY:"Tue",WEDNESDAY:"Wed",THURSDAY:"Thu",FRIDAY:"Fri",SATURDAY:"Sat",SUNDAY:"Sun" };

const OnboardStep6Review = ({ basicInfo, location, hours, hoursSettings, documents, bankDetails, onboardingId, onEditStep, onSubmitSuccess }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async () => {
        setLoading(true);
        setError("");
        try {
            await submitOnboarding(onboardingId);
            onSubmitSuccess();
        } catch (err) {
            setError(err.response?.data || "Submission failed. Ensure all required fields are filled.");
        } finally {
            setLoading(false);
        }
    };

    const Section = ({ title, step, children }) => (
        <div className="border rounded p-3 mb-3">
            <div className="d-flex justify-content-between align-items-center mb-2">
                <strong>{title}</strong>
                <button className="btn btn-sm btn-outline-primary" onClick={() => onEditStep(step)}>Edit</button>
            </div>
            {children}
        </div>
    );

    return (
        <div className="card p-4">
            <h5 className="mb-3">Step 6: Review & Submit</h5>
            {error && <div className="alert alert-danger">{error}</div>}

            <Section title="Basic Information" step={1}>
                <div className="row">
                    <div className="col-6"><small className="text-muted">Name</small><div>{basicInfo.restaurantName || "—"}</div></div>
                    <div className="col-6"><small className="text-muted">Type</small><div>{basicInfo.restaurantType || "—"}</div></div>
                    <div className="col-6 mt-2"><small className="text-muted">Phone</small><div>{basicInfo.phone || "—"}</div></div>
                    <div className="col-6 mt-2"><small className="text-muted">Email</small><div>{basicInfo.email || "—"}</div></div>
                    <div className="col-12 mt-2"><small className="text-muted">Cuisines</small><div>{basicInfo.cuisineTypes?.join(", ") || "—"}</div></div>
                </div>
            </Section>

            <Section title="Location" step={2}>
                <div>
                    {[location.street, location.city, location.state, location.zipCode].filter(Boolean).join(", ") || "—"}
                </div>
                <div className="mt-1">
                    <small className="text-muted">Delivery radius: {location.deliveryRadiusKm} km</small>
                    {location.latitude && <span className="ms-2 text-muted small">📍 {location.latitude?.toFixed(4)}, {location.longitude?.toFixed(4)}</span>}
                </div>
            </Section>

            <Section title="Operating Hours" step={3}>
                <div className="row">
                    {hours.map((h) => (
                        <div key={h.dayOfWeek} className="col-6 col-md-4 mb-1">
                            <small><strong>{DAY_LABELS[h.dayOfWeek]}:</strong> {h.isOpen ? `${h.openTime} – ${h.closeTime}` : "Closed"}</small>
                        </div>
                    ))}
                </div>
                <div className="mt-2">
                    <small className="text-muted">
                        Scheduled orders: {hoursSettings.acceptsScheduledOrders ? `Yes (${hoursSettings.slotDurationMinutes} min slots)` : "No"}
                    </small>
                </div>
            </Section>

            <Section title="Documents" step={4}>
                <div className="row">
                    <div className="col-6"><small className="text-muted">FSSAI</small><div>{documents.fssaiLicenseNumber || "—"}</div></div>
                    <div className="col-6"><small className="text-muted">PAN</small><div>{documents.panNumber || "—"}</div></div>
                    {documents.gstin && <div className="col-6 mt-2"><small className="text-muted">GSTIN</small><div>{documents.gstin}</div></div>}
                    <div className="col-12 mt-2">
                        <small className="text-muted">FSSAI Document: </small>
                        <small>{documents.fssaiFile ? documents.fssaiFile.name : "Not uploaded"}</small>
                    </div>
                </div>
            </Section>

            <Section title="Bank Details" step={5}>
                <div className="row">
                    <div className="col-6"><small className="text-muted">Account Holder</small><div>{bankDetails.bankAccountHolderName || "—"}</div></div>
                    <div className="col-6"><small className="text-muted">Bank</small><div>{bankDetails.bankName || "—"}</div></div>
                    <div className="col-6 mt-2"><small className="text-muted">IFSC</small><div>{bankDetails.bankIfscCode || "—"}</div></div>
                    <div className="col-6 mt-2"><small className="text-muted">Account No.</small><div>••••{bankDetails.bankAccountNumber?.slice(-4) || "—"}</div></div>
                </div>
            </Section>

            <div className="alert alert-info">
                By submitting, you agree to our partner terms and confirm all information is accurate.
            </div>
            <button className="btn btn-success w-100 py-2" onClick={handleSubmit} disabled={loading}>
                {loading ? "Submitting..." : "Submit Application"}
            </button>
        </div>
    );
};

export default OnboardStep6Review;
