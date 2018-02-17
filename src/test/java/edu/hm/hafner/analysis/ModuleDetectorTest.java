package edu.hm.hafner.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.ModuleDetector.FileInputStreamFactory;
import edu.hm.hafner.util.ResourceTest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link ModuleDetector}.
 */
class ModuleDetectorTest extends ResourceTest {
    private static final String MANIFEST = "MANIFEST.MF";
    private static final String MANIFEST_NAME = "MANIFEST-NAME.MF";
    private static final File ROOT = new File("/tmp");
    private static final String PREFIX = normalizeRoot();

    private static String normalizeRoot() {
        return ROOT.getAbsolutePath().replace("\\", "/") + "/";
    }

    private static final int NO_RESULT = 0;

    private static final String PATH_PREFIX_MAVEN = "path/to/maven";
    private static final String PATH_PREFIX_OSGI = "path/to/osgi";
    private static final String PATH_PREFIX_ANT = "path/to/ant";

    private static final String EXPECTED_MAVEN_MODULE = "ADT Business Logic";
    private static final String EXPECTED_ANT_MODULE = "checkstyle";
    private static final String EXPECTED_OSGI_MODULE = "de.faktorlogik.prototyp";

    private InputStream read(final String fileName) {
        return asInputStream(fileName);
    }

    @Test
    void shouldIdentifyModuleByReadingOsgiBundle() {
        FileInputStreamFactory factory = createFileSystemStub(stub -> {
            when(stub.find(any(), anyString())).thenReturn(new String[]{PATH_PREFIX_OSGI + ModuleDetector.OSGI_BUNDLE});
            when(stub.create(anyString())).thenReturn(read(MANIFEST));
        });
        
        ModuleDetector detector = new ModuleDetector(ROOT, factory);

        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_OSGI + "/something.txt")))
                .isEqualTo(EXPECTED_OSGI_MODULE);
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_OSGI + "/in/between/something.txt")))
                .isEqualTo(EXPECTED_OSGI_MODULE);
        assertThat(detector.guessModuleName(PREFIX + "/path/to/something.txt"))
                .isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldIdentifyModuleByReadingOsgiBundleWithVendorInL10nProperties() {
        FileInputStreamFactory factory = createFileSystemStub(stub -> {
            when(stub.find(any(), anyString())).thenReturn(new String[]{PATH_PREFIX_OSGI + ModuleDetector.OSGI_BUNDLE});
            when(stub.create(anyString())).thenReturn(read(MANIFEST), read("l10n.properties"));
        });

        ModuleDetector detector = new ModuleDetector(ROOT, factory);

        String expectedName = "de.faktorlogik.prototyp (My Vendor)";
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_OSGI + "/something.txt")))
                .isEqualTo(expectedName);
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_OSGI + "/in/between/something.txt")))
                .isEqualTo(expectedName);
        assertThat(detector.guessModuleName(PREFIX + "/path/to/something.txt"))
                .isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldIdentifyModuleByReadingOsgiBundleWithManifestName() {
        FileInputStreamFactory fileSystem = createFileSystemStub(stub -> {
            when(stub.find(any(), anyString())).thenReturn(
                    new String[]{PATH_PREFIX_OSGI + ModuleDetector.OSGI_BUNDLE});
            when(stub.create(anyString())).thenReturn(read(MANIFEST_NAME), read("l10n.properties"));
        });

        ModuleDetector detector = new ModuleDetector(ROOT, fileSystem);

        String expectedName = "My Bundle";
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_OSGI + "/something.txt")))
                .isEqualTo(expectedName);
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_OSGI + "/in/between/something.txt")))
                .isEqualTo(expectedName);
        assertThat(detector.guessModuleName(PREFIX + "/path/to/something.txt"))
                .isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldIdentifyModuleByReadingMavenPom() {
        FileInputStreamFactory factory = createFileSystemStub(stub -> {
            when(stub.find(any(), anyString())).thenReturn(
                    new String[]{PATH_PREFIX_MAVEN + ModuleDetector.MAVEN_POM});
            when(stub.create(anyString())).thenAnswer((fileName) -> read(ModuleDetector.MAVEN_POM));
        });

        ModuleDetector detector = new ModuleDetector(ROOT, factory);

        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_MAVEN + "/something.txt"))).isEqualTo(
                EXPECTED_MAVEN_MODULE);
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_MAVEN + "/in/between/something.txt"))).isEqualTo(
                EXPECTED_MAVEN_MODULE);
        assertThat(detector.guessModuleName(PREFIX + "/path/to/something.txt")).isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldIdentifyModuleByReadingMavenPomWithoutName() {
        FileInputStreamFactory factory = createFileSystemStub(stub -> {
            when(stub.find(any(), anyString())).thenReturn(new String[]{PATH_PREFIX_MAVEN + ModuleDetector.MAVEN_POM});
            when(stub.create(anyString())).thenAnswer((fileName) -> read("no-name-pom.xml"));
        });

        ModuleDetector detector = new ModuleDetector(ROOT, factory);

        String artifactId = "com.avaloq.adt.core";
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_MAVEN + "/something.txt")))
                .isEqualTo(artifactId);
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_MAVEN + "/in/between/something.txt")))
                .isEqualTo(artifactId);
        assertThat(detector.guessModuleName(PREFIX + "/path/to/something.txt"))
                .isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldIdentifyModuleByReadingAntProjectFile() {
        FileInputStreamFactory factory = createFileSystemStub(stub -> {
            when(stub.find(any(), anyString())).thenReturn(new String[]{PATH_PREFIX_ANT + ModuleDetector.ANT_PROJECT});
            when(stub.create(anyString())).thenAnswer((fileName) -> read(ModuleDetector.ANT_PROJECT));
        });
        ModuleDetector detector = new ModuleDetector(ROOT, factory);

        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_ANT + "/something.txt")))
                .isEqualTo(EXPECTED_ANT_MODULE);
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_ANT + "/in/between/something.txt")))
                .isEqualTo(EXPECTED_ANT_MODULE);
        assertThat(detector.guessModuleName(PREFIX + "/path/to/something.txt"))
                .isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldIgnoreExceptionsDuringParsing() {
        FileInputStreamFactory fileSystem = createFileSystemStub(stub -> {
            when(stub.find(any(), anyString())).thenReturn(new String[NO_RESULT]);
            when(stub.create(anyString())).thenThrow(new FileNotFoundException("File not found"));
        });

        ModuleDetector detector = new ModuleDetector(ROOT, fileSystem);

        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_ANT + "/something.txt")))
                .isEqualTo(StringUtils.EMPTY);
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_MAVEN + "/something.txt")))
                .isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldIdentifyModuleIfThereAreMoreEntries() {
        FileInputStreamFactory factory = createFileSystemStub(stub -> {
            String ant = PATH_PREFIX_ANT + ModuleDetector.ANT_PROJECT;
            String maven = PATH_PREFIX_MAVEN + ModuleDetector.MAVEN_POM;
            when(stub.find(any(), anyString())).thenReturn(new String[]{ant, maven});
            when(stub.create(PREFIX + ant)).thenReturn(read(ModuleDetector.ANT_PROJECT));
            when(stub.create(PREFIX + maven)).thenAnswer((fileName) -> read(ModuleDetector.MAVEN_POM));
        });

        ModuleDetector detector = new ModuleDetector(ROOT, factory);

        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_ANT + "/something.txt")))
                .isEqualTo(EXPECTED_ANT_MODULE);
        assertThat(detector.guessModuleName(PREFIX + (PATH_PREFIX_MAVEN + "/something.txt")))
                .isEqualTo(EXPECTED_MAVEN_MODULE);
    }

    @Test
    void shouldEnsureThatMavenHasPrecedenceOverAnt() {
        String prefix = "/prefix/";
        String ant = prefix + ModuleDetector.ANT_PROJECT;
        String maven = prefix + ModuleDetector.MAVEN_POM;

        verifyOrder(prefix, ant, maven, new String[]{ant, maven});
        verifyOrder(prefix, ant, maven, new String[]{maven, ant});
    }

    private void verifyOrder(final String prefix, final String ant, final String maven, final String[] foundFiles) {
        FileInputStreamFactory factory = createFileSystemStub(stub -> {
            when(stub.find(any(), anyString())).thenReturn(foundFiles);
            when(stub.create(ant)).thenReturn(read(ModuleDetector.ANT_PROJECT));
            when(stub.create(maven)).thenAnswer((fileName) -> read(ModuleDetector.MAVEN_POM));
        });

        ModuleDetector detector = new ModuleDetector(ROOT, factory);

        assertThat(detector.guessModuleName(prefix + "/something.txt")).isEqualTo(EXPECTED_MAVEN_MODULE);
    }

    @Test
    void shouldEnsureThatOsgiHasPrecedenceOverMavenAndAnt() {
        String prefix = "/prefix/";
        String ant = prefix + ModuleDetector.ANT_PROJECT;
        String maven = prefix + ModuleDetector.MAVEN_POM;
        String osgi = prefix + ModuleDetector.OSGI_BUNDLE;

        verifyOrder(prefix, ant, maven, osgi, new String[]{ant, maven, osgi});
        verifyOrder(prefix, ant, maven, osgi, new String[]{ant, osgi, maven});
        verifyOrder(prefix, ant, maven, osgi, new String[]{maven, ant, osgi});
        verifyOrder(prefix, ant, maven, osgi, new String[]{maven, osgi, ant});
        verifyOrder(prefix, ant, maven, osgi, new String[]{osgi, ant, maven});
        verifyOrder(prefix, ant, maven, osgi, new String[]{osgi, maven, osgi});
    }

    private void verifyOrder(final String prefix, final String ant, final String maven, final String osgi,
            final String[] foundFiles) {
        FileInputStreamFactory fileSystem = createFileSystemStub(stub -> {
            when(stub.find(any(), anyString())).thenReturn(foundFiles);
            when(stub.create(ant)).thenReturn(read(ModuleDetector.ANT_PROJECT));
            when(stub.create(maven)).thenAnswer((fileName) -> read(ModuleDetector.MAVEN_POM));
            when(stub.create(osgi)).thenReturn(read(MANIFEST));
        });

        ModuleDetector detector = new ModuleDetector(ROOT, fileSystem);

        assertThat(detector.guessModuleName(prefix + "/something.txt")).isEqualTo(EXPECTED_OSGI_MODULE);
    }

    private FileInputStreamFactory createFileSystemStub(final Stub stub) {
        try {
            FileInputStreamFactory fileSystem = mock(FileInputStreamFactory.class);
            stub.apply(fileSystem);
            return fileSystem;
        }
        catch (FileNotFoundException exception) {
            throw new AssertionError(exception);
        }
    }

    @FunctionalInterface
    private interface Stub {
        void apply(FileInputStreamFactory f) throws FileNotFoundException;
    }
}