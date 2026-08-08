import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getOnboardingStatus, startOnboarding } from "../../services/api";

const OnboardingStatus = () => {
    const navigate = useNavigate();
    const user = JSON.parse(localStorage.getItem("user"));
    const [onboarding, setOnboarding] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user) { navigate("/login"); return; }
        getOnboardingStatus(user.id)
            .then((res) => setOnboarding(res.data))
            .catch(() => setOnboarding(null))
            .finally(() => setLoading(false));
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    const handleStartNew = async () => {
        try {
            await startOnboarding(user.id);
            navigate("/restaurant/onboard");
        } catch {
            navigate("/restaurant/onboard");
        }
    };

    if (loading) return <div className="text-center mt-5"><div className="spinner-border" /></div>;

    if (!onboarding) return (
        <div className="text-center mt-5">
            <h4>No application found</h4>
            <button className="btn btn-primary mt-3" onClick={handleStartNew}>Start Your Application</button>
        </div>
    );

    const { status, rejectionReason, restaurantName } = onboarding;

    return (
        <div className="row justify-content-center">
            <div className="col-md-6 mt-4">
                <h4 className="mb-4">Application Status</h4>
                {restaurantName && <p className="text-muted">Restaurant: <strong>{restaurantName}</strong></p>}

                {(status === "DRAFT" || status === "SUBMITTED") && (
                    <div className="card p-4 text-center border-warning">
                        <div className="fs-1 mb-2">📝</div>
                        <h5>Application Not Submitted</h5>
                        <p className="text-muted">Your application is saved but not yet submitted for review.</p>
                        <Link to="/restaurant/onboard" className="btn btn-warning">Complete & Submit</Link>
                    </div>
                )}

                {status === "PENDING_REVIEW" && (
                    <div className="card p-4 text-center border-primary">
                        <div className="spinner-border text-primary mb-3" />
                        <h5>Under Review</h5>
                        <p className="text-muted">Our team is reviewing your application. This typically takes 2-3 business days.</p>
                        <small className="text-muted">You'll be notified once a decision is made.</small>
                    </div>
                )}

                {status === "DOCUMENTS_REQUIRED" && (
                    <div className="card p-4 border-warning">
                        <div className="fs-1 text-center mb-2">📋</div>
                        <h5 className="text-center">Additional Documents Required</h5>
                        {rejectionReason && (
                            <div className="alert alert-warning mt-2">{rejectionReason}</div>
                        )}
                        <Link to="/restaurant/onboard" className="btn btn-warning w-100 mt-2">Update Documents</Link>
                    </div>
                )}

                {status === "APPROVED" && (
                    <div className="card p-4 text-center border-success">
                        <div className="fs-1 mb-2">🎉</div>
                        <h5 className="text-success">Application Approved!</h5>
                        <p className="text-muted">Your restaurant is now live on the platform.</p>
                        <Link to="/restaurant/dashboard" className="btn btn-success">Go to Dashboard</Link>
                    </div>
                )}

                {status === "REJECTED" && (
                    <div className="card p-4 border-danger">
                        <div className="fs-1 text-center mb-2">❌</div>
                        <h5 className="text-center text-danger">Application Rejected</h5>
                        {rejectionReason && (
                            <div className="alert alert-danger mt-2">{rejectionReason}</div>
                        )}
                        <p className="text-muted text-center">You can start a new application addressing the above concerns.</p>
                        <button className="btn btn-primary w-100 mt-2" onClick={handleStartNew}>Start New Application</button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default OnboardingStatus;
