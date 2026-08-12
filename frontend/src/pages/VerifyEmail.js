import React, { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { verifyEmail, resendVerification, getCurrentUser } from "../services/api";

/** Lands here from the link in the confirmation email. */
const VerifyEmail = () => {
    const [params] = useSearchParams();
    const [state, setState] = useState("checking");
    const [message, setMessage] = useState("");

    // Asking for a new link replaces the old one, so an older email in the
    // inbox stops working. That is the usual reason for landing on the failure
    // below, and without a way out of it the account is stuck.
    const [email, setEmail] = useState(getCurrentUser()?.email || "");
    const [resending, setResending] = useState(false);
    const [resent, setResent] = useState(false);

    useEffect(() => {
        const token = params.get("token");
        if (!token) {
            setState("failed");
            setMessage("That link is missing its code.");
            return;
        }

        verifyEmail(token)
            .then(() => {
                setState("done");
                // The stored session predates verification, so refresh the flag
                // rather than making the user sign in again.
                try {
                    const user = JSON.parse(localStorage.getItem("user"));
                    if (user) {
                        localStorage.setItem("user", JSON.stringify({ ...user, emailVerified: true }));
                    }
                } catch {
                    // A malformed entry is not worth failing a confirmation over.
                }
            })
            .catch((e) => {
                setState("failed");
                setMessage(e.response?.data || "That link is not valid or has expired.");
            });
    }, [params]);

    const handleResend = async (e) => {
        e.preventDefault();
        setResending(true);
        try {
            await resendVerification(email);
            setResent(true);
        } catch {
            setMessage("Could not send it just now. Please try again.");
        } finally {
            setResending(false);
        }
    };

    if (state === "checking") {
        return <div className="text-center mt-5"><div className="spinner-border" /></div>;
    }

    if (state === "done") {
        return (
            <div className="text-center mt-5">
                <div className="fs-1 mb-2">✅</div>
                <h4>Your email is confirmed</h4>
                <p className="text-muted">
                    Thanks. You can order now, and reset your password if you ever need to.
                </p>
                <Link className="btn btn-success" to="/">Start ordering</Link>
            </div>
        );
    }

    return (
        <div className="text-center mt-5">
            <div className="fs-1 mb-2">⚠️</div>
            <h4>We could not confirm that</h4>
            <p className="text-muted mb-1">{message}</p>
            <p className="text-muted small">
                If you asked for more than one email, only the most recent link works.
                Check your inbox for the newest one.
            </p>

            {resent ? (
                <div className="alert alert-success d-inline-block mt-2">
                    A new link is on its way to {email}. Open that email and click the link in it.
                </div>
            ) : (
                <form className="mx-auto mt-3" style={{ maxWidth: 360 }} onSubmit={handleResend}>
                    <label className="form-label small text-muted">
                        Or send a fresh link to your address
                    </label>
                    <div className="input-group">
                        <input
                            type="email"
                            className="form-control"
                            placeholder="you@example.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                        <button className="btn btn-outline-success" disabled={resending || !email}>
                            {resending ? "Sending…" : "Send"}
                        </button>
                    </div>
                </form>
            )}

            <div className="mt-4">
                <Link className="btn btn-success" to="/">Start ordering</Link>
            </div>
        </div>
    );
};

export default VerifyEmail;
