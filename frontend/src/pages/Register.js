import { toast } from "react-toastify";
import React, { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { register, startOnboarding, saveSession } from "../services/api";
import { suggestEmail } from "../utils/emailSuggestion";

const Register = () => {
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [address, setAddress] = useState("");
    const [emailHint, setEmailHint] = useState(null);
    const [searchParams] = useSearchParams();
    const isPartner = searchParams.get("role") === "RESTAURANT_OWNER";

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const role = isPartner ? "RESTAURANT_OWNER" : "USER";
            const response = await register({ name, email, password, address, role });

            // Registering signs you in, so the token must be stored before any
            // follow-up call is made.
            saveSession(response.data);

            if (isPartner) {
                try {
                    await startOnboarding(response.data.user.id);
                } catch (err) {
                    // An application may already exist for this account.
                }
                window.location.assign("/partner/onboard");
            } else {
                window.location.assign("/");
            }
        } catch (error) {
            console.error("Registration failed", error);
            toast.error("Registration failed. Email may already be in use.");
        }
    };

    return (
        <div className="row justify-content-center">
            <div className="col-md-4">
                <h2>{isPartner ? "Register as Restaurant Partner" : "Register"}</h2>
                {isPartner && (
                    <div className="alert alert-info py-2 mb-3">
                        You're registering as a restaurant partner. After registration you'll complete your restaurant onboarding.
                    </div>
                )}
                <form onSubmit={handleSubmit}>
                    <div className="mb-3">
                        <label>Your name <span className="text-danger">*</span></label>
                        <input type="text" className="form-control" value={name}
                            onChange={(e) => setName(e.target.value)} required />
                        {isPartner && (
                            <div className="form-text">
                                Your own name, not the restaurant's — you'll enter that during onboarding.
                            </div>
                        )}
                    </div>
                    <div className="mb-3">
                        <label>Email <span className="text-danger">*</span></label>
                        <input type="email" className="form-control" value={email}
                            onChange={(e) => { setEmail(e.target.value); setEmailHint(null); }}
                            onBlur={(e) => setEmailHint(suggestEmail(e.target.value))}
                            required />
                        {emailHint && (
                            <div className="form-text text-warning">
                                Did you mean{" "}
                                <button type="button" className="btn btn-link btn-sm p-0 align-baseline"
                                    onClick={() => { setEmail(emailHint); setEmailHint(null); }}>
                                    {emailHint}
                                </button>?
                            </div>
                        )}
                    </div>
                    <div className="mb-3">
                        <label>Password <span className="text-danger">*</span></label>
                        <input type="password" className="form-control" value={password}
                            minLength={8}
                            onChange={(e) => setPassword(e.target.value)} required />
                        <div className="form-text">At least 8 characters.</div>
                    </div>
                    <div className="mb-3">
                        <label>Address <span className="text-danger">*</span></label>
                        <input type="text" className="form-control" value={address}
                            onChange={(e) => setAddress(e.target.value)} required />
                    </div>
                    <button type="submit" className="btn btn-primary w-100">
                        {isPartner ? "Register & Start Onboarding" : "Register"}
                    </button>
                </form>
                <div className="text-center mt-3">
                    {isPartner ? (
                        <><Link to="/register">Customer registration</Link> | <Link to="/login">Login</Link></>
                    ) : (
                        <>
                            <span className="text-muted">Want to partner with us? </span>
                            <Link to="/register?role=RESTAURANT_OWNER">Register as Restaurant Partner</Link>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Register;
