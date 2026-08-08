import React, { useState, useEffect } from "react";
import { placeOrder, getCart, getRestaurantLiveStatus, getUserAddresses, addUserAddress, updateUserAddress, deleteUserAddress, validateDelivery } from "../services/api";
import { useNavigate } from "react-router-dom";

import AddressMap from "../components/AddressMap";

const Checkout = () => {
    const [address, setAddress] = useState("");
    const [orderType, setOrderType] = useState("DELIVERY");
    const [deliveryTiming, setDeliveryTiming] = useState("");
    const [selectedTimeSlot, setSelectedTimeSlot] = useState("");
    const [restaurantInfo, setRestaurantInfo] = useState(null);
    const [timeSlots, setTimeSlots] = useState([]);
    const [restaurantId, setRestaurantId] = useState(null);
    const [error, setError] = useState("");

    // Address Management
    const [savedAddresses, setSavedAddresses] = useState([]);
    const [showAddAddress, setShowAddAddress] = useState(false);
    const [newAddress, setNewAddress] = useState("");
    const [newAddressLabel, setNewAddressLabel] = useState("Home");
    const [newAddressPincode, setNewAddressPincode] = useState("");
    const [newAddressLat, setNewAddressLat] = useState(null);
    const [newAddressLng, setNewAddressLng] = useState(null);
    const [editingAddressId, setEditingAddressId] = useState(null);
    const [validationResult, setValidationResult] = useState({ possible: true, message: "" });
    const [isCheckingDelivery, setIsCheckingDelivery] = useState(false);

    const navigate = useNavigate();
    const user = JSON.parse(localStorage.getItem("user"));

    useEffect(() => {
        // Fetch cart and restaurant info
        if (user) {
            getCart(user.id).then(response => {
                if (response.data?.items?.length > 0) {
                    const resId = response.data.items[0].foodItem.restaurant.id;
                    setRestaurantId(resId);

                    getRestaurantLiveStatus(resId).then(res => {
                        const status = res.data;
                        setRestaurantInfo(status);
                        if (!status.isCurrentlyOpen) {
                            setDeliveryTiming("scheduled");
                        }
                        const slots = status.availableTimeSlots || [];
                        setTimeSlots(slots);
                        if (slots.length > 0) setSelectedTimeSlot(slots[0]);
                    }).catch(err => console.error("Failed to fetch restaurant status:", err));
                }
            }).catch(error => console.error(error));

            // Fetch user addresses
            getUserAddresses(user.id).then(res => {
                setSavedAddresses(res.data);
            }).catch(err => console.error("Failed to fetch addresses:", err));
        }
    }, [user]);

    const validateAddress = async (addrText, lat = null, lng = null) => {
        if (!restaurantId || !addrText) return;
        setIsCheckingDelivery(true);
        try {
            const res = await validateDelivery(restaurantId, addrText, lat, lng);
            setValidationResult(res.data);
        } catch (err) {
            console.error("Validation failed:", err);
            setValidationResult({ possible: false, message: "Validation failed" });
        } finally {
            setIsCheckingDelivery(false);
        }
    };

    const handleSelectAddress = (addr) => {
        setAddress(addr.addressLine);
        // Use saved coordinates if available
        validateAddress(addr.addressLine, addr.latitude, addr.longitude);
    };

    const handleManualAddressChange = (e) => {
        const val = e.target.value;
        setAddress(val);
        // Reset validation state on edit so we don't show stale errors or success status
        setValidationResult({ possible: true, message: "" });
    };

    const handleAddressBlur = () => {
        validateAddress(address);
    };

    const handleEditAddress = (addr, e) => {
        e.stopPropagation();
        setNewAddress(addr.addressLine);
        setNewAddressLabel(addr.label);
        setNewAddressPincode(addr.pincode || "");
        setNewAddressLat(addr.latitude);
        setNewAddressLng(addr.longitude);
        setEditingAddressId(addr.id);
        setShowAddAddress(true);
    };

    const handleLocationSelect = (lat, lng) => {
        setNewAddressLat(lat);
        setNewAddressLng(lng);
    };

    const handleAddAddress = async () => {
        if (!newAddress || !newAddressPincode) {
            alert("Address and Pincode are required");
            return;
        }
        try {
            let res;
            const payload = {
                addressLine: newAddress,
                label: newAddressLabel,
                pincode: newAddressPincode,
                latitude: newAddressLat,
                longitude: newAddressLng
            };

            if (editingAddressId) {
                res = await updateUserAddress(user.id, editingAddressId, payload);
                setSavedAddresses(savedAddresses.map(a => a.id === editingAddressId ? res.data : a));
                setEditingAddressId(null);
            } else {
                res = await addUserAddress(user.id, payload);
                setSavedAddresses([...savedAddresses, res.data]);
            }

            setAddress(res.data.addressLine);
            validateAddress(res.data.addressLine, res.data.latitude, res.data.longitude);
            setShowAddAddress(false);
            setNewAddress("");
            setNewAddressPincode("");
            setNewAddressLabel("Home");
            setNewAddressLat(null);
            setNewAddressLng(null);
        } catch (err) {
            console.error("Failed to save address:", err);
            alert("Failed to save address");
        }
    };

    const handleDeleteAddress = async (addrId, e) => {
        e.stopPropagation();
        if (!window.confirm("Are you sure you want to delete this address?")) return;
        try {
            await deleteUserAddress(user.id, addrId);
            setSavedAddresses(savedAddresses.filter(a => a.id !== addrId));
            if (editingAddressId === addrId) {
                setEditingAddressId(null);
                setShowAddAddress(false);
                setNewAddress("");
                setNewAddressPincode("");
                setNewAddressLabel("Home");
                setNewAddressLat(null);
                setNewAddressLng(null);
            }
            if (address && savedAddresses.find(a => a.id === addrId)?.addressLine === address) {
                setAddress("");
                setValidationResult({ possible: true, message: "" });
            }
        } catch (err) {
            console.error("Failed to delete address:", err);
            alert("Failed to delete address");
        }
    };

    const handlePlaceOrder = async (e) => {
        e.preventDefault();
        setError("");

        try {
            const scheduledTime = deliveryTiming === "scheduled" ? selectedTimeSlot : null;
            const response = await placeOrder(user.id, orderType === "DELIVERY" ? address : "", scheduledTime, orderType);

            if (typeof response.data === 'string') {
                setError(response.data);
                return;
            }

            alert("Order placed successfully!");
            navigate("/orders");
        } catch (error) {
            console.error(error);
            const errorMessage = error.response?.data || "Failed to place order";
            setError(errorMessage);
        }
    };

    const formatTime = (timeString) => {
        if (!timeString) return "";
        if (timeString.match(/AM|PM/i)) return timeString;
        const [hours, minutes] = timeString.split(":");
        const hour = parseInt(hours);
        const ampm = hour >= 12 ? "PM" : "AM";
        const displayHour = hour % 12 || 12;
        return `${displayHour}:${minutes} ${ampm}`;
    };

    const formatDateTime = (dateTimeString) => {
        const date = new Date(dateTimeString);
        return date.toLocaleString('en-US', {
            weekday: 'short',
            month: 'short',
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    };

    const isOpen = restaurantInfo?.isCurrentlyOpen || false;
    const closingTime = restaurantInfo?.todayHours?.closeTime ? formatTime(restaurantInfo.todayHours.closeTime) : "";
    const nextOpenTime = restaurantInfo?.nextOpenTime ? formatTime(restaurantInfo.nextOpenTime) : "";

    return (
        <div className="row justify-content-center">
            <div className="col-md-6">
                <h2>Checkout</h2>

                {restaurantInfo && (
                    <div className={`alert ${isOpen ? 'alert-success' : 'alert-warning'} mb-3`}>
                        <strong>Restaurant Status:</strong>
                        <span className={`ms-2 badge ${isOpen ? 'bg-success' : 'bg-danger'}`}>
                            {isOpen ? 'Open' : 'Closed'}
                        </span>
                        <div className="mt-1 small">
                            {isOpen
                                ? (closingTime ? `Closes at ${closingTime}` : "Currently open")
                                : (nextOpenTime ? `Opens at ${nextOpenTime}` : "Currently closed")}
                        </div>
                        {!isOpen && restaurantInfo.acceptsScheduledOrders && (
                            <div className="mt-1 small">You can schedule this order for a later time slot.</div>
                        )}
                    </div>
                )}

                {error && (
                    <div className="alert alert-danger" role="alert">
                        {error}
                    </div>
                )}

                <form onSubmit={handlePlaceOrder}>
                    <div className="mb-3">
                        <label className="form-label"><strong>Order Type</strong></label>
                        <div className="d-flex gap-3">
                            <div className="form-check">
                                <input
                                    className="form-check-input"
                                    type="radio"
                                    name="orderType"
                                    id="typeDelivery"
                                    value="DELIVERY"
                                    checked={orderType === "DELIVERY"}
                                    onChange={(e) => setOrderType(e.target.value)}
                                />
                                <label className="form-check-label" htmlFor="typeDelivery">
                                    Delivery
                                </label>
                            </div>
                            <div className="form-check">
                                <input
                                    className="form-check-input"
                                    type="radio"
                                    name="orderType"
                                    id="typePickup"
                                    value="PICKUP"
                                    checked={orderType === "PICKUP"}
                                    onChange={(e) => setOrderType(e.target.value)}
                                />
                                <label className="form-check-label" htmlFor="typePickup">
                                    Pickup
                                </label>
                            </div>
                        </div>
                    </div>

                    <div className="mb-3">
                        <label className="form-label"><strong>Delivery Timing</strong></label>

                        {isOpen && (
                            <div className="form-check">
                                <input
                                    className="form-check-input"
                                    type="radio"
                                    name="deliveryTiming"
                                    id="orderNow"
                                    value="now"
                                    checked={deliveryTiming === "now"}
                                    onChange={(e) => setDeliveryTiming(e.target.value)}
                                />
                                <label className="form-check-label" htmlFor="orderNow">
                                    Order Now (for immediate delivery)
                                </label>
                            </div>
                        )}

                        <div className="form-check">
                            <input
                                className="form-check-input"
                                type="radio"
                                name="deliveryTiming"
                                id="scheduleLater"
                                value="scheduled"
                                checked={deliveryTiming === "scheduled"}
                                onChange={(e) => setDeliveryTiming(e.target.value)}
                                disabled={!isOpen && timeSlots.length === 0}
                            />
                            <label className="form-check-label" htmlFor="scheduleLater">
                                Schedule for Later
                            </label>
                        </div>
                    </div>

                    {deliveryTiming === "scheduled" && (
                        <div className="mb-3">
                            <label className="form-label">Select Time Slot</label>
                            <select
                                className="form-select"
                                value={selectedTimeSlot}
                                onChange={(e) => setSelectedTimeSlot(e.target.value)}
                                required
                            >
                                {timeSlots.map((slot, index) => (
                                    <option key={index} value={slot}>
                                        {formatDateTime(slot)}
                                    </option>
                                ))}
                            </select>
                            {timeSlots.length === 0 && (
                                <div className="form-text text-danger">
                                    No available time slots. Please try again later.
                                </div>
                            )}
                        </div>
                    )}

                    {orderType === "DELIVERY" && (
                        <div className="mb-3">
                            <label className="form-label">Delivery Address</label>

                            {savedAddresses.length > 0 && (
                                <div className="list-group mb-2">
                                    {savedAddresses.map(addr => (
                                        <div
                                            key={addr.id}
                                            className={`list-group-item list-group-item-action ${address === addr.addressLine ? 'active' : ''}`}
                                            onClick={() => handleSelectAddress(addr)}
                                            style={{ cursor: 'pointer' }}
                                        >
                                            <div className="d-flex justify-content-between align-items-center w-100">
                                                <span><strong>{addr.label}:</strong> {addr.addressLine}</span>
                                                <div>
                                                    <button
                                                        type="button"
                                                        className="btn btn-sm btn-outline-secondary"
                                                        onClick={(e) => handleEditAddress(addr, e)}
                                                    >
                                                        ✏️ Edit
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="btn btn-sm btn-outline-danger ms-2"
                                                        onClick={(e) => handleDeleteAddress(addr.id, e)}
                                                    >
                                                        🗑️ Delete
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}

                            {!showAddAddress && (
                                <button type="button" className="btn btn-outline-primary btn-sm mb-2" onClick={() => {
                                    setEditingAddressId(null);
                                    setNewAddress("");
                                    setNewAddressPincode("");
                                    setNewAddressLabel("Home");
                                    setNewAddressLat(null);
                                    setNewAddressLng(null);
                                    setShowAddAddress(true);
                                }}>
                                    + Add New Address
                                </button>
                            )}

                            {showAddAddress && (
                                <div className="card p-3 mb-2 bg-light border-0">
                                    <h6 className="card-title">{editingAddressId ? 'Edit Address' : 'Add New Address'}</h6>

                                    <AddressMap
                                        initialLat={newAddressLat}
                                        initialLng={newAddressLng}
                                        onLocationSelect={handleLocationSelect}
                                    />
                                    <div className="form-text mb-2 text-muted">
                                        Drag the marker or click "Locate Me" to pinpoint your exact location for better delivery accuracy.
                                    </div>

                                    <div className="mb-2">
                                        <label className="form-label small">Label (e.g., Home, Work)</label>
                                        <input
                                            type="text"
                                            className="form-control form-control-sm"
                                            value={newAddressLabel}
                                            onChange={(e) => setNewAddressLabel(e.target.value)}
                                        />
                                    </div>
                                    <div className="mb-2">
                                        <label className="form-label small">Address</label>
                                        <textarea
                                            className="form-control form-control-sm"
                                            rows="2"
                                            value={newAddress}
                                            onChange={(e) => setNewAddress(e.target.value)}
                                            placeholder="Enter full address including city and zip code"
                                        ></textarea>
                                    </div>
                                    <div className="mb-2">
                                        <label className="form-label small">Pincode</label>
                                        <input
                                            type="text"
                                            className="form-control form-control-sm"
                                            value={newAddressPincode}
                                            onChange={(e) => setNewAddressPincode(e.target.value)}
                                            placeholder="e.g. 10001"
                                        />
                                    </div>
                                    <div className="d-flex gap-2">
                                        <button type="button" className="btn btn-primary btn-sm" onClick={handleAddAddress} disabled={!newAddress || !newAddressPincode}>
                                            {editingAddressId ? 'Update & Select' : 'Save & Select'}
                                        </button>
                                        <button type="button" className="btn btn-secondary btn-sm" onClick={() => setShowAddAddress(false)}>Cancel</button>
                                    </div>
                                </div>
                            )}

                            <textarea
                                className="form-control"
                                rows="3"
                                value={address}
                                onChange={handleManualAddressChange}
                                onBlur={handleAddressBlur}
                                placeholder="Enter delivery address or select from above..."
                                required
                            ></textarea>

                            {address && (
                                <>
                                    {isCheckingDelivery && (
                                        <div className="mt-2 text-muted small">Checking delivery availability...</div>
                                    )}
                                    {!isCheckingDelivery && !validationResult.possible && (
                                        <div className="mt-2 alert alert-danger py-2 px-3 small">
                                            <strong>❌ Delivery Not Available</strong>
                                            <div className="mt-1">{validationResult.message}</div>
                                            {validationResult.distanceKm !== undefined && <div className="mt-1 fw-bold">📏 Distance: {validationResult.distanceKm.toFixed(2)} km (Max: {validationResult.maxRadiusKm} km)</div>}
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                    )}

                    <div className="mb-3">
                        <label className="form-label">Payment Method</label>
                        <select className="form-select">
                            <option>Cash on Delivery</option>
                            <option>Credit Card (Mock)</option>
                        </select>
                    </div>

                    <button
                        type="submit"
                        className="btn btn-success w-100"
                        disabled={
                            !deliveryTiming ||
                            (deliveryTiming === "scheduled" && timeSlots.length === 0) ||
                            (orderType === "DELIVERY" && (!validationResult.possible || isCheckingDelivery))
                        }
                    >
                        {deliveryTiming === "scheduled" ? "Schedule Order" : "Place Order Now"}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default Checkout;
