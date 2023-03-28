# lnd-rest-client

![GitHub](https://img.shields.io/github/license/osslabz/lnd-http-client)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/osslabz/lnd-http-client/maven.yml?branch=main)
[![Maven Central](https://img.shields.io/maven-central/v/net.osslabz/lnd-http-client?label=Maven%20Central)](https://search.maven.org/artifact/net.osslabz/lnd-http-client)

lnd-http-client is a simple client to interact with [Lightning Network Daemon (LND)](https://github.com/lightningnetwork/lnd) via it's REST API.
Thanks to the [API's excellent documentation](https://lightning.engineering/api-docs/api/lnd/) the client is simply generated based on it's swagger
definition.

If you prefer LND's gRPC API please have a look at [LighntingJ](https://www.lightningj.org/).

## Why this client?

This library mostly exists because when I needed such a library lighninj was't (yet) supported on Apple Silicon....

Other advantages are

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
    <version>0.0.1</version>
</dependency>
```

### Usage


```java
    String lndHost="127.0.0.1";
    int lndPort=8080;

    String lndCertPath="/path/to/tls.cert";
    String lndMacaroonPath="/path/to/readonly.macaroon";

    LndApiClient lndApiClient=new LndApiClient(lndHost,lndPort,lndCertPath,lndMacaroonPath,true);

    LnrpcNetworkInfo lnrpcNetworkInfo=lndApiClient.getLightningApi().getNetworkInfo();

    log.debug("network info: {}",lnrpcNetworkInfo);
```        

### Logging

The actual client uses slf4j-api but doesn't package an implementation. This is up to the using application.