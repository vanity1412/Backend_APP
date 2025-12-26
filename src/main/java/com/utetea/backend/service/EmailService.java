package com.utetea.backend.service;

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
    
    /**
     * FIX High #8: Gửi email async để không block request
     */
    @Async
    public void sendOrderConfirmationEmail(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Xác nhận đơn hàng #" + order.getId() + " - UTE Tea");
            helper.setText(buildOrderConfirmationEmailContent(order), true);
            
            mailSender.send(message);
            log.info("Order confirmation email sent successfully to: {}", order.getUser().getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send order confirmation email to: {}", order.getUser().getEmail(), e);
        }
    }
    
    /**
     * FIX High #8: Gửi email async để không block request
     */
    @Async
    public void sendOrderCompletionEmail(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Đơn hàng #" + order.getId() + " đã hoàn thành - UTE Tea");
            helper.setText(buildOrderCompletionEmailContent(order), true);
            
            mailSender.send(message);
            log.info("Order completion email sent successfully to: {}", order.getUser().getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send order completion email to: {}", order.getUser().getEmail(), e);
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
        content.append("<p><strong>Cửa hàng:</strong> ").append(order.getStore().getStoreName()).append("</p>");
        content.append("<p><strong>Loại đơn:</strong> ").append(order.getType() == com.utetea.backend.model.OrderType.DELIVERY ? "Giao hàng" : "Lấy tại cửa hàng").append("</p>");
        content.append("<p><strong>Địa chỉ:</strong> ").append(order.getAddress()).append("</p>");
        
        if (order.getPickupTime() != null) {
            content.append("<p><strong>Thời gian:</strong> ").append(order.getPickupTime().format(formatter)).append("</p>");
        }
        
        content.append("<p><strong>Phương thức thanh toán:</strong> ");
        content.append(order.getPaymentMethod() == com.utetea.backend.model.PaymentMethod.COD ? "Tiền mặt" : 
                      order.getPaymentMethod() == com.utetea.backend.model.PaymentMethod.VNPAY ? "VNPay" : "Khác");
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
        
        // Price Summary
        content.append("<div class='order-info'>");
        content.append("<p>Tổng tiền: ").append(formatPrice(order.getTotalPrice())).append("</p>");
        
        if (order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            content.append("<p>Giảm giá");
            if (order.getPromotion() != null) {
                content.append(" (").append(order.getPromotion().getCode()).append(")");
            }
            content.append(": -").append(formatPrice(order.getDiscount())).append("</p>");
        }
        
        content.append("<p class='total'>Thành tiền: ").append(formatPrice(order.getFinalPrice())).append("</p>");
        content.append("</div>");
        
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
        content.append("<p><strong>Cửa hàng:</strong> ").append(order.getStore().getStoreName()).append("</p>");
        content.append("<p><strong>Loại đơn:</strong> ").append(order.getType() == com.utetea.backend.model.OrderType.DELIVERY ? "Giao hàng" : "Lấy tại cửa hàng").append("</p>");
        content.append("<p><strong>Địa chỉ:</strong> ").append(order.getAddress()).append("</p>");
        
        if (order.getPickupTime() != null) {
            content.append("<p><strong>Thời gian:</strong> ").append(order.getPickupTime().format(formatter)).append("</p>");
        }
        
        content.append("<p><strong>Phương thức thanh toán:</strong> ");
        content.append(order.getPaymentMethod() == com.utetea.backend.model.PaymentMethod.COD ? "Tiền mặt" : 
                      order.getPaymentMethod() == com.utetea.backend.model.PaymentMethod.VNPAY ? "VNPay" : "Khác");
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
        
        // Price Summary
        content.append("<div class='order-info'>");
        content.append("<p>Tổng tiền: ").append(formatPrice(order.getTotalPrice())).append("</p>");
        
        if (order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            content.append("<p>Giảm giá");
            if (order.getPromotion() != null) {
                content.append(" (").append(order.getPromotion().getCode()).append(")");
            }
            content.append(": -").append(formatPrice(order.getDiscount())).append("</p>");
        }
        
        content.append("<p class='total'>Thành tiền: ").append(formatPrice(order.getFinalPrice())).append("</p>");
        content.append("</div>");
        
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
}
