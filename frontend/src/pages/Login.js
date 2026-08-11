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

    return (
        <div className="row justify-content-center">
            <div className="col-md-4">
                <h2>Login</h2>
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
