package com.utetea.backend.service;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.utetea.backend.config.PayPalConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {
    
    private final APIContext apiContext;
    private final PayPalConfig payPalConfig;
    
    public String getClientId() {
        return payPalConfig.getClientId();
    }
    
    public String getClientSecret() {
        return payPalConfig.getClientSecret();
    }
    
    public String getMode() {
        return payPalConfig.getMode();
    }
    
    public String createPayment(Double total, String currency, String description) throws PayPalRESTException {
        if (currency == null || currency.isEmpty()) {
            currency = "USD";
        }
        if (description == null || description.isEmpty()) {
            description = "UTE Tea Payment";
        }
        
        Amount amount = new Amount();
        amount.setTotal(String.format("%.2f", total));
        amount.setCurrency(currency);
        
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription(description);
        
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");
        
        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(Arrays.asList(transaction));
        
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(payPalConfig.getCancelUrl());
        redirectUrls.setReturnUrl(payPalConfig.getSuccessUrl());
        payment.setRedirectUrls(redirectUrls);
        
        Payment createdPayment = payment.create(apiContext);
        log.info("PayPal payment created: {}", createdPayment.getId());
        
        List<Links> links = createdPayment.getLinks();
        for (Links link : links) {
            if ("approval_url".equalsIgnoreCase(link.getRel())) {
                return link.getHref();
            }
        }
        
        throw new PayPalRESTException("Approval URL not found");
    }
    
    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
        Payment payment = new Payment();
        payment.setId(paymentId);
        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);
        Payment executedPayment = payment.execute(apiContext, paymentExecution);
        log.info("PayPal payment executed: {}", executedPayment.getId());
        return executedPayment;
    }
}
