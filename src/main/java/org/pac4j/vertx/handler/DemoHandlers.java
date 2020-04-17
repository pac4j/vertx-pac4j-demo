package org.pac4j.vertx.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.handler.impl.LogoutHandler;
import org.pac4j.vertx.handler.impl.LogoutHandlerOptions;
import org.pac4j.vertx.handler.impl.SecurityHandler;
import org.pac4j.vertx.handler.impl.SecurityHandlerOptions;
import org.pac4j.vertx.http.VertxHttpActionAdapter;

import java.util.List;
import java.util.function.BiConsumer;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 * A collection of basic handlers printing dynamic html for the demo application.
 * 
 * @author Michael Remond/Jeremy Prime
 * @since 1.0.0
 *
 */
public class DemoHandlers {

    public static Handler<RoutingContext> indexHandler(final Vertx vertx, final SessionStore<VertxWebContext> sessionStore) {
        final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(vertx);

        return rc -> {
            final List<CommonProfile> profile = getUserProfiles(rc, sessionStore);

            final JsonObject json = new JsonObject();
            json.put("name", "Vert.x Web").put("userProfiles", profile);

            // and now delegate to the engine to render it.
            engine.render(json, "templates/index.hbs", res -> {
                if (res.succeeded()) {
                    rc.response().end(res.result());
                } else {
                    rc.fail(res.cause());
                }
            });
        };
    }

    public static Handler<RoutingContext> setContentTypeHandler(final CharSequence contentType) {
        return rc -> {
            rc.response().putHeader(CONTENT_TYPE, contentType);
            rc.next();
        };
    }

    public static Handler<RoutingContext> authHandler(final Vertx vertx,
                                                      final SessionStore<VertxWebContext> sessionStore,
                                                      final Config config,
                                                      final Pac4jAuthProvider provider,
                                                      final SecurityHandlerOptions options) {
        return new SecurityHandler(vertx, sessionStore, config, provider, options);
    }

    public static Handler<RoutingContext> logoutHandler(final Vertx vertx, final Config config,
                                                        final SessionStore<VertxWebContext> sessionStore) {
        return new LogoutHandler(vertx, sessionStore, new LogoutHandlerOptions(), config);
    }

    public static Handler<RoutingContext> centralLogoutHandler(final Vertx vertx, final Config config,
                                                        final SessionStore<VertxWebContext> sessionStore) {
        final LogoutHandlerOptions options = new LogoutHandlerOptions()
                .setCentralLogout(true)
                .setLocalLogout(false)
                .setDefaultUrl("http://localhost:8080/?defaulturlaftercentrallogout");
        return new LogoutHandler(vertx, sessionStore, options, config);
    }


    public static Handler<RoutingContext> protectedIndexHandler(final Vertx vertx, final SessionStore<VertxWebContext> sessionStore) {
        return generateProtectedIndex(vertx, (rc, buf) -> rc.response().end(buf), sessionStore);
    }

    public static Handler<RoutingContext> loginFormHandler(final Vertx vertx, final Config config) {
        final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(vertx);
        final FormClient formClient = (FormClient) config.getClients().findClient("FormClient").get();
        final String url = formClient.getCallbackUrl();

        return rc -> {

            final JsonObject json = new JsonObject();
            json.put("url", url);

            engine.render(json, "templates/loginForm.hbs", res -> {
                if (res.succeeded()) {
                    rc.response().end(res.result());
                } else {
                    rc.fail(res.cause());
                }
            });
        };
    }

    public static Handler<RoutingContext> formIndexJsonHandler(final Vertx vertx, final SessionStore<VertxWebContext> sessionStore) {

        return generateProtectedIndex(vertx, (rc, buf) -> {
            final JsonObject json = new JsonObject()
                    .put("content", buf.toString());
            rc.response().end(json.encodePrettily());
        }, sessionStore);

    }

    public static Handler<RoutingContext> jwtGenerator(final Vertx vertx, final JsonObject jsonConf,
                                                       final SessionStore<VertxWebContext> sessionStore) {

        final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(vertx);

        return rc -> {
            final List<CommonProfile> profiles = getUserProfiles(rc, sessionStore);
            final JwtGenerator generator = new JwtGenerator(new SecretSignatureConfiguration(jsonConf.getString("jwtSalt")));
            String token = "";
            if (CommonHelper.isNotEmpty(profiles)) {
                token = generator.generate(profiles.get(0));
            }

            final JsonObject json = new JsonObject();
            json.put("token", token);

            engine.render(json, "templates/jwt.hbs", res -> {
                if (res.succeeded()) {
                    rc.response().end(res.result());
                } else {
                    rc.fail(res.cause());
                }
            });
        };
    }

    public static Handler<RoutingContext> generateProtectedIndex(final Vertx vertx,
                                                                 final BiConsumer<RoutingContext, Buffer> generatedContentConsumer,
                                                                 final SessionStore<VertxWebContext> sessionStore) {
        final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(vertx);

        return rc -> {
            final List<CommonProfile> profile = getUserProfiles(rc, sessionStore);

            final JsonObject json = new JsonObject();
            json.put("userProfiles", profile);

            engine.render(json, "templates/protectedIndex.hbs", res -> {
                if (res.succeeded()) {
                    generatedContentConsumer.accept(rc, res.result());
                } else {
                    rc.fail(res.cause());
                }
            });
        };
    }

    public static Handler<RoutingContext> forceLogin(final Config config, final SessionStore<VertxWebContext> sessionStore) {
        return rc -> {
            final VertxWebContext context = new VertxWebContext(rc, sessionStore);
            final Client client = config.getClients().findClient(context.getRequestParameter(Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER).get()).get();
            try {
                final RedirectionAction action = (RedirectionAction) client.getRedirectionAction(context).get();
                VertxHttpActionAdapter.INSTANCE.adapt(action, context);
            } catch (final HttpAction exceptionAction) {
                rc.fail(exceptionAction);
                VertxHttpActionAdapter.INSTANCE.adapt(exceptionAction, context);
            }
        };
    }

    private static List<CommonProfile> getUserProfiles(final RoutingContext rc, final SessionStore<VertxWebContext> sessionStore) {
        final ProfileManager<CommonProfile> profileManager = new VertxProfileManager(new VertxWebContext(rc, sessionStore));
        return profileManager.getAll(true);
    }
}
