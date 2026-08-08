import { useState, useEffect, useRef } from 'react';
import axios from 'axios';

/**
 * Custom React hook for polling orders with automatic refresh.
 * 
 * @param {number} userId - User ID to fetch orders for
 * @param {number} interval - Polling interval in milliseconds (default: 15000 = 15s)
 * @param {boolean} enabled - Whether polling is enabled
 * @returns {object} - { data, loading, error, refresh }
 */
const useOrderPolling = (userId, interval = 15000, enabled = true) => {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const intervalRef = useRef(null);
    const isActiveRef = useRef(true);

    const fetchOrders = async () => {
        try {
            const response = await axios.get(`/api/order/user/${userId}`);
            setData(response.data);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const refresh = () => {
        fetchOrders();
    };

    useEffect(() => {
        if (!enabled || !userId) return;

        // Initial fetch
        fetchOrders();

        // Set up polling
        intervalRef.current = setInterval(() => {
            // Only poll if tab is active
            if (document.visibilityState === 'visible' && isActiveRef.current) {
                fetchOrders();
            }
        }, interval);

        // Pause polling when tab becomes inactive (battery optimization)
        const handleVisibilityChange = () => {
            if (document.visibilityState === 'hidden') {
                isActiveRef.current = false;
            } else {
                isActiveRef.current = true;
                // Fetch immediately when tab becomes active again
                fetchOrders();
            }
        };

        document.addEventListener('visibilitychange', handleVisibilityChange);

        // Cleanup
        return () => {
            if (intervalRef.current) {
                clearInterval(intervalRef.current);
            }
            document.removeEventListener('visibilitychange', handleVisibilityChange);
        };
    }, [userId, interval, enabled]);

    return { data, loading, error, refresh };
};

export default useOrderPolling;
