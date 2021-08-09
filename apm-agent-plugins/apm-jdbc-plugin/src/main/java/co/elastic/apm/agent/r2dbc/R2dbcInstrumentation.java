package co.elastic.apm.agent.r2dbc;

import co.elastic.apm.agent.bci.TracerAwareInstrumentation;

import java.util.Collection;
import java.util.Collections;

public abstract class R2dbcInstrumentation extends TracerAwareInstrumentation {

    private static final Collection<String> R2DBC_GROUPS = Collections.singleton("r2dbc");

    @Override
    public Collection<String> getInstrumentationGroupNames() {
        return R2DBC_GROUPS;
    }
}
