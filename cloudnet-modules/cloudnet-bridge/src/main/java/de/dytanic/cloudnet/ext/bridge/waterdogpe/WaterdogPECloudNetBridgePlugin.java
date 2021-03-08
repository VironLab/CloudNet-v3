package de.dytanic.cloudnet.ext.bridge.waterdogpe;


import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.command.CommandCloudNet;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.command.CommandHub;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.listener.WaterdogPECloudNetListener;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.listener.WaterdogPEPlayerExecutorListener;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.listener.WaterdogPEPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import dev.waterdog.network.ServerInfo;
import dev.waterdog.plugin.Plugin;

import java.net.InetSocketAddress;
import java.util.Collection;

public class WaterdogPECloudNetBridgePlugin extends Plugin {

    @Override
    public void onEnable() {
        CloudNetDriver.getInstance().getServicesRegistry().registerService(IPlayerManager.class, "BridgePlayerManager", new BridgePlayerManager());

        this.initListeners();
        this.registerCommands();
        this.initServers();
        this.runPlayerDisconnectTask();

        WaterdogPECloudNetReconnectHandler reconnectHandler = new WaterdogPECloudNetReconnectHandler();

        this.getProxy().setJoinHandler(reconnectHandler);
        this.getProxy().setReconnectHandler(reconnectHandler);

        BridgeHelper.updateServiceInfo();
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    private void runPlayerDisconnectTask() {
        super.getProxy().getScheduler().scheduleRepeating(() -> {
            if (WaterdogPECloudNetHelper.getLastOnlineCount() != -1 &&
                    super.getProxy().getPlayers().size() != WaterdogPECloudNetHelper.getLastOnlineCount()) {
                Wrapper.getInstance().publishServiceInfoUpdate();
            }
        }, 10);
    }

    private void initServers() {
        for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices()) {
            if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer()) {
                if ((serviceInfoSnapshot.getProperties().contains("Online-Mode") && serviceInfoSnapshot.getProperties().getBoolean("Online-Mode")) ||
                        serviceInfoSnapshot.getLifeCycle() != ServiceLifeCycle.RUNNING) {
                    continue;
                }

                String name = serviceInfoSnapshot.getServiceId().getName();
                InetSocketAddress address = new InetSocketAddress(
                        serviceInfoSnapshot.getConnectAddress().getHost(),
                        serviceInfoSnapshot.getConnectAddress().getPort()
                );

                super.getProxy().registerServerInfo(new ServerInfo(name, address, address));
                BridgeProxyHelper.cacheServiceInfoSnapshot(serviceInfoSnapshot);
            }
        }
    }

    private void registerCommands() {
        super.getProxy().getCommandMap().registerCommand("cloudnet", new CommandCloudNet());

        Collection<String> hubCommandNames = BridgeConfigurationProvider.load().getHubCommandNames();

        if (!hubCommandNames.isEmpty()) {
            String[] hubCommandArray = hubCommandNames.toArray(new String[0]);

            super.getProxy().getCommandMap().registerCommand(hubCommandArray[0], new CommandHub(hubCommandArray));
        }
    }

    private void initListeners() {
        //WaterdogPE API
        new WaterdogPEPlayerListener();

        //CloudNet
        CloudNetDriver.getInstance().getEventManager().registerListener(new WaterdogPECloudNetListener());
        CloudNetDriver.getInstance().getEventManager().registerListener(new WaterdogPEPlayerExecutorListener());
        CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
    }

}