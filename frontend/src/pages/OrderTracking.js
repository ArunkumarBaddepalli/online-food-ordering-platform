import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getOrderDetails, cancelOrder } from '../services/api';
import './OrderTracking.css';

function OrderTracking() {
    const { orderId } = useParams();
    const [order, setOrder] = useState(null);

    useEffect(() => {
        if (orderId) {
            getOrderDetails(orderId)
                .then(response => setOrder(response.data))
                .catch(error => console.error('Error fetching order:', error));
        }
    }, [orderId]);

    const handleCancelOrder = async () => {
        if (!window.confirm("Are you sure you want to cancel this order?")) return;
        try {
            await cancelOrder(order.id);
            setOrder(prev => ({ ...prev, status: 'CANCELLED' }));
        } catch (error) {
            alert(error.response?.data || "Failed to cancel order");
        }
    };

    if (!order) return <div className="loading">Loading...</div>;

    const getStatusStep = (status) => {
        const steps = ['PENDING', 'CONFIRMED', 'PREPARING', 'DELIVERED'];
        return steps.indexOf(status) + 1;
    };

    const currentStep = getStatusStep(order.status);

    return (
        <div className="order-tracking-container">
            <h2>Order Details</h2>

            <div className="tracking-card">
                <div className="order-info">
                    <div className="d-flex justify-content-between align-items-center">
                        <h3>Order #{order.id}</h3>
                        {['PLACED', 'CONFIRMED'].includes(order.status) && (
                            <button className="btn btn-outline-danger btn-sm" onClick={handleCancelOrder}>
                                Cancel Order
                            </button>
                        )}
                    </div>
                    <p className="order-date">{new Date(order.orderDate).toLocaleString()}</p>
                </div>

                {/* Visual Timeline */}
                <div className="timeline">
                    <div className={`timeline-step ${currentStep >= 1 ? 'active completed' : ''}`}>
                        <div className="step-icon" style={{
                            background: currentStep >= 1 ? '#007bff' : '#e9ecef',
                            color: currentStep >= 1 ? 'white' : '#6c757d'
                        }}>
                            {currentStep >= 1 ? '✓' : '⏳'}
                        </div>
                        <div className="step-label">Order Placed</div>
                    </div>
                    <div className="timeline-line" style={{
                        background: currentStep >= 2 ? '#007bff' : '#e9ecef'
                    }}></div>
                    <div className={`timeline-step ${currentStep >= 2 ? 'active completed' : ''}`}>
                        <div className="step-icon" style={{
                            background: currentStep >= 2 ? '#007bff' : '#e9ecef',
                            color: currentStep >= 2 ? 'white' : '#6c757d'
                        }}>
                            {currentStep >= 2 ? '✓' : '📋'}
                        </div>
                        <div className="step-label">Confirmed</div>
                    </div>
                    <div className="timeline-line" style={{
                        background: currentStep >= 3 ? '#007bff' : '#e9ecef'
                    }}></div>
                    <div className={`timeline-step ${currentStep >= 3 ? 'active completed' : ''}`}>
                        <div className="step-icon" style={{
                            background: currentStep >= 3 ? '#007bff' : '#e9ecef',
                            color: currentStep >= 3 ? 'white' : '#6c757d'
                        }}>
                            {currentStep >= 3 ? '✓' : '🍳'}
                        </div>
                        <div className="step-label">Preparing</div>
                    </div>
                    <div className="timeline-line" style={{
                        background: currentStep >= 4 ? '#007bff' : '#e9ecef'
                    }}></div>
                    <div className={`timeline-step ${currentStep >= 4 ? 'active completed' : ''}`}>
                        <div className="step-icon" style={{
                            background: currentStep >= 4 ? '#007bff' : '#e9ecef',
                            color: currentStep >= 4 ? 'white' : '#6c757d'
                        }}>
                            {currentStep >= 4 ? '✓' : '🚚'}
                        </div>
                        <div className="step-label">Delivered</div>
                    </div>
                </div>

                <div className="order-details">
                    <h4>Items</h4>
                    {order.orderItems?.map((item, index) => (
                        <div key={index} className="order-item">
                            <span>{item.foodItem?.name || 'Item'} x{item.quantity}</span>
                            <span>${item.price.toFixed(2)}</span>
                        </div>
                    ))}

                    <div className="order-summary">
                        <p><strong>Delivery Address:</strong> {order.deliveryAddress}</p>
                        <p className="total"><strong>Total:</strong> ${order.totalAmount.toFixed(2)}</p>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default OrderTracking;
