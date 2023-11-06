package co.elastic.apm.agent.awslambda.lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;

import java.util.Map;

public class ApplicationLoadBalancerRequestLambdaFunction extends AbstractFunction<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
    public static final String EXPECTED_BODY = "This is some body";
    public static final String EXPECTED_RESPONSE_HEADER_1_KEY = "EXPECTED_HEADER_1_KEY";
    public static final String EXPECTED_RESPONSE_HEADER_1_VALUE = "EXPECTED_HEADER_1_VALUE";
    public static final String EXPECTED_RESPONSE_HEADER_2_KEY = "EXPECTED_HEADER_2_KEY";
    public static final String EXPECTED_RESPONSE_HEADER_2_VALUE = "EXPECTED_HEADER_2_VALUE";
    public static final int EXPECTED_STATUS_CODE = 202;
    public static final int ERROR_STATUS_CODE = 505;

    @Override
    public ApplicationLoadBalancerResponseEvent handleRequest(ApplicationLoadBalancerRequestEvent applicationLoadBalancerRequestEvent, Context context) {
        createChildSpan();

        ApplicationLoadBalancerResponseEvent response = new ApplicationLoadBalancerResponseEvent();
        response.setBody(EXPECTED_BODY);
        response.setHeaders(Map.of(EXPECTED_RESPONSE_HEADER_1_KEY, EXPECTED_RESPONSE_HEADER_1_VALUE, EXPECTED_RESPONSE_HEADER_2_KEY, EXPECTED_RESPONSE_HEADER_2_VALUE));

        if (((TestContext) context).shouldSetErrorStatusCode()) {
            response.setStatusCode(ERROR_STATUS_CODE);
        } else {
            response.setStatusCode(EXPECTED_STATUS_CODE);
        }
        raiseException(context);
        return response;
    }
}
