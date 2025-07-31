package org.base.api.service.mobile_services.arcitecture;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class ParameterDescriptor<P> {
    private final String configName;
    private final Map<String, Function<P, String>> parameterMappings;

    private ParameterDescriptor(String configName, Map<String, Function<P, String>> parameterMappings) {
        this.configName = Objects.requireNonNull(configName, "Config name cannot be null");
        this.parameterMappings = Map.copyOf(parameterMappings);
    }

    public String getConfigName() {
        return configName;
    }

    public Map<String, Function<P, String>> getParameterMappings() {
        return parameterMappings;
    }

    public boolean isValid(P params) {
        return parameterMappings.values().stream().anyMatch(f -> f.apply(params) != null);
    }

    public static class Builder<P> {
        private final String configName;
        private final Map<String, Function<P, String>> parameterMappings;

        public Builder(String configName) {
            this.configName = configName;
            this.parameterMappings = new HashMap<>();
        }

        public Builder<P> addParameter(String column, Function<P, String> valueExtractor) {
            parameterMappings.put(column, valueExtractor);
            return this;
        }

        public ParameterDescriptor<P> build() {
            return new ParameterDescriptor<>(configName, parameterMappings);
        }
    }
}
