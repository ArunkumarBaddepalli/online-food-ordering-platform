import React, { useEffect, useState } from "react";
import { getRestaurants } from "../services/api";
import { Link } from "react-router-dom";

const Home = () => {
    const [restaurants, setRestaurants] = useState([]);

    useEffect(() => {
        getRestaurants().then(response => {
            setRestaurants(response.data);
        }).catch(error => console.error(error));
    }, []);

    return (
        <div className="container mt-4">
            <h2 className="mb-4">🍽️ Restaurants</h2>
            <div className="row">
                {restaurants.map(rest => (
                    <div className="col-md-4 mb-4" key={rest.id}>
                        <div className="card h-100 shadow-sm" style={{ borderRadius: '12px', overflow: 'hidden' }}>
                            {/* Restaurant Image */}
                            {rest.imageUrl && (
                                <img
                                    src={rest.imageUrl}
                                    className="card-img-top"
                                    alt={rest.name}
                                    style={{ height: '200px', objectFit: 'cover' }}
                                />
                            )}

                            <div className="card-body">
                                {/* Restaurant Name with Open/Closed Badge */}
                                <div className="d-flex justify-content-between align-items-start mb-2">
                                    <h5 className="card-title mb-0">{rest.name}</h5>
                                    <span
                                        className={`badge ${rest.isOpen ? 'bg-success' : 'bg-danger'}`}
                                        style={{ fontSize: '0.75rem' }}
                                    >
                                        {rest.isOpen ? '🟢 OPEN' : '🔴 CLOSED'}
                                    </span>
                                </div>

                                <p className="card-text text-muted small">{rest.description}</p>

                                {/* Operating Hours - Dynamic Message */}
                                {(() => {
                                    if (!rest.openingTime || !rest.closingTime) return null;

                                    const now = new Date();

                                    // Robust time parser (handles "11:00", "11:00 AM", "23:00")
                                    const parseTime = (timeStr) => {
                                        if (!timeStr) return { h: 0, m: 0 };
                                        const match = timeStr.match(/(\d+):(\d+)\s*([APap][Mm])?/);
                                        if (!match) return { h: 0, m: 0 };

                                        let [_, hStr, mStr, ampm] = match;
                                        let h = parseInt(hStr, 10);
                                        let m = parseInt(mStr, 10);

                                        if (isNaN(h)) h = 0;
                                        if (isNaN(m)) m = 0;

                                        if (ampm) {
                                            ampm = ampm.toUpperCase();
                                            if (ampm === 'PM' && h < 12) h += 12;
                                            if (ampm === 'AM' && h === 12) h = 0;
                                        }
                                        return { h, m };
                                    };

                                    const start = parseTime(rest.openingTime);
                                    const end = parseTime(rest.closingTime);

                                    // Helper for display formatting (12-hour format)
                                    const formatDisplayTime = (h, m) => {
                                        const ampm = h >= 12 ? 'PM' : 'AM';
                                        const hour = h % 12 || 12;
                                        return `${hour}:${m.toString().padStart(2, '0')} ${ampm}`;
                                    };

                                    const openTimeStr = formatDisplayTime(start.h, start.m);
                                    const closeTimeStr = formatDisplayTime(end.h, end.m);

                                    // Create Date objects for logic comparison
                                    const openDate = new Date();
                                    openDate.setHours(start.h, start.m, 0, 0);

                                    const closeDate = new Date();
                                    closeDate.setHours(end.h, end.m, 0, 0);

                                    // If closing time is earlier than opening time, it means it closes the next day (e.g. 11 AM to 2 AM)
                                    if (closeDate <= openDate) {
                                        closeDate.setDate(closeDate.getDate() + 1);
                                    }

                                    let message = "";
                                    const isCloseTomorrow = closeDate.getDate() !== now.getDate();

                                    if (rest.isOpen) {
                                        // Case 1: Restaurant is OPEN
                                        // Check if it closes today or tomorrow
                                        if (isCloseTomorrow) {
                                            message = `Closes tomorrow at ${closeTimeStr}`;
                                        } else {
                                            message = `Closes today at ${closeTimeStr}`;
                                        }
                                    } else {
                                        // Case 2: Restaurant is CLOSED
                                        // Determine when it opens next

                                        // If "now" is before today's opening time, it opens today
                                        if (now < openDate) {
                                            message = `Opens today at ${openTimeStr}`;
                                        } else {
                                            // Assume it reads "Opens tomorrow"
                                            message = `Opens tomorrow at ${openTimeStr}`;
                                        }
                                    }

                                    return (
                                        <p className="mb-2" style={{ fontSize: '0.9rem', color: rest.isOpen ? '#28a745' : '#dc3545' }}>
                                            🕒 {message}
                                        </p>
                                    );
                                })()}

                                {/* Contact Info */}
                                {rest.phone && (
                                    <p className="mb-1 text-secondary" style={{ fontSize: '0.8rem' }}>
                                        📞 {rest.phone}
                                    </p>
                                )}

                                {rest.isOpen ? (
                                    <Link
                                        to={`/restaurant/${rest.id}`}
                                        className="btn btn-primary btn-sm mt-2 w-100"
                                        style={{ borderRadius: '8px' }}
                                    >
                                        View Menu
                                    </Link>
                                ) : (
                                    <button
                                        className="btn btn-secondary btn-sm mt-2 w-100"
                                        style={{ borderRadius: '8px' }}
                                        disabled
                                    >
                                        Currently Closed
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default Home;
