package co.elastic.apm.agent.r2dbc;

import co.elastic.apm.agent.AbstractInstrumentationTest;
import co.elastic.apm.agent.db.signature.SignatureParser;
import co.elastic.apm.agent.impl.context.Db;
import co.elastic.apm.agent.impl.context.Destination;
import co.elastic.apm.agent.impl.transaction.Outcome;
import co.elastic.apm.agent.impl.transaction.Span;
import co.elastic.apm.agent.impl.transaction.Transaction;
import co.elastic.apm.agent.jdbc.helper.JdbcGlobalState;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Statement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static co.elastic.apm.agent.jdbc.helper.JdbcHelper.DB_SPAN_ACTION;
import static co.elastic.apm.agent.jdbc.helper.JdbcHelper.DB_SPAN_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class AbstractR2dbcInstrumentationTest extends AbstractInstrumentationTest {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private static final String PREPARED_STATEMENT_SQL = "SELECT * FROM ELASTIC_APM WHERE FOO=?";
    private static final String UPDATE_PREPARED_STATEMENT_SQL = "UPDATE ELASTIC_APM SET BAR=? WHERE FOO=11";
    private static final long PREPARED_STMT_TIMEOUT = 10000;

    private final String expectedDbVendor;
    private Connection connection;
    @Nullable
    private Statement statement;
    @Nullable
    private Statement updateStatement;

    private final Transaction transaction;
    private final SignatureParser signatureParser;

    AbstractR2dbcInstrumentationTest(Connection connection, String expectedDbVendor) {
        this.connection = connection;
        this.expectedDbVendor = expectedDbVendor;

        Mono.from(connection.createStatement("CREATE TABLE ELASTIC_APM (FOO INT NOT NULL, BAR VARCHAR(255))").execute()).block();
        Mono.from(connection.createStatement("ALTER TABLE ELASTIC_APM ADD PRIMARY KEY (FOO)").execute()).block();

        transaction = startTestRootTransaction("r2dbc-test");
        signatureParser = new SignatureParser();
    }

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        statement = EXECUTOR_SERVICE.submit(new Callable<Statement>() {
            @Override
            public Statement call() throws Exception {
                return connection.createStatement(PREPARED_STATEMENT_SQL);
            }
        }).get(PREPARED_STMT_TIMEOUT, TimeUnit.MILLISECONDS);
        updateStatement = EXECUTOR_SERVICE.submit(new Callable<Statement>() {
            @Override
            public Statement call() throws Exception {
                return connection.createStatement(UPDATE_PREPARED_STATEMENT_SQL);
            }
        }).get(PREPARED_STMT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @After
    public void tearDown() {
        if (statement != null) {
            System.out.println(String.format("Statement is not null"));
        }
        statement = null;
        Mono.from(connection.createStatement("DROP TABLE ELASTIC_APM").execute()).block();
        Mono.from(connection.close()).block();
        transaction.deactivate().end();
    }

    @Test
    public void test() {
        executeTest(this::testStatement);
    }

    private void executeTest(R2dbcTask task) throws R2dbcException {
        Mono.from(connection.createStatement("INSERT INTO ELASTIC_APM (FOO, BAR) VALUES (1, 'APM')").execute()).block();
        Mono.from(connection.createStatement("INSERT INTO ELASTIC_APM (FOO, BAR) VALUES (11, 'BEFORE')").execute()).block();
        reporter.reset();
        try {
            task.execute();
        } catch (R2dbcException e) {
            fail("unexpected exception", e);
        } finally {
            try {
                Mono.from(connection.createStatement("DELETE FROM ELASTIC_APM").execute()).block();
            } catch (R2dbcException e) {
                e.printStackTrace();
            }
            // reset reporter is important otherwise one test may pollute results of the following test
            reporter.reset();

            // clear internal jdbc helper required due to metadata caching and global state about unsupported
            // JDBC driver features (based on classes instances)
            JdbcGlobalState.clearInternalStorage();
        }
    }

    private interface R2dbcTask {
        void execute() throws R2dbcException;
    }

    private void testStatement() throws R2dbcException {
        final String sql = "SELECT FOO, BAR FROM ELASTIC_APM WHERE FOO=1";
        Statement statement = connection.createStatement(sql);
        AtomicBoolean isCheckRowData = new AtomicBoolean(false);
        Flux.from(statement.execute()).flatMap(result ->
            result.map((row, metadata) -> {
                    Integer foo = row.get(0, Integer.class);
                    String bar = row.get(1, String.class);
                    System.out.println(String.format("Foo = %s, bar = %s", foo, bar));
                    assertThat(foo).isEqualTo(1);
                    assertThat(bar).isEqualTo("APM");
                    isCheckRowData.set(true);
                    return "handle";
                }
            )).blockLast();
        assertThat(isCheckRowData).isTrue();
        Span span = assertSpanRecorded(sql, false, -1);
        assertThat(span.getOutcome()).isEqualTo(Outcome.SUCCESS);
    }

    private Span assertSpanRecorded(String rawSql, boolean preparedStatement, long expectedAffectedRows) throws R2dbcException {
        assertThat(reporter.getSpans())
            .describedAs("one span is expected")
            .hasSize(1);
        Span span = reporter.getFirstSpan();
        StringBuilder processedSql = new StringBuilder();
        signatureParser.querySignature(rawSql, processedSql, preparedStatement);
        assertThat(span.getNameAsString()).isEqualTo(processedSql.toString());
        assertThat(span.getType()).isEqualTo(DB_SPAN_TYPE);
        assertThat(span.getSubtype()).isEqualTo(expectedDbVendor);
        assertThat(span.getAction()).isEqualTo(DB_SPAN_ACTION);

        Db db = span.getContext().getDb();
        assertThat(db.getStatement()).isEqualTo(rawSql);
//        DatabaseMetaData metaData = connection.getMetaData();
//        assertThat(db.getInstance()).isEqualToIgnoringCase(connection.getCatalog());
//        assertThat(db.getUser()).isEqualToIgnoringCase(metaData.getUserName());
//        assertThat(db.getType()).isEqualToIgnoringCase("sql");

//        assertThat(db.getAffectedRowsCount())
//            .describedAs("unexpected affected rows count for statement %s", rawSql)
//            .isEqualTo(expectedAffectedRows);

        Destination destination = span.getContext().getDestination();
        assertThat(destination.getAddress().toString()).isEqualTo("localhost");
        if (expectedDbVendor.equals("h2")) {
            assertThat(destination.getPort()).isEqualTo(-1);
        } else {
            assertThat(destination.getPort()).isGreaterThan(0);
        }

        Destination.Service service = destination.getService();
        assertThat(service.getResource().toString()).isEqualTo(expectedDbVendor);

        assertThat(span.getOutcome())
            .describedAs("span outcome should be explicitly set to either failure or success")
            .isNotEqualTo(Outcome.UNKNOWN);

        return span;
    }


}
