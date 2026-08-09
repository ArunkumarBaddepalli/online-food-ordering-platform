import React, { useEffect, useState, useCallback } from "react";
import { getCart, getRestaurantLiveStatus, validateCart, updateCartItem, removeCartItem , getCurrentUser } from "../services/api";
import { Link } from "react-router-dom";

const Cart = () => {
    const [cart, setCart] = useState(null);
    const [liveStatus, setLiveStatus] = useState(null);
    const [validation, setValidation] = useState(null);
    const user = getCurrentUser();

    const fetchCart = useCallback(async () => {
        if (!user) return;
        try {
            const res = await getCart(user.id);
            setCart(res.data);

            if (res.data?.items?.length > 0) {
                const restaurantId = res.data.items[0].foodItem.restaurant.id;
                const [statusRes, validRes] = await Promise.all([
                    getRestaurantLiveStatus(restaurantId),
                    validateCart(user.id),
                ]);
                setLiveStatus(statusRes.data);
                setValidation(validRes.data);
            } else {
                setLiveStatus(null);
                setValidation(null);
            }
        } catch (err) {
            console.error("Failed to load cart", err);
        }
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => { fetchCart(); }, [fetchCart]);

    const handleUpdateQuantity = async (itemId, newQuantity) => {
        if (newQuantity < 1) {
            if (window.confirm("Remove this item from cart?")) {
                await handleRemoveItem(itemId);
            }
            return;
        }
        const item = cart.items.find((i) => i.id === itemId);
        if (item?.foodItem?.stockQuantity && newQuantity > item.foodItem.stockQuantity) {
            if (window.confirm(`Only ${item.foodItem.stockQuantity} available. Adjust to maximum?`)) {
                newQuantity = item.foodItem.stockQuantity;
            } else return;
        }
        try {
            await updateCartItem(itemId, newQuantity);
            await fetchCart();
        } catch (err) {
            alert(err.response?.data || "Failed to update quantity");
        }
    };

    const handleRemoveItem = async (itemId) => {
        try {
            await removeCartItem(itemId);
            await fetchCart();
        } catch {
            alert("Failed to remove item");
        }
    };

    if (!cart || !cart.items || cart.items.length === 0) {
        return (
            <div className="text-center mt-5">
                <h4>Your cart is empty</h4>
                <Link to="/" className="btn btn-primary mt-3">Browse Restaurants</Link>
            </div>
        );
    }

    const total = cart.items.reduce((acc, item) => acc + (item.totalPrice ?? 0), 0);
    const restaurant = cart.items[0].foodItem.restaurant;
    const hasOOS = validation && !validation.valid;
    const canCheckout = !hasOOS && (liveStatus ? (liveStatus.canAcceptOrders || liveStatus.acceptsScheduledOrders) : true);

    const getItemValidation = (cartItemId) =>
        validation?.items?.find((v) => v.cartItemId === cartItemId);

    const formatAvailableAt = (ts) => {
        if (!ts) return null;
        const d = new Date(ts);
        return d.toLocaleDateString(undefined, { weekday: "short", hour: "2-digit", minute: "2-digit" });
    };

    return (
        <div>
            <h2>Shopping Cart</h2>

            {/* Restaurant status banner */}
            {liveStatus && (
                <div className={`alert ${liveStatus.canAcceptOrders ? "alert-info" : "alert-warning"} mb-3`}>
                    <div className="d-flex align-items-center justify-content-between">
                        <div>
                            <strong>{restaurant.name}</strong>
                            <span className={`ms-2 badge ${liveStatus.isCurrentlyOpen ? "bg-success" : "bg-danger"}`}>
                                {liveStatus.isCurrentlyOpen ? "Open" : "Closed"}
                            </span>
                        </div>
                        {liveStatus.todayHours && (
                            <small className="text-muted">
                                {liveStatus.isCurrentlyOpen
                                    ? `Closes ${liveStatus.todayHours.closeTime}`
                                    : liveStatus.todayHours.isOpen
                                        ? `Opens ${liveStatus.todayHours.openTime}`
                                        : "Closed today"}
                            </small>
                        )}
                    </div>
                    {!liveStatus.isCurrentlyOpen && liveStatus.acceptsScheduledOrders && (
                        <small className="mt-1 d-block">ℹ️ You can schedule this order for later at checkout</small>
                    )}
                    {!liveStatus.canAcceptOrders && !liveStatus.acceptsScheduledOrders && (
                        <small className="mt-1 d-block text-danger">Restaurant is closed and doesn't accept scheduled orders</small>
                    )}
                </div>
            )}

            {/* OOS warning banner */}
            {hasOOS && (
                <div className="alert alert-danger mb-3">
                    <strong>Some items are out of stock.</strong> Remove them to proceed to checkout.
                </div>
            )}

            <table className="table">
                <thead>
                    <tr>
                        <th>Item</th>
                        <th>Quantity</th>
                        <th>Price</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {cart.items.map((item) => {
                        const v = getItemValidation(item.id);
                        const isOOS = v?.isOOS;
                        return (
                            <tr key={item.id} className={isOOS ? "table-danger" : ""}>
                                <td>
                                    <div className="d-flex align-items-center gap-2">
                                        {item.foodItem.name}
                                        {isOOS && (
                                            <span className="badge bg-danger">Out of Stock</span>
                                        )}
                                        {!isOOS && item.foodItem.stockQuantity != null && item.foodItem.stockQuantity <= 5 && item.foodItem.stockQuantity > 0 && (
                                            <span className="badge bg-warning text-dark">Only {item.foodItem.stockQuantity} left</span>
                                        )}
                                    </div>
                                    {isOOS && v.oosReason && (
                                        <small className="text-danger d-block">{v.oosReason}</small>
                                    )}
                                    {isOOS && v.nextAvailableAt && (
                                        <small className="text-muted d-block">Available: {formatAvailableAt(v.nextAvailableAt)}</small>
                                    )}
                                    {item.selectedModifiers?.length > 0 && (
                                        <ul className="small text-muted mb-0 ps-3 mt-1">
                                            {item.selectedModifiers.map((mod, idx) => (
                                                <li key={idx}>{mod.modifier.name} (+${(mod.modifier?.priceAdjustment ?? 0).toFixed(2)})</li>
                                            ))}
                                        </ul>
                                    )}
                                </td>
                                <td>
                                    <div className="btn-group">
                                        <button className="btn btn-outline-secondary btn-sm"
                                            onClick={() => handleUpdateQuantity(item.id, item.quantity - 1)}>−</button>
                                        <span className="btn btn-outline-secondary btn-sm disabled text-dark" style={{ minWidth: 40 }}>
                                            {item.quantity}
                                        </span>
                                        <button className="btn btn-outline-secondary btn-sm"
                                            onClick={() => handleUpdateQuantity(item.id, item.quantity + 1)}
                                            disabled={item.quantity >= item.foodItem.stockQuantity}>+</button>
                                    </div>
                                </td>
                                <td>${(item.totalPrice ?? 0).toFixed(2)}</td>
                                <td>
                                    <button className="btn btn-danger btn-sm"
                                        onClick={() => handleRemoveItem(item.id)}>Remove</button>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>

            <h3>Total: ${total.toFixed(2)}</h3>

            {hasOOS && (
                <p className="text-danger">Remove out-of-stock items before checkout.</p>
            )}
            {!canCheckout && !hasOOS && (
                <p className="text-warning">Restaurant is not accepting orders right now.</p>
            )}

            <Link
                to="/checkout"
                className={`btn btn-primary ${!canCheckout ? "disabled" : ""}`}
                aria-disabled={!canCheckout}
            >
                Proceed to Checkout
            </Link>
        </div>
    );
};

export default Cart;
