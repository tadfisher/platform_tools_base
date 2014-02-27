package com.android.manifmerger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.OutputStream;

/**
 * merges android manifest files, idempotent.
 */
public class ManifestMerger2 {

    private final File mainManifestFile;
    private final ImmutableList<File> libraryFiles;
    private final ImmutableList<File> flavorsAndBuildTypeFiles;

    private ManifestMerger2(File mainManifestFile,
            ImmutableList<File> libraryFiles,
            ImmutableList<File> flavorsAndBuildTypeFiles) {
        this.mainManifestFile = mainManifestFile;
        this.libraryFiles = libraryFiles;
        this.flavorsAndBuildTypeFiles = flavorsAndBuildTypeFiles;
    }

    private void merge(OutputStream outputStream) throws MergeFailureException {
        Optional<XmlDocument> xmlDocumentOptional = Optional.absent();
        for (File inputFile : libraryFiles.reverse()) {
            xmlDocumentOptional = merge(xmlDocumentOptional, inputFile);
        }
        xmlDocumentOptional = merge(xmlDocumentOptional, mainManifestFile);
        for (File inputFile : flavorsAndBuildTypeFiles.reverse()) {
            xmlDocumentOptional = merge(xmlDocumentOptional, inputFile);
        }
        if (xmlDocumentOptional.isPresent()) {
            xmlDocumentOptional.get().write(outputStream);
        }
    }

    // merge the optionally existing xmlDocument with a higher priority xml file.
    private static Optional<XmlDocument> merge(Optional<XmlDocument> xmlDocument, File xmlFile)
            throws MergeFailureException {
        XmlLoader xmlLoader = new XmlLoader();
        XmlDocument xmlFileDocument;
        try {
            xmlFileDocument = xmlLoader.load(xmlFile);
        } catch (Exception e) {
            throw new MergeFailureException(e);
        }
        return xmlDocument.isPresent()
                ? xmlFileDocument.merge(xmlDocument.get())
                : Optional.of(xmlFileDocument);
    }

    public static Builder newBuilder(File mainManifestFile) {
        return new Builder(mainManifestFile);
    }

    // a wrapper exception to all sorts of failure exceptions that can be thrown during merging.
    public static class MergeFailureException extends Exception {

        private MergeFailureException(Exception cause) {
            super(cause);
        }
    }

    public static final class Builder {

        private final File mainManifestFile;
        private final ImmutableList.Builder<File> libraryFilesBuilder =
                new ImmutableList.Builder<File>();
        private final ImmutableList.Builder<File> flavorsAndBuildTypeFiles =
                new ImmutableList.Builder<File>();

        Builder(File mainManifestFile) {
            this.mainManifestFile = mainManifestFile;
        }

        public Builder addLibraryManifest(File file) {
            libraryFilesBuilder.add(file);
            return this;
        }

        public Builder addLibraryManifests(File... files) {
            libraryFilesBuilder.add(files);
            return this;
        }

        public Builder addFlavorAndBuildTypeManifest(File file) {
            this.flavorsAndBuildTypeFiles.add(file);
            return this;
        }

        public Builder addFlavorAndBuildTypeManifests(File... files) {
            this.flavorsAndBuildTypeFiles.add(files);
            return this;
        }

        public void merge(OutputStream outputStream) throws MergeFailureException {
            ManifestMerger2 manifestMerger =
                    new ManifestMerger2(mainManifestFile,
                            libraryFilesBuilder.build(), flavorsAndBuildTypeFiles.build());
            manifestMerger.merge(outputStream);
        }
    }
}
