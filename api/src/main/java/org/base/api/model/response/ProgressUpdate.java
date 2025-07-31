package org.base.api.model.response;

import lombok.Getter;

@Getter
public class ProgressUpdate {
    private final double progress;
    private final String message;

    public ProgressUpdate(double progress, String message) {
        this.progress = progress;
        this.message = message;
    }

}
