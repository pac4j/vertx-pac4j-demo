/*
  Copyright 2014 - 2014 pac4j organization

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

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Vert.x pac4j demo launcher.<br><br>
 * This launcher deploys either the stateful server if a sessionManager configuration is available or the stateless server otherwise.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class Pac4jDemo extends Verticle {

    @Override
    public void start() {

        container.deployModule("org.pac4j~vertx-pac4j-module~1.1.0",
                container.config().getObject("pac4jManager"));

        JsonObject sessionManagerConf = container.config().getObject("sessionManager");
        if (sessionManagerConf != null) {
            container.deployModule("com.campudus~session-manager~2.0.1-final", sessionManagerConf);
            container.deployVerticle("org.pac4j.vertx.DemoServerVerticle");
        } else {
            container.deployVerticle("org.pac4j.vertx.DemoRestServerVerticle");
        }
    }

}
