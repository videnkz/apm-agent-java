package co.elastic.apm.agent.microprofilerestclient;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@RegisterRestClient
@Path("/")
public interface WireMockRestService {

    @GET
    @Path("/")
    String dummyRequest();

    @GET
    @Path("/error")
    String errorRequest();

    @GET
    @Path("/redirect")
    String redirectRequest();

    @GET
    @Path("/circular-redirect")
    String circularRedirectRequest();
}
