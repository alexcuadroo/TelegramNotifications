package com.telegramnotifications;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;

public final class TelegramUtil {

    private static final Logger LOGGER = Logger.getLogger("TelegramNotifications");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 500;

    private TelegramUtil() {
    }

    public static void sendMessage(String token, String chatId, String message) {
        // Enviar de forma asincrónica sin bloquear
        Thread.startVirtualThread(() -> sendMessageWithRetries(token, chatId, message, 0));
    }

    private static void sendMessageWithRetries(String token, String chatId, String message, int attempt) {
        try {
            String body = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                    + "&parse_mode=Markdown"
                    + "&text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + token + "/sendMessage"))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOGGER.fine("Mensaje Telegram enviado exitosamente");
            } else if (response.statusCode() == 404 || response.statusCode() == 401) {
                LOGGER.severe("Token o Chat ID inválido. Por favor, verifica tu configuración.");
                // No reintentar - error permanente
            } else if (response.statusCode() >= 400 && response.statusCode() < 500) {
                LOGGER.warning("Error Telegram: código " + response.statusCode() + " (error de configuración)");
                // No reintentar otros errores 4xx - son errores permanentes
            } else {
                LOGGER.warning("Error Telegram: código " + response.statusCode());
                retryWithBackoff(token, chatId, message, attempt);
            }
        } catch (java.net.http.HttpTimeoutException e) {
            LOGGER.warning("Timeout conectando a Telegram (intento " + (attempt + 1) + "/" + MAX_RETRIES + ")");
            retryWithBackoff(token, chatId, message, attempt);
        } catch (Exception e) {
            LOGGER.warning("Error enviando mensaje Telegram: " + e.getMessage());
            retryWithBackoff(token, chatId, message, attempt);
        }
    }

    private static void retryWithBackoff(String token, String chatId, String message, int attempt) {
        if (attempt >= MAX_RETRIES) {
            LOGGER.severe("Mensaje descartado después de " + MAX_RETRIES + " reintentos");
            return;
        }

        long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt);
        LOGGER.info("Reintentando en " + backoffMs + "ms...");

        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        sendMessageWithRetries(token, chatId, message, attempt + 1);
    }
}
