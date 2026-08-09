import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { API_BASE } from '../services/api';
import { useNavigate } from 'react-router-dom';
import './LiveOrderBanner.css';

/**
 * Floating banner component that displays active order status with ETA.
 * Appears on home page when user has active orders.
 */
const LiveOrderBanner = ({ userId }) => {
    const [activeOrders, setActiveOrders] = useState([]);
    const [dismissed, setDismissed] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        if (!userId) return;

        const fetchActiveOrders = async () => {
            try {
                const response = await axios.get(`${API_BASE}/api/order/user/${userId}/active`);
                setActiveOrders(response.data);
            } catch (error) {
                console.error('Error fetching active orders:', error);
            }
        };

        fetchActiveOrders();

        // Poll every 15 seconds for updates
        const interval = setInterval(fetchActiveOrders, 15000);

        return () => clearInterval(interval);
    }, [userId]);

    if (!activeOrders.length || dismissed) return null;

    const order = activeOrders[0]; // Show only the first active order
    const { eta } = order;

    return (
        <div className="live-order-banner">
            <div className="banner-content">
                <div className="banner-icon">
                    <span>🛵</span>
                </div>
                <div className="banner-info">
                    <div className="banner-status">{eta.statusMessage}</div>
                    <div className="banner-eta">
                        {eta.minutesRemaining > 0 ? (
                            <>Arriving in <strong>{eta.minutesRemaining} min</strong></>
                        ) : (
                            <strong>Order delivered!</strong>
                        )}
                    </div>
                </div>
                <div className="banner-actions">
                    <button
                        className="track-button"
                        onClick={() => navigate(`/orders/${order.order.id}`)}
                    >
                        Track Order
                    </button>
                    <button
                        className="dismiss-button"
                        onClick={() => setDismissed(true)}
                        aria-label="Dismiss"
                    >
                        ×
                    </button>
                </div>
            </div>
        </div>
    );
};

export default LiveOrderBanner;
