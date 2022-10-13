### New Relic Instrumentation for Java

To replace javamelody for performance monitoring of the kuali java application, we are [instrumenting](https://blog.overops.com/the-complete-guide-to-instrumentation-how-to-measure-your-application/) it with [New Relic APM](https://docs.newrelic.com/docs/agents/manage-apm-agents/installation/install-agent).  Specifically we are instrumenting the java runtime on which kc operates with the [New Relic APM Java Agent](https://docs.newrelic.com/docs/agents/java-agent). And since KC is containerized, there are recommendations on how to do this with docker covered [here](https://docs.newrelic.com/docs/agents/java-agent/additional-installation/install-new-relic-java-agent-docker).

```
yum install -y unzip && \
cd /opt/ && \
curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip && \
unzip newrelic-java.zip && \
rm -f newrelic-java.zip
```

