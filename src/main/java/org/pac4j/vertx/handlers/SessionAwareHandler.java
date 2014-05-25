package org.pac4j.vertx.handlers;

import org.pac4j.vertx.VertxWebContext;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.campudus.vertx.sessionmanager.java.SessionHelper;

public abstract class SessionAwareHandler implements Handler<HttpServerRequest> {

    private static final String SESSION_ATTRIBUTES = "session_attributes";

    protected SessionHelper sessionHelper;

    public SessionAwareHandler(SessionHelper sessionHelper) {
        this.sessionHelper = sessionHelper;
    }

    @Override
    public void handle(final HttpServerRequest req) {
        sessionHelper.withSessionData(req, new JsonArray(new Object[] { SESSION_ATTRIBUTES }),
                new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {
                        if ("error".equals(event.getString("status"))
                                && "SESSION_GONE".equals(event.getString("error"))) {
                            sessionHelper.startSession(req, new Handler<String>() {

                                @Override
                                public void handle(String sessionId) {
                                    doHandle(req, new VertxWebContext(req, sessionId, new JsonObject()));
                                }
                            });
                        } else if ("ok".equals(event.getString("status"))) {
                            JsonObject data = event.getObject("data").getObject(SESSION_ATTRIBUTES);
                            if (data == null) {
                                data = new JsonObject();
                            }
                            doHandle(req, new VertxWebContext(req, sessionHelper.getSessionId(req), data));
                        }
                    }
                });

    }

    protected void writeSessionAttribute(VertxWebContext context, Handler<JsonObject> handler) {
        JsonObject attribute = new JsonObject().putObject(SESSION_ATTRIBUTES, context.getOutAttributes());
        sessionHelper.putSessionData(context.getSessionId(), attribute, handler);
    }

    protected abstract void doHandle(final HttpServerRequest req, final VertxWebContext webContext);

}
