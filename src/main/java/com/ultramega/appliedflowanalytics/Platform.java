package com.ultramega.appliedflowanalytics;

import com.ultramega.appliedflowanalytics.config.ServerConfig;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public final class Platform {
    @Nullable
    private static Supplier<ServerConfig> serverConfigProvider;

    private Platform() {
    }

    public static void setServerConfigProvider(final Supplier<ServerConfig> provider) {
        serverConfigProvider = provider;
    }

    public static ServerConfig getServerConfig() {
        return requireNonNull(serverConfigProvider, "Server config is not loaded yet").get();
    }
}
