package com.craftboard.desktop;

import com.craftboard.server.CraftBoardServerApplication;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Demarre le serveur Spring Boot integre si aucun serveur local ne repond.
 */
final class EmbeddedServerLauncher {

    private static final URI HEALTH_URI = URI.create("http://localhost:8080/api/health");
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);
    private static final long STARTUP_TIMEOUT_SECONDS = 60;

    private EmbeddedServerLauncher() {
    }

    static ConfigurableApplicationContext startIfNeeded(String[] args) {
        if (isServerAlreadyRunning()) {
            return null;
        }

        // Le serveur part sur un thread dedie pour ne pas bloquer le demarrage JavaFX.
        CompletableFuture<ConfigurableApplicationContext> startup = new CompletableFuture<>();
        Thread serverThread = new Thread(() -> {
            try {
                ConfigurableApplicationContext context = new SpringApplicationBuilder(CraftBoardServerApplication.class)
                        .headless(false)
                        .run(args);
                startup.complete(context);
            } catch (RuntimeException exception) {
                startup.completeExceptionally(exception);
            }
        }, "craftboard-server");
        serverThread.setDaemon(false);
        serverThread.start();

        try {
            return startup.get(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Le demarrage du serveur a ete interrompu.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Le serveur CraftBoard n'a pas pu demarrer.", exception.getCause());
        } catch (TimeoutException exception) {
            throw new IllegalStateException("Le serveur CraftBoard ne repond pas apres "
                    + STARTUP_TIMEOUT_SECONDS + " secondes.", exception);
        }
    }

    private static boolean isServerAlreadyRunning() {
        // Le healthcheck evite de lancer deux serveurs sur le meme port.
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(HEALTH_TIMEOUT)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(HEALTH_URI)
                .timeout(HEALTH_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
