package org.pac4j.vertx.handlers;

import org.pac4j.vertx.HttpResponseHelper;
import org.pac4j.vertx.StorageHelper;
import org.vertx.java.core.http.HttpServerRequest;

public class LogoutHandler extends HttpSafeHandler {

    @Override
    protected void handleInternal(HttpServerRequest req) {
        String sessionId = StorageHelper.getOrCreateSessionId(req);
        StorageHelper.saveProfile(sessionId, null);
        HttpResponseHelper.redirect(req, "/");
    }

}
