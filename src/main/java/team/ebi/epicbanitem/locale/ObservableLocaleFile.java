package team.ebi.epicbanitem.locale;

import com.google.common.base.Charsets;
import team.ebi.epicbanitem.util.file.ObservableFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;

/**
 * @author The EpicBanItem Team
 */
public class ObservableLocaleFile implements ObservableFile {
    private final FileConsumer<Reader> updateConsumer;
    private final Path path;
    private boolean closed;

    private ObservableLocaleFile(Builder builder) throws IOException {
        this.updateConsumer = builder.updateConsumer;
        this.path = builder.path;
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }

        this.next(StandardWatchEventKinds.ENTRY_MODIFY, path);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void next(Kind<Path> kind, Path path) throws IOException {
        if (closed) {
            return;
        }
        if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
            Files.createFile(path);
        } else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind) || StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
            Reader reader = new InputStreamReader(Files.newInputStream(path), Charsets.UTF_8);
            this.updateConsumer.accept(reader);
        }
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
    }

    public static final class Builder {
        private FileConsumer<Reader> updateConsumer;
        private Path path;

        public Builder updateConsumer(final FileConsumer<Reader> updateConsumer) {
            this.updateConsumer = updateConsumer;
            return this;
        }

        public Builder path(final Path path) {
            this.path = path;
            return this;
        }

        public ObservableLocaleFile build() throws IOException {
            return new ObservableLocaleFile(this);
        }
    }
}
