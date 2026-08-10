import React from "react";
import { Link } from "react-router-dom";
import "./Footer.css";

/**
 * Deliberately only links to things this app actually has.
 *
 * No app store badges, no social accounts and no office address: inventing
 * them would look tidy and be untrue, and anyone clicking would find nothing.
 */
const Footer = () => (
    <footer className="site-footer mt-5">
        <div className="container py-5">
            <div className="row gy-4">
                <div className="col-lg-4">
                    <h5 className="mb-2">🍕 Food Delivery</h5>
                    <p className="text-muted small mb-0">
                        Order from restaurants near you, follow your order while it is
                        prepared, and pay by card or on delivery.
                    </p>
                </div>

                <div className="col-6 col-lg-2">
                    <h6 className="footer-heading">Order</h6>
                    <ul className="footer-links">
                        <li><Link to="/">Restaurants</Link></li>
                        <li><Link to="/cart">Your cart</Link></li>
                        <li><Link to="/orders">Your orders</Link></li>
                    </ul>
                </div>

                <div className="col-6 col-lg-2">
                    <h6 className="footer-heading">Account</h6>
                    <ul className="footer-links">
                        <li><Link to="/profile">Profile</Link></li>
                        <li><Link to="/login">Sign in</Link></li>
                        <li><Link to="/register">Create account</Link></li>
                    </ul>
                </div>

                <div className="col-6 col-lg-2">
                    <h6 className="footer-heading">Partners</h6>
                    <ul className="footer-links">
                        <li><Link to="/register?role=RESTAURANT_OWNER">List your restaurant</Link></li>
                        <li><Link to="/partner/dashboard">Partner portal</Link></li>
                    </ul>
                </div>

                <div className="col-6 col-lg-2">
                    <h6 className="footer-heading">How it works</h6>
                    <ul className="footer-links">
                        <li><span className="text-muted">Delivery or pickup</span></li>
                        <li><span className="text-muted">Cash or card</span></li>
                        <li><span className="text-muted">Live order tracking</span></li>
                    </ul>
                </div>
            </div>

            <hr className="my-4" />

            <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
                <span className="text-muted small">
                    © {new Date().getFullYear()} Food Delivery
                </span>
                <span className="text-muted small">
                    A full stack project built with Spring Boot and React
                </span>
            </div>
        </div>
    </footer>
);

export default Footer;
