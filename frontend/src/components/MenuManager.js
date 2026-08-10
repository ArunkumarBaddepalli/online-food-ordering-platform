import React, { useCallback, useEffect, useState } from "react";
import { toast } from "react-toastify";
import {
    getRestaurantMenu,
    createMenuItem,
    updateMenuItem,
    deleteMenuItem,
    updateItemStock,
    markItemOOS,
    markItemAvailable,
} from "../services/api";

const EMPTY = {
    name: "",
    description: "",
    price: "",
    imageUrl: "",
    stockResetType: "UNLIMITED",
    stockQuantity: 100,
    dailyStockLimit: 20,
    dailyRestockTime: "09:00",
};

const STOCK_HELP = {
    UNLIMITED: "Always available. Nothing is counted.",
    DAILY: "A fixed batch each day, topped back up automatically at the restock time.",
    MANUAL: "You set the number yourself and mark it sold out when it runs out.",
};

const MenuManager = ({ restaurantId }) => {
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState(EMPTY);
    const [editingId, setEditingId] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [busyId, setBusyId] = useState(null);

    const load = useCallback(async () => {
        try {
            const res = await getRestaurantMenu(restaurantId);
            setItems(res.data || []);
        } catch {
            toast.error("Could not load the menu.");
        } finally {
            setLoading(false);
        }
    }, [restaurantId]);

    useEffect(() => {
        load();
    }, [load]);

    const startAdd = () => {
        setForm(EMPTY);
        setEditingId(null);
        setShowForm(true);
    };

    const startEdit = (item) => {
        setForm({
            name: item.name || "",
            description: item.description || "",
            price: item.price ?? "",
            imageUrl: item.imageUrl || "",
            stockResetType: item.stockResetType || "UNLIMITED",
            stockQuantity: item.stockQuantity ?? 100,
            dailyStockLimit: item.dailyStockLimit ?? 20,
            dailyRestockTime: item.dailyRestockTime || "09:00",
        });
        setEditingId(item.id);
        setShowForm(true);
    };

    const save = async (e) => {
        e.preventDefault();

        const price = Number(form.price);
        if (!form.name.trim()) return toast.warning("The dish needs a name.");
        if (!Number.isFinite(price) || price <= 0) return toast.warning("Enter a price above zero.");

        const details = {
            name: form.name.trim(),
            description: form.description.trim(),
            price,
            imageUrl: form.imageUrl.trim(),
        };

        const stock = {
            stockResetType: form.stockResetType,
            stockQuantity: form.stockResetType === "DAILY"
                ? Number(form.dailyStockLimit)
                : Number(form.stockQuantity),
            dailyStockLimit: form.stockResetType === "DAILY" ? Number(form.dailyStockLimit) : null,
            dailyRestockTime: form.stockResetType === "DAILY" ? form.dailyRestockTime : null,
        };

        try {
            // Creating and editing only cover the descriptive fields, so stock
            // settings are saved through their own endpoint either way.
            const id = editingId
                ? (await updateMenuItem(editingId, details), editingId)
                : (await createMenuItem(restaurantId, details)).data.id;

            await updateItemStock(id, stock);

            toast.success(editingId ? "Dish updated" : "Dish added to the menu");
            setShowForm(false);
            setEditingId(null);
            setForm(EMPTY);
            await load();
        } catch (err) {
            toast.error(err.response?.data || "Could not save the dish.");
        }
    };

    const remove = async (item) => {
        if (!window.confirm(`Remove "${item.name}" from the menu? This cannot be undone.`)) return;
        setBusyId(item.id);
        try {
            await deleteMenuItem(item.id);
            toast.success("Dish removed");
            await load();
        } catch (err) {
            toast.error(err.response?.data || "Could not remove the dish.");
        } finally {
            setBusyId(null);
        }
    };

    const toggleAvailability = async (item) => {
        setBusyId(item.id);
        try {
            if (item.inStock) {
                const reason = window.prompt("Why is it unavailable?", "Sold out") || "Sold out";
                await markItemOOS(item.id, reason);
                toast.success(`${item.name} marked unavailable`);
            } else {
                await markItemAvailable(item.id);
                toast.success(`${item.name} is back on the menu`);
            }
            await load();
        } catch (err) {
            toast.error(err.response?.data || "Could not change availability.");
        } finally {
            setBusyId(null);
        }
    };

    if (loading) return <div className="text-center my-4"><div className="spinner-border" /></div>;

    return (
        <div className="mt-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h5 className="mb-0">Menu ({items.length})</h5>
                <button className="btn btn-success btn-sm" onClick={startAdd}>+ Add a dish</button>
            </div>

            {showForm && (
                <div className="card mb-3">
                    <form className="card-body" onSubmit={save}>
                        <h6 className="mb-3">{editingId ? "Edit dish" : "New dish"}</h6>

                        <div className="row g-2">
                            <div className="col-md-6">
                                <label className="form-label small">Name *</label>
                                <input className="form-control" value={form.name}
                                    onChange={(e) => setForm({ ...form, name: e.target.value })} />
                            </div>
                            <div className="col-md-3">
                                <label className="form-label small">Price *</label>
                                <input className="form-control" type="number" step="0.01" min="0" value={form.price}
                                    onChange={(e) => setForm({ ...form, price: e.target.value })} />
                            </div>
                            <div className="col-md-3">
                                <label className="form-label small">Availability</label>
                                <select className="form-select" value={form.stockResetType}
                                    onChange={(e) => setForm({ ...form, stockResetType: e.target.value })}>
                                    <option value="UNLIMITED">Always available</option>
                                    <option value="DAILY">Limited each day</option>
                                    <option value="MANUAL">I manage the count</option>
                                </select>
                            </div>

                            <div className="col-12">
                                <label className="form-label small">Description</label>
                                <input className="form-control" value={form.description}
                                    onChange={(e) => setForm({ ...form, description: e.target.value })} />
                            </div>

                            <div className="col-12">
                                <label className="form-label small">Image URL</label>
                                <input className="form-control" value={form.imageUrl} placeholder="https://…"
                                    onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
                            </div>

                            {form.stockResetType === "DAILY" && (
                                <>
                                    <div className="col-md-3">
                                        <label className="form-label small">Made each day</label>
                                        <input className="form-control" type="number" min="1" value={form.dailyStockLimit}
                                            onChange={(e) => setForm({ ...form, dailyStockLimit: e.target.value })} />
                                    </div>
                                    <div className="col-md-3">
                                        <label className="form-label small">Restocked at</label>
                                        <input className="form-control" type="time" value={form.dailyRestockTime}
                                            onChange={(e) => setForm({ ...form, dailyRestockTime: e.target.value })} />
                                    </div>
                                </>
                            )}

                            {form.stockResetType === "MANUAL" && (
                                <div className="col-md-3">
                                    <label className="form-label small">How many left</label>
                                    <input className="form-control" type="number" min="0" value={form.stockQuantity}
                                        onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })} />
                                </div>
                            )}

                            <div className="col-12">
                                <p className="text-muted small mb-2">{STOCK_HELP[form.stockResetType]}</p>
                            </div>
                        </div>

                        <div className="d-flex gap-2">
                            <button className="btn btn-success btn-sm" type="submit">
                                {editingId ? "Save changes" : "Add to menu"}
                            </button>
                            <button className="btn btn-outline-secondary btn-sm" type="button"
                                onClick={() => { setShowForm(false); setEditingId(null); }}>
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            )}

            {items.length === 0 ? (
                <p className="text-muted small">
                    Nothing on the menu yet. Customers cannot order until you add a dish.
                </p>
            ) : (
                <div className="table-responsive">
                    <table className="table align-middle">
                        <thead>
                            <tr>
                                <th>Dish</th>
                                <th>Price</th>
                                <th>Availability</th>
                                <th className="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {items.map((item) => (
                                <tr key={item.id}>
                                    <td>
                                        <div className="fw-semibold">{item.name}</div>
                                        {item.description && (
                                            <div className="text-muted small">{item.description}</div>
                                        )}
                                    </td>
                                    <td>${(item.price ?? 0).toFixed(2)}</td>
                                    <td>
                                        {item.inStock
                                            ? <span className="badge bg-success">Available</span>
                                            : <span className="badge bg-secondary">{item.oosReason || "Unavailable"}</span>}
                                        <div className="text-muted small">
                                            {item.stockResetType === "UNLIMITED"
                                                ? "Always available"
                                                : `${item.stockQuantity ?? 0} left`}
                                        </div>
                                    </td>
                                    <td className="text-end">
                                        <div className="btn-group btn-group-sm">
                                            <button className="btn btn-outline-secondary" disabled={busyId === item.id}
                                                onClick={() => startEdit(item)}>Edit</button>
                                            <button className="btn btn-outline-secondary" disabled={busyId === item.id}
                                                onClick={() => toggleAvailability(item)}>
                                                {item.inStock ? "Mark unavailable" : "Mark available"}
                                            </button>
                                            <button className="btn btn-outline-danger" disabled={busyId === item.id}
                                                onClick={() => remove(item)}>Remove</button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default MenuManager;
