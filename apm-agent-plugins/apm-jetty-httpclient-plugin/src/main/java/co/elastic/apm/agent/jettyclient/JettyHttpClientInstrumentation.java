/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.apm.agent.jettyclient;

import co.elastic.apm.agent.http.client.HttpClientHelper;
import co.elastic.apm.agent.impl.transaction.AbstractSpan;
import co.elastic.apm.agent.impl.transaction.Span;
import co.elastic.apm.agent.jettyclient.helper.HttpFieldAccessor;
import co.elastic.apm.agent.jettyclient.helper.SpanResponseCompleteListenerWrapper;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;

import javax.annotation.Nullable;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

public class JettyHttpClientInstrumentation extends AbstractJettyClientInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return named("org.eclipse.jetty.client.HttpClient");
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("send")
            .and(takesArgument(0, namedOneOf("org.eclipse.jetty.client.HttpRequest", "org.eclipse.jetty.client.api.Request"))
                .and(takesArgument(1, List.class)));
    }

    @Override
    public String getAdviceClassName() {
        return "co.elastic.apm.agent.jettyclient.JettyHttpClientInstrumentation$JettyHttpClientAdvice";
    }

    public static class JettyHttpClientAdvice {
        @Nullable
        @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
        public static Object onBeforeSend(@Advice.Argument(0) Request request,
                                          @Advice.Argument(1) List<Response.ResponseListener> responseListeners) {
            final AbstractSpan<?> parent = tracer.getActive();
            if (parent == null || request == null) {
                return null;
            }
            Span span = HttpClientHelper.startHttpClientSpan(parent, request.getMethod(), request.getURI(), request.getHost());
            if (span != null) {
                span.activate();
                span.propagateTraceContext(request, HttpFieldAccessor.INSTANCE);
                responseListeners.add(new SpanResponseCompleteListenerWrapper(span));
            } else {
                parent.propagateTraceContext(request, HttpFieldAccessor.INSTANCE);
            }
            return span;
        }

        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class, inline = false)
        public static void onAfterSend(@Advice.Enter @Nullable Object spanObject) {
            if (spanObject instanceof Span) {
                ((Span) spanObject).deactivate();
            }
        }
    }
}
