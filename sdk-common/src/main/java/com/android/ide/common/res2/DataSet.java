/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.common.res2;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.ILogger;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a set of {@link DataItem}s.
 *
 * The items can be coming from multiple source folders, and duplicates are detected.
 *
 * Each source folders is considered to be at the same level. To use overlays, a
 * {@link DataMerger} must be used.
 *
 * Creating the set and adding folders does not load the data.
 * The data can be loaded from the files, or from a blob which is generated by the set itself.
 *
 * Upon loading the data from the blob, the data can be updated with fresher files. Each item
 * that is updated is flagged as such, in order to manage incremental update.
 *
 * Writing/Loading the blob is not done through this class directly, but instead through the
 * {@link DataMerger} which contains DataSet objects.
 */
abstract class DataSet<I extends DataItem<F>, F extends DataFile<I>> implements SourceSet, DataMap<I> {

    static final String NODE_SOURCE = "source";
    static final String NODE_FILE = "file";
    static final String ATTR_CONFIG = "config";
    static final String ATTR_PATH = "path";
    static final String ATTR_NAME = "name";

    private final String mConfigName;

    /**
     * List of source files. The may not have been loaded yet.
     */
    private final List<File> mSourceFiles = Lists.newArrayList();

    /**
     * The key is the {@link DataItem#getKey()}.
     * This is a multimap to support moving a data item from one file to another (values file)
     * during incremental update.
     */
    private final ListMultimap<String, I> mItems = ArrayListMultimap.create();

    /**
     * Map of source files to DataFiles. This is a multimap because the key is the source
     * file/folder, not the File for the DataFile itself.
     */
    private final ListMultimap<File, F> mSourceFileToDataFilesMap = ArrayListMultimap.create();

    /**
     * Map from a File to its DataFile.
     */
    private final Map<File, F> mDataFileMap = Maps.newHashMap();

    /**
     * Creates a DataSet with a given configName. The name is used to identify the set
     * across sessions.
     *
     * @param configName the name of the config this set is associated with.
     */
    public DataSet(String configName) {
        mConfigName = configName;
    }

    protected abstract DataSet<I, F> createSet(String name);

    /**
     * Creates a DataFile and associated DataItems from an XML node from a file created with
     * {@link DataSet#appendToXml(org.w3c.dom.Node, org.w3c.dom.Document, MergeConsumer)}
     *
     * @param file the file represented by the DataFile
     * @param fileNode the XML node.
     * @return a DataFile
     */
    protected abstract F createFileAndItems(@NonNull File file, @NonNull Node fileNode);

    /**
     * Reads the content of a data folders and loads the DataItem.
     *
     * This should generate DataFiles, and process them with
     * {@link #processNewDataFile(java.io.File, DataFile, boolean)}.
     *
     * @param sourceFolder the source folder to load the resources from.
     *
     * @throws DuplicateDataException
     * @throws IOException
     */
    protected abstract void readSourceFolder(File sourceFolder, ILogger logger)
            throws DuplicateDataException, IOException;

    @Nullable
    protected abstract F createFileAndItems(File sourceFolder, File file, ILogger logger)
            throws IOException;

    /**
     * Adds a collection of source files.
     * @param files the source files to add.
     */
    public void addSources(Collection<File> files) {
        mSourceFiles.addAll(files);
    }

    /**
     * Adds a new source file
     * @param file the source file.
     */
    public void addSource(File file) {
        mSourceFiles.add(file);
    }

    /**
     * Get the list of source files.
     * @return the source files.
     */
    @NonNull
    @Override
    public List<File> getSourceFiles() {
        return mSourceFiles;
    }

    /**
     * Returns the config name.
     * @return the config name.
     */
    public String getConfigName() {
        return mConfigName;
    }

    /**
     * Returns a matching Source file that contains a given file.
     *
     * "contains" means that the source file/folder is the root folder
     * of this file. The folder and/or file doesn't have to exist.
     *
     * @param file the file to search for
     * @return the Source file or null if no match is found.
     */
    @Override
    public File findMatchingSourceFile(File file) {
        for (File sourceFile : mSourceFiles) {
            if (sourceFile.equals(file)) {
                return sourceFile;
            } else if (sourceFile.isDirectory()) {
                String sourcePath = sourceFile.getAbsolutePath() + File.separator;
                if (file.getAbsolutePath().startsWith(sourcePath)) {
                    return sourceFile;
                }
            }
        }

        return null;
    }

    /**
     * Returns the number of items.
     * @return the number of items.
     *
     * @see DataMap
     */
    @Override
    public int size() {
        // returns the number of keys, not the size of the multimap which would include duplicate
        // ResourceItem objects.
        return mItems.keySet().size();
    }

    /**
     * Returns whether the set is empty of items.
     * @return true if the set contains no items.
     */
    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    /**
     * Returns a map of the items.
     * @return a map of items.
     *
     * @see DataMap
     */
    @NonNull
    @Override
    public ListMultimap<String, I> getDataMap() {
        return mItems;
    }

    /**
     * Loads the DataSet from the files its source folders contain.
     *
     * All loaded items are set to TOUCHED. This is so that after loading the resources from
     * the files, they can be written directly (since touched force them to be written).
     *
     * This also checks for duplicates items.
     *
     * @throws DuplicateDataException
     * @throws IOException
     */
    public void loadFromFiles(ILogger logger) throws DuplicateDataException, IOException {
        for (File file : mSourceFiles) {
            if (file.isDirectory()) {
                readSourceFolder(file, logger);

            } else if (file.isFile()) {
                // TODO support resource bundle
            }
        }
        checkItems();
    }

    /**
     * Appends the DataSet to a given DOM object.
     *
     * @param setNode the root node for this set.
     * @param document The root XML document
     */
    void appendToXml(@NonNull Node setNode, @NonNull Document document,
            @NonNull MergeConsumer<I> consumer) {
        // add the config name attribute
        NodeUtils.addAttribute(document, setNode, null, ATTR_CONFIG, mConfigName);

        // add the source files.
        // we need to loop on the source files themselves and not the map to ensure we
        // write empty resourceSets
        for (File sourceFile : mSourceFiles) {

            // the node for the source and its path attribute
            Node sourceNode = document.createElement(NODE_SOURCE);
            setNode.appendChild(sourceNode);
            NodeUtils.addAttribute(document, sourceNode, null, ATTR_PATH,
                    sourceFile.getAbsolutePath());

            Collection<F> dataFiles = mSourceFileToDataFilesMap.get(sourceFile);

            for (F dataFile : dataFiles) {
                if (!dataFile.hasNotRemovedItems()) {
                    continue;
                }

                // the node for the file and its path and qualifiers attribute
                Node fileNode = document.createElement(NODE_FILE);
                sourceNode.appendChild(fileNode);
                NodeUtils.addAttribute(document, fileNode, null, ATTR_PATH,
                        dataFile.getFile().getAbsolutePath());
                dataFile.addExtraAttributes(document, fileNode, null);

                if (dataFile.getType() == DataFile.FileType.MULTI) {
                    for (I item : dataFile.getItems()) {
                        if (item.isRemoved()|| consumer.ignoreItemInMerge(item)) {
                            continue;
                        }
                        Node adoptedNode = item.getAdoptedNode(document);
                        if (adoptedNode != null) {
                            fileNode.appendChild(adoptedNode);
                        }
                    }
                } else {
                    // no need to check for isRemoved here since it's checked
                    // at the file level and there's only one item.
                    I dataItem = dataFile.getItem();
                    NodeUtils.addAttribute(document, fileNode, null, ATTR_NAME, dataItem.getName());
                    dataItem.addExtraAttributes(document, fileNode, null);
                }
            }
        }
    }

    /**
     * Creates and returns a new DataSet from an XML node that was created with
     * {@link #appendToXml(org.w3c.dom.Node, org.w3c.dom.Document, MergeConsumer)}
     *
     * The object this method is called on is not modified. This should be static but can't be
     * due to children classes.
     *
     * @param dataSetNode the node to read from.
     * @return a new DataSet object or null.
     */
    DataSet<I,F> createFromXml(Node dataSetNode) {
        // get the config name
        Attr configNameAttr = (Attr) dataSetNode.getAttributes().getNamedItem(ATTR_CONFIG);
        if (configNameAttr == null) {
            return null;
        }

        // create the DataSet that will be filled with the content of the XML.
        DataSet<I, F> dataSet = createSet(configNameAttr.getValue());

        // loop on the source nodes
        NodeList sourceNodes = dataSetNode.getChildNodes();
        for (int i = 0, n = sourceNodes.getLength(); i < n; i++) {
            Node sourceNode = sourceNodes.item(i);

            if (sourceNode.getNodeType() != Node.ELEMENT_NODE ||
                    !NODE_SOURCE.equals(sourceNode.getLocalName())) {
                continue;
            }

            Attr pathAttr = (Attr) sourceNode.getAttributes().getNamedItem(ATTR_PATH);
            if (pathAttr == null) {
                continue;
            }

            File sourceFolder = new File(pathAttr.getValue());
            dataSet.mSourceFiles.add(sourceFolder);

            // now loop on the files inside the source folder.
            NodeList fileNodes = sourceNode.getChildNodes();
            for (int j = 0, m = fileNodes.getLength(); j < m; j++) {
                Node fileNode = fileNodes.item(j);

                if (fileNode.getNodeType() != Node.ELEMENT_NODE ||
                        !NODE_FILE.equals(fileNode.getLocalName())) {
                    continue;
                }

                pathAttr = (Attr) fileNode.getAttributes().getNamedItem(ATTR_PATH);
                if (pathAttr == null) {
                    continue;
                }
                
                F dataFile = createFileAndItems(new File(pathAttr.getValue()), fileNode);

                if (dataFile != null) {
                    dataSet.processNewDataFile(sourceFolder, dataFile, false /*setTouched*/);
                }
            }
        }

        return dataSet;
    }

    /**
     * Checks for duplicate items across all source files.
     *
     * @throws DuplicateDataException if a duplicated item is found.
     */
    protected void checkItems() throws DuplicateDataException {
        // check a list for duplicate, ignoring removed items.
        for (Map.Entry<String, Collection<I>> entry : mItems.asMap().entrySet()) {
            Collection<I> items = entry.getValue();

            // there can be several version of the same key if some are "removed"
            I lastItem = null;
            for (I item : items) {
                if (!item.isRemoved()) {
                    if (lastItem == null) {
                        lastItem = item;
                    } else {
                        throw new DuplicateDataException(item, lastItem);
                    }
                }
            }
        }
    }

    /**
     * Update the DataSet with a given file.
     *
     * @param sourceFolder the sourceFile containing the changedFile
     * @param changedFile The changed file
     * @param fileStatus the change state
     * @return true if the set was properly updated, false otherwise
     */
    public boolean updateWith(File sourceFolder, File changedFile, FileStatus fileStatus,
                              ILogger logger)
            throws IOException {
        switch (fileStatus) {
            case NEW:
                return handleNewFile(sourceFolder, changedFile, logger);
            case CHANGED:
                return handleChangedFile(sourceFolder, changedFile);
            case REMOVED:
                F dataFile = mDataFileMap.get(changedFile);

                // flag all resource items are removed
                for (I dataItem : dataFile.getItems()) {
                    dataItem.setRemoved();
                }
                return true;
        }

        return false;
    }

    protected boolean isValidSourceFile(@NonNull File sourceFolder, @NonNull File file) {
        return checkFileForAndroidRes(file);
    }

    protected boolean handleNewFile(File sourceFolder, File file, ILogger logger)
            throws IOException {
        F dataFile = createFileAndItems(sourceFolder, file, logger);
        if (dataFile != null) {
            processNewDataFile(sourceFolder, dataFile, true /*setTouched*/);
        }
        return true;
    }

    protected void processNewDataFile(@NonNull File sourceFolder,
                                      @NonNull F dataFile,
                                      boolean setTouched) {
        Collection<I> dataItems = dataFile.getItems();

        addDataFile(sourceFolder, dataFile);

        for (I dataItem : dataItems) {
            mItems.put(dataItem.getKey(), dataItem);
            if (setTouched) {
                dataItem.setTouched();
            }
        }
    }

    protected boolean handleChangedFile(@NonNull File sourceFolder,
                                        @NonNull File changedFile) throws IOException {
        F dataFile = mDataFileMap.get(changedFile);
        dataFile.getItem().setTouched();
        return true;
    }

    protected void addItem(I item, String key) {
        if (key == null) {
            key = item.getKey();
        }

        mItems.put(key, item);
    }

    protected F getDataFile(File file) {
        return mDataFileMap.get(file);
    }

    /**
     * Adds a new DataFile to this.
     *
     * @param sourceFile the parent source file.
     * @param dataFile the DataFile
     */
    private void addDataFile(@NonNull File sourceFile, @NonNull F dataFile) {
        mSourceFileToDataFilesMap.put(sourceFile, dataFile);
        mDataFileMap.put(dataFile.getFile(), dataFile);
    }

    @Override
    public String toString() {
        return Arrays.toString(mSourceFiles.toArray());
    }

    /**
     * Checks a file to make sure it is a valid file in the android res/asset folders.
     * @param file the file to check
     * @return true if it is a valid file, false if it should be ignored.
     */
    protected boolean checkFileForAndroidRes(File file) {
        // TODO: use the aapt ignore pattern value.
        // We should move this somewhere else when introduce aapt pattern

        String name = file.getName();
        int pos = name.lastIndexOf('.');
        String extension = "";
        if (pos != -1) {
            extension = name.substring(pos + 1);
        }

        // ignore hidden files and backup files
        return !(name.charAt(0) == '.' || name.charAt(name.length() - 1) == '~') &&
                !"scc".equalsIgnoreCase(extension) &&     // VisualSourceSafe
                !"swp".equalsIgnoreCase(extension) &&     // vi swap file
                !"thumbs.db".equalsIgnoreCase(name) &&    // image index file
                !"picasa.ini".equalsIgnoreCase(name);     // image index file
    }
}
