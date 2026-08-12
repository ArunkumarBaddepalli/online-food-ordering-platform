import { toast } from "react-toastify";
import React, { useState, useEffect } from 'react';
import { getUserOrders, reorder, cancelOrder } from '../services/api';
import { useNavigate } from 'react-router-dom';
import './OrderHistory.css';

function OrderHistory() {
    const [orders, setOrders] = useState([]);
    const [activeTab, setActiveTab] = useState('all');
    const navigate = useNavigate();

    useEffect(() => {
        const user = JSON.parse(localStorage.getItem('user'));
        if (!user) {
            navigate('/login', { state: { from: window.location.pathname } });
            return;
        }

        getUserOrders(user.id)
            .then(response => {
                console.log("User Orders Response:", response.data);
                if (Array.isArray(response.data)) {
                    setOrders(response.data);
                } else {
                    console.error("Expected array but got:", response.data);
                    setOrders([]);
                }
            })
            .catch(error => console.error('Error fetching orders:', error));
    }, [navigate]);

    const filterOrders = (status) => {
        if (status === 'all') return orders;
        // Ongoing: PLACED, WAITING, RECEIVED, PREPARING, CONFIRMED, OUT_FOR_DELIVERY, PENDING, READY_FOR_PICKUP
        if (status === 'ongoing') {
            return orders.filter(o =>
                ['PLACED', 'WAITING', 'RECEIVED', 'PREPARING', 'CONFIRMED', 'OUT_FOR_DELIVERY', 'PENDING', 'READY_FOR_PICKUP'].includes(o.status)
            );
        }
        // Completed: DELIVERED, CANCELLED, PICKED_UP
        if (status === 'completed') {
            return orders.filter(o => ['DELIVERED', 'CANCELLED', 'PICKED_UP'].includes(o.status));
        }
        return orders;
    };

    const handleCancelOrder = async (e, orderId) => {
        e.stopPropagation();
        if (!window.confirm("Are you sure you want to cancel this order?")) return;
        try {
            await cancelOrder(orderId);
            setOrders(orders.map(o => o.id === orderId ? { ...o, status: 'CANCELLED' } : o));
        } catch (error) {
            toast.error(error.response?.data || "Failed to cancel order");
        }
    };

    const handleReorder = async (e, orderId) => {
        e.stopPropagation(); // Prevent card click navigation
        const user = JSON.parse(localStorage.getItem('user'));
        if (!user) return;

        if (window.confirm("This will clear your current cart and add items from this order. Continue?")) {
            try {
                await reorder(user.id, orderId);
                navigate('/cart');
            } catch (error) {
                console.error("Reorder failed:", error);
                toast.error("Failed to reorder items. Some items might be unavailable.");
            }
        }
    };

    const filteredOrders = filterOrders(activeTab);

    return (
        <div className="order-history-container">
            <h2>My Orders</h2>

            <div className="order-tabs">
                <button
                    className={activeTab === 'all' ? 'tab active' : 'tab'}
                    onClick={() => setActiveTab('all')}
                >
                    All ({orders.length})
                </button>
                <button
                    className={activeTab === 'ongoing' ? 'tab active' : 'tab'}
                    onClick={() => setActiveTab('ongoing')}
                >
                    Ongoing ({orders.filter(o => ['PLACED', 'WAITING', 'RECEIVED', 'PREPARING', 'CONFIRMED', 'OUT_FOR_DELIVERY', 'PENDING', 'READY_FOR_PICKUP'].includes(o.status)).length})
                </button>
                <button
                    className={activeTab === 'completed' ? 'tab active' : 'tab'}
                    onClick={() => setActiveTab('completed')}
                >
                    Completed ({orders.filter(o => ['DELIVERED', 'CANCELLED', 'PICKED_UP'].includes(o.status)).length})
                </button>
            </div>

            <div className="orders-list">
                {filteredOrders.length === 0 ? (
                    <div className="empty-state">
                        <div className="empty-icon">📦</div>
                        <p>No orders found</p>
                        <button className="browse-btn" onClick={() => navigate('/')}>Browse our menu</button>
                    </div>
                ) : (
                    filteredOrders.map(order => (
                        <div key={order.id} className="order-card" onClick={() => navigate(`/orders/${order.id}`)}>
                            <div className="order-header">
                                <h3>Order #{order.id}</h3>
                                <div className="d-flex align-items-center gap-2">
                                    <span className={`status-badge ${order.status.toLowerCase()}`}>{order.status}</span>
                                    {['PLACED', 'CONFIRMED'].includes(order.status) && (
                                        <button
                                            className="btn btn-sm btn-outline-danger"
                                            style={{ fontSize: '0.8rem', padding: '0.2rem 0.5rem' }}
                                            onClick={(e) => handleCancelOrder(e, order.id)}
                                        >
                                            Cancel
                                        </button>
                                    )}
                                    {['DELIVERED', 'PICKED_UP', 'CANCELLED'].includes(order.status) && (
                                        <button
                                            className="btn btn-sm btn-primary"
                                            style={{ fontSize: '0.8rem', padding: '0.2rem 0.5rem' }}
                                            onClick={(e) => handleReorder(e, order.id)}
                                        >
                                            Reorder
                                        </button>
                                    )}
                                </div>
                            </div>
                            <p className="order-date">{new Date(order.orderDate).toLocaleDateString()}</p>
                            <p className="order-items">{order.orderItems?.length || 0} items</p>
                            <p className="order-total">${(order.totalAmount ?? 0).toFixed(2)}</p>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}

export default OrderHistory;
