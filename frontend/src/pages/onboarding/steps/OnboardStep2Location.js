import React, { useState, useCallback } from "react";
import { saveLocation } from "../../../services/api";
import AddressMap from "../../../components/AddressMap";

// Whatever this browser is set to, which is right in the ordinary case of an
// owner applying from their own restaurant.
const browserZone = Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";

// The browser's own zone first, then a short list covering the usual cases.
const zoneOptions = Array.from(new Set([
    browserZone,
    "Asia/Kolkata", "Asia/Dubai", "Asia/Singapore", "Asia/Tokyo",
    "Europe/London", "Europe/Berlin", "Europe/Paris",
    "America/New_York", "America/Chicago", "America/Los_Angeles",
    "Australia/Sydney", "UTC",
]));

const OnboardStep2Location = ({ data, onChange, onboardingId, onNext, onBack }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleLocationSelect = useCallback((lat, lng) => {
        onChange((prev) => ({ ...prev, latitude: lat, longitude: lng }));
    }, [onChange]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!data.street.trim() || !data.city.trim()) {
            setError("Street and city are required"); return;
        }
        setLoading(true);
        setError("");
        try {
            await saveLocation(onboardingId, data);
            onNext();
        } catch (err) {
            setError(err.response?.data || "Failed to save location.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card p-4">
            <h5 className="mb-3">Step 2: Location</h5>
            {error && <div className="alert alert-danger">{error}</div>}
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label className="form-label">Street Address *</label>
                    <input className="form-control" value={data.street}
                        onChange={(e) => onChange({ ...data, street: e.target.value })} required />
                </div>
                <div className="row">
                    <div className="col-md-6 mb-3">
                        <label className="form-label">City *</label>
                        <input className="form-control" value={data.city}
                            onChange={(e) => onChange({ ...data, city: e.target.value })} required />
                    </div>
                    <div className="col-md-4 mb-3">
                        <label className="form-label">State</label>
                        <input className="form-control" value={data.state}
                            onChange={(e) => onChange({ ...data, state: e.target.value })} />
                    </div>
                    <div className="col-md-2 mb-3">
                        <label className="form-label">ZIP</label>
                        <input className="form-control" value={data.zipCode}
                            onChange={(e) => onChange({ ...data, zipCode: e.target.value })} />
                    </div>
                </div>
                <div className="mb-3">
                    <label className="form-label">Time zone</label>
                    <select className="form-select"
                        value={data.timeZone || browserZone}
                        onChange={(e) => onChange({ ...data, timeZone: e.target.value })}>
                        {zoneOptions.map((z) => <option key={z} value={z}>{z}</option>)}
                    </select>
                    <div className="form-text">
                        Your opening hours are read on this clock. Change it if the restaurant is
                        somewhere other than where you are now.
                    </div>
                </div>

                <div className="mb-3">
                    <label className="form-label">
                        Delivery Radius: <strong>{data.deliveryRadiusKm} km</strong>
                    </label>
                    <input type="range" className="form-range" min={1} max={50} value={data.deliveryRadiusKm}
                        onChange={(e) => onChange({ ...data, deliveryRadiusKm: Number(e.target.value) })} />
                </div>
                <div className="mb-3">
                    <label className="form-label">Pin your restaurant location on map</label>
                    {data.latitude && data.longitude && (
                        <small className="text-muted d-block mb-1">
                            Pinned: {data.latitude.toFixed(5)}, {data.longitude.toFixed(5)}
                        </small>
                    )}
                    <AddressMap
                        initialLat={data.latitude}
                        initialLng={data.longitude}
                        onLocationSelect={handleLocationSelect}
                    />
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

export default OnboardStep2Location;
