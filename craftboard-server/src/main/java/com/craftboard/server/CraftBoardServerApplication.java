package com.craftboard.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree Spring Boot du serveur CraftBoard.
 */
@SpringBootApplication
public class CraftBoardServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CraftBoardServerApplication.class, args);
    }
}