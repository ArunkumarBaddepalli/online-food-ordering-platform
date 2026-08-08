import React, { useState } from "react";
import { saveBankDetails } from "../../../services/api";

const OnboardStep5BankDetails = ({ data, onChange, onboardingId, onNext, onBack }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!data.bankAccountNumber.trim()) { setError("Account number is required"); return; }
        if (!data.bankIfscCode.trim() || data.bankIfscCode.length !== 11) {
            setError("IFSC code must be exactly 11 characters"); return;
        }
        setLoading(true);
        setError("");
        try {
            await saveBankDetails(onboardingId, data);
            onNext();
        } catch (err) {
            setError(err.response?.data || "Failed to save bank details.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card p-4">
            <h5 className="mb-3">Step 5: Bank Details</h5>
            <p className="text-muted small">Payments will be settled to this account after order completion.</p>
            {error && <div className="alert alert-danger">{error}</div>}
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label className="form-label">Account Holder Name *</label>
                    <input className="form-control" value={data.bankAccountHolderName}
                        onChange={(e) => onChange({ ...data, bankAccountHolderName: e.target.value })} required />
                </div>
                <div className="mb-3">
                    <label className="form-label">Account Number *</label>
                    <input className="form-control" type="password" value={data.bankAccountNumber}
                        onChange={(e) => onChange({ ...data, bankAccountNumber: e.target.value })} required />
                </div>
                <div className="mb-3">
                    <label className="form-label">IFSC Code * (11 characters)</label>
                    <input className="form-control" maxLength={11} value={data.bankIfscCode}
                        style={{ textTransform: "uppercase" }}
                        onChange={(e) => onChange({ ...data, bankIfscCode: e.target.value.toUpperCase() })} required />
                    <small className="text-muted">{data.bankIfscCode.length}/11</small>
                </div>
                <div className="mb-3">
                    <label className="form-label">Bank Name *</label>
                    <input className="form-control" value={data.bankName}
                        onChange={(e) => onChange({ ...data, bankName: e.target.value })} required />
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

export default OnboardStep5BankDetails;
