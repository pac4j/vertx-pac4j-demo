package org.pac4j.vertx;

import org.pac4j.core.context.HttpConstants;
import org.vertx.java.core.http.HttpServerRequest;

public class HttpResponseHelper {

    public static void ok(HttpServerRequest req, String content) {
        req.response().setStatusCode(HttpConstants.OK);
        req.response().headers().add("Content-Type", Constants.HTML_CONTENT_TYPE);
        req.response().end(content);
    }

    public static void redirect(HttpServerRequest req, String location) {
        req.response().setStatusCode(HttpConstants.TEMP_REDIRECT);
        if (location != null) {
            req.response().putHeader(HttpConstants.LOCATION_HEADER, location);
        }
        req.response().end();
    }

    public static void redirect(HttpServerRequest req) {
        redirect(req, null);
    }

    public static void unauthorized(HttpServerRequest req, String page) {
        req.response().setStatusCode(HttpConstants.UNAUTHORIZED);
        req.response().end(page);
    }

    public static void forbidden(HttpServerRequest req, String page) {
        req.response().setStatusCode(HttpConstants.FORBIDDEN);
        req.response().end(page);
    }

}
