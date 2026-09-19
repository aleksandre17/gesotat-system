package org.base.api.service.artifact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Accepted manifest documents (contract §25): the derived file claims and row-to-file edges of an admitted
 * package, persisted once per manifest. Bytes are content-addressed in Object Storage; the Data Plane row is
 * the immutable pointer. Recording the same manifest again is a no-op, so an admission retry is safe.
 */
@Service
public class ArtifactManifestDocuments {
    private static final String EXTENSION = "json";

    private final JdbcTemplate dataPlane;
    private final ObjectProvider<ArtifactObjectStore> stores;
    private final ObjectMapper canonicalJson;
    private final String prefix;
    private final int maxBytes;

    public record Pointer(long manifestId, String sha256, long byteSize, int fileCount, int edgeCount) {}

    public ArtifactManifestDocuments(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane, ObjectProvider<ArtifactObjectStore> stores, ObjectMapper json,
                                     @Value("${platform.artifacts.manifest-document-prefix:artifacts/manifests/}") String prefix,
                                     @Value("${platform.artifacts.max-manifest-document-bytes:268435456}") int maxBytes) {
        this.dataPlane = dataPlane;
        this.stores = stores;
        // Key order is fixed so equal documents have equal bytes and therefore one address.
        this.canonicalJson = json.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true).configure(SerializationFeature.INDENT_OUTPUT, false);
        this.prefix = ArtifactKeys.requirePrefix(prefix);
        if (maxBytes < 1) throw new IllegalArgumentException("platform.artifacts.max-manifest-document-bytes must be positive");
        this.maxBytes = maxBytes;
    }

    public Pointer record(long manifestId, PackageManifestDocument document) {
        Optional<Pointer> existing = pointer(manifestId);
        if (existing.isPresent()) return existing.get();
        byte[] bytes;
        try {
            bytes = canonicalJson.writeValueAsBytes(document);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("Manifest document could not be serialized", impossible);
        }
        if (bytes.length > maxBytes) throw new IllegalArgumentException("Manifest document exceeds " + maxBytes + " bytes");
        String sha256;
        try {
            sha256 = Sha256.of(new ByteArrayInputStream(bytes));
        } catch (IOException impossible) {
            throw new IllegalStateException("Manifest document could not be hashed", impossible);
        }
        var location = store().putContentAddressed(prefix, sha256, EXTENSION, MediaType.APPLICATION_JSON_VALUE, new ByteArrayInputStream(bytes), bytes.length);
        // A concurrent identical admission may have recorded it meanwhile; the primary key keeps one row.
        dataPlane.update("INSERT INTO ingest.artifact_manifest_document(artifact_manifest_id,document_schema,sha256,byte_size,bucket,object_key,file_count,edge_count)"
                        + " SELECT ?,?,?,?,?,?,?,? WHERE NOT EXISTS(SELECT 1 FROM ingest.artifact_manifest_document WITH (UPDLOCK,HOLDLOCK) WHERE artifact_manifest_id=?)",
                manifestId, document.schema(), sha256, bytes.length, location.bucket(), location.key(), document.files().size(), document.edges().size(), manifestId);
        return new Pointer(manifestId, sha256, bytes.length, document.files().size(), document.edges().size());
    }

    public Optional<Pointer> pointer(long manifestId) {
        return dataPlane.query("SELECT artifact_manifest_id,sha256,byte_size,file_count,edge_count FROM ingest.artifact_manifest_document WHERE artifact_manifest_id=?",
                (rs, n) -> new Pointer(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getInt(4), rs.getInt(5)), manifestId).stream().findFirst();
    }

    /** The stored document, verified against its recorded checksum before it is returned. */
    public PackageManifestDocument read(long manifestId) {
        var row = dataPlane.query("SELECT bucket,object_key,sha256 FROM ingest.artifact_manifest_document WHERE artifact_manifest_id=?",
                (rs, n) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3)}, manifestId).stream().findFirst()
                .orElseThrow(() -> new ArtifactNotFoundException("Manifest " + manifestId + " has no accepted document"));
        byte[] bytes = store().read(new ArtifactObjectStore.ObjectLocation(row[0], row[1]), maxBytes);
        try {
            if (!Sha256.of(new ByteArrayInputStream(bytes)).equals(row[2])) throw new ArtifactStorageException("Manifest document does not match its checksum", null);
            return canonicalJson.readValue(bytes, PackageManifestDocument.class);
        } catch (IOException corrupt) {
            throw new ArtifactStorageException("Manifest document is unreadable", corrupt);
        }
    }

    private ArtifactObjectStore store() {
        ArtifactObjectStore store = stores.getIfAvailable();
        if (store == null) throw new ArtifactStorageException("Object Storage is not configured", null);
        return store;
    }
}
