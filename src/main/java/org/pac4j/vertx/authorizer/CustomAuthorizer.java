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
package org.pac4j.vertx.authorizer;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.authorization.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.profile.HttpProfile;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class CustomAuthorizer implements Authorizer {

    public boolean isAuthorized(WebContext context, UserProfile profile) {
        if (profile == null) {
            return false;
        }
        if (!(profile instanceof HttpProfile)) {
            return false;
        }
        final HttpProfile httpProfile = (HttpProfile) profile;
        final String username = httpProfile.getUsername();
        return StringUtils.startsWith(username, "jle");
    }
}