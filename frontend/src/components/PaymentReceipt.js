import React from "react";
import "./PaymentReceipt.css";

const money = (n) => `$${(n ?? 0).toFixed(2)}`;

const when = (ts) => (ts ? new Date(ts).toLocaleString() : "—");

/**
 * Receipt for an order that was paid online.
 *
 * Cash orders deliberately do not get one: the money is handed over at the
 * door and the restaurant provides its own bill, so issuing a "receipt" here
 * would be claiming a payment we never took.
 */
const PaymentReceipt = ({ order, onClose }) => {
    const payment = order?.payment;
    const items = order?.orderItems || [];

    if (!payment) return null;

    return (
        <div className="receipt-backdrop" onClick={onClose}>
            <div className="receipt-sheet" onClick={(e) => e.stopPropagation()}>
                <div className="receipt-actions no-print">
                    <button className="btn btn-success btn-sm" onClick={() => window.print()}>
                        Download / Print
                    </button>
                    <button className="btn btn-outline-secondary btn-sm ms-2" onClick={onClose}>
                        Close
                    </button>
                </div>

                <div className="receipt-body">
                    <div className="receipt-head">
                        <h4 className="mb-1">Payment Receipt</h4>
                        <div className="text-muted small">Receipt for order #{order.id}</div>
                    </div>

                    <div className="receipt-meta">
                        <div>
                            <strong>{order.restaurant?.name || "Restaurant"}</strong>
                            <div className="text-muted small">{order.restaurant?.address}</div>
                        </div>
                        <div className="text-end">
                            <div className="text-muted small">Paid on</div>
                            <div>{when(payment.paymentDate)}</div>
                        </div>
                    </div>

                    <table className="receipt-table">
                        <thead>
                            <tr>
                                <th>Item</th>
                                <th className="text-center">Qty</th>
                                <th className="text-end">Amount</th>
                            </tr>
                        </thead>
                        <tbody>
                            {items.map((item) => (
                                <tr key={item.id}>
                                    <td>
                                        {item.foodItem?.name}
                                        {(item.selectedModifiers || []).length > 0 && (
                                            <div className="text-muted small">
                                                {item.selectedModifiers.map((m) => m.modifierName).join(", ")}
                                            </div>
                                        )}
                                    </td>
                                    <td className="text-center">{item.quantity}</td>
                                    <td className="text-end">{money(item.price)}</td>
                                </tr>
                            ))}
                        </tbody>
                        <tfoot>
                            <tr>
                                <th colSpan="2">Total paid</th>
                                <th className="text-end">{money(payment.amount ?? order.totalAmount)}</th>
                            </tr>
                        </tfoot>
                    </table>

                    <div className="receipt-payment">
                        <div><span className="text-muted">Method</span><span>Online payment</span></div>
                        <div><span className="text-muted">Status</span><span>{payment.paymentStatus}</span></div>
                        {payment.razorpayPaymentId && (
                            <div>
                                <span className="text-muted">Transaction</span>
                                <span className="font-monospace">{payment.razorpayPaymentId}</span>
                            </div>
                        )}
                        <div><span className="text-muted">Order type</span><span>{order.orderType}</span></div>
                        {order.deliveryAddress && (
                            <div><span className="text-muted">Delivered to</span><span>{order.deliveryAddress}</span></div>
                        )}
                    </div>

                    <p className="receipt-note text-muted small">
                        This is a computer generated receipt and does not require a signature.
                    </p>
                </div>
            </div>
        </div>
    );
};

export default PaymentReceipt;
