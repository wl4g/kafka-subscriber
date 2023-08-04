/*
 * Copyright 2017 ~ 2025 the original authors James Wong.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ALL_OR KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wl4g.kafkasubscriber.facade;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.wl4g.infra.common.lang.Assert2;
import com.wl4g.kafkasubscriber.bean.SubscriberInfo;
import com.wl4g.kafkasubscriber.config.KafkaSubscribeConfiguration;
import com.wl4g.kafkasubscriber.coordinator.KafkaSubscribeCoordinator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.wl4g.infra.common.serialize.JacksonUtils.toJSONString;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * The {@link SubscribeEventPublisher}
 *
 * @author James Wong
 * @since v1.0
 **/
@Slf4j
@Getter
public class SubscribeEventPublisher {

    private final KafkaSubscribeConfiguration config;
    private final Producer<String, String> producer;

    public SubscribeEventPublisher(KafkaSubscribeConfiguration config) {
        this.config = Assert2.notNullOf(config, "config");

        Properties props = new Properties();
        // TODO
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "my-producer");
        props.put(ProducerConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG, "10000");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "100");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-transactional-id");
        props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, "60000");
        // TODO support using other?? avro/protobuf
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = new KafkaProducer<>(props);
        // TODO
//        this.producer.initTransactions();
    }

    public void publishSync(@NotNull List<SubscribeEvent> events,
                            @NotNull Duration timeout) throws InterruptedException, TimeoutException {
        Assert2.notNullOf(timeout, "timeout");

        final List<Future<RecordMetadata>> futures = publishAsync(events);
        final CountDownLatch latch = new CountDownLatch(futures.size());

        final List<Object> failures = futures.stream().map(future -> {
            try {
                return future.get();
            } catch (Throwable th) {
                log.error("Failed to getting publish subscribe event", th);
                return null;
            } finally {
                latch.countDown();
            }
        }).filter(Objects::isNull).collect(toList());

        if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new TimeoutException(String.format("Failed to getting publish subscribe " +
                    "events result within timeout %s", timeout));
        }
        if (failures.size() > 0) {
            throw new IllegalStateException(String.format("Failed to publish subscribe events" +
                    ", failures: %s", failures.size()));
        }
    }

    public List<Future<RecordMetadata>> publishAsync(@NotNull List<SubscribeEvent> events) {
        Assert2.notNullOf(events, "events");
        events.forEach(SubscribeEvent::validate);
        try {
            producer.beginTransaction();

            final List<Future<RecordMetadata>> futures = events.stream().map(event ->
                            producer.send(new ProducerRecord<>(KafkaSubscribeCoordinator.SUBSCRIBE_COORDINATOR_TOPIC,
                                    // TODO
                                    "my-key", toJSONString(event))))
                    .collect(toList());

            producer.commitTransaction();

            return futures;
        } catch (Throwable th) {
            producer.abortTransaction();
            log.error(String.format("Failed to publish subscribe events ::: %s", events), th);
        }
        return emptyList();
    }

    @Schema(oneOf = {AddSubscribeEvent.class, UpdateSubscribeEvent.class, RemoveSubscribeEvent.class},
            discriminatorProperty = "type")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
    @JsonSubTypes({@Type(value = AddSubscribeEvent.class, name = "ADD"),
            @Type(value = UpdateSubscribeEvent.class, name = "UPDATE"),
            @Type(value = RemoveSubscribeEvent.class, name = "REMOVE")})
    @Getter
    @Setter
    @SuperBuilder
    @ToString(callSuper = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class SubscribeEvent {
        private EventType type;
        private String pipelineName;

        public void validate() {
            Assert2.notNullOf(type, "type");
            Assert2.hasText(pipelineName, "pipelineName");
        }
    }

    @Getter
    @AllArgsConstructor
    public enum EventType {
        ADD(AddSubscribeEvent.class),
        UPDATE(UpdateSubscribeEvent.class),
        REMOVE(RemoveSubscribeEvent.class);
        final Class<? extends SubscribeEvent> eventClass;
    }

    @Getter
    @Setter
    @SuperBuilder
    @ToString(callSuper = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddSubscribeEvent extends SubscribeEvent {
        private List<SubscriberInfo> subscribers;

        @Override
        public void validate() {
            super.validate();
            Assert2.notEmptyOf(subscribers, "subscribers");
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @ToString(callSuper = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSubscribeEvent extends SubscribeEvent {
        private List<SubscriberInfo> subscribers;

        @Override
        public void validate() {
            super.validate();
            Assert2.notEmptyOf(subscribers, "subscribers");
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @ToString(callSuper = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemoveSubscribeEvent extends SubscribeEvent {
        private List<String> subscriberIds;

        @Override
        public void validate() {
            super.validate();
            Assert2.notEmptyOf(subscriberIds, "subscriberIds");
        }
    }

}
