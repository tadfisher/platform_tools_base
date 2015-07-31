package com.android.build.gradle.tasks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.FilterData;
import com.android.build.OutputFile.FilterType;
import com.android.build.OutputFile.OutputType;
import com.android.build.gradle.api.ApkOutputFile;
import com.android.build.gradle.internal.model.FilterDataImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Callables;

import org.gradle.api.Action;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.ParallelizableTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task to zip align all the splits
 */
@ParallelizableTask
public class SplitZipAlign extends SplitRelatedTask {

    private List<File> densityOrLanguageInputFiles = new ArrayList<File>();

    private List<File> abiInputFiles = new ArrayList<File>();

    private String outputBaseName;

    private Set<String> densityFilters;

    private Set<String> abiFilters;

    private Set<String> languageFilters;

    private File outputDirectory;

    private File zipAlignExe;

    private File apkMetadataFile;

    @OutputFiles
    public List<File> getOutputFiles() {
        ImmutableList.Builder<File> builder = ImmutableList.builder();
        for (ApkOutputFile apk : getOutputSplitFiles()) {
            builder.add(apk.getOutputFile());
        }
        return builder.build();
    }


    @NonNull
    public List<File> getInputFiles() {
        return ImmutableList.<File>builder()
                .addAll(getDensityOrLanguageInputFiles())
                .addAll(getAbiInputFiles())
                .build();
    }

    @Override
    @NonNull
    public synchronized ImmutableList<ApkOutputFile> getOutputSplitFiles() {
        final ImmutableList.Builder<ApkOutputFile> outputFiles = ImmutableList.builder();
        FileHandler addingLogic = new FileHandler() {
            @Override
            public void call(String split, File file) {
                String archivesBaseName = (String)getProject().getProperties().get("archivesBaseName");
                outputFiles.add(
                        new ApkOutputFile(OutputType.SPLIT,
                                ImmutableList.of(
                                        FilterDataImpl.build(
                                                getFilterType(split).toString(),
                                                getFilter(split))),
                        Callables.returning(new File(outputDirectory,
                                archivesBaseName + "-" + outputBaseName + "_" + split + ".apk"))));
            }
        };

        forEachUnalignedInput(addingLogic);
        forEachUnsignedInput(addingLogic);
        return outputFiles.build();
    }

    public FilterType getFilterType(String filter) {
        String languageName = PackageSplitRes.unMangleSplitName(filter);
        if (languageFilters.contains(languageName)) {
            return FilterType.LANGUAGE;
        }

        if (abiFilters.contains(filter)) {
            return FilterType.ABI;
        }

        return FilterType.DENSITY;
    }

    public String getFilter(String filterWithPossibleSuffix) {
        FilterType type = getFilterType(filterWithPossibleSuffix);
        if (type == FilterType.DENSITY) {
            for (String density : densityFilters) {
                if (filterWithPossibleSuffix.startsWith(density)) {
                    return density;
                }
            }
        }
        if (type == FilterType.LANGUAGE) {
            return PackageSplitRes.unMangleSplitName(filterWithPossibleSuffix);
        }
        return filterWithPossibleSuffix;
    }

    /**
     * Returns true if the passed string is one of the filter we must process potentially followed
     * by a prefix (some density filters get V4, V16, etc... appended).
     */
    public boolean isFilter(String potentialFilterWithSuffix) {
        for (String density : densityFilters) {
            if (potentialFilterWithSuffix.startsWith(density)) {
                return true;
            }
        }
        return abiFilters.contains(potentialFilterWithSuffix)
                || languageFilters.contains(
                        PackageSplitRes.unMangleSplitName(potentialFilterWithSuffix));

    }

    private interface FileHandler {
        void call(String split, File file);
    }

    private void forEachUnalignedInput(FileHandler closure) {
        final String archivesBaseName = (String)getProject().getProperties().get("archivesBaseName");
        Pattern unalignedPattern = Pattern.compile(
                archivesBaseName + "-" + outputBaseName + "_(.*)-unaligned.apk");

        for (File file : getInputFiles()) {
            Matcher unaligned = unalignedPattern.matcher(file.getName());
            if (unaligned.matches() && isFilter(unaligned.group(1))) {
                closure.call(unaligned.group(1), file);
            }
        }

    }

    private void forEachUnsignedInput(FileHandler closure) {
        final String archivesBaseName = (String)getProject().getProperties().get("archivesBaseName");
        Pattern unsignedPattern = Pattern.compile(
                archivesBaseName + "-" + outputBaseName + "_(.*)-unsigned.apk");

        for (File file : getInputFiles()) {
            Matcher unsigned = unsignedPattern.matcher(file.getName());
            if (unsigned.matches() && isFilter(unsigned.group(1))) {
                closure.call(unsigned.group(1), file);
            }
        }
    }

    @TaskAction
    public void splitZipAlign() throws IOException {
        final String archivesBaseName = (String)getProject().getProperties().get("archivesBaseName");

        FileHandler zipAlignIt = new FileHandler() {
            @Override
            public void call(String split, final File file) {
                final File out = new File(getOutputDirectory(),
                        archivesBaseName + "-" + outputBaseName + "_" + split + ".apk");
                getProject().exec(new Action<ExecSpec>() {
                            @Override
                            public void execute(ExecSpec spec) {
                                spec.executable(getZipAlignExe());
                                spec.args("-f", "4");
                                spec.args(file.getAbsolutePath());
                                spec.args(out);
                            }
                        });
            }
        };
        forEachUnalignedInput(zipAlignIt);
        forEachUnsignedInput(zipAlignIt);
        saveApkMetadataFile();
    }

    @Override
    public List<FilterData> getSplitsData() {
        ImmutableList.Builder<FilterData> filterDataBuilder = ImmutableList.builder();
        addAllFilterData(filterDataBuilder, densityFilters, FilterType.DENSITY);
        addAllFilterData(filterDataBuilder, languageFilters, FilterType.LANGUAGE);
        addAllFilterData(filterDataBuilder, abiFilters, FilterType.ABI);
        return filterDataBuilder.build();
    }

    @InputFiles
    public List<File> getDensityOrLanguageInputFiles() {
        return densityOrLanguageInputFiles;
    }

    public void setDensityOrLanguageInputFiles(List<File> densityOrLanguageInputFiles) {
        this.densityOrLanguageInputFiles = densityOrLanguageInputFiles;
    }

    @InputFiles
    public List<File> getAbiInputFiles() {
        return abiInputFiles;
    }

    public void setAbiInputFiles(List<File> abiInputFiles) {
        this.abiInputFiles = abiInputFiles;
    }

    @Input
    public String getOutputBaseName() {
        return outputBaseName;
    }

    public void setOutputBaseName(String outputBaseName) {
        this.outputBaseName = outputBaseName;
    }

    @Input
    public Set<String> getDensityFilters() {
        return densityFilters;
    }

    public void setDensityFilters(Set<String> densityFilters) {
        this.densityFilters = densityFilters;
    }

    @Input
    public Set<String> getAbiFilters() {
        return abiFilters;
    }

    public void setAbiFilters(Set<String> abiFilters) {
        this.abiFilters = abiFilters;
    }

    @Input
    public Set<String> getLanguageFilters() {
        return languageFilters;
    }

    public void setLanguageFilters(Set<String> languageFilters) {
        this.languageFilters = languageFilters;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @InputFile
    public File getZipAlignExe() {
        return zipAlignExe;
    }

    public void setZipAlignExe(File zipAlignExe) {
        this.zipAlignExe = zipAlignExe;
    }

    @Override
    @OutputFile
    @Nullable
    public File getApkMetadataFile() {
        return apkMetadataFile;
    }

    public void setApkMetadataFile(@Nullable File apkMetadataFile) {
        this.apkMetadataFile = apkMetadataFile;
    }
}
