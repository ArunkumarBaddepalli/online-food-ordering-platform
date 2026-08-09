import React, { useState, useEffect, useCallback } from "react";
import { getRestaurantOrders, updateOrderStatus, cancelOrder } from "../services/api";

const REFRESH_MS = 20000;

const STATUS_STYLES = {
    PLACED: "bg-warning text-dark",
    CONFIRMED: "bg-info text-dark",
    PREPARING: "bg-primary",
    OUT_FOR_DELIVERY: "bg-primary",
    READY_FOR_PICKUP: "bg-primary",
    DELIVERED: "bg-success",
    PICKED_UP: "bg-success",
    CANCELLED: "bg-danger",
};

const money = (n) => `$${(n ?? 0).toFixed(2)}`;

const OwnerOrdersPanel = ({ restaurantId }) => {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [busyId, setBusyId] = useState(null);

    const load = useCallback(async () => {
        try {
            const res = await getRestaurantOrders(restaurantId);
            setRows(res.data);
            setError(null);
        } catch (e) {
            setError("Could not load orders.");
        } finally {
            setLoading(false);
        }
    }, [restaurantId]);

    useEffect(() => {
        load();
        const timer = setInterval(load, REFRESH_MS);
        return () => clearInterval(timer);
    }, [load]);

    const advance = async (orderId, status) => {
        setBusyId(orderId);
        try {
            await updateOrderStatus(orderId, status);
            await load();
        } catch (e) {
            alert(e.response?.data || "Could not update the order.");
        } finally {
            setBusyId(null);
        }
    };

    const reject = async (orderId) => {
        if (!window.confirm("Reject and cancel this order? Stock will be returned.")) return;
        setBusyId(orderId);
        try {
            await cancelOrder(orderId);
            await load();
        } catch (e) {
            alert(e.response?.data || "Could not cancel the order.");
        } finally {
            setBusyId(null);
        }
    };

    if (loading) return <div className="text-center my-4"><div className="spinner-border" /></div>;
    if (error) return <div className="alert alert-danger">{error}</div>;

    const active = rows.filter((r) => !r.isFinished);
    const finished = rows.filter((r) => r.isFinished);

    const renderOrder = (row) => {
        const { order, nextStatus, nextLabel, canCancel } = row;
        const items = order.orderItems || [];
        const busy = busyId === order.id;

        return (
            <div className="card mb-3" key={order.id}>
                <div className="card-body">
                    <div className="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            <h6 className="mb-1">
                                Order #{order.id}
                                <span className={`badge ms-2 ${STATUS_STYLES[order.status] || "bg-secondary"}`}>
                                    {order.status}
                                </span>
                                <span className="badge bg-light text-dark border ms-1">{order.orderType}</span>
                            </h6>
                            <div className="text-muted small">
                                {order.orderDate ? new Date(order.orderDate).toLocaleString() : ""}
                                {order.user?.name ? ` · ${order.user.name}` : ""}
                            </div>
                        </div>
                        <div className="text-end">
                            <div className="fw-bold">{money(order.totalAmount)}</div>
                            <div className="text-muted small">
                                {order.payment?.paymentMethod || "—"} · {order.payment?.paymentStatus || "—"}
                            </div>
                        </div>
                    </div>

                    <ul className="list-unstyled small mb-2">
                        {items.map((item) => (
                            <li key={item.id}>
                                {item.quantity} × {item.foodItem?.name}
                                {(item.selectedModifiers || []).length > 0 && (
                                    <span className="text-muted">
                                        {" "}({item.selectedModifiers.map((m) => m.modifierName).join(", ")})
                                    </span>
                                )}
                                <span className="text-muted"> — {money(item.price)}</span>
                            </li>
                        ))}
                    </ul>

                    {order.orderType === "DELIVERY" && order.deliveryAddress && (
                        <div className="small text-muted mb-2">Deliver to: {order.deliveryAddress}</div>
                    )}

                    <div className="d-flex gap-2">
                        {nextStatus && (
                            <button
                                className="btn btn-success btn-sm"
                                disabled={busy}
                                onClick={() => advance(order.id, nextStatus)}
                            >
                                {busy ? "Working…" : nextLabel}
                            </button>
                        )}
                        {canCancel && (
                            <button
                                className="btn btn-outline-danger btn-sm"
                                disabled={busy}
                                onClick={() => reject(order.id)}
                            >
                                Reject
                            </button>
                        )}
                    </div>
                </div>
            </div>
        );
    };

    return (
        <div className="mt-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h5 className="mb-0">Orders</h5>
                <button className="btn btn-outline-secondary btn-sm" onClick={load}>Refresh</button>
            </div>

            <h6 className="text-muted">In progress ({active.length})</h6>
            {active.length === 0
                ? <p className="text-muted small">No orders waiting on you right now.</p>
                : active.map(renderOrder)}

            <h6 className="text-muted mt-4">Finished ({finished.length})</h6>
            {finished.length === 0
                ? <p className="text-muted small">Nothing completed yet.</p>
                : finished.slice(0, 10).map(renderOrder)}
        </div>
    );
};

export default OwnerOrdersPanel;
