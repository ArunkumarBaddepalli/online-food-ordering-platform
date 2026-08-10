import { toast } from "react-toastify";
import React, { useState, useEffect } from "react";


const ModifierSelectionModal = ({ foodItem, onClose, onAddToOrder }) => {
    // Use modifierGroups from props (foodItem)
    const [modifierGroups] = useState(foodItem.modifierGroups || []);
    const [selections, setSelections] = useState({});
    const [totalPrice, setTotalPrice] = useState(foodItem.price);
    const [quantity, setQuantity] = useState(1);

    // No need for useEffect fetch since we have data in foodItem

    // Recalculate total price whenever selections change
    useEffect(() => {
        let modifierTotal = 0;
        Object.values(selections).forEach(selectedIds => {
            if (Array.isArray(selectedIds)) {
                // Checkbox (multiple)
                selectedIds.forEach(modId => {
                    const mod = findModifier(modId);
                    if (mod) modifierTotal += mod.priceAdjustment;
                });
            } else {
                // Radio (single)
                const mod = findModifier(selectedIds);
                if (mod) modifierTotal += mod.priceAdjustment;
            }
        });
        setTotalPrice(foodItem.price + modifierTotal);
    }, [selections, modifierGroups, foodItem.price]);

    const findModifier = (modId) => {
        for (const group of modifierGroups) {
            const found = group.modifiers.find(m => m.id === parseInt(modId));
            if (found) return found;
        }
        return null;
    };

    const handleSelectionChange = (groupId, modId, type) => {
        setSelections(prev => {
            const newSelections = { ...prev };

            if (type === 'one') {
                // Radio button behavior
                newSelections[groupId] = modId;
            } else {
                // Checkbox behavior
                const currentGroupSelections = newSelections[groupId] || [];
                if (currentGroupSelections.includes(modId)) {
                    newSelections[groupId] = currentGroupSelections.filter(id => id !== modId);
                } else {
                    newSelections[groupId] = [...currentGroupSelections, modId];
                }
            }
            return newSelections;
        });
    };

    const validateSelections = () => {
        for (const group of modifierGroups) {
            const groupSelection = selections[group.id];

            // Check required / min selection
            if (group.required || group.minSelection > 0) {
                const count = Array.isArray(groupSelection) ? groupSelection.length : (groupSelection ? 1 : 0);
                if (count < group.minSelection) {
                    toast.warning(`Please select at least ${group.minSelection} option(s) for ${group.name}`);
                    return false;
                }
            }

            // Check max selection
            if (group.maxSelection > 0) {
                const count = Array.isArray(groupSelection) ? groupSelection.length : (groupSelection ? 1 : 0);
                if (count > group.maxSelection) {
                    toast.warning(`You can only select up to ${group.maxSelection} option(s) for ${group.name}`);
                    return false;
                }
            }
        }
        return true;
    };

    const handleSubmit = () => {
        if (!validateSelections()) return;

        // Collect all selected modifier IDs
        const allSelectedIds = [];
        Object.values(selections).forEach(val => {
            if (Array.isArray(val)) {
                allSelectedIds.push(...val);
            } else if (val) {
                allSelectedIds.push(val);
            }
        });

        onAddToOrder(foodItem, allSelectedIds, quantity);
    };



    return (
        <div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(5px)' }}>
            <div className="modal-dialog modal-dialog-centered modal-lg">
                <div className="modal-content border-0 shadow-lg" style={{ borderRadius: '16px', overflow: 'hidden' }}>

                    {/* Header */}
                    <div className="modal-header border-bottom-0 pb-0">
                        <div>
                            <h4 className="modal-title fw-bold">Customize {foodItem.name}</h4>
                            <p className="text-muted mb-0 small">{foodItem.description}</p>
                        </div>
                        <button type="button" className="btn-close" onClick={onClose} style={{ marginTop: '-10px' }}></button>
                    </div>

                    {/* Body */}
                    <div className="modal-body px-4 pt-3 pb-5" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
                        {modifierGroups.length === 0 ? (
                            <div className="text-center py-5 text-muted">
                                <p>No customization options available for this item.</p>
                            </div>
                        ) : (
                            modifierGroups.map((group, index) => (
                                <div key={group.id} className={`mb-4 ${index !== modifierGroups.length - 1 ? 'border-bottom pb-4' : ''}`}>
                                    <div className="d-flex justify-content-between align-items-center mb-3">
                                        <div>
                                            <h6 className="fw-bold mb-0">{group.name}</h6>
                                            <small className="text-muted">
                                                {group.required ? 'Required' : 'Optional'} •
                                                {group.minSelection === group.maxSelection && group.maxSelection === 1
                                                    ? " Select 1"
                                                    : ` Select ${group.minSelection} - ${group.maxSelection}`}
                                            </small>
                                        </div>
                                        {group.required && <span className="badge bg-light text-dark border">Required</span>}
                                    </div>

                                    <div className="d-flex flex-column gap-2">
                                        {group.modifiers.map(mod => {
                                            const isSelected = group.maxSelection === 1
                                                ? selections[group.id] === mod.id
                                                : (selections[group.id] || []).includes(mod.id);

                                            return (
                                                <div key={mod.id}
                                                    className={`card border-1 ${isSelected ? 'border-primary bg-light' : ''}`}
                                                    style={{ cursor: 'pointer', transition: 'all 0.2s' }}
                                                    onClick={() => mod.available && handleSelectionChange(group.id, mod.id, group.maxSelection === 1 ? 'one' : 'many')}
                                                >
                                                    <div className="card-body py-2 px-3 d-flex justify-content-between align-items-center">
                                                        <div className="form-check w-100 m-0">
                                                            <input
                                                                className="form-check-input"
                                                                type={group.maxSelection === 1 ? "radio" : "checkbox"}
                                                                name={`group-${group.id}`}
                                                                id={`mod-${mod.id}`}
                                                                checked={isSelected}
                                                                onChange={() => handleSelectionChange(group.id, mod.id, group.maxSelection === 1 ? 'one' : 'many')}
                                                                disabled={!mod.available}
                                                                style={{ cursor: 'pointer' }}
                                                            />
                                                            <label className="form-check-label w-100 ps-2" htmlFor={`mod-${mod.id}`} style={{ cursor: 'pointer' }}>
                                                                <span className={!mod.available ? 'text-muted text-decoration-line-through' : ''}>
                                                                    {mod.name}
                                                                </span>
                                                                {!mod.available && <span className="badge bg-secondary ms-2">Sold Out</span>}
                                                            </label>
                                                        </div>
                                                        <span className="fw-medium text-nowrap">
                                                            {mod.priceAdjustment > 0 ? `+$${mod.priceAdjustment.toFixed(2)}` :
                                                                mod.priceAdjustment < 0 ? `-$${Math.abs(mod.priceAdjustment).toFixed(2)}` : 'Free'}
                                                        </span>
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                </div>
                            ))
                        )}
                    </div>

                    {/* Footer */}
                    <div className="modal-footer border-top-0 d-flex justify-content-between p-3" style={{ boxShadow: '0 -4px 10px rgba(0,0,0,0.05)' }}>
                        <div className="d-flex align-items-center gap-3">
                            {/* Quantity Control */}
                            <div className="btn-group" role="group" aria-label="Quantity">
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary"
                                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                                    disabled={quantity <= 1}
                                >-</button>
                                <span className="btn btn-outline-secondary disabled text-dark border-top-0 border-bottom-0" style={{ minWidth: '40px' }}>
                                    {quantity}
                                </span>
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary"
                                    onClick={() => setQuantity(quantity + 1)}
                                >+</button>
                            </div>

                            <div className="d-flex align-items-center gap-2">
                                <span className="text-muted">Total:</span>
                                <span className="fs-4 fw-bold text-primary">${(totalPrice * quantity).toFixed(2)}</span>
                            </div>
                        </div>
                        <div className="d-flex gap-2">
                            <button type="button" className="btn btn-light" onClick={onClose} style={{ borderRadius: '8px', fontWeight: '500' }}>Cancel</button>
                            <button
                                type="button"
                                className="btn btn-primary px-4"
                                onClick={handleSubmit}
                                style={{ borderRadius: '8px', fontWeight: '600' }}
                            >
                                Add to Order - ${(totalPrice * quantity).toFixed(2)}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ModifierSelectionModal;
