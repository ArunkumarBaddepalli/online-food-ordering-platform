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

            // Land on the page that sent us here, otherwise the home page —
            // for every role. Routing owners straight to the dashboard pushed
            // them into the onboarding flow and left them unable to order.
            // Owners reach their dashboard via "My Restaurant" in the navbar.
            const destination = location.state?.from || "/";

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
