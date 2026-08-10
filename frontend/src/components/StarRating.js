import React from "react";

/**
 * Stars, either as a read-only score or as something to click.
 */
const StarRating = ({ value = 0, onChange, size = "1rem" }) => {
    const interactive = typeof onChange === "function";

    return (
        <span style={{ whiteSpace: "nowrap", lineHeight: 1 }}>
            {[1, 2, 3, 4, 5].map((star) => {
                const filled = star <= Math.round(value);
                const content = (
                    <span style={{ color: filled ? "#f0a500" : "#d0d0d0", fontSize: size }}>★</span>
                );

                if (!interactive) {
                    return <span key={star}>{content}</span>;
                }

                return (
                    <button
                        key={star}
                        type="button"
                        onClick={() => onChange(star)}
                        aria-label={`${star} star${star > 1 ? "s" : ""}`}
                        style={{ background: "none", border: "none", padding: "0 2px", cursor: "pointer" }}
                    >
                        {content}
                    </button>
                );
            })}
        </span>
    );
};

export default StarRating;
