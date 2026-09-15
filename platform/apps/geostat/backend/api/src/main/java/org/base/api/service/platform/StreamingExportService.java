package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

/** Bounded, cancellable export stream boundary independent of storage/provider. */
public final class StreamingExportService {
    public record Progress(long emitted, long total, boolean complete) {
        public Progress { if (emitted < 0 || total < 0 || emitted > total) throw new IllegalArgumentException("Invalid progress"); }
    }
    private final ExportCodecRegistry codecs; private final ObjectMapper mapper;
    public StreamingExportService(ExportCodecRegistry codecs, ObjectMapper mapper) { this.codecs=Objects.requireNonNull(codecs); this.mapper=Objects.requireNonNull(mapper); }
    public void write(String format, Map<String,Object> response, OutputStream out, Consumer<Progress> progress) throws Exception {
        write(format,response,out,progress,()->false,Long.MAX_VALUE);
    }
    public void write(String format, Map<String,Object> response, OutputStream out, Consumer<Progress> progress,
                      BooleanSupplier cancelled, long maxBytes) throws Exception {
        Objects.requireNonNull(response); Objects.requireNonNull(out); Objects.requireNonNull(progress);
        Objects.requireNonNull(cancelled); if(maxBytes<0) throw new IllegalArgumentException("maxBytes must be non-negative");
        byte[] bytes=codecs.require(format).encode(response, mapper); int chunk=8192; long emitted=0;
        if(bytes.length>maxBytes) throw new IllegalStateException("Export exceeds configured byte budget");
        progress.accept(new Progress(0, bytes.length, false));
        while(emitted<bytes.length){ if(cancelled.getAsBoolean()) throw new java.util.concurrent.CancellationException("Export cancelled by consumer"); int n=(int)Math.min(chunk, bytes.length-emitted); out.write(bytes,(int)emitted,n); emitted+=n; progress.accept(new Progress(emitted,bytes.length,emitted==bytes.length)); }
        if(bytes.length==0) progress.accept(new Progress(0,0,true)); out.flush();
    }
}
