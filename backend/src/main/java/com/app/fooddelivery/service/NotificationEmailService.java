package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.model.Payment;
import com.app.fooddelivery.model.User;
import com.app.fooddelivery.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Tells people when something happened that they would otherwise only discover
 * by logging in and looking.
 */
@Service
public class NotificationEmailService {

    private final EmailService email;
    private final UserRepository userRepository;

    public NotificationEmailService(EmailService email, UserRepository userRepository) {
        this.email = email;
        this.userRepository = userRepository;
    }

    private String nameOf(User user) {
        return user == null || user.getName() == null || user.getName().isBlank()
                ? "there"
                : user.getName();
    }

    private String money(Double amount) {
        return String.format("$%.2f", amount == null ? 0.0 : amount);
    }

    // --- Restaurant partners ------------------------------------------------

    public void restaurantApproved(Long applicantUserId, String restaurantName) {
        userRepository.findById(applicantUserId).ifPresent(user ->
                email.send(user.getEmail(), "Your restaurant has been approved",
                        email.layout("You're approved", """
                                <p>Hello %s,</p>
                                <p><strong>%s</strong> has been approved and is now on the platform.</p>
                                <p>Next: add your menu and open for orders. Nothing can be ordered
                                   until there are dishes on the menu.</p>
                                %s
                                """.formatted(
                                nameOf(user), restaurantName,
                                email.button(email.baseUrl() + "/partner/dashboard", "Open my dashboard")))));
    }

    public void restaurantRejected(Long applicantUserId, String restaurantName, String reason) {
        userRepository.findById(applicantUserId).ifPresent(user ->
                email.send(user.getEmail(), "About your restaurant application",
                        email.layout("Your application was not approved", """
                                <p>Hello %s,</p>
                                <p>We were unable to approve <strong>%s</strong>.</p>
                                <p style="background:#f8f9fa;border-left:3px solid #dc3545;padding:12px">%s</p>
                                <p>You can start a new application addressing this.</p>
                                %s
                                """.formatted(
                                nameOf(user), restaurantName,
                                reason == null ? "No reason was given." : reason,
                                email.button(email.baseUrl() + "/partner/onboard/status", "View my application")))));
    }

    public void documentsRequested(Long applicantUserId, String restaurantName, String reason) {
        userRepository.findById(applicantUserId).ifPresent(user ->
                email.send(user.getEmail(), "More documents needed for your application",
                        email.layout("We need a little more", """
                                <p>Hello %s,</p>
                                <p>Before we can approve <strong>%s</strong>, we need the following.</p>
                                <p style="background:#f8f9fa;border-left:3px solid #0dcaf0;padding:12px">%s</p>
                                %s
                                """.formatted(
                                nameOf(user), restaurantName,
                                reason == null ? "Please check your documents." : reason,
                                email.button(email.baseUrl() + "/partner/onboard", "Update my documents")))));
    }

    // --- Customers ----------------------------------------------------------

    public void orderCompleted(Order order) {
        User customer = order.getUser();
        if (customer == null) {
            return;
        }

        boolean collected = "PICKED_UP".equals(order.getStatus());
        String restaurant = order.getRestaurant() == null ? "the restaurant" : order.getRestaurant().getName();

        email.send(customer.getEmail(),
                collected ? "Your order has been collected" : "Your order has been delivered",
                email.layout(collected ? "Enjoy your food" : "Your order has arrived", """
                        <p>Hello %s,</p>
                        <p>Order <strong>#%d</strong> from <strong>%s</strong> has been %s.</p>
                        <p>Total: <strong>%s</strong></p>
                        <p>If it was good, a rating helps other people choose.</p>
                        %s
                        """.formatted(
                        nameOf(customer), order.getId(), restaurant,
                        collected ? "collected" : "delivered",
                        money(order.getTotalAmount()),
                        email.button(email.baseUrl() + "/orders/" + order.getId(), "Rate this order"))));
    }

    public void orderCancelled(Order order, boolean refundIssued) {
        User customer = order.getUser();
        if (customer == null) {
            return;
        }

        Payment payment = order.getPayment();
        String restaurant = order.getRestaurant() == null ? "the restaurant" : order.getRestaurant().getName();

        // Only promise a refund when one was actually taken and returned.
        String moneyLine;
        if (refundIssued) {
            moneyLine = """
                    <p style="background:#f8f9fa;border-left:3px solid #198754;padding:12px">
                      A refund of <strong>%s</strong> has been issued to your original payment method.
                      Banks usually take five to seven working days to show it.
                    </p>
                    """.formatted(money(payment == null ? order.getTotalAmount() : payment.getAmount()));
        } else if (payment != null && "REFUND_PENDING".equals(payment.getPaymentStatus())) {
            moneyLine = """
                    <p style="background:#f8f9fa;border-left:3px solid #ffc107;padding:12px">
                      Your refund of <strong>%s</strong> is being processed and we will confirm
                      once it has been sent.
                    </p>
                    """.formatted(money(payment.getAmount()));
        } else {
            moneyLine = "<p>Nothing was charged for this order.</p>";
        }

        email.send(customer.getEmail(), "Your order has been cancelled",
                email.layout("Order cancelled", """
                        <p>Hello %s,</p>
                        <p>Order <strong>#%d</strong> from <strong>%s</strong> has been cancelled.</p>
                        %s
                        %s
                        """.formatted(
                        nameOf(customer), order.getId(), restaurant, moneyLine,
                        email.button(email.baseUrl() + "/", "Order something else"))));
    }
}
