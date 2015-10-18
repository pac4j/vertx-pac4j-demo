Please note that branch 1.0.0 of this code is the version for use with vertx-pac4j v1 and vert.x v 2.

Master is for use with vertx-pac4j 2 and vert.x 3 onwards.

## What is this project ?

This **vertx-pac4j-demo** project is a Java web demo to test the [vertx-pac4j library](https://github.com/pac4j/vertx-pac4j) with Facebook, Twitter, form authentication, basic auth, CAS...  
The **vertx-pac4j** library is built to:

- delegate authentication to a provider and be authenticated back in the protected application with a complete user profile retrieved from the provider
- support direct authenticated calls.


## Quick start & test

To build the demo:

    cd vertx-pac4j-demo
    mvn install

You can then run the stateful demo from maven with:

    mvn vertx:runMod

or run the stateless demo:

    mvn vertx:runMod -Prest

If you have vert.x installed, run the module from the repository:

    vertx runMod org.pac4j~vertx-pac4j-demo~1.0.0

Browse the demo on *http://localhost:8080*
