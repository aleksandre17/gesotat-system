package org.base.api.config;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Safe development/single-node provider. Multi-replica deployments must replace it with a shared store. */
public final class BoundedInMemoryRateLimitStore implements RateLimitStore {
    private final Map<String, MutableWindow> windows = new ConcurrentHashMap<>();

    @Override public Window acquire(String key, long now, long windowMillis, int maxKeys) {
        if (windows.size() >= maxKeys) evict(now, windowMillis, maxKeys);
        MutableWindow w=windows.compute(key,(k,current)->current==null||now-current.startedAt>=windowMillis?new MutableWindow(now):current);
        return new Window(w.startedAt, w.increment());
    }
    private void evict(long now,long window,int maxKeys){
        for(Iterator<Map.Entry<String,MutableWindow>> i=windows.entrySet().iterator();i.hasNext();) if(now-i.next().getValue().startedAt>=window)i.remove();
        if(windows.size()>=maxKeys) windows.clear();
    }
    private static final class MutableWindow {private final long startedAt;private int used;private MutableWindow(long s){startedAt=s;}private synchronized int increment(){return ++used;}}
}
