package co.elastic.apm.agent.quartzjob;

import co.elastic.apm.agent.impl.ElasticApmTracer;
import co.elastic.apm.agent.impl.transaction.Outcome;
import co.elastic.apm.agent.impl.transaction.Transaction;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class Quartz2JobTransactionNameInstrumentationTest extends AbstractJobTransactionNameInstrumentationTest {

    @Override
    public Transaction verifyTransactionFromJobDetails(JobDetail job, Outcome expectedOutcome) {
        reporter.awaitTransactionCount(1);

        Transaction transaction = reporter.getFirstTransaction();
        await().untilAsserted(() -> assertThat(reporter.getTransactions().size()).isEqualTo(1));

        verifyTransaction(transaction, String.format("%s.%s", job.getKey().getGroup(), job.getKey().getName()));

        assertThat(transaction.getOutcome()).isEqualTo(expectedOutcome);
        return transaction;
    }

    @Override
    public SimpleTrigger createTrigger() {
        return TriggerBuilder.newTrigger()
            .withIdentity("myTrigger")
            .withSchedule(
                SimpleScheduleBuilder.repeatSecondlyForTotalCount(1, 1))
            .build();
    }

    @Override
    String quartzVersion() {
        return "2.3.1";
    }

    @Override
    JobDetail buildJobDetailTestJob(String name, @Nullable String groupName) {
        if (groupName == null) {
            return buildJobDetail(TestJob.class, name);
        }
        return buildJobDetail(TestJob.class, name, groupName);
    }

    @Override
    void executeTestJobCreatingSpan(ElasticApmTracer tracer, boolean traced) throws JobExecutionException {
        new TestJobCreatingSpan(tracer, traced).execute(null);
    }

    @Override
    JobDetail buildJobDetailTestJobWithResult(String name) {
        return buildJobDetail(TestJobWithResult.class, name);
    }

    @Override
    JobDetail buildJobDetailTestJobWithException(String name) {
        return buildJobDetail(TestJobWithException.class, name);
    }

    @Override
    JobDetail buildJobDetailTestSpringJob(String name, String groupName) {
        return buildJobDetail(TestSpringJob.class, name, groupName);
    }

    @Override
    boolean ignoreTestSpringJob() {
        return false;
    }

    @Override
    boolean ignoreDirectoryScanTest() {
        return false;
    }

    private JobDetail buildJobDetail(Class jobClass, String name) {
        return JobBuilder.newJob(jobClass)
            .withIdentity(name)
            .build();
    }

    private JobDetail buildJobDetail(Class jobClass, String name, String groupName) {
        if (groupName == null) {
            return buildJobDetail(jobClass, name);
        }
        return JobBuilder.newJob(jobClass)
            .withIdentity(name, groupName)
            .build();
    }

    public static class TestJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
        }
    }

    public static class TestJobCreatingSpan implements Job {
        private final ElasticApmTracer tracer;
        private final boolean traced;

        public TestJobCreatingSpan(ElasticApmTracer tracer, boolean traced) {
            this.tracer = tracer;
            this.traced = traced;
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Transaction transaction = tracer.currentTransaction();
            if (traced) {
                assertThat(transaction).isNotNull();
                transaction.createSpan().end();
            } else {
                assertThat(transaction).isNull();
                assertThat(tracer.getActive()).isNull();
            }
        }
    }

    public static class TestJobWithResult implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            context.setResult("this is the result");
        }
    }

    public static class TestJobWithException implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            throw new JobExecutionException("intentional job exception");
        }
    }

    public static class TestSpringJob extends QuartzJobBean {

        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        }
    }

}
