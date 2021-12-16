package com.example.jar.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * This component is executed after spring boot has loaded
 */
@Component
public class ReadJarFile implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        final Resource springCoreJarResource = new ClassPathResource("/spring-core-5.3.13.jar");
        final InputStream inputStream = springCoreJarResource.getInputStream();
        // Options:
        // alternative: InputStream inputStream = getClass().getResourceAsStream("/spring-core-5.3.13.jar");
        // Use spring's FileSystemResource for jars located in file system
        try (BufferedInputStream bufferedIs = new BufferedInputStream(inputStream);
             ZipInputStream zipInputStream = new ZipInputStream(bufferedIs)) {

            final Optional<ZipEntryData> foundEntry = findSingleEntry("META-INF/license.txt", zipInputStream);
            foundEntry.ifPresent(matchedEntry -> {
                System.out.printf("Found match: %s file-size=[%s]%n", matchedEntry.zipEntry.getName(), matchedEntry.zipEntry.getSize());
                try {
                    final String saveTo = "/tmp/license.txt";
                    Files.copy(matchedEntry.inputStream, Paths.get(saveTo), StandardCopyOption.REPLACE_EXISTING);
                    final File file = new File("/tmp/license.txt");
                    if (file.exists()) {
                        System.out.printf("ZipEntry %s successfully saved to %s size=%sB%n", matchedEntry.zipEntry.getName(), saveTo, file.length());
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to save zip entry " + matchedEntry.zipEntry.getName(), e);
                }
            });
        }
    }

    private Optional<ZipEntryData> findSingleEntry(String path, final ZipInputStream zipInputStream) throws IOException {
        Optional<ZipEntryData> found = empty();
        Optional<ZipEntry> foundZipEntry = empty();
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null && found.isEmpty()) {
            String name = zipEntry.getName();
            if (name.startsWith("META-INF/license.txt")) {
                System.out.printf("File: %s%n", zipEntry.getName());
            }
            if (path.equalsIgnoreCase(name)) {
                final ZipEntryData zipEntryData = new ZipEntryData(zipEntry, zipInputStream);
                found = of(zipEntryData);
            }
        }
        return found;
    }

    static class ZipEntryData {
        ZipEntry zipEntry;
        InputStream inputStream;

        public ZipEntryData(ZipEntry zipEntry, InputStream inputStream) {
            this.zipEntry = zipEntry;
            this.inputStream = inputStream;
        }
    }
}
