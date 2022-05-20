package com.sycrow.api.service.helpers;

import com.google.cloud.tasks.v2.*;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.sycrow.api.dto.TaskQueueModel;
import com.sycrow.api.exception.TaskQueueException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.env.Environment;

import javax.inject.Named;
import java.nio.charset.Charset;

@Log4j2
@Named
public class TaskQueueHelper {
    private static final String BASE_URL_KEY = "spring.cloud.gcp.base-url";
    private static final String BASE_LOCATION = "gcp.cloud.task.base.location";

    private static final Gson GSON = new Gson();

    private final Environment environment;

    public TaskQueueHelper(Environment environment) {
        this.environment = environment;
    }

    public void queueTask(TaskQueues taskQueues, TaskQueueModel<?> taskQueueModel, String chainId) {
        QueueName queueName = QueueName.parse(String.format("%s%s%s", environment.getRequiredProperty(BASE_LOCATION), taskQueues.getQueueName(), chainId));

        try (CloudTasksClient cloudTasksClient = CloudTasksClient.create()) {
            Task.Builder taskBuilder = Task.newBuilder()
                    .setHttpRequest(
                            HttpRequest.newBuilder()
                                    .setBody(ByteString.copyFrom(GSON.toJson(taskQueueModel.getData()), Charset.defaultCharset()))
                                    .setUrl(String.format("%s%s%s", environment.getRequiredProperty(BASE_URL_KEY), taskQueues.getQueueEndPoint(), chainId))
                                    .setHttpMethod(HttpMethod.POST)
                                    .putHeaders("Content-Type", "application/json")
                                    .build()
                    );
            // Send create task request.
            cloudTasksClient.createTask(queueName, taskBuilder.build());
        } catch (Exception e) {
            log.error("Task Queue Error: ", e);
            throw new TaskQueueException(e.getMessage());
        }
    }

    @Getter
    public enum TaskQueues {
        BARTER_CREATION_EVENT_QUEUE_FOR_CHAIN("barter-creation-event-", "/api/v1/tasks/barter/token/events/create/"),
        BARTER_TRADE_EVENT_QUEUE_FOR_CHAIN("barter-trade-event-", "/api/v1/tasks/barter/token/events/trade/"),
        BARTER_WITHDRAW_EVENT_QUEUE_FOR_CHAIN("barter-withdraw-event-", "/api/v1/tasks/barter/token/events/withdraw/");

        private final String queueName;
        private final String queueEndPoint;

        TaskQueues(String queueName, String queueEndPoint) {
            this.queueName = queueName;
            this.queueEndPoint = queueEndPoint;
        }
    }
}
