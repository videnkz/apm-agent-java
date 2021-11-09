package co.elastic.apm.agent.jaxrs;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;

public class JaxRsTransactionNameInstrumentationTest extends AbstractJaxRsTransactionNameInstrumentationTest {

    /**
     * @return configuration for the jersey test server. Includes all resource classes in the co.elastic.apm.agent.jaxrs.resources package.
     */
    @Override
    protected Application configure() {
        return new ResourceConfig(
            ResourceWithPath.class,
            ResourceWithPathOnInterface.class,
            ResourceWithPathOnAbstract.class,
            ProxiedClass$$$view.class,
            ProxiedClass$Proxy.class,
            ResourceWithPathOnMethod.class,
            ResourceWithPathOnMethodSlash.class,
            MethodDelegationResource.class,
            FooBarResource.class,
            EmptyPathResource.class,
            ResourceWithPathAndWithPathOnInterface.class);
    }


    public interface SuperResourceInterface {
        @GET
        String testMethod();
    }

    @Path("testInterface")
    public interface ResourceInterfaceWithPath extends SuperResourceInterface {
        String testMethod();
    }

    public interface ResourceInterfaceWithoutPath extends SuperResourceInterface {
        String testMethod();
    }

    public abstract static class AbstractResourceClassWithoutPath implements ResourceInterfaceWithoutPath {
    }

    @Path("testAbstract")
    public abstract static class AbstractResourceClassWithPath implements ResourceInterfaceWithoutPath {
    }

    @Path("testViewProxy")
    public static class ProxiedClass$$$view implements SuperResourceInterface {
        public String testMethod() {
            return "ok";
        }
    }

    @Path("testProxyProxy")
    public static class ProxiedClass$Proxy implements SuperResourceInterface {
        public String testMethod() {
            return "ok";
        }
    }

    @Path("test")
    public static class ResourceWithPath extends AbstractResourceClassWithoutPath {
        public String testMethod() {
            return "ok";
        }
    }

    @Path("methodDelegation")
    public static class MethodDelegationResource {
        @GET
        @Path("methodA")
        public String methodA(){
            methodB();
            return "ok";
        }

        @POST
        public void methodB(){
        }
    }

    @Path("/foo/")
    public static class FooResource {
        @GET
        @Path("/ignore")
        public String testMethod() {
            return "ok";
        }
    }

    public static class FooBarResource extends FooResource {
        @GET
        @Path("/bar")
        @Override
        public String testMethod() {
            return "ok";
        }
    }

    @Path("testWithPathMethod")
    public static class ResourceWithPathOnMethod extends AbstractResourceClassWithoutPath {

        @Override
        public String testMethod() {
            return "ok";
        }

        @GET
        @Path("{id}/")
        public String testMethodById(@PathParam("id") String id) {
            return "ok";
        }
    }

    @Path("testWithPathMethodSlash")
    public static class ResourceWithPathOnMethodSlash extends AbstractResourceClassWithoutPath {

        @Override
        public String testMethod() {
            return "ok";
        }

        @GET
        @Path("/{id}")
        public String testMethodById(@PathParam("id") String id) {
            return "ok";
        }
    }

    @Path("")
    public static class EmptyPathResource {
        @GET
        public String testMethod() {
            return "ok";
        }
    }

    public static class ResourceWithPathAndWithPathOnInterface implements ResourceInterfaceWithPath {
        @Override
        @GET
        @Path("test")
        public String testMethod() {
            return "ok";
        }
    }

    public static class ResourceWithPathOnAbstract extends AbstractResourceClassWithPath {
        public String testMethod() {
            return "ok";
        }
    }

    public static class ResourceWithPathOnInterface implements ResourceInterfaceWithPath {
        public String testMethod() {
            return "ok";
        }
    }
}
