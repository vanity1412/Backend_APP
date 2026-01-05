package com.utetea.backend.service;

import com.utetea.backend.model.DiscountType;
import com.utetea.backend.model.Order;
import com.utetea.backend.model.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final SendGridEmailService sendGridEmailService;
    private final MemberTierService memberTierService;
    
    /**
     * Gửi email xác nhận đơn hàng - ưu tiên SendGrid, fallback SMTP
     */
    @Async
    public void sendOrderConfirmationEmail(Order order) {
        String toEmail = order.getUser().getEmail();
        String subject = "Xác nhận đơn hàng #" + order.getId() + " - UTE Tea";
        String htmlContent = buildOrderConfirmationEmailContent(order);
        
        // Try SendGrid first
        if (sendGridEmailService.isEnabled()) {
            log.info("Sending order confirmation via SendGrid to: {}", toEmail);
            if (sendGridEmailService.sendHtmlEmail(toEmail, subject, htmlContent)) {
                log.info("Order confirmation email sent successfully via SendGrid to: {}", toEmail);
                return;
            }
            log.warn("SendGrid failed, falling back to SMTP...");
        }
        
        // Fallback to SMTP
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Order confirmation email sent successfully via SMTP to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send order confirmation email to: {}", toEmail, e);
        } catch (Exception e) {
            log.error("Unexpected error sending order confirmation email to: {}", toEmail, e);
        }
    }
    
    /**
     * Gửi email hoàn thành đơn hàng - ưu tiên SendGrid, fallback SMTP
     */
    @Async
    public void sendOrderCompletionEmail(Order order) {
        String toEmail = order.getUser().getEmail();
        String subject = "Đơn hàng #" + order.getId() + " đã hoàn thành - UTE Tea";
        String htmlContent = buildOrderCompletionEmailContent(order);
        
        // Try SendGrid first
        if (sendGridEmailService.isEnabled()) {
            log.info("Sending order completion via SendGrid to: {}", toEmail);
            if (sendGridEmailService.sendHtmlEmail(toEmail, subject, htmlContent)) {
                log.info("Order completion email sent successfully via SendGrid to: {}", toEmail);
                return;
            }
            log.warn("SendGrid failed, falling back to SMTP...");
        }
        
        // Fallback to SMTP
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Order completion email sent successfully via SMTP to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send order completion email to: {}", toEmail, e);
        } catch (Exception e) {
            log.error("Unexpected error sending order completion email to: {}", toEmail, e);
        }
    }
    
    private String buildOrderConfirmationEmailContent(Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html>");
        content.append("<head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".header { background-color: #FF9800; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }");
        content.append(".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }");
        content.append(".order-info { background-color: white; padding: 15px; margin: 10px 0; border-radius: 5px; }");
        content.append(".item { border-bottom: 1px solid #eee; padding: 10px 0; }");
        content.append(".item:last-child { border-bottom: none; }");
        content.append(".total { font-size: 18px; font-weight: bold; color: #FF9800; margin-top: 15px; }");
        content.append(".footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }");
        content.append(".thank-you { background-color: #fff3cd; padding: 15px; margin: 15px 0; border-left: 4px solid #FF9800; border-radius: 5px; }");
        content.append("</style>");
        content.append("</head>");
        content.append("<body>");
        content.append("<div class='container'>");
        
        // Header
        content.append("<div class='header'>");
        content.append("<h1>🎉 Đặt hàng thành công!</h1>");
        content.append("</div>");
        
        // Content
        content.append("<div class='content'>");
        content.append("<p>Xin chào <strong>").append(order.getUser().getFullName()).append("</strong>,</p>");
        content.append("<p>Cảm ơn bạn đã đặt hàng tại UTE Tea! Đơn hàng của bạn đã được tiếp nhận và đang được xử lý.</p>");
        
        // Order Info
        content.append("<div class='order-info'>");
        content.append("<h3>Thông tin đơn hàng #").append(order.getId()).append("</h3>");
        content.append("<p><strong>Khách hàng:</strong> ").append(order.getUser().getFullName()).append("</p>");
        if (order.getUser().getPhone() != null && !order.getUser().getPhone().isEmpty()) {
            content.append("<p><strong>Số điện thoại:</strong> ").append(order.getUser().getPhone()).append("</p>");
        }
        content.append("<p><strong>Cửa hàng:</strong> ").append(order.getStore().getStoreName()).append("</p>");
        content.append("<p><strong>Loại đơn:</strong> ").append(order.getType() == com.utetea.backend.model.OrderType.DELIVERY ? "Giao hàng" : "Lấy tại cửa hàng").append("</p>");
        content.append("<p><strong>Địa chỉ:</strong> ").append(order.getAddress()).append("</p>");
        
        if (order.getPickupTime() != null) {
            content.append("<p><strong>Thời gian:</strong> ").append(order.getPickupTime().format(formatter)).append("</p>");
        }
        
        content.append("<p><strong>Phương thức thanh toán:</strong> ");
        content.append(getPaymentMethodDisplayName(order.getPaymentMethod()));
        content.append("</p>");
        content.append("<p><strong>Trạng thái:</strong> <span style='color: #FF9800;'>Đang chờ xử lý</span></p>");
        content.append("</div>");
        
        // Order Items
        content.append("<div class='order-info'>");
        content.append("<h3>Chi tiết sản phẩm</h3>");
        
        for (OrderItem item : order.getItems()) {
            content.append("<div class='item'>");
            content.append("<p><strong>").append(item.getDrinkNameSnapshot()).append("</strong></p>");
            content.append("<p>Size: ").append(item.getSizeNameSnapshot()).append("</p>");
            
            if (item.getToppings() != null && !item.getToppings().isEmpty()) {
                content.append("<p>Topping: ");
                content.append(item.getToppings().stream()
                    .map(t -> t.getToppingNameSnapshot())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
                content.append("</p>");
            }
            
            if (item.getNote() != null && !item.getNote().isEmpty()) {
                content.append("<p><em>Ghi chú: ").append(item.getNote()).append("</em></p>");
            }
            
            content.append("<p>Số lượng: ").append(item.getQuantity()).append(" x ");
            content.append(formatPrice(item.getItemPrice().divide(BigDecimal.valueOf(item.getQuantity()), 0, java.math.RoundingMode.HALF_UP)));
            content.append(" = <strong>").append(formatPrice(item.getItemPrice())).append("</strong></p>");
            content.append("</div>");
        }
        content.append("</div>");
        
        // Price Summary với chi tiết giảm giá
        content.append(buildPriceSummaryHtml(order, "#FF9800"));
        
        // Thank you message
        content.append("<div class='thank-you' style='background-color: #fff3cd; padding: 15px; margin: 15px 0; border-left: 4px solid #FF9800; border-radius: 5px;'>");
        content.append("<p style='margin: 0;'><strong>💝 Cảm ơn bạn đã tin tưởng và lựa chọn UTE Tea!</strong></p>");
        content.append("<p style='margin: 5px 0 0 0;'>Chúng tôi sẽ chuẩn bị đơn hàng của bạn một cách tốt nhất. Hẹn gặp lại bạn!</p>");
        content.append("</div>");
        
        content.append("<p>Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi.</p>");
        content.append("<p>Trân trọng,<br><strong>UTE Tea Team</strong></p>");
        content.append("</div>");
        
        // Footer
        content.append("<div class='footer'>");
        content.append("<p>Email này được gửi tự động, vui lòng không trả lời.</p>");
        content.append("<p>&copy; 2024 UTE Tea. All rights reserved.</p>");
        content.append("</div>");
        
        content.append("</div>");
        content.append("</body>");
        content.append("</html>");
        
        return content.toString();
    }
    
    private String buildOrderCompletionEmailContent(Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html>");
        content.append("<head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }");
        content.append(".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }");
        content.append(".order-info { background-color: white; padding: 15px; margin: 10px 0; border-radius: 5px; }");
        content.append(".item { border-bottom: 1px solid #eee; padding: 10px 0; }");
        content.append(".item:last-child { border-bottom: none; }");
        content.append(".total { font-size: 18px; font-weight: bold; color: #4CAF50; margin-top: 15px; }");
        content.append(".footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }");
        content.append("</style>");
        content.append("</head>");
        content.append("<body>");
        content.append("<div class='container'>");
        
        // Header
        content.append("<div class='header'>");
        content.append("<h1>✅ Đơn hàng đã hoàn thành!</h1>");
        content.append("</div>");
        
        // Content
        content.append("<div class='content'>");
        content.append("<p>Xin chào <strong>").append(order.getUser().getFullName()).append("</strong>,</p>");
        content.append("<p>Đơn hàng của bạn đã được hoàn thành thành công. Cảm ơn bạn đã sử dụng dịch vụ của UTE Tea!</p>");
        
        // Order Info
        content.append("<div class='order-info'>");
        content.append("<h3>Thông tin đơn hàng #").append(order.getId()).append("</h3>");
        content.append("<p><strong>Khách hàng:</strong> ").append(order.getUser().getFullName()).append("</p>");
        if (order.getUser().getPhone() != null && !order.getUser().getPhone().isEmpty()) {
            content.append("<p><strong>Số điện thoại:</strong> ").append(order.getUser().getPhone()).append("</p>");
        }
        content.append("<p><strong>Cửa hàng:</strong> ").append(order.getStore().getStoreName()).append("</p>");
        content.append("<p><strong>Loại đơn:</strong> ").append(order.getType() == com.utetea.backend.model.OrderType.DELIVERY ? "Giao hàng" : "Lấy tại cửa hàng").append("</p>");
        content.append("<p><strong>Địa chỉ:</strong> ").append(order.getAddress()).append("</p>");
        
        if (order.getPickupTime() != null) {
            content.append("<p><strong>Thời gian:</strong> ").append(order.getPickupTime().format(formatter)).append("</p>");
        }
        
        content.append("<p><strong>Phương thức thanh toán:</strong> ");
        content.append(getPaymentMethodDisplayName(order.getPaymentMethod()));
        content.append("</p>");
        content.append("</div>");
        
        // Order Items
        content.append("<div class='order-info'>");
        content.append("<h3>Chi tiết sản phẩm</h3>");
        
        for (OrderItem item : order.getItems()) {
            content.append("<div class='item'>");
            content.append("<p><strong>").append(item.getDrinkNameSnapshot()).append("</strong></p>");
            content.append("<p>Size: ").append(item.getSizeNameSnapshot()).append("</p>");
            
            if (item.getToppings() != null && !item.getToppings().isEmpty()) {
                content.append("<p>Topping: ");
                content.append(item.getToppings().stream()
                    .map(t -> t.getToppingNameSnapshot())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
                content.append("</p>");
            }
            
            if (item.getNote() != null && !item.getNote().isEmpty()) {
                content.append("<p><em>Ghi chú: ").append(item.getNote()).append("</em></p>");
            }
            
            content.append("<p>Số lượng: ").append(item.getQuantity()).append(" x ");
            content.append(formatPrice(item.getItemPrice().divide(BigDecimal.valueOf(item.getQuantity()), 0, java.math.RoundingMode.HALF_UP)));
            content.append(" = <strong>").append(formatPrice(item.getItemPrice())).append("</strong></p>");
            content.append("</div>");
        }
        content.append("</div>");
        
        // Price Summary với chi tiết giảm giá
        content.append(buildPriceSummaryHtml(order, "#4CAF50"));
        
        // Thank you message
        content.append("<div class='thank-you' style='background-color: #fff3cd; padding: 15px; margin: 15px 0; border-left: 4px solid #4CAF50; border-radius: 5px;'>");
        content.append("<p style='margin: 0;'><strong>💝 Cảm ơn bạn đã sử dụng dịch vụ của UTE Tea!</strong></p>");
        content.append("<p style='margin: 5px 0 0 0;'>Hy vọng bạn hài lòng với sản phẩm và dịch vụ của chúng tôi. Hẹn gặp lại bạn lần sau!</p>");
        content.append("</div>");
        
        content.append("<p>Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi.</p>");
        content.append("<p>Trân trọng,<br><strong>UTE Tea Team</strong></p>");
        content.append("</div>");
        
        // Footer
        content.append("<div class='footer'>");
        content.append("<p>Email này được gửi tự động, vui lòng không trả lời.</p>");
        content.append("<p>&copy; 2024 UTE Tea. All rights reserved.</p>");
        content.append("</div>");
        
        content.append("</div>");
        content.append("</body>");
        content.append("</html>");
        
        return content.toString();
    }
    
    private String formatPrice(BigDecimal price) {
        return String.format("%,d VND", price.longValue());
    }
    
    /**
     * Tính giảm giá từ voucher
     */
    private BigDecimal calculateVoucherDiscount(Order order) {
        if (order.getPromotion() == null) {
            return BigDecimal.ZERO;
        }
        
        var promotion = order.getPromotion();
        BigDecimal totalPrice = order.getTotalPrice();
        
        if (promotion.getDiscountType() == DiscountType.PERCENT) {
            BigDecimal discount = totalPrice.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));
            if (promotion.getMaxDiscountAmount() != null && 
                discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discount = promotion.getMaxDiscountAmount();
            }
            return discount;
        } else {
            return promotion.getDiscountValue();
        }
    }
    
    /**
     * Tính giảm giá từ hạng thành viên
     */
    private BigDecimal calculateTierDiscountFromOrder(Order order) {
        BigDecimal totalDiscount = order.getDiscount();
        BigDecimal voucherDiscount = calculateVoucherDiscount(order);
        
        // Tier discount = Total discount - Voucher discount
        BigDecimal tierDiscount = totalDiscount.subtract(voucherDiscount);
        return tierDiscount.compareTo(BigDecimal.ZERO) > 0 ? tierDiscount : BigDecimal.ZERO;
    }
    
    /**
     * Build Price Summary HTML với chi tiết giảm giá và phí ship
     */
    private String buildPriceSummaryHtml(Order order, String totalColor) {
        StringBuilder content = new StringBuilder();
        content.append("<div class='order-info'>");
        content.append("<p>Tổng tiền hàng: ").append(formatPrice(order.getTotalPrice())).append("</p>");
        
        // Phí ship (nếu là giao hàng)
        BigDecimal shippingFee = order.getShippingFee();
        if (shippingFee != null && shippingFee.compareTo(BigDecimal.ZERO) > 0) {
            content.append("<p>🚚 Phí giao hàng: <strong>").append(formatPrice(shippingFee)).append("</strong></p>");
        }
        
        BigDecimal totalDiscount = order.getDiscount();
        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            // Giảm giá từ voucher
            BigDecimal voucherDiscount = calculateVoucherDiscount(order);
            if (voucherDiscount.compareTo(BigDecimal.ZERO) > 0 && order.getPromotion() != null) {
                content.append("<p style='color: #E91E63;'>🎫 Giảm giá voucher (")
                       .append(order.getPromotion().getCode())
                       .append("): <strong>-").append(formatPrice(voucherDiscount)).append("</strong></p>");
            }
            
            // Giảm giá từ hạng thành viên
            BigDecimal tierDiscount = calculateTierDiscountFromOrder(order);
            if (tierDiscount.compareTo(BigDecimal.ZERO) > 0) {
                String tierName = order.getUser().getMemberTier() != null ? 
                        order.getUser().getMemberTier().name() : "BRONZE";
                content.append("<p style='color: #FFB300;'>👑 Ưu đãi hạng ")
                       .append(tierName)
                       .append(": <strong>-").append(formatPrice(tierDiscount)).append("</strong></p>");
            }
        }
        
        content.append("<p class='total' style='color: ").append(totalColor).append(";'>Thành tiền: ")
               .append(formatPrice(order.getFinalPrice())).append("</p>");
        content.append("</div>");
        
        return content.toString();
    }
    
    /**
     * Lấy tên hiển thị của phương thức thanh toán
     */
    private String getPaymentMethodDisplayName(com.utetea.backend.model.PaymentMethod paymentMethod) {
        if (paymentMethod == null) return "Khác";
        switch (paymentMethod) {
            case COD: return "Tiền mặt";
            case VNPAY: return "VNPay";
            case VIETQR: return "VietQR";
            case MOMO: return "MoMo";
            case PAYPAL: return "PayPal";
            default: return "Khác";
        }
    }
    
    /**
     * Gửi email xác nhận thanh toán thành công - ưu tiên SendGrid, fallback SMTP
     */
    @Async
    public void sendPaymentSuccessEmail(Order order) {
        String toEmail = order.getUser().getEmail();
        String subject = "Thanh toán thành công - Đơn hàng #" + order.getId() + " - UTE Tea";
        String htmlContent = buildPaymentSuccessEmailContent(order);
        
        // Try SendGrid first
        if (sendGridEmailService.isEnabled()) {
            log.info("Sending payment success email via SendGrid to: {}", toEmail);
            if (sendGridEmailService.sendHtmlEmail(toEmail, subject, htmlContent)) {
                log.info("Payment success email sent successfully via SendGrid to: {}", toEmail);
                return;
            }
            log.warn("SendGrid failed, falling back to SMTP...");
        }
        
        // Fallback to SMTP
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Payment success email sent successfully via SMTP to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send payment success email to: {}", toEmail, e);
        } catch (Exception e) {
            log.error("Unexpected error sending payment success email to: {}", toEmail, e);
        }
    }
    
    private String buildPaymentSuccessEmailContent(Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html>");
        content.append("<head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".header { background-color: #2196F3; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }");
        content.append(".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }");
        content.append(".order-info { background-color: white; padding: 15px; margin: 10px 0; border-radius: 5px; }");
        content.append(".item { border-bottom: 1px solid #eee; padding: 10px 0; }");
        content.append(".item:last-child { border-bottom: none; }");
        content.append(".total { font-size: 18px; font-weight: bold; color: #2196F3; margin-top: 15px; }");
        content.append(".payment-badge { background-color: #4CAF50; color: white; padding: 5px 15px; border-radius: 20px; display: inline-block; }");
        content.append(".footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }");
        content.append(".thank-you { background-color: #e3f2fd; padding: 15px; margin: 15px 0; border-left: 4px solid #2196F3; border-radius: 5px; }");
        content.append("</style>");
        content.append("</head>");
        content.append("<body>");
        content.append("<div class='container'>");
        
        // Header
        content.append("<div class='header'>");
        content.append("<h1>💳 Thanh toán thành công!</h1>");
        content.append("</div>");
        
        // Content
        content.append("<div class='content'>");
        content.append("<p>Xin chào <strong>").append(order.getUser().getFullName()).append("</strong>,</p>");
        content.append("<p>Chúng tôi xác nhận đã nhận được thanh toán cho đơn hàng của bạn.</p>");
        
        // Payment Info
        content.append("<div class='order-info'>");
        content.append("<h3>💰 Thông tin thanh toán</h3>");
        content.append("<p><strong>Mã đơn hàng:</strong> #").append(order.getId()).append("</p>");
        content.append("<p><strong>Phương thức:</strong> <span class='payment-badge'>").append(getPaymentMethodDisplayName(order.getPaymentMethod())).append("</span></p>");
        content.append("<p><strong>Số tiền:</strong> <span style='color: #4CAF50; font-weight: bold;'>").append(formatPrice(order.getFinalPrice())).append("</span></p>");
        content.append("<p><strong>Thời gian:</strong> ").append(java.time.LocalDateTime.now().format(formatter)).append("</p>");
        content.append("<p><strong>Trạng thái:</strong> <span style='color: #4CAF50;'>✓ Đã thanh toán</span></p>");
        content.append("</div>");
        
        // Order Info
        content.append("<div class='order-info'>");
        content.append("<h3>📦 Thông tin đơn hàng</h3>");
        content.append("<p><strong>Khách hàng:</strong> ").append(order.getUser().getFullName()).append("</p>");
        if (order.getUser().getPhone() != null && !order.getUser().getPhone().isEmpty()) {
            content.append("<p><strong>Số điện thoại:</strong> ").append(order.getUser().getPhone()).append("</p>");
        }
        content.append("<p><strong>Cửa hàng:</strong> ").append(order.getStore().getStoreName()).append("</p>");
        content.append("<p><strong>Loại đơn:</strong> ").append(order.getType() == com.utetea.backend.model.OrderType.DELIVERY ? "Giao hàng" : "Lấy tại cửa hàng").append("</p>");
        if (order.getAddress() != null && !order.getAddress().isEmpty()) {
            content.append("<p><strong>Địa chỉ:</strong> ").append(order.getAddress()).append("</p>");
        }
        content.append("</div>");
        
        // Order Items
        content.append("<div class='order-info'>");
        content.append("<h3>🧋 Chi tiết sản phẩm</h3>");
        
        for (OrderItem item : order.getItems()) {
            content.append("<div class='item'>");
            content.append("<p><strong>").append(item.getDrinkNameSnapshot()).append("</strong> (").append(item.getSizeNameSnapshot()).append(")</p>");
            
            if (item.getToppings() != null && !item.getToppings().isEmpty()) {
                content.append("<p style='color: #666; font-size: 14px;'>+ ");
                content.append(item.getToppings().stream()
                    .map(t -> t.getToppingNameSnapshot())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
                content.append("</p>");
            }
            
            if (item.getNote() != null && !item.getNote().isEmpty()) {
                content.append("<p><em style='color: #FF9800;'>📝 Ghi chú: ").append(item.getNote()).append("</em></p>");
            }
            
            content.append("<p>x").append(item.getQuantity()).append(" = <strong>").append(formatPrice(item.getItemPrice())).append("</strong></p>");
            content.append("</div>");
        }
        content.append("</div>");
        
        // Price Summary với chi tiết giảm giá
        content.append(buildPriceSummaryHtml(order, "#2196F3"));
        
        // Thank you message
        content.append("<div class='thank-you'>");
        content.append("<p style='margin: 0;'><strong>🎉 Cảm ơn bạn đã thanh toán!</strong></p>");
        content.append("<p style='margin: 5px 0 0 0;'>Đơn hàng của bạn đang được chuẩn bị. Chúng tôi sẽ thông báo khi đơn hàng sẵn sàng.</p>");
        content.append("</div>");
        
        content.append("<p>Trân trọng,<br><strong>UTE Tea Team</strong></p>");
        content.append("</div>");
        
        // Footer
        content.append("<div class='footer'>");
        content.append("<p>Email này được gửi tự động, vui lòng không trả lời.</p>");
        content.append("<p>&copy; 2024 UTE Tea. All rights reserved.</p>");
        content.append("</div>");
        
        content.append("</div>");
        content.append("</body>");
        content.append("</html>");
        
        return content.toString();
    }
}
