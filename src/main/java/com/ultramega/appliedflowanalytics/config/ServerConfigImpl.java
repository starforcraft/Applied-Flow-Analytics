package com.ultramega.appliedflowanalytics.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfigImpl implements ServerConfig {
    private final ModConfigSpec spec;
    private final ModConfigSpec.DoubleValue terminalEnergyUsage;
    private final ModConfigSpec.DoubleValue monitorEnergyUsage;
    private final ModConfigSpec.DoubleValue emitterEnergyUsage;

    public ServerConfigImpl() {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        this.terminalEnergyUsage = builder.defineInRange("flowTerminal.energyUsage", 0.5, 0.0, 1_000_000.0);
        this.monitorEnergyUsage = builder.defineInRange("flowMonitor.energyUsage", 0.0625, 0.0, 1_000_000.0);
        this.emitterEnergyUsage = builder.defineInRange("flowLevelEmitter.energyUsage", 1, 0.0, 1_000_000.0);

        this.spec = builder.build();
    }

    public ModConfigSpec getSpec() {
        return this.spec;
    }

    @Override
    public double getFlowTerminalEnergyUsage() {
        return this.terminalEnergyUsage.get();
    }

    @Override
    public double getFlowMonitorEnergyUsage() {
        return this.monitorEnergyUsage.get();
    }

    @Override
    public double getFlowLevelEmitterEnergyUsage() {
        return this.emitterEnergyUsage.get();
    }
}
