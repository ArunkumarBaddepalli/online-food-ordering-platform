import axios from "axios";

// Override with REACT_APP_API_URL when the backend is not on the default port.
export const API_BASE = process.env.REACT_APP_API_URL || "http://localhost:8080";
const API_URL = `${API_BASE}/api`;

const api = axios.create({
    baseURL: API_URL,
});

// Auth
export const register = (user) => api.post("/auth/register", user);
export const login = (user) => api.post("/auth/login", user);

// Restaurants
export const getRestaurants = () => api.get("/restaurants");
export const getRestaurantById = (id) => api.get(`/restaurants/${id}`);
export const getRestaurantMenu = (restaurantId) => api.get(`/foods/${restaurantId}`);
export const getRestaurantLiveStatus = (restaurantId) => api.get(`/restaurants/${restaurantId}/live-status`);
export const getRestaurantMenuStatus = (restaurantId) => api.get(`/restaurants/${restaurantId}/menu-status`);

// Cart
export const getCart = (userId) => api.get(`/cart/${userId}`);
export const validateCart = (userId) => api.get(`/cart/${userId}/validate`);
export const addToCart = (userId, foodId, quantity, modifierIds = []) => {
    const params = { userId, foodId, quantity };
    if (modifierIds && modifierIds.length > 0) {
        params.modifierIds = modifierIds.join(",");
    }
    return api.post(`/cart/add`, null, { params });
};
export const getModifiersForFoodItem = (foodId) => api.get(`/modifiers/food-item/${foodId}`);

// Order
export const placeOrder = (userId, deliveryAddress, scheduledTime, orderType = "DELIVERY") => {
    const params = { userId, deliveryAddress, orderType };
    if (scheduledTime) {
        params.scheduledTime = scheduledTime;
    }
    return api.post("/order/place", null, { params });
};
export const cancelOrder = (orderId) => api.put(`/order/${orderId}/cancel`);

// Restaurant owner: incoming orders and moving them through their stages
export const getRestaurantOwnedBy = (userId) => api.get(`/restaurants/owned-by/${userId}`);
export const setAutoAccept = (restaurantId, enabled) =>
    api.put(`/restaurants/${restaurantId}/auto-accept`, null, { params: { enabled } });
export const getRestaurantOrders = (restaurantId) => api.get(`/order/restaurant/${restaurantId}`);
export const updateOrderStatus = (orderId, status) =>
    api.put(`/order/${orderId}/status`, null, { params: { status } });

// Restaurant Hours & Scheduling (legacy — use getRestaurantLiveStatus instead)
export const getRestaurantHours = (restaurantId) => api.get(`/restaurants/${restaurantId}/settings`);
export const getAvailableTimeSlots = (restaurantId) => api.get(`/restaurants/${restaurantId}/time-slots`);

// Payment
export const getPaymentConfig = () => api.get("/payment/config");
export const createRazorpayCheckout = (orderId) => api.post(`/payment/razorpay/order/${orderId}`);
export const verifyRazorpayPayment = (payload) => api.post("/payment/razorpay/verify", payload);

// Order History
export const getUserOrders = (userId) => api.get(`/order/user/${userId}`);
export const getOrderDetails = (orderId) => api.get(`/order/${orderId}`);

// User Profile
export const getUserProfile = (userId) => api.get(`/users/${userId}`);
export const updateUserProfile = (userId, userData) => api.put(`/users/${userId}`, userData);

// Cart Operations
export const updateCartItem = (itemId, quantity) => api.put(`/cart/item/${itemId}`, null, { params: { quantity } });
export const removeCartItem = (itemId) => api.delete(`/cart/item/${itemId}`);
export const clearCart = (userId) => api.delete(`/cart/${userId}`);
export const reorder = (userId, orderId) => api.post(`/cart/reorder`, null, { params: { userId, orderId } });

// User Addresses
export const getUserAddresses = (userId) => api.get(`/users/${userId}/addresses`);
export const addUserAddress = (userId, address) => api.post(`/users/${userId}/addresses`, address);
export const deleteUserAddress = (userId, addressId) => api.delete(`/users/${userId}/addresses/${addressId}`);
export const updateUserAddress = (userId, addressId, address) => api.put(`/users/${userId}/addresses/${addressId}`, address);

// Validation
export const validateDelivery = (restaurantId, address, latitude = null, longitude = null) => {
    return api.post(`/restaurants/${restaurantId}/validate-delivery`, { address, latitude, longitude });
};

// Stock Management (restaurant owner)
export const updateItemStock = (itemId, data) => api.put(`/menu/${itemId}/stock`, data);
export const markItemOOS = (itemId, reason) => api.put(`/menu/${itemId}/mark-oos`, null, { params: { reason } });
export const markItemAvailable = (itemId) => api.put(`/menu/${itemId}/mark-available`);

// Restaurant Onboarding
export const startOnboarding = (userId) => api.post(`/onboarding/start`, null, { params: { userId } });
export const getOnboarding = (id) => api.get(`/onboarding/${id}`);
export const getOnboardingStatus = (userId) => api.get(`/onboarding/status/${userId}`);
export const saveBasicInfo = (id, data) => api.put(`/onboarding/${id}/step/basic-info`, data);
export const saveLocation = (id, data) => api.put(`/onboarding/${id}/step/location`, data);
export const saveHours = (id, data) => api.put(`/onboarding/${id}/step/hours`, data);
export const saveDocuments = (id, formData) =>
    api.post(`/onboarding/${id}/step/documents`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
    });
export const saveBankDetails = (id, data) => api.put(`/onboarding/${id}/step/bank-details`, data);
export const submitOnboarding = (id) => api.post(`/onboarding/${id}/submit`);

// Admin Onboarding
export const adminListOnboarding = () => api.get(`/admin/onboarding`);
export const adminApprove = (id) => api.put(`/admin/onboarding/${id}/approve`);
export const adminReject = (id, reason) => api.put(`/admin/onboarding/${id}/reject`, { reason });
export const adminRequestDocuments = (id, reason) => api.put(`/admin/onboarding/${id}/request-documents`, { reason });

export default api;
