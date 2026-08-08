import React, { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { register, startOnboarding } from "../services/api";
import { useNavigate } from "react-router-dom";

const Register = () => {
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [address, setAddress] = useState("");
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const isPartner = searchParams.get("role") === "RESTAURANT_OWNER";

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const role = isPartner ? "RESTAURANT_OWNER" : "USER";
            const response = await register({ name, email, password, address, role });
            if (isPartner && response.data) {
                localStorage.setItem("user", JSON.stringify(response.data));
                try {
                    await startOnboarding(response.data.id);
                } catch (err) {
                    // onboarding may already exist
                }
                navigate("/restaurant/onboard");
            } else {
                alert("Registration successful! Please login.");
                navigate("/login");
            }
        } catch (error) {
            console.error("Registration failed", error);
            alert("Registration failed. Email may already be in use.");
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
                        <label>Name</label>
                        <input type="text" className="form-control" value={name}
                            onChange={(e) => setName(e.target.value)} required />
                    </div>
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
                    <div className="mb-3">
                        <label>Address</label>
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
