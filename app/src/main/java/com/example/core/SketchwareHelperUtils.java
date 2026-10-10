package com.example.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sketchware-compatible Java Helper utility class.
 * Merchants using Sketchware or standard Java Android environments
 * can drop this single class directly into their projects.
 */
public class SketchwareHelperUtils {

    public static class TransactionData {
        public boolean isValid;
        public String provider;
        public String senderId;
        public String transactionId;
        public double amount;
        public String currency;
        public String reference;
        public double balance;
        public int confidence;
        public boolean isSensitiveBlocked;

        public TransactionData() {
            this.currency = "BDT";
            this.provider = "Unknown";
            this.transactionId = "";
            this.reference = "";
        }
    }

    public static boolean isApprovedSender(String sender, List<String> approvedSenderList) {
        if (sender == null || sender.trim().isEmpty() || approvedSenderList == null) {
            return false;
        }
        String s = sender.trim().toLowerCase();
        for (String approved : approvedSenderList) {
            if (approved != null && !approved.trim().isEmpty()) {
                String a = approved.trim().toLowerCase();
                if (s.contains(a) || a.contains(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSensitiveMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return true;
        }
        String lower = message.toLowerCase();
        String[] keywords = new String[]{
            "otp", "one time password", "pin", "verification code",
            "security code", "password", "passcode", "login code",
            "card number", "cvv", "nid", "do not share", "never share",
            "ওটিপি", "পিন", "গোপন কোড", "কাউকে বলবেন না"
        };
        for (String k : keywords) {
            if (lower.contains(k)) {
                return true;
            }
        }
        return false;
    }

    public static TransactionData parseTransactionSms(String sender, String message) {
        TransactionData data = new TransactionData();
        data.senderId = sender != null ? sender : "";

        if (isSensitiveMessage(message)) {
            data.isValid = false;
            data.isSensitiveBlocked = true;
            data.confidence = 0;
            return data;
        }

        if (sender != null) {
            String s = sender.toLowerCase();
            if (s.contains("bkash")) data.provider = "bKash";
            else if (s.contains("nagad")) data.provider = "Nagad";
            else if (s.contains("16216") || s.contains("rocket")) data.provider = "Rocket";
            else if (s.contains("upay")) data.provider = "Upay";
            else data.provider = sender;
        }

        // Extract TrxID
        Pattern txnPattern = Pattern.compile("(?:TrxID|Trx ID|TxnId|TXN|লেনদেন আইডি)[:\\s]+([A-Za-z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher txnMatcher = txnPattern.matcher(message);
        if (txnMatcher.find()) {
            data.transactionId = txnMatcher.group(1);
        }

        // Extract Balance first
        Pattern balancePattern = Pattern.compile("(?:Balance|ব্যালেন্স)[:\\s]+(?:Tk|BDT)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
        Matcher balanceMatcher = balancePattern.matcher(message);
        if (balanceMatcher.find()) {
            try {
                String bStr = balanceMatcher.group(1).replace(",", "");
                data.balance = Double.parseDouble(bStr);
            } catch (Exception ignored) {}
        }

        // Extract Amount
        Pattern amountPattern = Pattern.compile("(?:received|Payment received|টাকা গ্রহণ)[:\\s]*(?:Tk|BDT)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
        Matcher amountMatcher = amountPattern.matcher(message);
        if (amountMatcher.find()) {
            try {
                String aStr = amountMatcher.group(1).replace(",", "");
                double parsedAmt = Double.parseDouble(aStr);
                if (parsedAmt != data.balance) {
                    data.amount = parsedAmt;
                }
            } catch (Exception ignored) {}
        }

        // Fallback amount finder
        if (data.amount <= 0) {
            Pattern fallbackAmount = Pattern.compile("(?:Tk|BDT)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
            Matcher fallbackMatcher = fallbackAmount.matcher(message);
            while (fallbackMatcher.find()) {
                try {
                    String aStr = fallbackMatcher.group(1).replace(",", "");
                    double candidate = Double.parseDouble(aStr);
                    if (candidate > 0 && candidate != data.balance) {
                        data.amount = candidate;
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        // Customer Ref
        Pattern refPattern = Pattern.compile("(?:from|হতে)[:\\s]+([0-9+]+)", Pattern.CASE_INSENSITIVE);
        Matcher refMatcher = refPattern.matcher(message);
        if (refMatcher.find()) {
            data.reference = refMatcher.group(1);
        }

        data.isValid = !data.transactionId.isEmpty() && data.amount > 0;
        data.confidence = calculateConfidence(data);
        return data;
    }

    public static int calculateConfidence(TransactionData data) {
        int score = 0;
        if (data.transactionId != null && data.transactionId.length() >= 6) {
            score += 40;
        }
        if (data.amount > 0) {
            score += 30;
        }
        if (data.provider != null && !data.provider.equals("Unknown")) {
            score += 15;
        }
        if (data.reference != null && !data.reference.isEmpty()) {
            score += 15;
        }
        return Math.min(100, Math.max(0, score));
    }

    public static boolean isDuplicateTransaction(String txnId, List<String> existingTxnIds) {
        if (txnId == null || txnId.trim().isEmpty() || existingTxnIds == null) {
            return false;
        }
        for (String existing : existingTxnIds) {
            if (txnId.equalsIgnoreCase(existing)) {
                return true;
            }
        }
        return false;
    }

    public static String buildJsonPayload(String merchantId, String provider, String txnId, double amount, String currency, String reference, String deviceId) {
        return "{\n" +
            "  \"merchant_id\": \"" + merchantId + "\",\n" +
            "  \"provider\": \"" + provider + "\",\n" +
            "  \"transaction_id\": \"" + txnId + "\",\n" +
            "  \"amount\": " + amount + ",\n" +
            "  \"currency\": \"" + currency + "\",\n" +
            "  \"reference\": \"" + reference + "\",\n" +
            "  \"source\": \"merchant_sms_review\",\n" +
            "  \"device_id\": \"" + deviceId + "\"\n" +
            "}";
    }

    public static String calculateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static int sendTransactionToApi(String httpsUrl, String jsonPayload, String authHeaderName, String authSecret) throws Exception {
        if (httpsUrl == null || !httpsUrl.toLowerCase().startsWith("https://")) {
            throw new IllegalArgumentException("Security violation: Only HTTPS URLs are permitted.");
        }
        URL url = new URL(httpsUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        if (authSecret != null && !authSecret.trim().isEmpty()) {
            conn.setRequestProperty(authHeaderName, authSecret);
        }

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return conn.getResponseCode();
    }
}
