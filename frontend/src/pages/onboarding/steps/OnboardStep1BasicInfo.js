import React, { useState } from "react";
import { saveBasicInfo } from "../../../services/api";

const CUISINES = [
    "Indian", "Chinese", "Italian", "Mexican", "Continental",
    "South Indian", "North Indian", "Fast Food", "Desserts", "Beverages",
];

const OnboardStep1BasicInfo = ({ data, onChange, onboardingId, onNext }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const toggleCuisine = (cuisine) => {
        const updated = data.cuisineTypes.includes(cuisine)
            ? data.cuisineTypes.filter((c) => c !== cuisine)
            : [...data.cuisineTypes, cuisine];
        onChange({ ...data, cuisineTypes: updated });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!data.restaurantName.trim()) { setError("Restaurant name is required"); return; }
        if (!data.phone.trim()) { setError("Phone number is required"); return; }
        setLoading(true);
        setError("");
        try {
            await saveBasicInfo(onboardingId, {
                ...data,
                cuisineTypes: data.cuisineTypes.join(","),
            });
            onNext();
        } catch (err) {
            setError(err.response?.data || "Failed to save. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card p-4">
            <h5 className="mb-3">Step 1: Basic Information</h5>
            {error && <div className="alert alert-danger">{error}</div>}
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label className="form-label">Restaurant Name *</label>
                    <input className="form-control" value={data.restaurantName}
                        onChange={(e) => onChange({ ...data, restaurantName: e.target.value })} required />
                </div>
                <div className="mb-3">
                    <label className="form-label">Description</label>
                    <textarea className="form-control" rows={3} value={data.description}
                        onChange={(e) => onChange({ ...data, description: e.target.value })} />
                </div>
                <div className="mb-3">
                    <label className="form-label">Cuisine Types</label>
                    <div className="d-flex flex-wrap gap-2">
                        {CUISINES.map((c) => (
                            <button key={c} type="button"
                                className={`btn btn-sm ${data.cuisineTypes.includes(c) ? "btn-primary" : "btn-outline-secondary"}`}
                                onClick={() => toggleCuisine(c)}>
                                {c}
                            </button>
                        ))}
                    </div>
                </div>
                <div className="mb-3">
                    <label className="form-label">Restaurant Type</label>
                    <select className="form-select" value={data.restaurantType}
                        onChange={(e) => onChange({ ...data, restaurantType: e.target.value })}>
                        <option value="VEG">Pure Veg</option>
                        <option value="NON_VEG">Non-Veg</option>
                        <option value="BOTH">Both Veg & Non-Veg</option>
                    </select>
                </div>
                <div className="row">
                    <div className="col-md-6 mb-3">
                        <label className="form-label">Phone *</label>
                        <input type="tel" className="form-control" value={data.phone}
                            onChange={(e) => onChange({ ...data, phone: e.target.value })} required />
                    </div>
                    <div className="col-md-6 mb-3">
                        <label className="form-label">Email</label>
                        <input type="email" className="form-control" value={data.email}
                            onChange={(e) => onChange({ ...data, email: e.target.value })} />
                    </div>
                </div>
                <div className="d-flex justify-content-end">
                    <button type="submit" className="btn btn-primary" disabled={loading}>
                        {loading ? "Saving..." : "Next →"}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default OnboardStep1BasicInfo;
