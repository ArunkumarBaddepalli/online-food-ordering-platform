import React, { useEffect, useState } from "react";
import { toast } from "react-toastify";
import { getOrderReview, submitReview } from "../services/api";
import StarRating from "./StarRating";

/**
 * Rating for a single order, shown once the food has arrived.
 *
 * Renders nothing at all until the order is delivered or collected — there is
 * nothing to rate before then.
 */
const OrderReview = ({ orderId }) => {
    const [canReview, setCanReview] = useState(false);
    const [existing, setExisting] = useState(null);
    const [rating, setRating] = useState(0);
    const [comment, setComment] = useState("");
    const [saving, setSaving] = useState(false);
    const [loaded, setLoaded] = useState(false);

    useEffect(() => {
        getOrderReview(orderId)
            .then((res) => {
                setCanReview(Boolean(res.data.canReview));
                setExisting(res.data.review);
            })
            .catch(() => {
                setCanReview(false);
            })
            .finally(() => setLoaded(true));
    }, [orderId]);

    const save = async () => {
        if (rating < 1) return toast.warning("Pick a rating first.");
        setSaving(true);
        try {
            const res = await submitReview(orderId, rating, comment.trim());
            setExisting(res.data);
            setCanReview(false);
            toast.success("Thanks for the review");
        } catch (e) {
            toast.error(e.response?.data || "Could not save your review.");
        } finally {
            setSaving(false);
        }
    };

    if (!loaded) return null;

    if (existing) {
        return (
            <div className="card mt-3">
                <div className="card-body py-3">
                    <h6 className="mb-2">Your review</h6>
                    <StarRating value={existing.rating} />
                    {existing.comment && <p className="text-muted small mb-0 mt-2">{existing.comment}</p>}
                </div>
            </div>
        );
    }

    if (!canReview) return null;

    return (
        <div className="card mt-3">
            <div className="card-body py-3">
                <h6 className="mb-2">How was it?</h6>
                <StarRating value={rating} onChange={setRating} size="1.6rem" />
                <textarea
                    className="form-control mt-2"
                    rows="2"
                    placeholder="Anything you'd like to add? (optional)"
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                />
                <button className="btn btn-success btn-sm mt-2" disabled={saving} onClick={save}>
                    {saving ? "Saving…" : "Submit review"}
                </button>
            </div>
        </div>
    );
};

export default OrderReview;
