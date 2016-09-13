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
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class CustomAuthorizer extends ProfileAuthorizer<CommonProfile> {

    @Override
    protected boolean isProfileAuthorized(WebContext context, CommonProfile profile) throws HttpAction {
        if (profile == null) {
            return false;
        }
        return StringUtils.startsWith(profile.getUsername(), "jle");
    }

    @Override
    public boolean isAuthorized(WebContext context, List<CommonProfile> profiles) throws HttpAction {
        return isAnyAuthorized(context, profiles);
    }
}