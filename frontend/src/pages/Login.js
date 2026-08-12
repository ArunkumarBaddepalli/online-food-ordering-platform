import { toast } from "react-toastify";
import React, { useState } from "react";
import { Link } from "react-router-dom";
import { login, saveSession } from "../services/api";
import { useLocation } from "react-router-dom";

const Login = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const location = useLocation();

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await login({ email, password });
            saveSession(response.data);

            // Come back to wherever the user was sent from. The ?next= form is
            // used when an expired token bounced them out mid-page.
            const nextParam = new URLSearchParams(location.search).get("next");
            const destination = nextParam || location.state?.from || "/";

            // Full reload so every component re-reads the new session.
            window.location.assign(destination);
        } catch (error) {
            console.error("Login failed", error);
            toast.error("Invalid credentials");
        }
    };

    // Arriving here from somewhere else means the user did not choose to sign
    // in, they were sent. Saying so, and that they will be put back, stops the
    // form looking like the button they pressed did the wrong thing.
    const sentHere = Boolean(new URLSearchParams(location.search).get("next")
        || location.state?.from);

    return (
        <div className="row justify-content-center">
            <div className="col-md-4">
                <h2>Login</h2>
                {sentHere && (
                    <div className="alert alert-light border py-2 small">
                        Sign in to carry on. We will take you back to where you were.
                    </div>
                )}
                <form onSubmit={handleSubmit}>
                    <div className="mb-3">
                        <label>Email</label>
                        <input type="email" className="form-control" value={email}
                            onChange={(e) => setEmail(e.target.value)} required />
                    </div>
                    <div className="mb-3">
                        <label>Password</label>
                        <input type="password" className="form-control" value={password}
                            onChange={(e) => setPassword(e.target.value)} required />
                    </div>
                    <button type="submit" className="btn btn-primary w-100">Login</button>
                </form>
                <div className="text-center mt-3">
                    <Link to="/forgot-password">Forgotten your password?</Link>
                </div>
                <div className="text-center mt-2">
                    <span className="text-muted">Want to partner with us? </span>
                    <Link to="/register?role=RESTAURANT_OWNER">Register as Restaurant Partner</Link>
                </div>
            </div>
        </div>
    );
};

export default Login;
