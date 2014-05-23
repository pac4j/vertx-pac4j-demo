package org.pac4j.vertx.handlers;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public abstract class HttpSafeHandler implements Handler<HttpServerRequest> {

    protected abstract void handleInternal(HttpServerRequest req);

    @Override
    public void handle(HttpServerRequest req) {
        try {
            handleInternal(req);
        } catch (Exception e) {
            e.printStackTrace();
            req.response().setStatusCode(500);
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            e.printStackTrace(writer);
            req.response().end(stringWriter.toString());
        }
    }

}
