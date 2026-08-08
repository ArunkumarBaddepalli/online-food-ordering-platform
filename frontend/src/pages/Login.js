import React, { useState } from "react";
import { Link } from "react-router-dom";
import { login } from "../services/api";
import { useNavigate } from "react-router-dom";

const Login = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await login({ email, password });
            localStorage.setItem("user", JSON.stringify(response.data));
            const role = response.data.role;
            if (role === "RESTAURANT_OWNER") {
                navigate("/restaurant/dashboard");
            } else if (role === "ADMIN") {
                navigate("/");
            } else {
                navigate("/");
            }
            window.location.reload();
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
