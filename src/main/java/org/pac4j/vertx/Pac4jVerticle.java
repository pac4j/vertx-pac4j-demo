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
import org.pac4j.core.client.Clients;
import org.pac4j.http.client.BasicAuthClient;
import org.pac4j.http.client.FormClient;
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.saml.client.Saml2Client;
import org.pac4j.vertx.handlers.CallbackHandler;
import org.pac4j.vertx.handlers.DemoHandlers;
import org.pac4j.vertx.handlers.LogoutHandler;
import org.pac4j.vertx.handlers.RequiresAuthenticationHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

/**
 * Vert.x pac4j demo verticle. This verticle is currently a worker because some authentication mechanisms require
 * blocking operations. 
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class Pac4jVerticle extends Verticle {

    @Override
    public void start() {

        Clients clients = buildClients();
        Config.setClients(clients);

        RouteMatcher rm = new RouteMatcher();

        rm.get("/facebook/index.html", new RequiresAuthenticationHandler("FacebookClient",
                DemoHandlers.authenticatedHandler));
        rm.get("/twitter/index.html", new RequiresAuthenticationHandler("TwitterClient",
                DemoHandlers.authenticatedHandler));
        rm.get("/form/index.html", new RequiresAuthenticationHandler("FormClient", DemoHandlers.authenticatedHandler));
        rm.get("/form/index.html.json", new RequiresAuthenticationHandler("FormClient", true,
                DemoHandlers.authenticatedJsonHandler));
        rm.get("/basicauth/index.html", new RequiresAuthenticationHandler("BasicAuthClient",
                DemoHandlers.authenticatedHandler));
        rm.get("/cas/index.html", new RequiresAuthenticationHandler("CasClient", DemoHandlers.authenticatedHandler));
        rm.get("/saml2/index.html", new RequiresAuthenticationHandler("Saml2Client", DemoHandlers.authenticatedHandler));

        rm.get("/theForm.html", DemoHandlers.formHandler);

        CallbackHandler callback = new CallbackHandler();
        rm.get("/callback", callback);
        rm.post("/callback", callback);

        rm.get("/logout", new LogoutHandler());

        rm.get("/", DemoHandlers.indexHandler);

        rm.get("/assets/js/app.js", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                req.response().sendFile("./app.js");
            }
        });

        vertx.createHttpServer().requestHandler(rm).listen(8080, "localhost");

    }

    private Clients buildClients() {
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
}
