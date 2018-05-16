package nl.kb.decoderservice.core.resolve;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toSet;

class FileCacher {
    @JsonProperty("dir")
    private String cacheDir;

    @JsonProperty
    private Integer expireMinutes;

    @JsonProperty
    private Long maxSizeMB;

    private class CacheStats {
        LocalDateTime lastAccess;
        Long fileSize;

        CacheStats(Long fileSize) {
            this.lastAccess = LocalDateTime.now();
            this.fileSize = fileSize;
        }

        @Override
        public String toString() {
            return String.format("lastAccess=%s, fileSize=%d", lastAccess.toString(), fileSize);
        }
    }

    private final Map<String, CacheStats> cacheMap = new ConcurrentHashMap<>();
    private final Set<String> locks = ConcurrentHashMap.newKeySet();

    File fetchLocal(String identifier) {
        final String filename = new String(Base64.getEncoder().encode(identifier.getBytes()));
        while (locks.contains(filename)) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {

            }
        }
        System.err.println(filename);
        final File file = new File(String.format("%s/%s", cacheDir, filename));
        if (file.exists()) {
            cacheMap.put(filename, new CacheStats(file.length()));
        }
        return file;
    }

    public void save(InputStream is, File file) throws IOException {
        locks.add(file.getName());
        try {
            IOUtils.copy(is, new FileOutputStream(file));
            cacheMap.put(file.getName(), new CacheStats(file.length()));
        } catch (IOException e) {
            locks.remove(file.getName());
            throw e;
        }
        locks.remove(file.getName());
    }

    public void clear(String identifier, boolean encode) {
        final String filename = encode ? new String(Base64.getEncoder().encode(identifier.getBytes())) : identifier;
        final File file = new File(String.format("%s/%s", cacheDir, filename));
        if (file.exists()) {
            file.delete();
            cacheMap.remove(filename);
        }
    }

    void expire() {
        long currentSize = cacheMap.values().stream().mapToLong(x -> x.fileSize).sum();
        final long maxSizeBytes = maxSizeMB * 1024 * 1024;

        cacheMap.entrySet().stream()
                .filter(entry -> Duration.between(entry.getValue().lastAccess, LocalDateTime.now()).toMinutes() > expireMinutes)
                .forEach(entry -> clear(entry.getKey(), false));

        if (currentSize >= maxSizeBytes) {

            for (Map.Entry<String, CacheStats> entry : cacheMap.entrySet().stream()
                    .sorted(Comparator.comparing(a -> a.getValue().lastAccess))
                    .collect(toSet())) {

                clear(entry.getKey(), false);
                currentSize -= entry.getValue().fileSize;
                if (currentSize < maxSizeBytes) {
                    break;
                }
            }
        }
    }
}