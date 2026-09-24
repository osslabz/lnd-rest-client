package net.osslabz.lnd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.net.ssl.SSLHandshakeException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import net.osslabz.lnd.dto.LnrpcNetworkInfo;
import okhttp3.OkHttpClient;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LndApiClientTest {

    /** One address, so a failed handshake is not retried on a second route to the same name. */
    private static final String HOST = "127.0.0.1";

    private static final byte[] MACAROON = {0x02, 0x01, 0x03, (byte) 0xab, (byte) 0xff};

    private static final String MACAROON_HEX = "020103abff";

    private static final String NETWORK_INFO = """
            {"num_nodes":"42","num_channels":"7"}""";

    /** OkHttp's HttpLoggingInterceptor writes through java.util.logging under this name. */
    private final Logger okHttpLogger = Logger.getLogger(OkHttpClient.class.getName());

    private final List<String> okHttpLog = new CopyOnWriteArrayList<>();

    private final Handler okHttpLogCapture = new Handler() {
        @Override
        public void publish(LogRecord logRecord) {
            okHttpLog.add(logRecord.getMessage());
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    };

    @TempDir
    private Path dir;

    private MockWebServer server;

    @BeforeEach
    void createServer() {
        server = new MockWebServer();
    }

    @BeforeEach
    void captureOkHttpLog() {
        okHttpLogger.addHandler(okHttpLogCapture);
        okHttpLogger.setUseParentHandlers(false);
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @AfterEach
    void releaseOkHttpLog() {
        okHttpLogger.setUseParentHandlers(true);
        okHttpLogger.removeHandler(okHttpLogCapture);
    }

    private static HeldCertificate nodeCertificate() {
        return new HeldCertificate.Builder().addSubjectAlternativeName(HOST).build();
    }

    private void serveTls(HeldCertificate certificate) throws IOException {
        HandshakeCertificates serverCertificates =
                new HandshakeCertificates.Builder().heldCertificate(certificate).build();
        server.useHttps(serverCertificates.sslSocketFactory());
        start();
    }

    private void start() throws IOException {
        server.start(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0);
    }

    private String certFile(HeldCertificate certificate) throws IOException {
        return Files.writeString(dir.resolve("tls.cert"), certificate.certificatePem(), StandardCharsets.US_ASCII)
                .toString();
    }

    private String macaroonFile() throws IOException {
        return Files.write(dir.resolve("readonly.macaroon"), MACAROON).toString();
    }

    private void respond(int code, String body) {
        server.enqueue(new MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build());
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        return server.takeRequest(5, TimeUnit.SECONDS);
    }

    @Test
    void callsTheNodeOverTlsTrustingItsCertificateAndSendsTheMacaroonAsHex() throws Exception {
        HeldCertificate certificate = nodeCertificate();
        serveTls(certificate);
        respond(200, NETWORK_INFO);
        LndApiClient client = new LndApiClient(HOST, server.getPort(), certFile(certificate), macaroonFile());

        LnrpcNetworkInfo networkInfo = client.getLightningApi().getNetworkInfo();

        assertEquals(42L, networkInfo.getNumNodes());
        assertEquals(7L, networkInfo.getNumChannels());
        RecordedRequest request = takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/v1/graph/info", request.getUrl().encodedPath());
        assertEquals(MACAROON_HEX, request.getHeaders().get("Grpc-Metadata-macaroon"));
        assertTrue(okHttpLog.isEmpty(), () -> "logged without debug: " + okHttpLog);
    }

    @Test
    void logsTheExchangeWhenDebugIsOn() throws Exception {
        HeldCertificate certificate = nodeCertificate();
        serveTls(certificate);
        respond(200, NETWORK_INFO);
        LndApiClient client = new LndApiClient(HOST, server.getPort(), certFile(certificate), macaroonFile(), true);

        client.getLightningApi().getNetworkInfo();

        assertTrue(
                okHttpLog.contains("--> GET https://" + HOST + ":" + server.getPort() + "/v1/graph/info"),
                () -> "request line missing from " + okHttpLog);
        assertTrue(okHttpLog.contains(NETWORK_INFO), () -> "response body missing from " + okHttpLog);
    }

    @Test
    void refusesANodeThatPresentsAnotherCertificate() throws Exception {
        serveTls(nodeCertificate());
        LndApiClient client = new LndApiClient(HOST, server.getPort(), certFile(nodeCertificate()), macaroonFile());

        ApiException thrown =
                assertThrows(ApiException.class, () -> client.getLightningApi().getNetworkInfo());

        assertInstanceOf(SSLHandshakeException.class, thrown.getCause());
    }

    @Test
    void reportsAnErrorResponseWithItsStatusAndBody() throws Exception {
        HeldCertificate certificate = nodeCertificate();
        serveTls(certificate);
        String error = """
                {"code":2,"message":"permission denied","details":[]}""";
        respond(500, error);
        LndApiClient client = new LndApiClient(HOST, server.getPort(), certFile(certificate), macaroonFile());

        ApiException thrown =
                assertThrows(ApiException.class, () -> client.getLightningApi().getNetworkInfo());

        assertEquals(500, thrown.getCode());
        assertEquals(error, thrown.getResponseBody());
    }

    @Test
    void failsOnAMissingCertificateFile() throws Exception {
        String missingCert = dir.resolve("missing.cert").toString();
        String macaroon = macaroonFile();

        RuntimeException thrown =
                assertThrows(RuntimeException.class, () -> new LndApiClient(HOST, 8080, missingCert, macaroon));

        assertInstanceOf(FileNotFoundException.class, thrown.getCause());
    }

    @Test
    void failsOnAMissingMacaroonFile() throws Exception {
        String cert = certFile(nodeCertificate());
        String missingMacaroon = dir.resolve("missing.macaroon").toString();

        RuntimeException thrown =
                assertThrows(RuntimeException.class, () -> new LndApiClient(HOST, 8080, cert, missingMacaroon));

        assertInstanceOf(FileNotFoundException.class, thrown.getCause());
    }

    @Test
    void buildsTheLightningApiOnTheSuppliedApiClient() throws Exception {
        start();
        respond(200, NETWORK_INFO);
        ApiClient apiClient = new ApiClient().setBasePath("http://" + HOST + ":" + server.getPort());
        LndApiClient client = new LndApiClient(apiClient);

        LnrpcNetworkInfo networkInfo = client.getLightningApi().getNetworkInfo();

        assertSame(apiClient, client.getLightningApi().getApiClient());
        assertEquals(42L, networkInfo.getNumNodes());
        RecordedRequest request = takeRequest();
        assertEquals("/v1/graph/info", request.getUrl().encodedPath());
        assertNull(request.getHeaders().get("Grpc-Metadata-macaroon"));
    }
}
