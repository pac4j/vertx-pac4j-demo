package org.pac4j.vertx.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.sstore.SessionStore;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.ParameterClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.StravaClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.vertx.authorizer.CustomAuthorizer;

import java.io.File;
import java.util.Optional;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class Pac4jConfigurationFactory implements ConfigFactory {

    private static final Logger LOG = LoggerFactory.getLogger(Pac4jConfigurationFactory.class);
    public static final String AUTHORIZER_ADMIN = "admin";
    public static final String AUTHORIZER_CUSTOM = "custom";

    private final JsonObject jsonConf;
    private final Vertx vertx;
    private final SessionStore sessionStore;

    public Pac4jConfigurationFactory(final JsonObject jsonConf, final Vertx vertx, final SessionStore sessionStore) {
        this.jsonConf = jsonConf;
        this.vertx = vertx;
        this.sessionStore = sessionStore;
    }

    @Override
    public Config build(Object... parameters) {
        final String baseUrl = jsonConf.getString("baseUrl");

        // REST authent with JWT for a token passed in the url as the token parameter
        final String jwtSalt = jsonConf.getString("jwtSalt");
        final ParameterClient parameterClient = new ParameterClient("token",
                new JwtAuthenticator(new SecretSignatureConfiguration(jwtSalt)));
        parameterClient.setSupportGetRequest(true);
        parameterClient.setSupportPostRequest(false);

        // basic auth
        final DirectBasicAuthClient directBasicAuthClient = new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

        final Clients clients = new Clients(baseUrl + "/callback",
                // oAuth clients
                facebookClient(jsonConf),
                twitterClient(),
                casClient(jsonConf, vertx, sessionStore),
                saml2Client(),
                formClient(baseUrl),
                directBasicAuthClient(),
                oidcClient(),
                stravaClient(),
                parameterClient,
                directBasicAuthClient,
                new AnonymousClient());
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
        return new TwitterClient("CoxUiYwQOSFDReZYdjigBA",
                "2kAzunH5Btc4gRSaMr7D7MkyoJ5u1VzbOOzE8rBofs");
    }

    public static CasClient casClient(final JsonObject jsonConf, final Vertx vertx, final SessionStore sessionStore) {
        final String casUrl = jsonConf.getString("casUrl");
        final CasConfiguration casConfiguration = new CasConfiguration(casUrl);
        /*final CasProxyReceptor casProxyReceptor = new CasProxyReceptor();
        casConfiguration.setProxyReceptor(casProxyReceptor);*/
        final CasClient casClient = new CasClient(casConfiguration);
        return casClient;
    }

    public static SAML2Client saml2Client() {

        final SAML2Configuration cfg = new SAML2Configuration("samlConfig/samlKeystore.jks",
                "pac4j-demo-passwd",
                "pac4j-demo-passwd",
                "samlConfig/metadata-okta.xml");
        cfg.setMaximumAuthenticationLifetime(3600);
        cfg.setServiceProviderEntityId("http://localhost:8080/callback?client_name=SAML2Client");
        cfg.setServiceProviderMetadataPath(new File("target", "sp-metadata.xml").getAbsolutePath());
        return new SAML2Client(cfg);
    }

    public static FormClient formClient(final String baseUrl) {
        return new FormClient(baseUrl + "/loginForm", new SimpleTestUsernamePasswordAuthenticator());
    }

    public static IndirectBasicAuthClient directBasicAuthClient() {
        return new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());
    }

    public static StravaClient stravaClient() {
        final StravaClient stravaClient = new StravaClient();
        stravaClient.setApprovalPrompt("auto");
        // client_id
        stravaClient.setKey("3945");
        // client_secret
        stravaClient.setSecret("f03df80582396cddfbe0b895a726bac27c8cf739");
        stravaClient.setScope("view_private");
        return stravaClient;
    }

    public static OidcClient oidcClient() {
        // OpenID Connect
        final OidcConfiguration oidcConfiguration = new OidcConfiguration();
        oidcConfiguration.setClientId("736887899191-s2lsd8pakdjugkbp6v3lou7jd631rka2.apps.googleusercontent.com");
        oidcConfiguration.setSecret("18B4WAQgzs2RhUY8V_Pl0qSh");
        oidcConfiguration.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration");
        oidcConfiguration.addCustomParam("prompt", "consent");
        final OidcClient oidcClient = new OidcClient(oidcConfiguration);
        oidcClient.addAuthorizationGenerator((ctx, profile) -> { profile.addRole("ROLE_ADMIN"); return Optional.of(profile); });
        return oidcClient;
    }
}
