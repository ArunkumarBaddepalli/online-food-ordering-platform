import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getOnboardingStatus, getRestaurantOwnedBy, API_BASE } from "../services/api";
import OwnerOrdersPanel from "../components/OwnerOrdersPanel";

const RestaurantDashboard = () => {
    const navigate = useNavigate();
    const user = JSON.parse(localStorage.getItem("user"));
    const [onboarding, setOnboarding] = useState(null);
    const [restaurant, setRestaurant] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user) { navigate("/login"); return; }

        // Owning a restaurant is what decides whether there is a dashboard to
        // show. The onboarding application only matters for owners who do not
        // run one yet.
        getRestaurantOwnedBy(user.id)
            .then((res) => setRestaurant(res.data))
            .catch(() => setRestaurant(null))
            .then(() => getOnboardingStatus(user.id))
            .then((res) => setOnboarding(res.data))
            .catch(() => setOnboarding(null))
            .finally(() => setLoading(false));
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    if (loading) return <div className="text-center mt-5"><div className="spinner-border" /></div>;

    // No restaurant yet: send them to whichever onboarding step applies.
    if (!restaurant) {
        return (
            <div className="text-center mt-5">
                <h4>No restaurant yet</h4>
                <p className="text-muted">
                    {onboarding
                        ? "Your application is still being processed."
                        : "You have not applied to list a restaurant yet."}
                </p>
                <Link className="btn btn-primary" to={onboarding ? "/restaurant/onboard/status" : "/restaurant/onboard"}>
                    {onboarding ? "View application status" : "Start your application"}
                </Link>
            </div>
        );
    }

    return (
        <div>
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h3>Restaurant Dashboard</h3>
                <span className={`badge fs-6 ${restaurant.isOpen ? "bg-success" : "bg-secondary"}`}>
                    {restaurant.isOpen ? "Open" : "Closed"}
                </span>
            </div>

            <div className="row g-3 mb-4">
                <div className="col-md-6">
                    <div className="card p-3">
                        <h6 className="text-muted mb-2">Restaurant</h6>
                        <h5>{restaurant.name}</h5>
                        {restaurant.description && (
                            <p className="text-muted small mt-2 mb-0">{restaurant.description}</p>
                        )}
                    </div>
                </div>
                <div className="col-md-6">
                    <div className="card p-3">
                        <h6 className="text-muted mb-2">Contact</h6>
                        <div>{restaurant.phone || "—"}</div>
                        <div className="text-muted small">{restaurant.email || "—"}</div>
                    </div>
                </div>
                <div className="col-md-6">
                    <div className="card p-3">
                        <h6 className="text-muted mb-2">Location</h6>
                        <div>{restaurant.address || "—"}</div>
                        <small className="text-muted">Delivery radius: {restaurant.deliveryRadiusKm ?? "—"} km</small>
                    </div>
                </div>
                <div className="col-md-6">
                    <div className="card p-3">
                        <h6 className="text-muted mb-2">Hours</h6>
                        <div>{restaurant.openingTime || "—"} – {restaurant.closingTime || "—"}</div>
                        <small className="text-muted">Restaurant ID #{restaurant.id}</small>
                    </div>
                </div>
            </div>

            <div className="card p-3">
                <h6 className="mb-3">Quick Actions</h6>
                <div className="d-flex gap-2 flex-wrap">
                    <Link to={`/restaurant/${restaurant.id}`} className="btn btn-outline-primary btn-sm">
                        View Menu Page
                    </Link>
                    <a href={`${API_BASE}/swagger-ui.html`} target="_blank" rel="noreferrer"
                        className="btn btn-outline-secondary btn-sm">
                        Manage via API (Swagger)
                    </a>
                </div>
            </div>

            <OwnerOrdersPanel restaurantId={restaurant.id} />
        </div>
    );
};

export default RestaurantDashboard;
