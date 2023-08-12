/*
 *  Copyright (C) 2023 ~ 2035 the original authors WL4G (James Wong).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.wl4g.streamconnect.checkpoint.minio;

import com.wl4g.streamconnect.checkpoint.AbstractCheckpoint;
import com.wl4g.streamconnect.coordinator.CachingChannelRegistry;
import com.wl4g.streamconnect.config.ChannelInfo;
import com.wl4g.streamconnect.config.StreamConnectConfiguration.ConnectorConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * The {@link MinIOCheckpoint}
 *
 * @author James Wong
 * @since v1.0
 **/
@Getter
@Setter
public class MinIOCheckpoint extends AbstractCheckpoint {
    public static final String TYPE_NAME = "MINIO_CHECKPOINT";

    private MinIOCheckpointConfig checkpointConfig;

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @Override
    public void init() {
        // Ignore
    }

    @Override
    public PointWriter createWriter(@NotNull ConnectorConfig connectorConfig,
                                    @NotNull ChannelInfo channel,
                                    @NotNull CachingChannelRegistry registry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PointReader createReader(@NotNull ConnectorConfig connectorConfig,
                                    @NotNull ChannelInfo channel,
                                    @NotNull ReadPointListener listener) {
        throw new UnsupportedOperationException();
    }


    @Getter
    @Setter
    @NoArgsConstructor
    public static class MinIOCheckpointConfig extends CheckpointConfig {

    }

}
