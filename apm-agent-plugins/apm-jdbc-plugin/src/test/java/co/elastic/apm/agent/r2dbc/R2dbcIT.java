package co.elastic.apm.agent.r2dbc;

import io.r2dbc.spi.ConnectionFactories;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class R2dbcIT extends AbstractR2dbcInstrumentationTest {

    static {
        System.setProperty("oracle.jdbc.timezoneAsRegion", "false");
    }

    public R2dbcIT(String url, String expectedDbVendor) {
        super(Mono.from(ConnectionFactories.get(url).create()).block(), expectedDbVendor);
    }

    @Parameterized.Parameters(name = "{1} {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"r2dbc:tc:mariadb://hostname/databasename?TC_IMAGE_TAG=10", "mariadb"}
        });
    }
}
