package org.pac4j.vertx;

import io.vertx.core.Vertx;
import org.pac4j.vertx.verticle.MainVerticle;

/**
 * Simple main class to launch the Vert.x application
 */
public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }
}