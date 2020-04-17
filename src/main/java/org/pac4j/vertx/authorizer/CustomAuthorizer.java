package org.pac4j.vertx.authorizer;

import org.apache.commons.lang.StringUtils;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class CustomAuthorizer extends ProfileAuthorizer<CommonProfile> {

    @Override
    protected boolean isProfileAuthorized(WebContext context, CommonProfile profile) {
        if (profile == null) {
            return false;
        }
        return StringUtils.startsWith(profile.getUsername(), "jle");
    }

    @Override
    public boolean isAuthorized(WebContext context, List<CommonProfile> profiles) {
        return isAnyAuthorized(context, profiles);
    }
}