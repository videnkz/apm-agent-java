package co.elastic.apm.agent.httpclient;

import co.elastic.apm.agent.bci.ElasticApmInstrumentation;
import co.elastic.apm.agent.impl.transaction.Db;
import co.elastic.apm.agent.impl.transaction.Span;
import co.elastic.apm.agent.impl.transaction.TraceContextHolder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class InternalHttpAsyncClientInstrumentation extends ElasticApmInstrumentation {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    private static void onBeforeExecute(@Advice.Local("span") Span span,
                                        @Advice.This Object thiz) {
        if (tracer == null || tracer.getActive() == null) {
            return;
        }
//        final TraceContextHolder<?> parent = tracer.getActive();
        if (span != null) {
            System.out.println("Span not null");
            Db dbContext = span.getContext().getDb();
            System.out.println("Is db null = " + (dbContext == null));
            if (thiz instanceof CloseableHttpAsyncClient) {
                System.out.println("Is closeable");
                CloseableHttpAsyncClient client = (CloseableHttpAsyncClient) thiz;

            }
        }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onAfterExecute(@Advice.Local("span") @Nullable Span span) {
        System.out.println("Exit from.");
    }

    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return named("org.apache.http.impl.nio.client.InternalHttpAsyncClient");
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("execute");
    }

    @Override
    public Collection<String> getInstrumentationGroupNames() {
        return Arrays.asList("apache-httpclient-internal");
    }
}
