package co.elastic.apm.agent.es.restclient;

import co.elastic.apm.agent.AbstractInstrumentationTest;
import co.elastic.apm.agent.impl.error.ErrorCapture;
import co.elastic.apm.agent.impl.transaction.Db;
import co.elastic.apm.agent.impl.transaction.Http;
import co.elastic.apm.agent.impl.transaction.Span;
import co.elastic.apm.agent.impl.transaction.TraceContext;
import co.elastic.apm.agent.impl.transaction.Transaction;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static co.elastic.apm.agent.es.restclient.ElasticsearchRestClientInstrumentationHelperImpl.ELASTICSEARCH;
import static co.elastic.apm.agent.es.restclient.ElasticsearchRestClientInstrumentationHelperImpl.SEARCH_QUERY_PATH_SUFFIX;
import static co.elastic.apm.agent.es.restclient.ElasticsearchRestClientInstrumentationHelperImpl.SPAN_ACTION;
import static co.elastic.apm.agent.es.restclient.ElasticsearchRestClientInstrumentationHelperImpl.SPAN_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractEsClientInstrumentationTest extends AbstractInstrumentationTest {

    protected static final String USER_NAME = "elastic-user";
    protected static final String PASSWORD = "elastic-pass";

    @SuppressWarnings("NullableProblems")
    protected static ElasticsearchContainer container;
    @SuppressWarnings("NullableProblems")
    protected static RestClient lowLevelClient;

    protected static final String INDEX = "my-index";
    protected static final String SECOND_INDEX = "my-second-index";
    protected static final String DOC_ID = "38uhjds8s4g";
    protected static final String DOC_TYPE = "_doc";
    protected static final String FOO = "foo";
    protected static final String BAR = "bar";
    protected static final String BAZ = "baz";

    protected boolean async;

    @Parameterized.Parameters(name = "Async={0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{{Boolean.FALSE}, {Boolean.TRUE}});
    }

    @Before
    public void startTransaction() {
        Transaction transaction = tracer.startTransaction(TraceContext.asRoot(), null, null).activate();
        transaction.setName("ES Transaction");
        transaction.withType("request");
        transaction.withResult("success");
    }

    @After
    public void endTransaction() {
        try {
            Transaction currentTransaction = tracer.currentTransaction();
            if (currentTransaction != null) {
                currentTransaction.deactivate().end();
            }
        } finally {
            reporter.reset();
        }
    }

    public void assertThatErrorsExistWhenDeleteNonExistingIndex() throws IOException {

        System.out.println(reporter.generateErrorPayloadJson());

        List<ErrorCapture> errorCaptures = reporter.getErrors();
        assertThat(errorCaptures).hasSize(1);
        ErrorCapture errorCapture = errorCaptures.get(0);
        assertThat(errorCapture.getException()).isNotNull();
    }

    protected void validateSpanContentWithoutContext(Span span, String expectedName, int statusCode, String method) {
        assertThat(span.getType()).isEqualTo(SPAN_TYPE);
        assertThat(span.getSubtype()).isEqualTo(ELASTICSEARCH);
        assertThat(span.getAction()).isEqualTo(SPAN_ACTION);
        assertThat(span.getName().toString()).isEqualTo(expectedName);

        assertThat(span.getContext().getDb().getType()).isEqualTo(ELASTICSEARCH);

        if (!expectedName.contains(SEARCH_QUERY_PATH_SUFFIX)) {
            assertThat((CharSequence) (span.getContext().getDb().getStatementBuffer())).isNull();
        }
    }

    protected void validateDbContextContent(Span span, String statement) {
        Db db = span.getContext().getDb();
        assertThat(db.getType()).isEqualTo(ELASTICSEARCH);
        assertThat((CharSequence) db.getStatementBuffer()).isNotNull();
        assertThat(db.getStatementBuffer().toString()).isEqualTo(statement);
    }


    protected void validateSpanContent(Span span, String expectedName, int statusCode, String method) {
        validateSpanContentWithoutContext(span, expectedName, statusCode, method);
        validateHttpContextContent(span.getContext().getHttp(), statusCode, method);
    }

    private void validateHttpContextContent(Http http, int statusCode, String method) {
        assertThat(http).isNotNull();
        assertThat(http.getMethod()).isEqualTo(method);
        assertThat(http.getStatusCode()).isEqualTo(statusCode);
        assertThat(http.getUrl()).isEqualTo("http://" + container.getHttpHostAddress());
    }

    protected void validateSpanContentAfterIndexCreateRequest() {
        System.out.println(reporter.generateTransactionPayloadJson());

        List<Span> spans = reporter.getSpans();
        assertThat(spans).hasSize(1);
        validateSpanContent(spans.get(0), String.format("Elasticsearch: PUT /%s", SECOND_INDEX), 200, "PUT");
    }

    protected void validateSpanContentAfterIndexDeleteRequest() {
        System.out.println(reporter.generateTransactionPayloadJson());

        List<Span>spans = reporter.getSpans();
        assertThat(spans).hasSize(1);
        validateSpanContent(spans.get(0), String.format("Elasticsearch: DELETE /%s", SECOND_INDEX), 200, "DELETE");
    }

    protected void validateSpanContentAfterBulkRequest() {
        System.out.println(reporter.generateTransactionPayloadJson());

        List<Span> spans = reporter.getSpans();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getName().toString()).isEqualTo("Elasticsearch: POST /_bulk");
    }

    protected Response doPerformRequest(String method, String path) throws IOException, ExecutionException {
        if (async) {
            final CompletableFuture<Response> resultFuture = new CompletableFuture<>();
            lowLevelClient.performRequestAsync(method, path, new ResponseListener() {
                @Override
                public void onSuccess(Response response) {
                    resultFuture.complete(response);
                }

                @Override
                public void onFailure(Exception exception) {
                    resultFuture.completeExceptionally(exception);
                }
            });
            try {
                return resultFuture.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return lowLevelClient.performRequest(method, path);
    }

//    private void validateHttpContextContent(Http http, int statusCode, String method, String url) {
//        assertThat(http).isNotNull();
//        assertThat(http.getMethod()).isEqualTo(method);
//        assertThat(http.getStatusCode()).isEqualTo(statusCode);
//        assertThat(http.getUrl()).isEqualTo(url);
//    }
//
//    private void validateDbContextContent(Span span, String statement) {
//        Db db = span.getContext().getDb();
//        assertThat(db.getType()).isEqualTo(ELASTICSEARCH);
//        assertThat((Object) db.getStatementBuffer()).isNotNull();
//        assertThat(db.getStatementBuffer().toString()).isEqualTo(statement);
//    }


    public abstract void testCreateAndDeleteIndex() throws IOException, ExecutionException, InterruptedException;

    public abstract void testDocumentScenario() throws IOException, ExecutionException, InterruptedException, Exception;

    public abstract void testScenarioAsBulkRequest() throws IOException, ExecutionException, InterruptedException;


}
