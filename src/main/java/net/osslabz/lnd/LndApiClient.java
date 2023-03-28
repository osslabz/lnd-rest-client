package net.osslabz.lnd;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import net.osslabz.lnd.api.LightningApi;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

public class LndApiClient {


    private final ApiClient apiClient;

    private final LightningApi lightningClient;


    public LndApiClient(String host, int port, String lndCertPath, String lndMacaroonPath) {
        this(createApiClient(host, port, lndCertPath, lndMacaroonPath, false));
    }


    public LndApiClient(String host, int port, String lndCertPath, String lndMacaroonPath, boolean debug) {
        this(createApiClient(host, port, lndCertPath, lndMacaroonPath, debug));
    }


    public LndApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.lightningClient = new LightningApi(this.apiClient);
    }


    public LightningApi getLightningApi() {
        return this.lightningClient;
    }


    private static ApiClient createApiClient(String host, int port, String lndCertPath, String lndMacaroonPath, boolean debug) {
        try {
            ApiClient apiClient = new ApiClient();
            apiClient.setBasePath("https://" + host + ":" + port);
            apiClient.setSslCaCert(new BufferedInputStream(new FileInputStream(lndCertPath)));
            apiClient.setConnectTimeout(10 * 1000);
            apiClient.setReadTimeout(60 * 1000);
            apiClient.addDefaultHeader("Grpc-Metadata-macaroon", Hex.encodeHexString(IOUtils.toByteArray(new BufferedInputStream(new FileInputStream(lndMacaroonPath)))));
            apiClient.setDebugging(debug);
            return apiClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}