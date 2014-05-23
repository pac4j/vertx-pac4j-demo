package org.pac4j.vertx.handlers;

import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.vertx.Config;
import org.pac4j.vertx.HttpResponseHelper;
import org.pac4j.vertx.StorageHelper;
import org.pac4j.vertx.VertxWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class RequiresAuthenticationHandler extends HttpSafeHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequiresAuthenticationHandler.class);

    private final String clientName;

    private final Boolean isAjax;

    private final Handler<HttpServerRequest> delegate;

    public RequiresAuthenticationHandler(String clientName, Handler<HttpServerRequest> delegate) {
        this(clientName, false, delegate);
    }

    public RequiresAuthenticationHandler(String clientName, boolean isAjax, Handler<HttpServerRequest> delegate) {
        this.clientName = clientName;
        this.delegate = delegate;
        this.isAjax = isAjax;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void handleInternal(HttpServerRequest req) {

        String sessionId = StorageHelper.getOrCreateSessionId(req);
        if (StorageHelper.getProfile(sessionId) != null) {
            delegate.handle(req);
        } else {
            WebContext webContext = new VertxWebContext(req);
            // requested url to save
            final String requestedUrlToSave = webContext.getFullRequestURL();
            logger.debug("requestedUrlToSave : {}", requestedUrlToSave);
            StorageHelper.saveRequestedUrl(sessionId, requestedUrlToSave);
            // get client
            final BaseClient client = (BaseClient) Config.getClients().findClient(clientName);
            logger.debug("client : {}", client);
            try {
                RedirectAction action = client.getRedirectAction(webContext, true, isAjax);
                switch (action.getType()) {
                case REDIRECT:
                    HttpResponseHelper.redirect(req, action.getLocation());
                    break;
                case SUCCESS:
                    HttpResponseHelper.ok(req, action.getContent());
                    break;
                default:
                    throw new TechnicalException("Invalid redirect action type");
                }
            } catch (final RequiresHttpAction e) {
                // requires some specific HTTP action
                final int code = e.getCode();
                logger.debug("requires HTTP action : {}", code);
                if (code == HttpConstants.UNAUTHORIZED) {
                    HttpResponseHelper.unauthorized(req, Config.getErrorPage401());
                } else if (code == HttpConstants.FORBIDDEN) {
                    HttpResponseHelper.forbidden(req, Config.getErrorPage403());
                } else {
                    final String message = "Unsupported HTTP action : " + code;
                    logger.error(message);
                    throw new TechnicalException(message);
                }
            }
        }
    }

}
