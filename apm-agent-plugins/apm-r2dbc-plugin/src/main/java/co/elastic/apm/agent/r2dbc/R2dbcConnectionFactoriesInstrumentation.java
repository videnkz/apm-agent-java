package co.elastic.apm.agent.r2dbc;

import co.elastic.apm.agent.r2dbc.helper.R2dbcHelper;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;


import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

public class R2dbcConnectionFactoriesInstrumentation extends AbstractR2dbcInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return not(isInterface())
            .and(named("io.r2dbc.spi.ConnectionFactories"));
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("get")
            .and(returns(named("io.r2dbc.spi.ConnectionFactory")))
            .and(takesArgument(0, named("io.r2dbc.spi.ConnectionFactoryOptions")))
            .and(isPublic())
            .and(isStatic());
    }

    public static class AdviceClass {
        @Advice.OnMethodExit(suppress = Throwable.class, inline = false)
        public static void storeConnectionFactoryOptions(@Advice.Return ConnectionFactory connectionFactory,
                                                         @Advice.Argument(0) ConnectionFactoryOptions connectionFactoryOptions) {
            if (connectionFactory == null) {
                return;
            }
            R2dbcHelper helper = R2dbcHelper.get();
            helper.mapConnectionFactoryData(connectionFactory, connectionFactoryOptions);
        }
    }
}
