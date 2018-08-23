/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.jdbc.orchestration.internal.config;

import io.shardingsphere.core.event.ShardingEventBusInstance;
import io.shardingsphere.core.event.orche.config.MasterSlaveConfigurationEventBusEvent;
import io.shardingsphere.core.event.orche.config.ShardingConfigurationEventBusEvent;
import io.shardingsphere.core.event.orche.config.ProxyConfigurationEventBusEvent;
import io.shardingsphere.core.rule.ShardingRule;
import io.shardingsphere.jdbc.orchestration.internal.listener.ListenerManager;
import io.shardingsphere.jdbc.orchestration.internal.state.datasource.DataSourceService;
import io.shardingsphere.jdbc.orchestration.reg.api.RegistryCenter;
import io.shardingsphere.jdbc.orchestration.reg.listener.DataChangedEvent;
import io.shardingsphere.jdbc.orchestration.reg.listener.EventListener;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Configuration listener manager.
 *
 * @author caohao
 * @author panjuan
 */
public final class ConfigurationListenerManager implements ListenerManager {
    
    private final ConfigurationNode configNode;
    
    private final RegistryCenter regCenter;
    
    private final ConfigurationService configService;
    
    private final DataSourceService dataSourceService;
    
    public ConfigurationListenerManager(final String name, final RegistryCenter regCenter) {
        configNode = new ConfigurationNode(name);
        this.regCenter = regCenter;
        configService = new ConfigurationService(name, regCenter);
        dataSourceService = new DataSourceService(name, regCenter);
    }
    
    @Override
    public void watchSharding() {
        shardingStart(ConfigurationNode.DATA_SOURCE_NODE_PATH);
        shardingStart(ConfigurationNode.SHARDING_RULE_NODE_PATH);
        shardingStart(ConfigurationNode.SHARDING_PROPS_NODE_PATH);
    }
    
    private void shardingStart(final String node) {
        String cachePath = configNode.getFullPath(node);
        regCenter.watch(cachePath, new EventListener() {
            
            @Override
            public void onChange(final DataChangedEvent event) {
                if (DataChangedEvent.Type.UPDATED == event.getEventType()) {
                    Map<String, DataSource> dataSourceMap = dataSourceService.getAvailableDataSources();
                    ShardingConfigurationEventBusEvent shardingEvent = new ShardingConfigurationEventBusEvent(dataSourceMap,
                            new ShardingRule(dataSourceService.getAvailableShardingRuleConfiguration(), dataSourceMap.keySet()), configService.loadShardingProperties());
                    ShardingEventBusInstance.getInstance().post(shardingEvent);
                }
            }
        });
    }
    
    @Override
    public void watchMasterSlave() {
        masterSlaveStart(ConfigurationNode.DATA_SOURCE_NODE_PATH);
        masterSlaveStart(ConfigurationNode.MASTER_SLAVE_RULE_NODE_PATH);
    }
    
    private void masterSlaveStart(final String node) {
        String cachePath = configNode.getFullPath(node);
        regCenter.watch(cachePath, new EventListener() {
            
            @Override
            public void onChange(final DataChangedEvent event) {
                if (DataChangedEvent.Type.UPDATED == event.getEventType()) {
                    MasterSlaveConfigurationEventBusEvent masterSlaveEvent = new MasterSlaveConfigurationEventBusEvent(dataSourceService.getAvailableDataSources(),
                            dataSourceService.getAvailableMasterSlaveRuleConfiguration());
                    ShardingEventBusInstance.getInstance().post(masterSlaveEvent);
                }
            }
        });
    }
    
    @Override
    public void proxyStart() {
        proxyStart(ConfigurationNode.DATA_SOURCE_NODE_PATH);
        proxyStart(ConfigurationNode.PROXY_RULE_NODE_PATH);
    }
    
    private void proxyStart(final String node) {
        String cachePath = configNode.getFullPath(node);
        regCenter.watch(cachePath, new EventListener() {
            
            @Override
            public void onChange(final DataChangedEvent event) {
                if (DataChangedEvent.Type.UPDATED == event.getEventType()) {
                    ShardingEventBusInstance.getInstance().post(new ProxyConfigurationEventBusEvent(dataSourceService.getAvailableDataSourceParameters(), dataSourceService.getAvailableYamlProxyConfiguration()));
                }
            }
        });
    }
}
