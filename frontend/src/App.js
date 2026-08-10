import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from "react-router-dom";
import Navbar from "./components/Navbar";
import PartnerNavbar from "./components/PartnerNavbar";
import RequireAuth from "./components/RequireAuth";
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
import { getCurrentUser } from "./services/api";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import "./App.css";

const OWNER = ["RESTAURANT_OWNER", "ADMIN"];

/**
 * The customer site and the partner portal are separate products that happen
 * to share a deployment: different audience, different navigation, different
 * pages. The URL prefix decides which shell you get.
 */
function Shell() {
  const location = useLocation();
  const isPartnerArea = location.pathname.startsWith("/partner");
  const user = getCurrentUser();

  return (
    <>
      {isPartnerArea ? <PartnerNavbar /> : <Navbar />}

      <div className="container mt-4">
        <Routes>
          {/* --- Customer site --- */}
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/restaurant/:id" element={<RestaurantMenu />} />
          <Route path="/cart" element={<RequireAuth><Cart /></RequireAuth>} />
          <Route path="/checkout" element={<RequireAuth><Checkout /></RequireAuth>} />
          <Route path="/orders" element={<RequireAuth><OrderHistory /></RequireAuth>} />
          <Route path="/orders/:orderId" element={<RequireAuth><OrderTracking /></RequireAuth>} />
          <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />

          {/* --- Partner portal --- */}
          <Route path="/partner" element={<Navigate to="/partner/dashboard" replace />} />
          <Route
            path="/partner/dashboard"
            element={<RequireAuth roles={OWNER}><RestaurantDashboard /></RequireAuth>}
          />
          <Route
            path="/partner/onboard"
            element={<RequireAuth roles={OWNER}><RestaurantOnboard /></RequireAuth>}
          />
          <Route
            path="/partner/onboard/status"
            element={<RequireAuth roles={OWNER}><OnboardingStatus /></RequireAuth>}
          />

          {/* Old owner URLs sat under /restaurant/, which also means a customer
              menu page. Kept as redirects so existing links still work. */}
          <Route path="/restaurant/dashboard" element={<Navigate to="/partner/dashboard" replace />} />
          <Route path="/restaurant/onboard" element={<Navigate to="/partner/onboard" replace />} />
          <Route path="/restaurant/onboard/status" element={<Navigate to="/partner/onboard/status" replace />} />

          <Route
            path="*"
            element={
              <div className="text-center mt-5">
                <h4>Page not found</h4>
                <a className="btn btn-outline-secondary mt-2" href="/">Back to home</a>
              </div>
            }
          />
        </Routes>
      </div>

      {/* Live order tracking belongs to the customer site only. */}
      {!isPartnerArea && user?.id && <LiveOrderBanner userId={user.id} />}

      <ToastContainer position="top-right" autoClose={3000} newestOnTop closeOnClick pauseOnHover />
    </>
  );
}

function App() {
  return (
    <Router>
      <Shell />
    </Router>
  );
}

export default App;
