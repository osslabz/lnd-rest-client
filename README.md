# lnd-rest-client

![GitHub](https://img.shields.io/github/license/osslabz/lnd-rest-client)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/osslabz/lnd-rest-client/build-on-push.yml?branch=dev&label=build&logo=git)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/osslabz/lnd-rest-client/release.yml?branch=dev&label=perform-release&logo=semanticrelease)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/net/osslabz/lnd-rest-client/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/net/osslabz/lnd-rest-client/README.md)
[![Maven Central](https://img.shields.io/maven-central/v/net.osslabz/lnd-rest-client?label=Maven%20Central)](https://search.maven.org/artifact/net.osslabz/lnd-rest-client)

lnd-http-client is a simple client to interact with [Lightning Network Daemon (LND)](https://github.com/lightningnetwork/lnd) via it's REST API.
Thanks to the [API's excellent documentation](https://lightning.engineering/api-docs/api/lnd/) the client is simply generated based on it's swagger
definition.

If you prefer LND's gRPC API please have a look at [LighntingJ](https://www.lightningj.org/).

1.0.1 is from June 2023 and is the only version on Maven Central; the 1.0.2 tag in this repo was never published. The hand-written code has
not changed since that release, and the only consumer is a private project of mine. Its tests run offline against a local HTTPS server.

## Why this client?

This library mostly exists because when I needed such a library [LighntingJ](https://www.lightningj.org/). wasn't (yet) supported on Apple Silicon....

Other advantages are:

- it's very easy to review (which you should always do when you handle any valuable assets, crypto or otherwise)
- it supports good old HTTP/1.1 while gRPC is build on http/2. In a perfect world we would all have migrated to http/2 already but there might be
  circumstances that might hinder its usage.
- It serves as my playground for reproducible builds via maven ;-)


## QuickStart

### Maven

```xml

<dependency>
    <groupId>net.osslabz</groupId>
    <artifactId>lnd-rest-client</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Snapshots

Every push to `dev` publishes the next version as a `-SNAPSHOT` to Central's snapshot repository. Maven doesn't
search that repository by default, so a build that wants a snapshot declares it:

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

### Usage


```java
    String lndHost="localhost";
    int lndPort=8080;

    String lndCertPath=Path.of(System.getProperty("user.home"),".lnd","tls.cert").toString();
    String lndMacaroonPath=Path.of(System.getProperty("user.home"),".lnd","data","chain","bitcoin","mainnet","readonly.macaroon").toString();

    LndApiClient lndApiClient=new LndApiClient(lndHost,lndPort,lndCertPath,lndMacaroonPath,true);

    LnrpcNetworkInfo lnrpcNetworkInfo=lndApiClient.getLightningApi().getNetworkInfo();

    log.debug("network info: {}",lnrpcNetworkInfo);
```        

### Logging

The actual client uses slf4j-api but doesn't package an implementation. This is up to the using application.