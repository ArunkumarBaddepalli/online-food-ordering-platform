import React from "react";
import { Link, useLocation } from "react-router-dom";
import { getCurrentUser, clearSession } from "../services/api";

/**
 * Navigation for the partner portal.
 *
 * Deliberately has no Cart or Orders: a partner account runs a restaurant, it
 * does not shop. An owner who wants to order food signs up as a customer, the
 * same as on any real platform.
 */
const PartnerNavbar = () => {
    const location = useLocation();
    const user = getCurrentUser();

    const handleLogout = () => {
        clearSession();
        window.location.assign("/login");
    };

    const active = (path) =>
        location.pathname.startsWith(path) ? "nav-link active fw-bold" : "nav-link";

    return (
        <nav className="navbar navbar-expand-lg navbar-dark" style={{ backgroundColor: "#1b2838" }}>
            <div className="container">
                <Link className="navbar-brand" to="/partner/dashboard">
                    🍕 Food Delivery <span className="badge bg-warning text-dark ms-2">Partner</span>
                </Link>

                <ul className="navbar-nav ms-auto align-items-lg-center">
                    <li className="nav-item">
                        <Link className={active("/partner/dashboard")} to="/partner/dashboard">My Restaurant</Link>
                    </li>
                    <li className="nav-item">
                        <Link className={active("/partner/onboard")} to="/partner/onboard">Onboarding</Link>
                    </li>

                    {user && (
                        <>
                            <li className="nav-item">
                                <span className="nav-link text-white-50">{user.name || user.email}</span>
                            </li>
                            <li className="nav-item">
                                <button className="btn btn-outline-light btn-sm ms-2" onClick={handleLogout}>
                                    Logout
                                </button>
                            </li>
                        </>
                    )}
                </ul>
            </div>
        </nav>
    );
};

export default PartnerNavbar;
