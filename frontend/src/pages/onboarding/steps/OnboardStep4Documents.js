import React, { useState } from "react";
import { saveDocuments } from "../../../services/api";

const OnboardStep4Documents = ({ data, onChange, onboardingId, onNext, onBack }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        onChange({ ...data, fssaiFile: file || null });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!data.fssaiLicenseNumber.trim()) { setError("FSSAI License Number is required"); return; }
        if (!data.panNumber.trim()) { setError("PAN Number is required"); return; }
        setLoading(true);
        setError("");
        try {
            const formData = new FormData();
            formData.append("fssaiLicenseNumber", data.fssaiLicenseNumber);
            formData.append("panNumber", data.panNumber);
            if (data.gstin) formData.append("gstin", data.gstin);
            if (data.fssaiFile) formData.append("fssaiDocument", data.fssaiFile);
            await saveDocuments(onboardingId, formData);
            onNext();
        } catch (err) {
            setError(err.response?.data || "Failed to save documents.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card p-4">
            <h5 className="mb-3">Step 4: Documents</h5>
            {error && <div className="alert alert-danger">{error}</div>}
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label className="form-label">FSSAI License Number *</label>
                    <input className="form-control" value={data.fssaiLicenseNumber}
                        onChange={(e) => onChange({ ...data, fssaiLicenseNumber: e.target.value })} required />
                </div>
                <div className="mb-3">
                    <label className="form-label">FSSAI Document Upload (JPG/PNG/PDF)</label>
                    <input type="file" className="form-control" accept=".jpg,.jpeg,.png,.pdf"
                        onChange={handleFileChange} />
                    {data.fssaiFile && (
                        <small className="text-success mt-1 d-block">Selected: {data.fssaiFile.name}</small>
                    )}
                </div>
                <div className="mb-3">
                    <label className="form-label">PAN Number *</label>
                    <input className="form-control" value={data.panNumber}
                        style={{ textTransform: "uppercase" }}
                        onChange={(e) => onChange({ ...data, panNumber: e.target.value.toUpperCase() })} required />
                </div>
                <div className="mb-3">
                    <label className="form-label">GSTIN <span className="text-muted">(optional)</span></label>
                    <input className="form-control" value={data.gstin}
                        onChange={(e) => onChange({ ...data, gstin: e.target.value })} />
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

export default OnboardStep4Documents;
