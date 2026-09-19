package org.base.api.service.artifact.run.stage;

import org.base.api.service.artifact.ArtifactAttachmentService;
import org.base.api.service.artifact.BindingStatus;
import org.base.api.service.artifact.run.PackageRun;
import org.base.api.service.artifact.run.PackageRunKeys;
import org.base.api.service.artifact.run.PackageRunStage;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Binds manifest files to snapshot rows by the approved relations; any error writes nothing. */
@Component
public class BindAttachmentsStage implements PackageRunStage {
    private final ArtifactAttachmentService attachments;

    public BindAttachmentsStage(ArtifactAttachmentService attachments) {
        this.attachments = attachments;
    }

    @Override
    public String code() {
        return "BIND_ATTACHMENTS";
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public Result execute(PackageRun run) {
        var report = attachments.bind(run.state().requireLong(PackageRunKeys.DATASET_SNAPSHOT_ID), run.manifestId(), false);
        Map<String, Object> detail = Map.of("status", report.status().name(), "relations", report.relations());
        return report.status() == BindingStatus.BLOCKED ? Result.blocked(run.state(), "BINDING_BLOCKED", detail) : Result.completed(run.state(), detail);
    }
}
