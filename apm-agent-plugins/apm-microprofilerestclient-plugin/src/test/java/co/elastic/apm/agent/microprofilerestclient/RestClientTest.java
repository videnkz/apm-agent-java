package co.elastic.apm.agent.microprofilerestclient;

import co.elastic.apm.agent.httpclient.AbstractHttpClientInstrumentationTest;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.Before;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;

public class RestClientTest extends AbstractHttpClientInstrumentationTest {

//    @RestClient
    WireMockRestService wireMockRestService;

    @Before
    public void setUp() throws URISyntaxException {
        String baseUrl = wireMockRule.baseUrl();
        wireMockRestService = RestClientBuilder
            .newBuilder()
            .baseUri(new URI(baseUrl))
            .build(WireMockRestService.class);
    }

    @Override
    protected void performGet(String path) throws Exception {
        int lastIndexOfSlashInPath = path.lastIndexOf("/");
        String pathValue = path.substring(lastIndexOfSlashInPath);
        switch (pathValue) {
            case "/":
                wireMockRestService.dummyRequest();
                break;
            case "/error":
                wireMockRestService.errorRequest();
                break;
            case "/redirect":
                wireMockRestService.redirectRequest();
                break;
            case "/circular-redirect":
                wireMockRestService.circularRedirectRequest();
                break;
            default:
                throw new IllegalStateException("Not supported path: " + path);
        }
    }
}
