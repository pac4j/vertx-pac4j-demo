/*
  Copyright 2014 - 2014 Michael Remond

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.vertx.handlers;

import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.vertx.Config;
import org.pac4j.vertx.HttpResponseHelper;
import org.pac4j.vertx.VertxWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import com.campudus.vertx.sessionmanager.java.SessionHelper;

/**
 * Wrapper handler acting as security barrier. If the user is authenticated, the next handler in chain is called.
 * Otherwise the user is redirected to the pac4j client security provider.
 * 
 *  The pac4j client to use is selected with the clientName attributes. 
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class RequiresAuthenticationHandler extends SessionAwareHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequiresAuthenticationHandler.class);

    private final String clientName;

    private final Boolean isAjax;

    private final Handler<HttpServerRequest> delegate;

    public RequiresAuthenticationHandler(String clientName, Handler<HttpServerRequest> delegate,
            SessionHelper sessionHelper) {
        this(clientName, false, delegate, sessionHelper);
    }

    public RequiresAuthenticationHandler(String clientName, boolean isAjax, Handler<HttpServerRequest> delegate,
            SessionHelper sessionHelper) {
        super(sessionHelper);
        this.clientName = clientName;
        this.delegate = delegate;
        this.isAjax = isAjax;
    }

    protected void doHandle(final HttpServerRequest req, final VertxWebContext webContext) {
        if (webContext.getSessionAttribute("pac4jUserProfile") != null) {
            delegate.handle(req);
        } else {
            final String requestedUrlToSave = webContext.getFullRequestURL();
            webContext.setSessionAttribute("requestedUrlToSave", requestedUrlToSave);
            logger.debug("requestedUrlToSave : {}", requestedUrlToSave);

            // get client
            final BaseClient client = (BaseClient) Config.getClients().findClient(clientName);
            logger.debug("client : {}", client);
            try {
                final RedirectAction action = client.getRedirectAction(webContext, true, isAjax);

                writeSessionAttribute(webContext, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {
                        doRedirect(req, action);
                    }
                });
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

    private void doRedirect(HttpServerRequest req, RedirectAction action) {
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
    }

}
