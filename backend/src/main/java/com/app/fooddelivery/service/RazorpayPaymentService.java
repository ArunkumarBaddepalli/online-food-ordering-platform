package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.model.Payment;
import com.app.fooddelivery.repository.OrderRepository;
import com.app.fooddelivery.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Online payment via Razorpay.
 *
 * The flow is the standard one:
 *   1. createCheckout  - we ask Razorpay for a payment order and hand the id
 *                        to the browser, which opens the Razorpay popup.
 *   2. confirmPayment  - the browser returns a signature, which we verify
 *                        with our secret before marking anything as paid.
 *
 * Step 2 matters: without verifying the signature server-side, a client could
 * simply claim it had paid.
 */
@Service
public class RazorpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key.id:}")
    private String keyId;

    @Value("${razorpay.key.secret:}")
    private String keySecret;

    public RazorpayPaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    /** Online payment is only offered when credentials are configured. */
    public boolean isEnabled() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    public String getKeyId() {
        return keyId;
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new RuntimeException("Online payment is not configured. Please choose cash on delivery.");
        }
    }

    /**
     * Creates a Razorpay order for one of our orders and returns everything the
     * browser needs to open the checkout popup.
     */
    @Transactional
    public Map<String, Object> createCheckout(Long orderId) {
        requireEnabled();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new RuntimeException("Order has no payment record");
        }
        if ("PAID".equals(payment.getPaymentStatus())) {
            throw new RuntimeException("This order has already been paid for");
        }
        // A checkout page left open while the order was cancelled must not be
        // able to take money for food nobody is going to cook.
        if ("CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("This order was cancelled, so it cannot be paid for.");
        }

        // Razorpay works in the smallest currency unit, so rupees become paise.
        long amountInPaise = Math.round(order.getTotalAmount() * 100);

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject request = new JSONObject();
            request.put("amount", amountInPaise);
            request.put("currency", "INR");
            request.put("receipt", "order_" + order.getId());

            com.razorpay.Order rzpOrder = client.orders.create(request);
            String rzpOrderId = rzpOrder.get("id");

            payment.setPaymentMethod("ONLINE");
            payment.setPaymentStatus("PENDING");
            payment.setRazorpayOrderId(rzpOrderId);
            paymentRepository.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("razorpayOrderId", rzpOrderId);
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("keyId", keyId);
            response.put("orderId", order.getId());
            return response;

        } catch (Exception e) {
            log.error("Could not create Razorpay order for order {}", orderId, e);
            throw new RuntimeException("Could not start the payment. Please try again.");
        }
    }

    /**
     * Verifies the signature Razorpay sent back and marks the payment paid.
     * A bad signature leaves the payment untouched.
     */
    @Transactional
    public Payment confirmPayment(Long orderId, String razorpayOrderId, String razorpayPaymentId, String signature) {
        requireEnabled();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new RuntimeException("Order has no payment record");
        }

        if (!razorpayOrderId.equals(payment.getRazorpayOrderId())) {
            throw new RuntimeException("This payment does not belong to that order");
        }

        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", signature);

            if (!Utils.verifyPaymentSignature(attributes, keySecret)) {
                // Deliberately left PENDING rather than FAILED. A rejected
                // signature may be a forgery attempt against someone else's
                // order, and the customer must still be able to pay.
                log.warn("Rejected an unverified payment for order {}", orderId);
                throw new RuntimeException("Payment could not be verified");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Signature check failed for order {}", orderId, e);
            throw new RuntimeException("Payment could not be verified");
        }

        payment.setPaymentMethod("ONLINE");
        payment.setPaymentStatus("PAID");
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Order {} paid online, razorpay payment {}", orderId, razorpayPaymentId);

        // The order can be cancelled between opening the checkout and paying.
        // The money is genuinely taken by then, so record it as paid and give it
        // straight back rather than keeping it for an order nobody will cook.
        if ("CANCELLED".equals(order.getStatus())) {
            log.warn("Order {} was cancelled before payment landed. Refunding.", orderId);
            refund(payment);
            paymentRepository.save(payment);
        }

        return payment;
    }

    /**
     * Refunds a payment that was actually taken.
     *
     * Returns true only when Razorpay accepted the refund. Anything else leaves
     * the payment marked for manual attention rather than silently claiming the
     * customer got their money back.
     */
    public boolean refund(Payment payment) {
        if (payment == null
                || !"ONLINE".equals(payment.getPaymentMethod())
                || !"PAID".equals(payment.getPaymentStatus())) {
            return false;
        }

        if (!isEnabled() || payment.getRazorpayPaymentId() == null) {
            payment.setPaymentStatus("REFUND_PENDING");
            paymentRepository.save(payment);
            log.warn("Cannot reach Razorpay to refund payment {}; left as REFUND_PENDING", payment.getId());
            return false;
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject request = new JSONObject();
            request.put("amount", Math.round(payment.getAmount() * 100));
            request.put("speed", "normal");

            client.payments.refund(payment.getRazorpayPaymentId(), request);

            payment.setPaymentStatus("REFUNDED");
            paymentRepository.save(payment);
            log.info("Refunded payment {} for razorpay payment {}", payment.getId(),
                    payment.getRazorpayPaymentId());
            return true;

        } catch (Exception e) {
            payment.setPaymentStatus("REFUND_PENDING");
            paymentRepository.save(payment);
            log.error("Refund failed for payment {}, left as REFUND_PENDING", payment.getId(), e);
            return false;
        }
    }
}
