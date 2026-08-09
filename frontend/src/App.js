import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Home from "./pages/Home";
import RestaurantMenu from "./pages/RestaurantMenu";
import Cart from "./pages/Cart";
import Checkout from "./pages/Checkout";
import OrderTracking from "./pages/OrderTracking";
import OrderHistory from "./pages/OrderHistory";
import Profile from "./pages/Profile";
import RestaurantOnboard from "./pages/onboarding/RestaurantOnboard";
import OnboardingStatus from "./pages/onboarding/OnboardingStatus";
import RestaurantDashboard from "./pages/RestaurantDashboard";
import LiveOrderBanner from "./components/LiveOrderBanner";
import "./App.css";

function App() {
  // A malformed entry should not take down the whole app shell.
  let user = null;
  try {
    user = JSON.parse(localStorage.getItem("user"));
  } catch {
    user = null;
  }

  return (
    <Router>
      <Navbar />
      <div className="container mt-4">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/restaurant/:id" element={<RestaurantMenu />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/checkout" element={<Checkout />} />
          <Route path="/orders" element={<OrderHistory />} />
          <Route path="/orders/:orderId" element={<OrderTracking />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/restaurant/onboard" element={<RestaurantOnboard />} />
          <Route path="/restaurant/onboard/status" element={<OnboardingStatus />} />
          <Route path="/restaurant/dashboard" element={<RestaurantDashboard />} />
        </Routes>
      </div>

      {/* Floating live-order tracker. The component existed but was never
          mounted, so it had never rendered. */}
      {user?.id && <LiveOrderBanner userId={user.id} />}
    </Router>
  );
}

export default App;
