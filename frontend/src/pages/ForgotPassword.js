import React, { useState } from "react";
import { Link } from "react-router-dom";
import { requestPasswordReset } from "../services/api";
import { suggestEmail } from "../utils/emailSuggestion";

const ForgotPassword = () => {
    const [email, setEmail] = useState("");
    const [hint, setHint] = useState(null);
    const [sent, setSent] = useState(false);
    const [busy, setBusy] = useState(false);

    const submit = async (e) => {
        e.preventDefault();
        setBusy(true);
        try {
            await requestPasswordReset(email);
        } finally {
            // Shown whatever happened. Saying "no such account" would let
            // anybody check which addresses are registered here.
            setSent(true);
            setBusy(false);
        }
    };

    if (sent) {
        return (
            <div className="row justify-content-center">
                <div className="col-md-5 text-center mt-5">
                    <div className="fs-1 mb-2">📮</div>
                    <h5>Check your inbox</h5>
                    <p className="text-muted">
                        If <strong>{email}</strong> has an account, a reset link is on its way.
                        It works for 30 minutes.
                    </p>
                    <Link className="btn btn-outline-secondary btn-sm" to="/login">Back to sign in</Link>
                </div>
            </div>
        );
    }

    return (
        <div className="row justify-content-center">
            <div className="col-md-5 mt-4">
                <h4>Forgotten your password?</h4>
                <p className="text-muted small">
                    Enter your email and we'll send you a link to choose a new one.
                </p>

                <form onSubmit={submit}>
                    <div className="mb-3">
                        <label className="form-label">Email <span className="text-danger">*</span></label>
                        <input
                            type="email"
                            className="form-control"
                            value={email}
                            onChange={(e) => { setEmail(e.target.value); setHint(null); }}
                            onBlur={(e) => setHint(suggestEmail(e.target.value))}
                            required
                        />
                        {hint && (
                            <div className="form-text text-warning">
                                Did you mean{" "}
                                <button type="button" className="btn btn-link btn-sm p-0 align-baseline"
                                    onClick={() => { setEmail(hint); setHint(null); }}>
                                    {hint}
                                </button>?
                            </div>
                        )}
                    </div>

                    <button className="btn btn-primary w-100" disabled={busy}>
                        {busy ? "Sending…" : "Send reset link"}
                    </button>
                </form>

                <div className="text-center mt-3">
                    <Link to="/login">Back to sign in</Link>
                </div>
            </div>
        </div>
    );
};

export default ForgotPassword;
