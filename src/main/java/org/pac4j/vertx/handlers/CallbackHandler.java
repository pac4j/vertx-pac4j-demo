package org.pac4j.vertx.handlers;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.Config;
import org.pac4j.vertx.Constants;
import org.pac4j.vertx.HttpResponseHelper;
import org.pac4j.vertx.StorageHelper;
import org.pac4j.vertx.VertxWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class CallbackHandler extends HttpSafeHandler {

    protected static final Logger logger = LoggerFactory.getLogger(CallbackHandler.class);

    private HttpSafeHandler handler = new HttpSafeHandler() {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void handleInternal(HttpServerRequest req) {
            // clients group from config
            final Clients clientsGroup = Config.getClients();

            // web context
            final WebContext context = new VertxWebContext(req);

            // get the client from its type
            final BaseClient client = (BaseClient) clientsGroup.findClient(context);
            logger.debug("client : {}", client);

            // get credentials
            Credentials credentials = null;
            try {
                credentials = client.getCredentials(context);
                logger.debug("credentials : {}", credentials);
            } catch (final RequiresHttpAction e) {
                // requires some specific HTTP action
                logger.debug("requires HTTP action : {}", e.getCode());
                if (e.getCode() == HttpConstants.OK) {
                    req.response().headers().add("Content-Type", Constants.HTML_CONTENT_TYPE);
                }
                // eventually end response
                req.response().end();
                return;
            }

            // get user profile
            final CommonProfile profile = client.getUserProfile(credentials, context);
            logger.debug("profile : {}", profile);

            // get or create sessionId
            final String sessionId = StorageHelper.getOrCreateSessionId(req);

            // save user profile only if it's not null
            if (profile != null) {
                StorageHelper.saveProfile(sessionId, profile);
            }

            // get requested url
            final String requestedUrl = StorageHelper.getRequestedUrl(sessionId);
            final String redirectUrl = defaultUrl(requestedUrl, Config.getDefaultSuccessUrl());
            StorageHelper.saveRequestedUrl(sessionId, null);

            // retrieve saved request and redirect
            HttpResponseHelper.redirect(req, redirectUrl);

        }
    };

    @Override
    public void handleInternal(final HttpServerRequest req) {
        String contentType = req.headers().get("Content-Type");
        if ("POST".equals(req.method()) && contentType != null
                && "application/x-www-form-urlencoded".equals(contentType)) {
            req.expectMultiPart(true);
            req.params().add(Constants.FORM_ATTRIBUTES, "true");
            req.endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    handler.handle(req);
                }
            });
        } else {
            handler.handle(req);
        }
    }

    /**
     * This method returns the default url from a specified url compared with a default url.
     * 
     * @param url
     * @param defaultUrl
     * @return the default url
     */
    public static String defaultUrl(final String url, final String defaultUrl) {
        String redirectUrl = defaultUrl;
        if (StringUtils.isNotBlank(url)) {
            redirectUrl = url;
        }
        logger.debug("defaultUrl : {}", redirectUrl);
        return redirectUrl;
    }

}
