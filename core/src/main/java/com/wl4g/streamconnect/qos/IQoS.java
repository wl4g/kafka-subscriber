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

package com.wl4g.streamconnect.qos;

import com.wl4g.streamconnect.config.StreamConnectConfiguration.ConnectorConfig;
import com.wl4g.streamconnect.framework.IStreamConnectSpi;

/**
 * The {@link IQoS}
 *
 * @author James Wong
 * @since v1.0
 **/
public interface IQoS extends IStreamConnectSpi {

    boolean supportRetry(ConnectorConfig connectorConfig);

    boolean canRetry(ConnectorConfig connectorConfig,
                     int retryTimes);

    void retryIfFail(ConnectorConfig connectorConfig,
                     int retryTimes,
                     Runnable call);

    default boolean supportPreferAcknowledge(ConnectorConfig connectorConfig) {
        return false;
    }

    default void acknowledgeIfFail(ConnectorConfig connectorConfig,
                                   Throwable ex,
                                   Runnable call) {
    }

}
