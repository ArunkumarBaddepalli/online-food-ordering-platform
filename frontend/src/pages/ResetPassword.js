import React, { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import { resetPassword } from "../services/api";

const ResetPassword = () => {
    const [params] = useSearchParams();
    const token = params.get("token");

    const [password, setPassword] = useState("");
    const [confirm, setConfirm] = useState("");
    const [busy, setBusy] = useState(false);
    const [done, setDone] = useState(false);
    const [error, setError] = useState(null);

    const submit = async (e) => {
        e.preventDefault();

        if (password.length < 8) return setError("Choose a password of at least 8 characters.");
        if (password !== confirm) return setError("Those two passwords are not the same.");

        setBusy(true);
        setError(null);
        try {
            await resetPassword(token, password);
            setDone(true);
            toast.success("Password changed");
        } catch (e2) {
            setError(e2.response?.data || "We could not change your password.");
        } finally {
            setBusy(false);
        }
    };

    if (!token) {
        return (
            <div className="text-center mt-5">
                <h5>That link is missing its code</h5>
                <Link className="btn btn-outline-secondary btn-sm mt-2" to="/forgot-password">
                    Ask for a new link
                </Link>
            </div>
        );
    }

    if (done) {
        return (
            <div className="text-center mt-5">
                <div className="fs-1 mb-2">✅</div>
                <h5>Your password has been changed</h5>
                <Link className="btn btn-success mt-2" to="/login">Sign in</Link>
            </div>
        );
    }

    return (
        <div className="row justify-content-center">
            <div className="col-md-5 mt-4">
                <h4>Choose a new password</h4>

                {error && <div className="alert alert-danger py-2">{error}</div>}

                <form onSubmit={submit}>
                    <div className="mb-3">
                        <label className="form-label">New password <span className="text-danger">*</span></label>
                        <input type="password" className="form-control" value={password} minLength={8}
                            onChange={(e) => setPassword(e.target.value)} required />
                        <div className="form-text">At least 8 characters.</div>
                    </div>

                    <div className="mb-3">
                        <label className="form-label">Confirm it <span className="text-danger">*</span></label>
                        <input type="password" className="form-control" value={confirm}
                            onChange={(e) => setConfirm(e.target.value)} required />
                    </div>

                    <button className="btn btn-primary w-100" disabled={busy}>
                        {busy ? "Saving…" : "Change my password"}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ResetPassword;
