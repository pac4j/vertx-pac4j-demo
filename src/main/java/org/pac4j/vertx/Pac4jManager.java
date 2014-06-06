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
package org.pac4j.vertx;

import org.pac4j.cas.client.CasClient;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.BasicAuthClient;
import org.pac4j.http.client.FormClient;
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.saml.client.Saml2Client;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * The pac4j manager module.<br>
 * The three operations are:
 * <ul>
 * <li>redirect: generate the required http information for building an authentication request</li> 
 * <li>redirectUrls: build the urls for direct authentication</li> 
 * <li>authenticate: verify the provided credentials and return the user profile</li>
 * </ul>
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class Pac4jManager extends BusModBase {

    private Handler<Message<JsonObject>> redirectHandler;
    private Handler<Message<JsonObject>> authenticateHandler;
    private Handler<Message<JsonObject>> redirectUrlsHandler;

    private Clients clients;

    private String address;

    protected Clients buildClients() {
        final Saml2Client saml2Client = new Saml2Client();
        saml2Client.setKeystorePath("resource:samlKeystore.jks");
        saml2Client.setKeystorePassword("pac4j-demo-passwd");
        saml2Client.setPrivateKeyPassword("pac4j-demo-passwd");
        saml2Client.setIdpMetadataPath("resource:testshib-providers.xml");

        final FacebookClient facebookClient = new FacebookClient("145278422258960", "be21409ba8f39b5dae2a7de525484da8");
        final TwitterClient twitterClient = new TwitterClient("CoxUiYwQOSFDReZYdjigBA",
                "2kAzunH5Btc4gRSaMr7D7MkyoJ5u1VzbOOzE8rBofs");
        // HTTP
        final FormClient formClient = new FormClient("http://localhost:8080/theForm.html",
                new SimpleTestUsernamePasswordAuthenticator());
        final BasicAuthClient basicAuthClient = new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

        // CAS
        final CasClient casClient = new CasClient();
        // casClient.setGateway(true);
        casClient.setCasLoginUrl("https://freeuse1.casinthecloud.com/leleujgithub/login");

        final Clients clients = new Clients("http://localhost:8080/callback", saml2Client, facebookClient,
                twitterClient, formClient, basicAuthClient, casClient);

        return clients;
    }

    public void start() {
        super.start();

        this.address = getOptionalStringConfig("address", "vertx.pac4jmodule");
        this.clients = buildClients();

        redirectHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                doRedirect(message);
            }
        };
        eb.registerHandler(address + ".redirect", redirectHandler);
        authenticateHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                doAuthenticate(message);
            }
        };
        eb.registerHandler(address + ".authenticate", authenticateHandler);
        redirectUrlsHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                doRedirectUrls(message);
            }
        };
        eb.registerHandler(address + ".redirectUrls", redirectUrlsHandler);
    }

    @SuppressWarnings("rawtypes")
    private void doRedirect(final Message<JsonObject> message) {
        final String clientName = getMandatoryString("clientName", message);
        final JsonObject json = getMandatoryObject("webContext", message);
        VertxWebContext webContext = decodeWebContext(json);
        final boolean protectedResource = message.body().getBoolean("protected");
        final boolean isAjax = message.body().getBoolean("isAjax");

        Client client = clients.findClient(clientName);
        logger.debug("client : " + client);

        try {
            client.redirect(webContext, protectedResource, isAjax);
        } catch (RequiresHttpAction e) {
            logger.debug("extra HTTP action required : " + e.getCode());
        }

        JsonObject response = encodeWebContext(webContext);

        sendOK(message, response);
    }

    @SuppressWarnings("rawtypes")
    private void doRedirectUrls(final Message<JsonObject> message) {
        final JsonArray clientsName = message.body().getArray("clients");
        final JsonObject json = getMandatoryObject("webContext", message);
        VertxWebContext webContext = decodeWebContext(json);
        JsonObject response = new JsonObject();

        for (Object clientName : clientsName) {
            final BaseClient client = (BaseClient) clients.findClient((String) clientName);
            logger.debug("client : " + client);
            RedirectAction action;
            try {
                action = client.getRedirectAction(webContext, false, false);
                response.putString((String) clientName, action.getLocation());
            } catch (RequiresHttpAction e) {

            }
        }

        response = encodeWebContext(webContext, response);

        sendOK(message, response);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doAuthenticate(final Message<JsonObject> message) {
        final JsonObject json = getMandatoryObject("webContext", message);
        VertxWebContext webContext = decodeWebContext(json);

        JsonObject response = new JsonObject();
        final Client client = clients.findClient(webContext);
        try {
            Credentials credentials = client.getCredentials(webContext);
            UserProfile userProfile = client.getUserProfile(credentials, webContext);
            response = response.putValue("userProfile", EventBusObjectConverter.encode(userProfile));
        } catch (RequiresHttpAction e) {
            logger.debug("requires HTTP action : " + e.getCode());
        }
        response = encodeWebContext(webContext, response);

        sendOK(message, response);
    }

    private JsonObject encodeWebContext(VertxWebContext webContext, JsonObject response) {
        response.putObject("sessionAttributes", webContext.getSessionAttributes());
        response.putString("content", webContext.getResponseContent());
        response.putNumber("code", webContext.getResponseStatus());
        response.putObject("headers", webContext.getResponseHeaders());
        return response;
    }

    private JsonObject encodeWebContext(VertxWebContext webContext) {
        return encodeWebContext(webContext, new JsonObject());
    }

    private VertxWebContext decodeWebContext(final JsonObject json) {
        VertxWebContext webContext = new VertxWebContext(json.getString("method"), json.getString("serverName"),
                json.getInteger("serverPort"), json.getString("fullUrl"), json.getString("scheme"),
                json.getObject("headers"), json.getObject("parameters"), json.getObject("sessionAttributes"));
        return webContext;
    }

}
