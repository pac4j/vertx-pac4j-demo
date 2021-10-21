package org.pac4j.vertx.authorizer;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;

import java.util.List;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class CustomAuthorizer extends ProfileAuthorizer {

    @Override
    protected boolean isProfileAuthorized(WebContext context, SessionStore sessionStore, UserProfile profile) {
        if (profile == null) {
            return false;
        }
        return StringUtils.startsWith(profile.getUsername(), "jle");
    }

    @Override
    public boolean isAuthorized(WebContext context, SessionStore sessionStore, List<UserProfile> profiles) {
        return isAnyAuthorized(context, sessionStore, profiles);
    }
}