import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getOnboardingStatus, API_BASE } from "../services/api";

const RestaurantDashboard = () => {
    const navigate = useNavigate();
    const user = JSON.parse(localStorage.getItem("user"));
    const [onboarding, setOnboarding] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user) { navigate("/login"); return; }
        getOnboardingStatus(user.id)
            .then((res) => {
                const ob = res.data;
                if (ob.status !== "APPROVED") {
                    navigate("/restaurant/onboard/status");
                    return;
                }
                setOnboarding(ob);
            })
            .catch(() => navigate("/restaurant/onboard"))
            .finally(() => setLoading(false));
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    if (loading) return <div className="text-center mt-5"><div className="spinner-border" /></div>;
    if (!onboarding) return null;

    return (
        <div>
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h3>Restaurant Dashboard</h3>
                <span className="badge bg-success fs-6">Approved</span>
            </div>

            <div className="row g-3 mb-4">
                <div className="col-md-6">
                    <div className="card p-3">
                        <h6 className="text-muted mb-2">Restaurant</h6>
                        <h5>{onboarding.restaurantName}</h5>
                        {onboarding.restaurantType && <span className="badge bg-light text-dark border">{onboarding.restaurantType}</span>}
                        {onboarding.cuisineTypes && (
                            <p className="text-muted small mt-2 mb-0">
                                {onboarding.cuisineTypes.replace(/,/g, ", ")}
                            </p>
                        )}
                    </div>
                </div>
                <div className="col-md-6">
                    <div className="card p-3">
                        <h6 className="text-muted mb-2">Contact</h6>
                        <div>{onboarding.phone}</div>
                        <div className="text-muted small">{onboarding.email}</div>
                    </div>
                </div>
                <div className="col-md-6">
                    <div className="card p-3">
                        <h6 className="text-muted mb-2">Location</h6>
                        <div>{[onboarding.street, onboarding.city, onboarding.state].filter(Boolean).join(", ")}</div>
                        <small className="text-muted">Delivery radius: {onboarding.deliveryRadiusKm} km</small>
                    </div>
                </div>
                <div className="col-md-6">
                    <div className="card p-3">
                        <h6 className="text-muted mb-2">Restaurant ID</h6>
                        <h5 className="font-monospace">#{onboarding.createdRestaurantId}</h5>
                        <small className="text-muted">Use this ID to manage menu items via API</small>
                    </div>
                </div>
            </div>

            <div className="card p-3">
                <h6 className="mb-3">Quick Actions</h6>
                <div className="d-flex gap-2 flex-wrap">
                    <Link to={`/restaurant/${onboarding.createdRestaurantId}`} className="btn btn-outline-primary btn-sm">
                        View Menu Page
                    </Link>
                    <a href={`${API_BASE}/swagger-ui.html`} target="_blank" rel="noreferrer"
                        className="btn btn-outline-secondary btn-sm">
                        Manage via API (Swagger)
                    </a>
                </div>
            </div>
        </div>
    );
};

export default RestaurantDashboard;
