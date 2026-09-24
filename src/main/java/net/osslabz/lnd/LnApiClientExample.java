package net.osslabz.lnd;

import java.nio.file.Path;
import net.osslabz.lnd.dto.LnrpcNetworkInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is not an actual unit test with any real verification, it just exists to validate that the generated classes keep the expected signature. As long as it compiles, it's fine -:-)
 */
public class LnApiClientExample {

    private static Logger log = LoggerFactory.getLogger(LnApiClientExample.class);

    public static void main(String[] args) throws Exception {

        String lndHost = "localhost";
        int lndPort = 8080;

        String lndCertPath =
                Path.of(System.getProperty("user.home"), ".lnd", "tls.cert").toString();
        String lndMacaroonPath = Path.of(
                        System.getProperty("user.home"),
                        ".lnd",
                        "data",
                        "chain",
                        "bitcoin",
                        "mainnet",
                        "readonly.macaroon")
                .toString();

        LndApiClient lndApiClient = new LndApiClient(lndHost, lndPort, lndCertPath, lndMacaroonPath, true);

        LnrpcNetworkInfo lnrpcNetworkInfo = lndApiClient.getLightningApi().getNetworkInfo();

        log.debug("network info: {}", lnrpcNetworkInfo);
    }
}
