/*
  Copyright 2014 - 2015 pac4j organization

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
package org.pac4j.vertx.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.handler.impl.ApplicationLogoutHandler;
import org.pac4j.vertx.handler.impl.Pac4jAuthHandlerOptions;
import org.pac4j.vertx.handler.impl.RequiresAuthenticationHandler;

/**
 * A collection of basic handlers printing dynamic html for the demo application.
 * 
 * @author Michael Remond/Jeremy Prime
 * @since 1.0.0
 *
 */
public class DemoHandlers {

    public static Handler<RoutingContext> indexHandler(final Config config) {
        final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
        return rc -> {
// we define a hardcoded title for our application
            rc.put("name", "Vert.x Web");
            final Clients clients = config.getClients();
            final WebContext context = new VertxWebContext(rc);
            final String urlFacebook;
            final String urlTwitter;
            final String urlCas;
            try {
                urlFacebook = ((IndirectClient) clients.findClient("FacebookClient")).getRedirectAction(context, false).getLocation();
                urlTwitter = ((IndirectClient) clients.findClient("TwitterClient")).getRedirectAction(context, false).getLocation();
                urlCas = ((IndirectClient) clients.findClient("CasClient")).getRedirectAction(context, false).getLocation();
            } catch (RequiresHttpAction requiresHttpAction) {
                throw new RuntimeException(requiresHttpAction);
            }

            rc.put("urlFacebook", urlFacebook);
            rc.put("urlTwitter", urlTwitter);
            rc.put("urlCas", urlCas);
            final UserProfile profile = getUserProfile(rc);
            rc.put("userProfile", profile);
            if (rc.user() != null) {
                rc.put("vertxUserPrincipal", rc.user().principal());
            }

            // and now delegate to the engine to render it.
            engine.render(rc, "templates/index.hbs", res -> {
                if (res.succeeded()) {
                    rc.response().end(res.result());
                } else {
                    rc.fail(res.cause());
                }
            });
        };
    }

    public static Handler<RoutingContext> authHandler(final Vertx vertx,
                                                      final Config config,
                                                      final Pac4jAuthProvider provider,
                                                      final Pac4jAuthHandlerOptions options) {
        return new RequiresAuthenticationHandler(vertx, config, provider, options);
    }

    public static Handler<RoutingContext> logoutHandler() {
        return new ApplicationLogoutHandler();
    }

    public static Handler<RoutingContext> protectedIndexHandler() {

        final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();

        return rc -> {
            // and now delegate to the engine to render it.

            final UserProfile profile = getUserProfile(rc);
            rc.put("userProfile", profile);
            if (rc.user() != null) {
                rc.put("vertxUserPrincipal", rc.user().principal());
            }

            engine.render(rc, "templates/protectedIndex.hbs", res -> {
                if (res.succeeded()) {
                    rc.response().end(res.result());
                } else {
                    rc.fail(res.cause());
                }
            });
        };
    }

    private static CommonProfile getUserProfile(final RoutingContext rc) {
        final ProfileManager<CommonProfile> profileManager = new VertxProfileManager<>(new VertxWebContext(rc));
        return profileManager.get(true);
    }

//    public static class IndexHandler extends SessionAwareHandler {
//
//        private final Pac4jHelper pac4jHelper;
//
//        public IndexHandler(Pac4jHelper pac4jHelper, SessionHelper sessionHelper) {
//            super(sessionHelper);
//            this.pac4jHelper = pac4jHelper;
//        }
//
//        @Override
//        protected void doHandle(final HttpServerRequest req, final String sessionId, final JsonObject sessionAttributes) {
//            pac4jHelper.getRedirectUrls(
//                    req,
//                    sessionAttributes,
//                    new Handler<Message<JsonObject>>() {
//
//                        @Override
//                        public void handle(Message<JsonObject> event) {
//                            JsonObject response = event.body();
//                            if (pac4jHelper.isErrorMessage(response)) {
//                                pac4jHelper.sendErrorResponse(req.response(), response);
//                                return;
//                            }
//
//                            JsonObject attributes = pac4jHelper.getSessionAttributes(response);
//                            attributes.putString(HttpConstants.REQUESTED_URL, null);
//
//                            final StringBuilder sb = new StringBuilder();
//                            sb.append("<h1>index</h1>");
//                            sb.append("<a href=\"form/index.html\">Protected url by form authentication : form/index.html</a><br />");
//                            sb.append("<a href=\"javascript:ajaxClick();\">Click here to send AJAX request after performing form authentication</a><br />");
//                            sb.append("<a href=\"basicauth/index.html\">Protected url by basic auth : basicauth/index.html</a><br />");
//                            sb.append("<a href=\"saml2/index.html\">Protected url by SAML2 : saml2/index.html</a><br />");
//                            sb.append("<a href=\"oidc/index.html\">Protected url by OpenID Connect : oidc/index.html</a><br />");
//                            sb.append("<br />");
//                            sb.append("<a href=\"logout\">logout</a>");
//                            sb.append("<br /><br />");
//                            sb.append("profile : ");
//                            sb.append(pac4jHelper.getUserProfileFromSession(attributes));
//                            sb.append("<br /><br />");
//                            sb.append("<hr />");
//                            sb.append("<a href=\"").append(response.getString("FormClient"))
//                                    .append("\">Authenticate with form</a><br />");
//                            sb.append("<a href=\"").append(response.getString("BasicAuthClient"))
//                                    .append("\">Authenticate with basic auth</a><br />");
//                            sb.append("<a href=\"").append(response.getString("Saml2Client"))
//                                    .append("\">Authenticate with SAML</a><br />");
//                            sb.append("<a href=\"").append(response.getString("OidcClient"))
//                                    .append("\">Authenticate with OpenID Connect</a><br />");
//                            sb.append("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js\"></script>");
//                            sb.append("<script src=\"assets/js/app.js\"></script>");
//
//                            saveSessionAttributes(sessionId, attributes, new Handler<JsonObject>() {
//                                @Override
//                                public void handle(JsonObject event) {
//                                    HttpResponseHelper.ok(req, sb.toString());
//                                }
//                            });
//
//                        }
//                    }, "FacebookClient", "FacebookClient", "TwitterClient", "FormClient", "BasicAuthClient",
//                    "CasClient",
//                    "Saml2Client", "OidcClient");
//
//        }
//    };
//
//    public static class AuthenticatedHandler implements Handler<HttpServerRequest> {
//
//        @Override
//        public void handle(HttpServerRequest req) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("<h1>protected area</h1>");
//            sb.append("<a href=\"..\">Back</a><br />");
//            sb.append("<br /><br />");
//            sb.append("profile : ");
//            sb.append(((AuthHttpServerRequest) req).getProfile());
//            sb.append("<br />");
//            HttpResponseHelper.ok(req, sb.toString());
//        }
//    };
//
//    public static class AuthenticatedJsonHandler implements Handler<HttpServerRequest> {
//
//        @Override
//        public void handle(final HttpServerRequest req) {
//            req.response().headers().add("Content-Type", "application/json");
//            UserProfile profile = ((AuthHttpServerRequest) req).getProfile();
//            HttpResponseHelper.ok(req, new JsonObject().putString("id", profile.getId()).toString());
//        }
//    };
//
//    public static Handler<HttpServerRequest> formHandler = new Handler<HttpServerRequest>() {
//        @Override
//        public void handle(HttpServerRequest req) {
//            String callbackUrl = "http://localhost:8080/callback?client_name=FormClient";
//            StringBuilder sb = new StringBuilder();
//            sb.append("<form action=\"").append(callbackUrl).append("\" method=\"POST\">");
//            sb.append("<input type=\"text\" name=\"username\" value=\"\" />");
//            sb.append("<p />");
//            sb.append("<input type=\"password\" name=\"password\" value=\"\" />");
//            sb.append("<p />");
//            sb.append("<input type=\"submit\" name=\"submit\" value=\"Submit\" />");
//            sb.append("</form>");
//
//            HttpResponseHelper.ok(req, sb.toString());
//        }
//    };

}
