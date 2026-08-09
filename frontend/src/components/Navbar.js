import React from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";

const Navbar = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const user = JSON.parse(localStorage.getItem("user"));

    const handleLogout = () => {
        localStorage.removeItem("user");
        navigate("/login");
    };

    return (
        <nav className="navbar navbar-expand-lg navbar-dark bg-dark">
            <div className="container">
                <div className="d-flex align-items-center me-3">
                    <button
                        className="btn btn-sm btn-outline-light me-2"
                        onClick={() => navigate(-1)}
                        style={{ fontSize: '1rem', padding: '0.25rem 0.5rem' }}
                        title="Go Back"
                    >
                        ←
                    </button>
                    <button
                        className="btn btn-sm btn-outline-light"
                        onClick={() => navigate(1)}
                        style={{ fontSize: '1rem', padding: '0.25rem 0.5rem' }}
                        title="Go Forward"
                    >
                        →
                    </button>
                </div>

                <Link className="navbar-brand" to="/">🍕 Food Delivery</Link>
                <div className="collapse navbar-collapse">
                    <ul className="navbar-nav ms-auto">
                        {user ? (
                            <>
                                {/* Everyone can shop, including restaurant owners and admins.
                                    Owners previously had no Home/Cart/Orders links at all, which
                                    left them stuck in the onboarding flow with no way to order. */}
                                <li className="nav-item">
                                    <Link className={`nav-link ${location.pathname === '/' ? 'active fw-bold' : ''}`} to="/">Home</Link>
                                </li>
                                <li className="nav-item">
                                    <Link className={`nav-link ${location.pathname === '/cart' ? 'active fw-bold' : ''}`} to="/cart">Cart</Link>
                                </li>
                                <li className="nav-item">
                                    <Link className={`nav-link ${location.pathname === '/orders' ? 'active fw-bold' : ''}`} to="/orders">Orders</Link>
                                </li>
                                <li className="nav-item">
                                    <Link className={`nav-link ${location.pathname === '/profile' ? 'active fw-bold' : ''}`} to="/profile">Profile</Link>
                                </li>

                                {user.role === "RESTAURANT_OWNER" && (
                                    <>
                                        <li className="nav-item">
                                            <Link className={`nav-link ${location.pathname.startsWith('/restaurant/dashboard') ? 'active fw-bold' : ''}`}
                                                to="/restaurant/dashboard">My Restaurant</Link>
                                        </li>
                                        <li className="nav-item">
                                            <Link className={`nav-link ${location.pathname.startsWith('/restaurant/onboard') ? 'active fw-bold' : ''}`}
                                                to="/restaurant/onboard">Onboarding</Link>
                                        </li>
                                    </>
                                )}

                                {user.role === "ADMIN" && (
                                    <li className="nav-item">
                                        <span className="nav-link text-warning">Admin</span>
                                    </li>
                                )}
                                <li className="nav-item">
                                    <button className="btn btn-danger btn-sm ms-2" onClick={handleLogout}>Logout</button>
                                </li>
                            </>
                        ) : (
                            <>
                                <li className="nav-item"><Link className="nav-link" to="/login">Login</Link></li>
                                <li className="nav-item"><Link className="nav-link" to="/register">Register</Link></li>
                            </>
                        )}
                    </ul>
                </div>
            </div>
        </nav>
    );
};

export default Navbar;
