import React, { useState, useEffect } from 'react';
import { getActiveOrders } from '../services/api';
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

        // Through the shared client, so the request carries the signed-in
        // token. Called bare, this endpoint answers 401 and the banner never
        // appeared at all.
        const fetchActiveOrders = async () => {
            try {
                const response = await getActiveOrders(userId);
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

    // Nothing is being ridden anywhere on a collection order, and while home
    // delivery is switched off every order is a collection.
    const isPickup = order.order?.orderType === 'PICKUP';
    const ready = order.order?.status === 'READY_FOR_PICKUP';

    return (
        <div className="live-order-banner">
            <div className="banner-content">
                <div className="banner-icon">
                    <span>{isPickup ? '🍽️' : '🛵'}</span>
                </div>
                <div className="banner-info">
                    <div className="banner-status">{eta.statusMessage}</div>
                    <div className="banner-eta">
                        {ready ? (
                            <strong>Waiting for you at the counter</strong>
                        ) : eta.minutesRemaining > 0 ? (
                            <>
                                {isPickup ? 'Ready in ' : 'Arriving in '}
                                <strong>{eta.minutesRemaining} min</strong>
                            </>
                        ) : (
                            // The countdown reaching zero only means the estimate
                            // elapsed, not that the order was actually handed over.
                            <strong>{isPickup ? 'Ready any moment' : 'Arriving any moment'}</strong>
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
