/*
  Copyright 2015 - 2015 pac4j organization

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
package org.pac4j.vertx.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.sstore.SessionStore;
import org.pac4j.cas.client.CasClient;
import org.pac4j.core.authorization.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.pac4j.vertx.authorizer.CustomAuthorizer;
import org.pac4j.vertx.cas.VertxLocalSharedDataLogoutHandler;

import java.io.File;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class Pac4jConfigurationFactory {

    private static final Logger LOG = LoggerFactory.getLogger(Pac4jConfigurationFactory.class);
    public static final String AUTHORIZER_ADMIN = "admin";
    public static final String AUTHORIZER_CUSTOM = "custom";

    public static Config configFor(final JsonObject jsonConf, final Vertx vertx, final SessionStore sessionStore) {
        final String baseUrl = jsonConf.getString("baseUrl");
        final Clients clients = new Clients(baseUrl + "/callback",
                // oAuth clients
                facebookClient(jsonConf),
                twitterClient(),
                casClient(jsonConf, vertx, sessionStore),
                saml2Client(),
                formClient(baseUrl));
        final Config config = new Config(clients);
        config.addAuthorizer(AUTHORIZER_ADMIN, new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
        config.addAuthorizer(AUTHORIZER_CUSTOM, new CustomAuthorizer());
        LOG.info("Config created " + config.toString());
        return config;
    }

    public static FacebookClient facebookClient(final JsonObject jsonConf) {
        final String fbId = jsonConf.getString("fbId");
        final String fbSecret = jsonConf.getString("fbSecret");
        return new FacebookClient(fbId, fbSecret);
    }

    public static TwitterClient twitterClient() {
        return new TwitterClient("K9dtF7hwOweVHMxIr8Qe4gshl",
                "9tlc3TBpl5aX47BGGgMNC8glDqVYi8mJKHG6LiWYVD4Sh1F9Oj");
    }

    public static CasClient casClient(final JsonObject jsonConf, final Vertx vertx, final SessionStore sessionStore) {
        final String casUrl = jsonConf.getString("casUrl");
        final CasClient casClient = new CasClient();
        casClient.setLogoutHandler(new VertxLocalSharedDataLogoutHandler(vertx, sessionStore));
        casClient.setCasProtocol(CasClient.CasProtocol.CAS20);
        casClient.setCasLoginUrl(casUrl);
        return casClient;
    }

    public static SAML2Client saml2Client() {
        final SAML2ClientConfiguration cfg = new SAML2ClientConfiguration("resource:samlKeystore.jks",
                "pac4j-demo-passwd", "pac4j-demo-passwd", "resource:openidp-feide.xml");
        cfg.setMaximumAuthenticationLifetime(3600);
        cfg.setServiceProviderEntityId("urn:mace:saml:vertx-demo.pac4j.org");
        cfg.setServiceProviderMetadataPath(new File("target", "sp-metadata.xml").getAbsolutePath());
        final SAML2Client saml2Client = new SAML2Client(cfg);
        return saml2Client;
    }

    public static FormClient formClient(final String baseUrl) {
        return new FormClient(baseUrl + "/loginForm", new SimpleTestUsernamePasswordAuthenticator());
    }

}
