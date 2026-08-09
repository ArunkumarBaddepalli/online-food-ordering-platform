import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { getCurrentUser, getToken } from "../services/api";

/**
 * Keeps a page from rendering for the wrong audience.
 *
 * This is a convenience, not the protection itself — the server checks every
 * request independently. Hiding a page here only saves the user from seeing
 * something that would fail anyway.
 */
const RequireAuth = ({ roles, children }) => {
    const location = useLocation();
    const user = getCurrentUser();
    const signedIn = Boolean(getToken() && user);

    if (!signedIn) {
        return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />;
    }

    if (roles && !roles.includes(user.role)) {
        return (
            <div className="text-center mt-5">
                <h4>Not available on this account</h4>
                <p className="text-muted">
                    {roles.includes("RESTAURANT_OWNER")
                        ? "This is the partner area. Sign in with a restaurant partner account to use it."
                        : "Your account does not have access to this page."}
                </p>
                <a className="btn btn-outline-secondary" href="/">Back to home</a>
            </div>
        );
    }

    return children;
};

export default RequireAuth;
