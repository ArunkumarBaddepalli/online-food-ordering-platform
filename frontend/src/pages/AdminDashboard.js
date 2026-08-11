import React, { useCallback, useEffect, useState } from "react";
import { toast } from "react-toastify";
import {
    adminListOnboarding,
    adminApprove,
    adminReject,
    adminRequestDocuments,
} from "../services/api";

const STATUS_STYLES = {
    DRAFT: "bg-secondary",
    SUBMITTED: "bg-warning text-dark",
    PENDING_REVIEW: "bg-warning text-dark",
    DOCUMENTS_REQUIRED: "bg-info text-dark",
    APPROVED: "bg-success",
    REJECTED: "bg-danger",
};

// Only an application that has actually been submitted is ours to decide on.
const AWAITING_DECISION = ["PENDING_REVIEW", "SUBMITTED", "DOCUMENTS_REQUIRED"];

const AdminDashboard = () => {
    const [applications, setApplications] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [busyId, setBusyId] = useState(null);

    const load = useCallback(async () => {
        try {
            const res = await adminListOnboarding();
            setApplications(res.data || []);
            setError(null);
        } catch (e) {
            setError(e.response?.status === 403
                ? "This account is not an administrator. If you signed in as someone else in another tab, sign in again here."
                : "Could not load applications.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    const act = async (id, action, successMessage) => {
        setBusyId(id);
        try {
            await action();
            toast.success(successMessage);
            await load();
        } catch (e) {
            toast.error(e.response?.data || "That did not work. Please try again.");
        } finally {
            setBusyId(null);
        }
    };

    const approve = (app) => {
        if (!window.confirm(`Approve "${app.restaurantName}"? This creates their restaurant and puts it live.`)) return;
        act(app.onboardingId, () => adminApprove(app.onboardingId), "Application approved");
    };

    const reject = (app) => {
        const reason = window.prompt("Why is this being rejected? The applicant will see this.");
        if (!reason) return;
        act(app.onboardingId, () => adminReject(app.onboardingId, reason), "Application rejected");
    };

    const requestDocuments = (app) => {
        const reason = window.prompt("What do they need to provide?");
        if (!reason) return;
        act(app.onboardingId, () => adminRequestDocuments(app.onboardingId, reason),
            "Asked the applicant for more documents");
    };

    if (loading) return <div className="text-center mt-5"><div className="spinner-border" /></div>;
    if (error) return <div className="alert alert-danger">{error}</div>;

    const waiting = applications.filter((a) => AWAITING_DECISION.includes(a.status));
    const decided = applications.filter((a) => !AWAITING_DECISION.includes(a.status));

    const renderApplication = (app, actionable) => {
        const busy = busyId === app.onboardingId;
        const address = [app.street, app.city, app.state, app.zipCode].filter(Boolean).join(", ");

        return (
            <div className="card mb-3" key={app.onboardingId}>
                <div className="card-body">
                    <div className="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            <h6 className="mb-1">
                                {app.restaurantName || <span className="text-muted">Unnamed application</span>}
                                <span className={`badge ms-2 ${STATUS_STYLES[app.status] || "bg-secondary"}`}>
                                    {app.status}
                                </span>
                            </h6>
                            <div className="text-muted small">
                                Application #{app.onboardingId}
                                {app.submittedAt && ` · submitted ${new Date(app.submittedAt).toLocaleDateString()}`}
                            </div>
                        </div>
                        {app.createdRestaurantId && (
                            <span className="text-muted small">Restaurant #{app.createdRestaurantId}</span>
                        )}
                    </div>

                    <div className="row small text-muted g-2 mb-2">
                        <div className="col-md-6">📍 {address || "No address given"}</div>
                        <div className="col-md-3">📞 {app.phone || "—"}</div>
                        <div className="col-md-3">✉️ {app.email || "—"}</div>
                        {app.cuisineTypes && (
                            <div className="col-12">🍽️ {app.cuisineTypes.replace(/,/g, ", ")}</div>
                        )}
                        <div className="col-12">
                            FSSAI {app.fssaiLicenseNumber || "—"} · PAN {app.panNumber || "—"} · GSTIN {app.gstin || "—"}
                        </div>
                    </div>

                    {app.rejectionReason && (
                        <div className="alert alert-warning py-2 small mb-2">{app.rejectionReason}</div>
                    )}

                    {actionable && (
                        <div className="d-flex gap-2 flex-wrap">
                            <button className="btn btn-success btn-sm" disabled={busy} onClick={() => approve(app)}>
                                {busy ? "Working…" : "Approve"}
                            </button>
                            <button className="btn btn-outline-info btn-sm" disabled={busy}
                                onClick={() => requestDocuments(app)}>
                                Request documents
                            </button>
                            <button className="btn btn-outline-danger btn-sm" disabled={busy}
                                onClick={() => reject(app)}>
                                Reject
                            </button>
                        </div>
                    )}
                </div>
            </div>
        );
    };

    return (
        <div>
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h3 className="mb-0">Restaurant Applications</h3>
                <button className="btn btn-outline-secondary btn-sm" onClick={load}>Refresh</button>
            </div>

            <h6 className="text-muted">Awaiting a decision ({waiting.length})</h6>
            {waiting.length === 0
                ? <p className="text-muted small">Nothing waiting on you.</p>
                : waiting.map((a) => renderApplication(a, true))}

            <h6 className="text-muted mt-4">Already decided ({decided.length})</h6>
            {decided.length === 0
                ? <p className="text-muted small">No decisions yet.</p>
                : decided.map((a) => renderApplication(a, false))}
        </div>
    );
};

export default AdminDashboard;
