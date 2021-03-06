/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
 *
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
 */
package org.traccar;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * Tracker server
 */
public abstract class TrackerServer {

    private ServerManager serverManager;
    private Bootstrap bootstrap;
    private String protocol;

    public String getProtocol() {
        return protocol;
    }

    public TrackerServer(ServerManager serverManager, Bootstrap bootstrap, String protocol) {
        this.serverManager = serverManager;
        this.bootstrap = bootstrap;
        this.protocol = protocol;

        bootstrap.setFactory(GlobalChannelFactory.getFactory());

        address = serverManager.getProperties().getProperty(protocol + ".address");
        String portProperty = serverManager.getProperties().getProperty(protocol + ".port");
        port = (portProperty != null) ? Integer.valueOf(portProperty) : 5000;

        bootstrap.setPipelineFactory(new BasePipelineFactory(serverManager, this, protocol) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                TrackerServer.this.addSpecificHandlers(pipeline);
            }
        });
    }

    protected abstract void addSpecificHandlers(ChannelPipeline pipeline);

    /**
     * Server port
     */
    private Integer port;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Server listening interface
     */
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Set endianness
     */
    void setEndianness(ByteOrder byteOrder) {
        bootstrap.setOption("child.bufferFactory", new HeapChannelBufferFactory(byteOrder));
    }

    /**
     * Opened channels
     */
    private ChannelGroup allChannels = new DefaultChannelGroup();

    public ChannelGroup getChannelGroup() {
        return allChannels;
    }

    public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
        bootstrap.setPipelineFactory(pipelineFactory);
    }

    /**
     * Start server
     */
    public void start() {
        InetSocketAddress endpoint;
        if (address == null) {
            endpoint = new InetSocketAddress(port);
        } else {
            endpoint = new InetSocketAddress(address, port);
        }

        Channel channel = null;
        if (bootstrap instanceof ServerBootstrap) {
            channel = ((ServerBootstrap) bootstrap).bind(endpoint);
        } else if (bootstrap instanceof ConnectionlessBootstrap) {
            channel = ((ConnectionlessBootstrap) bootstrap).bind(endpoint);
        }

        if (channel != null) {
            getChannelGroup().add(channel);
        }
    }

    /**
     * Stop server
     */
    public void stop() {
        ChannelGroupFuture future = getChannelGroup().close();
        future.awaitUninterruptibly();
    }

}
