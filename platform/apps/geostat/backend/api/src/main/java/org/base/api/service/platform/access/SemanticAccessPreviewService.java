package org.base.api.service.platform.access;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Locale;

/** Read-only preflight for a v3 semantic Access package. No artifact, metadata, or source rows are persisted. */
@Service
public class SemanticAccessPreviewService {
    private final SemanticAccessPackageReader reader;
    private final SemanticAccessPackageValidationService validation;
    private final SemanticAccessControlPlaneResolver resolver;
    public SemanticAccessPreviewService(SemanticAccessPackageReader reader, SemanticAccessPackageValidationService validation, SemanticAccessControlPlaneResolver resolver) { this.reader=reader; this.validation=validation; this.resolver=resolver; }

    public SemanticAccessPreview preview(MultipartFile upload) throws Exception {
        if(upload==null||upload.isEmpty()) throw new IllegalArgumentException("Access package must not be empty");
        String name=upload.getOriginalFilename()==null?"":upload.getOriginalFilename().toLowerCase(Locale.ROOT);
        if(!name.endsWith(".accdb")&&!name.endsWith(".mdb")) throw new IllegalArgumentException("Only .accdb and .mdb semantic packages are accepted");
        File temporary=File.createTempFile("semantic-access-preview-",name.endsWith(".mdb")?".mdb":".accdb");
        try {
            upload.transferTo(temporary);
            SemanticAccessPackage pack=reader.read(temporary);
            SemanticAccessPreview shape=validation.validate(temporary,pack);
            java.util.List<SemanticAccessIssue> issues=new java.util.ArrayList<>(shape.issues());
            issues.addAll(resolver.validate(pack));
            return new SemanticAccessPreview(pack.productCode(),pack.packageCode(),pack.packageVersion(),issues.isEmpty(),java.util.List.copyOf(issues),shape.datasets(),shape.fields(),shape.relations(),shape.projections());
        }
        finally { Files.deleteIfExists(temporary.toPath()); }
    }
}
