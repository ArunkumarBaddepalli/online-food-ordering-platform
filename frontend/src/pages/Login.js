import React, { useState } from "react";
import { Link } from "react-router-dom";
import { login } from "../services/api";
import { useLocation } from "react-router-dom";

const Login = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const location = useLocation();

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await login({ email, password });
            localStorage.setItem("user", JSON.stringify(response.data));

            // Return to the page that sent us here, otherwise use the role landing page.
            const cameFrom = location.state?.from;
            const role = response.data.role;
            const destination = cameFrom
                ? cameFrom
                : role === "RESTAURANT_OWNER"
                    ? "/restaurant/dashboard"
                    : "/";

            // Full reload so components re-read the stored user.
            window.location.assign(destination);
        } catch (error) {
            console.error("Login failed", error);
            alert("Invalid credentials");
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
                    <span className="text-muted">Want to partner with us? </span>
                    <Link to="/register?role=RESTAURANT_OWNER">Register as Restaurant Partner</Link>
                </div>
            </div>
        </div>
    );
};

export default Login;
