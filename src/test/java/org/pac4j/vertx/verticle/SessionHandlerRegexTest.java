package org.pac4j.vertx.verticle;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Simple test to validate the regular expression we will apply to session-related route handlers (for
 * some routes we don't want session/cookies/usersessionhandler active)
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class SessionHandlerRegexTest {

    @Test
    public void testRegexpExcludesCorrectly() {
        final Pattern pattern = pattern();
        assertThat(pattern.matcher("/dba/index.html").matches(), is(false));
        assertThat(pattern.matcher("/rest-jwt/index.html").matches(), is(false));
    }

    @Test
    public void testRegexpIncludesCorrectly() {
        final Pattern pattern = pattern();
        assertThat(pattern.matcher("/facebook/index.html").matches(), is(true));
        assertThat(pattern.matcher("/facebookadmin/index.html").matches(), is(true));
        assertThat(pattern.matcher("/facebookcustom/index.html").matches(), is(true));
        assertThat(pattern.matcher("/twitter/index.html").matches(), is(true));
        assertThat(pattern.matcher("/form/index.html").matches(), is(true));
        assertThat(pattern.matcher("/basicauth/index.html").matches(), is(true));
        assertThat(pattern.matcher("/cas/index.html").matches(), is(true));
        assertThat(pattern.matcher("/saml/index.html").matches(), is(true));
        assertThat(pattern.matcher("/oidc/index.html").matches(), is(true));
        assertThat(pattern.matcher("/protected/index.html").matches(), is(true));
        assertThat(pattern.matcher("/callback").matches(), is(true));
    }

    public Pattern pattern() {
        return Pattern.compile(DemoServerVerticle.SESSION_HANDLER_REGEXP); // This mimics the "starts with match" behaviour of regex route matching in vert.x
    }

}