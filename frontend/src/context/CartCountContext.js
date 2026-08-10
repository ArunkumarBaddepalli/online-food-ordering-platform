import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { getCart, getCurrentUser, getToken } from "../services/api";

/**
 * Holds the number of items in the cart so the navbar badge can show it.
 *
 * Kept in one place and refreshed explicitly after a change, rather than
 * polled. A badge that lags behind is worse than no badge, and polling for a
 * number that only changes when the user acts is wasted traffic.
 */
const CartCountContext = createContext({ count: 0, refresh: () => {} });

export const CartCountProvider = ({ children }) => {
    const [count, setCount] = useState(0);

    const refresh = useCallback(async () => {
        if (!getToken() || !getCurrentUser()?.id) {
            setCount(0);
            return;
        }
        try {
            const res = await getCart(getCurrentUser().id);
            const items = res.data?.items || [];
            setCount(items.reduce((sum, item) => sum + (item.quantity || 0), 0));
        } catch {
            // A failure here must never break the page; the badge just stays put.
        }
    }, []);

    useEffect(() => {
        refresh();
    }, [refresh]);

    return (
        <CartCountContext.Provider value={{ count, refresh }}>
            {children}
        </CartCountContext.Provider>
    );
};

export const useCartCount = () => useContext(CartCountContext);
