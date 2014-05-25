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

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.Config;
import org.pac4j.vertx.Constants;
import org.pac4j.vertx.HttpResponseHelper;
import org.pac4j.vertx.VertxWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import com.campudus.vertx.sessionmanager.java.SessionHelper;

/**
 * Callback handler for Vert.x pac4j binding. This handler finishes the authentication process.
 * <br>
 * This handler is in two parts:
 * <ul>
 * <li>the real handler which is called either directly if there's no data in the request (e.g. GET)
 *  or by the request endHanlder otherwise (e.g. POST)</li>
 *  <li>the handler which makes the decision based on the HTTP method and the Content-Type header</li>
 *  </ul>
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class CallbackHandler extends SessionAwareHandler {

    protected static final Logger logger = LoggerFactory.getLogger(CallbackHandler.class);

    public CallbackHandler(SessionHelper sessionHelper) {
        super(sessionHelper);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void doHandle(final HttpServerRequest req, final VertxWebContext webContext) {
        // clients group from config
        final Clients clientsGroup = Config.getClients();

        // get the client from its type
        final BaseClient client = (BaseClient) clientsGroup.findClient(webContext);
        logger.debug("client : {}", client);

        // get credentials
        Credentials credentials = null;
        try {
            credentials = client.getCredentials(webContext);
            logger.debug("credentials : {}", credentials);
        } catch (final RequiresHttpAction e) {
            // requires some specific HTTP action
            logger.debug("requires HTTP action : {}", e.getCode());
            if (e.getCode() == HttpConstants.OK) {
                req.response().headers().add("Content-Type", Constants.HTML_CONTENT_TYPE);
            }
            // eventually end response
            writeSessionAttribute(webContext, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject event) {
                    req.response().end();
                }
            });
            return;
        }

        // get user profile
        final CommonProfile profile = client.getUserProfile(credentials, webContext);
        logger.debug("profile : {}", profile);

        // save user profile only if it's not null
        if (profile != null) {
            webContext.setSessionAttribute(Constants.USER_PROFILE, profile);
        }

        final String requestedUrl = (String) webContext.getSessionAttribute(Constants.REQUESTED_URL);
        webContext.setSessionAttribute(Constants.REQUESTED_URL, null);

        final String redirectUrl = defaultUrl(requestedUrl, Config.getDefaultSuccessUrl());

        writeSessionAttribute(webContext, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                HttpResponseHelper.redirect(req, redirectUrl);
            }
        });

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
