import { toast } from "react-toastify";
import React, { useEffect, useState } from "react";
import { getRestaurantMenu, addToCart, clearCart, API_BASE } from "../services/api";
import ModifierSelectionModal from "../components/ModifierSelectionModal";
import { useCartCount } from "../context/CartCountContext";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import axios from "axios";

const RestaurantMenu = () => {
    const { id } = useParams();
    const [menu, setMenu] = useState([]);
    const [restaurant, setRestaurant] = useState(null);
    const navigate = useNavigate();
    const location = useLocation();
    const { refresh: refreshCartCount } = useCartCount();

    // A malformed localStorage entry should not blank the whole page.
    let storedUser = null;
    try {
        storedUser = JSON.parse(localStorage.getItem("user"));
    } catch {
        storedUser = null;
    }
    const user = storedUser;
    const isLoggedIn = Boolean(user && user.id);

    useEffect(() => {
        getRestaurantMenu(id).then(response => setMenu(response.data)).catch(console.error);
        axios.get(`${API_BASE}/api/restaurants/${id}`)
            .then(response => setRestaurant(response.data))
            .catch(console.error);
    }, [id]);

    const [selectedFoodItem, setSelectedFoodItem] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    // ... imports at top ...
    // import ModifierSelectionModal from "../components/ModifierSelectionModal";
    // import { getRestaurantMenu, addToCart, getModifiersForFoodItem } from "../services/api";


    const [showClearCartModal, setShowClearCartModal] = useState(false);
    const [pendingCartItem, setPendingCartItem] = useState(null);
    const [pendingModifierIds, setPendingModifierIds] = useState([]);
    const [quantities, setQuantities] = useState({});

    const updateQuantity = (itemId, delta) => {
        setQuantities(prev => {
            const currentQty = prev[itemId] || 1;
            const newQty = Math.max(1, currentQty + delta);
            return { ...prev, [itemId]: newQty };
        });
    };

    // ... imports at top ...
    // import { getRestaurantMenu, addToCart, clearCart } from "../services/api";

    const handleAddToCart = async (foodItem) => {
        if (!foodItem.inStock) {
            toast.warning("This item is currently out of stock");
            return;
        }
        if (!isLoggedIn) {
            // Say why the page is about to change. Without this the screen just
            // swaps to a sign-in form and looks like the button misfired.
            toast.info("Please sign in to add items to your cart.");
            // Remember where we were so login can send the user straight back.
            navigate("/login", { state: { from: location.pathname } });
            return;
        }

        // Check for modifiers
        if (foodItem.modifierGroups && foodItem.modifierGroups.length > 0) {
            setSelectedFoodItem(foodItem);
            setIsModalOpen(true);
        } else {
            try {
                const qty = quantities[foodItem.id] || 1;
                await addToCart(user.id, foodItem.id, qty);
                toast.success("Added to cart");
                refreshCartCount();
            } catch (error) {
                console.error("Add to cart error:", error);
                if (error.response) {
                    console.error("Error response:", error.response.data);
                    console.error("Error status:", error.response.status);
                }
                if (error.response && error.response.status === 500 && error.response.data.message === "Items from different restaurant") {
                    setPendingCartItem(foodItem);
                    setPendingModifierIds([]);
                    setShowClearCartModal(true);
                } else {
                    toast.error(`Failed to add to cart: ${error.response?.data?.message || error.message}`);
                }
            }
        }
    };

    const handleModalAddToOrder = async (foodItem, modifierIds, quantity = 1) => {
        try {
            await addToCart(user.id, foodItem.id, quantity, modifierIds);
            toast.success("Added to cart");
                refreshCartCount();
            setIsModalOpen(false);
            setSelectedFoodItem(null);
        } catch (error) {
            console.error(error);
            if (error.response && error.response.status === 500 && error.response.data.message === "Items from different restaurant") {
                setPendingCartItem(foodItem);
                setPendingModifierIds(modifierIds);
                setShowClearCartModal(true);
                setIsModalOpen(false); // Close selection modal
            } else {
                toast.error("Failed to add to cart");
            }
        }
    };

    const handleClearCartAndAdd = async () => {
        if (!pendingCartItem) return;

        try {
            await clearCart(user.id);
            await addToCart(user.id, pendingCartItem.id, 1, pendingModifierIds);
            toast.success("Started a new basket");
                refreshCartCount();
            setShowClearCartModal(false);
            setPendingCartItem(null);
            setPendingModifierIds([]);
        } catch (error) {
            console.error(error);
            toast.error("Failed to clear cart and add item");
        }
    };

    return (
        <div className="container mt-4">
            {/* Breadcrumb Navigation */}
            <nav aria-label="breadcrumb">
                <ol className="breadcrumb">
                    <li className="breadcrumb-item">
                        <a href="/" onClick={(e) => { e.preventDefault(); navigate('/'); }}>Home</a>
                    </li>
                    <li className="breadcrumb-item active" aria-current="page">
                        {restaurant?.name || 'Menu'}
                    </li>
                </ol>
            </nav>

            {/* Back Button */}
            <button
                className="btn btn-outline-secondary btn-sm mb-3"
                onClick={() => navigate(-1)}
            >
                ← Back to Restaurants
            </button>

            {/* Login state belongs to the page, not to each menu item. One notice
                here instead of repeating it on every card. */}
            {!isLoggedIn && (
                <div className="alert alert-light border d-flex align-items-center justify-content-between py-2 mb-3">
                    <span className="text-muted small mb-0">Browse freely — you'll need to sign in to place an order.</span>
                    <button
                        className="btn btn-sm btn-outline-success ms-3 flex-shrink-0"
                        onClick={() => navigate("/login", { state: { from: location.pathname } })}
                    >
                        Login
                    </button>
                </div>
            )}

            {/* Modifiers Modal */}
            {isModalOpen && selectedFoodItem && (
                <ModifierSelectionModal
                    foodItem={selectedFoodItem}
                    onClose={() => setIsModalOpen(false)}
                    onAddToOrder={handleModalAddToOrder}
                />
            )}

            {/* Clear Cart Confirmation Modal */}
            {showClearCartModal && (
                <div className="modal show d-block" tabIndex="-1" role="dialog" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog" role="document">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Start a new basket?</h5>
                                <button type="button" className="btn-close" onClick={() => setShowClearCartModal(false)}></button>
                            </div>
                            <div className="modal-body">
                                <p>Your cart contains items from another restaurant. Do you want to clear your cart and start a fresh order from {restaurant?.name}?</p>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowClearCartModal(false)}>No</button>
                                <button type="button" className="btn btn-primary" onClick={handleClearCartAndAdd}>Yes, Start New Basket</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Restaurant Header with Details */}
            {restaurant && (
                <div className="card mb-4 shadow-sm" style={{ borderRadius: '12px' }}>
                    <div className="card-body">
                        <div className="row align-items-center">
                            <div className="col-md-8">
                                <h2 className="mb-1">
                                    {restaurant.name}
                                    <span className={`badge ms-3 ${restaurant.currentlyOpen ? 'bg-success' : 'bg-danger'}`}>
                                        {restaurant.currentlyOpen ? '🟢 OPEN' : '🔴 CLOSED'}
                                    </span>
                                </h2>
                                <p className="text-muted mb-2">{restaurant.description}</p>
                                <p className="mb-1"><strong>📍 Address:</strong> {restaurant.address}</p>
                                {restaurant.phone && (
                                    <p className="mb-1"><strong>📞 Phone:</strong> {restaurant.phone}</p>
                                )}
                                {restaurant.email && (
                                    <p className="mb-1"><strong>✉️ Email:</strong> {restaurant.email}</p>
                                )}
                                {restaurant.openingTime && restaurant.closingTime && (
                                    <p className="mb-1">
                                        <strong>🕒 Hours:</strong> {restaurant.openingTime} - {restaurant.closingTime}
                                    </p>
                                )}
                            </div>
                            {restaurant.imageUrl && (
                                <div className="col-md-4">
                                    <img
                                        src={restaurant.imageUrl}
                                        alt={restaurant.name}
                                        className="img-fluid rounded"
                                        style={{ maxHeight: '150px', objectFit: 'cover', width: '100%' }}
                                    />
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Best Sellers Section */}
            {menu.some(item => item.isBestSeller) && (
                <div className="mb-5">
                    <h3 className="mb-3 text-warning">
                        <span role="img" aria-label="star">⭐</span> Best Sellers
                    </h3>
                    <div className="row">
                        {menu.filter(item => item.isBestSeller).map(item => (
                            <div className="col-md-4 mb-4" key={`bestseller-${item.id}`}>
                                <div className="card h-100 shadow-sm border-warning" style={{ borderRadius: '12px', position: 'relative', borderWidth: '2px' }}>
                                    <div className="position-absolute top-0 start-0 m-2 badge bg-warning text-dark" style={{ zIndex: 10 }}>
                                        BEST SELLER
                                    </div>
                                    {/* Reuse card logic - duplicate for now or componentize? Duplicating for speed as per instructions to not over-refactor */}
                                    {/* Out of Stock Badge */}
                                    {!item.inStock && (
                                        <div className="position-absolute top-0 end-0 m-2 badge bg-danger" style={{ fontSize: '0.8rem', zIndex: 10 }}>
                                            OUT OF STOCK
                                        </div>
                                    )}
                                    {/* Low Stock Indicator */}
                                    {item.inStock && item.stockQuantity <= 10 && (
                                        <div className="position-absolute top-0 end-0 m-2 badge bg-warning text-dark" style={{ fontSize: '0.75rem', zIndex: 10 }}>
                                            Only {item.stockQuantity} left
                                        </div>
                                    )}
                                    {/* Food Image */}
                                    {item.imageUrl && (
                                        <img
                                            src={item.imageUrl}
                                            className="card-img-top"
                                            alt={item.name}
                                            style={{
                                                height: '180px',
                                                objectFit: 'cover',
                                                filter: !item.inStock ? 'grayscale(100%)' : 'none',
                                                opacity: !item.inStock ? 0.6 : 1
                                            }}
                                        />
                                    )}
                                    <div className="card-body">
                                        <h5 className="card-title">{item.name}</h5>
                                        <p className="card-text text-muted small">{item.description}</p>
                                        <p className="card-text fw-bold text-primary">Price: ${item.price?.toFixed(2)}</p>
                                        <div className="d-flex align-items-center justify-content-between">
                                            {/* Quantity Control */}
                                            <div className="btn-group me-2" role="group">
                                                <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => updateQuantity(item.id, -1)} disabled={!item.inStock || (quantities[item.id] || 1) <= 1}>-</button>
                                                <span className="btn btn-outline-secondary btn-sm disabled text-dark border-top-0 border-bottom-0">{quantities[item.id] || 1}</span>
                                                <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => updateQuantity(item.id, 1)} disabled={!item.inStock}>+</button>
                                            </div>
                                            <button className={`btn ${item.inStock ? 'btn-success' : 'btn-secondary'} flex-grow-1`} onClick={() => handleAddToCart(item)} disabled={!item.inStock} style={{ borderRadius: '8px' }}>
                                                {item.inStock ? 'Add' : 'Out of Stock'}
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                    <hr />
                </div>
            )}

            <h3 className="mb-4">Menu Items</h3>
            <div className="row">
                {menu.map(item => (
                    <div className="col-md-4 mb-4" key={item.id}>
                        <div className="card h-100 shadow-sm" style={{ borderRadius: '12px', position: 'relative' }}>
                            {/* Out of Stock Badge */}
                            {!item.inStock && (
                                <div
                                    className="position-absolute top-0 end-0 m-2 badge bg-danger"
                                    style={{ fontSize: '0.8rem', zIndex: 10 }}
                                >
                                    OUT OF STOCK
                                </div>
                            )}

                            {/* Low Stock Indicator */}
                            {item.inStock && item.stockQuantity <= 10 && (
                                <div
                                    className="position-absolute top-0 end-0 m-2 badge bg-warning text-dark"
                                    style={{ fontSize: '0.75rem', zIndex: 10 }}
                                >
                                    Only {item.stockQuantity} left
                                </div>
                            )}

                            {/* Food Image */}
                            {item.imageUrl && (
                                <img
                                    src={item.imageUrl}
                                    className="card-img-top"
                                    alt={item.name}
                                    style={{
                                        height: '180px',
                                        objectFit: 'cover',
                                        filter: !item.inStock ? 'grayscale(100%)' : 'none',
                                        opacity: !item.inStock ? 0.6 : 1
                                    }}
                                />
                            )}

                            <div className="card-body">
                                <h5 className="card-title">{item.name}</h5>
                                <p className="card-text text-muted small">{item.description}</p>
                                <p className="card-text fw-bold text-primary">Price: ${item.price?.toFixed(2)}</p>

                                <div className="d-flex align-items-center justify-content-between">
                                    {/* Quantity Control */}
                                    <div className="btn-group me-2" role="group" aria-label="Quantity">
                                        <button
                                            type="button"
                                            className="btn btn-outline-secondary btn-sm"
                                            onClick={() => updateQuantity(item.id, -1)}
                                            disabled={!item.inStock || (quantities[item.id] || 1) <= 1}
                                        >-</button>
                                        <span className="btn btn-outline-secondary btn-sm disabled text-dark border-top-0 border-bottom-0">
                                            {quantities[item.id] || 1}
                                        </span>
                                        <button
                                            type="button"
                                            className="btn btn-outline-secondary btn-sm"
                                            onClick={() => updateQuantity(item.id, 1)}
                                            disabled={!item.inStock}
                                        >+</button>
                                    </div>

                                    <button
                                        className={`btn ${item.inStock ? 'btn-success' : 'btn-secondary'} flex-grow-1`}
                                        onClick={() => handleAddToCart(item)}
                                        disabled={!item.inStock}
                                        style={{ borderRadius: '8px' }}
                                    >
                                        {item.inStock ? 'Add' : 'Out of Stock'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default RestaurantMenu;
