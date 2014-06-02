/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.tasks;

import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PARENT;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.DOT_CLASS;
import static com.android.SdkConstants.DOT_GIF;
import static com.android.SdkConstants.DOT_JPEG;
import static com.android.SdkConstants.DOT_JPG;
import static com.android.SdkConstants.DOT_PNG;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.PREFIX_ANDROID;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_RESOURCES;
import static com.android.SdkConstants.TAG_STYLE;
import static com.android.utils.SdkUtils.endsWith;
import static com.android.utils.SdkUtils.endsWithIgnoreCase;
import static com.google.common.base.Charsets.UTF_8;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.resources.ResourceUrl;
import com.android.ide.common.xml.XmlPrettyPrinter;
import com.android.resources.FolderTypeRelationship;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.utils.XmlUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

public class ResourceUsageAnalyzer {
    private static final boolean DEBUG = false;
    public static final int TYPICAL_RESOURCE_COUNT = 200;

    private final File mResourceClassDir;
    private final File mProguardMapping;
    private final File mProguardedClasses;
    private final File mMergedManifest;
    private final File mMergedResourceDir;

    private boolean mVerbose = false;
    private boolean mDryRun = false;

    /** The computed set of unused resources */
    private List<Resource> mUnused;

    /** List of all known resources (parsed from R.java) */
    private List<Resource> mResources = Lists.newArrayListWithExpectedSize(TYPICAL_RESOURCE_COUNT);
    /** Map from R field value to corresponding resource */
    private Map<Integer, Resource> mValueToResource =
            Maps.newHashMapWithExpectedSize(TYPICAL_RESOURCE_COUNT);
    /** Map from resource type to map from resource name to resource object */
    private Map<ResourceType, Map<String, Resource>> mTypeToName =
            Maps.newEnumMap(ResourceType.class);
    /** Map from ProGuard renamed resource classes to corresponding resource types */
    private Map<String, ResourceType> mRenamedClasses = Maps.newHashMapWithExpectedSize(20);

    public ResourceUsageAnalyzer(
            @NonNull File rDir,
            @NonNull File classesJar,
            @NonNull File manifest,
            @Nullable File mapping,
            @NonNull File resources) {
        mResourceClassDir = rDir;
        mProguardMapping = mapping;
        mProguardedClasses = classesJar;
        mMergedManifest = manifest;
        mMergedResourceDir = resources;
    }

    public void analyze() throws IOException, ParserConfigurationException, SAXException {
        gatherResourceValues(mResourceClassDir);
        recordMapping(mProguardMapping);
        recordUsages(mProguardedClasses);
        recordManifestUsages(mMergedManifest);
        recordResources(mMergedResourceDir);
        dumpReferences();
        findUnused();
        dropIdResources();
        dumpStats();
    }

    private void dumpStats() {
        System.out.println(
            mUnused.size() + "/" + mResources.size() + " (" + mUnused.size() * 100 / mResources
                    .size() + "%) are unused and will be stripped out of the APK");
    }

    public boolean isDryRun() {
        return mDryRun;
    }

    public void setDryRun(boolean dryRun) {
        mDryRun = dryRun;
    }

    public boolean isVerbose() {
        return mVerbose;
    }

    public void setVerbose(boolean verbose) {
        mVerbose = verbose;
    }

    public void removeUnused() throws IOException, ParserConfigurationException, SAXException {
        Multimap<File, Resource> valueResources = ArrayListMultimap.create(100, 20);
        for (Resource resource : mUnused) {
            if (resource.declarations != null) {
                for (File file : resource.declarations) {
                    ResourceFolderType folderType = ResourceFolderType
                            .getFolderType(file.getParentFile().getName());
                    if (folderType != null && folderType != ResourceFolderType.VALUES) {
                        // TODO: Write to a proguard-like log file instead!
                        if (isVerbose()) {
                            System.out.println("Deleted " + file);
                        }
                        if (!isDryRun()) {
                            boolean delete = file.delete();
                            if (!delete) {
                                System.err.println("Could not delete " + file);
                            }
                        }
                    } else {
                        valueResources.put(file, resource);
                    }
                }
            }
        }

        // Delete value resources: Must rewrite the XML files
        for (File file : valueResources.keySet()) {
            String xml = Files.toString(file, UTF_8);
            Document document = XmlUtils.parseDocument(xml, true);
            Element root = document.getDocumentElement();
            if (root != null && TAG_RESOURCES.equals(root.getTagName())) {
                stripUnused(root);
                String formatted = XmlPrettyPrinter.prettyPrint(document, xml.endsWith("\n"));
                if (!mDryRun) {
                    Files.write(formatted, file, UTF_8);
                }
            }
        }
    }

    private void stripUnused(Element element) {
        ResourceType type = getResourceType(element);
        if (type == ResourceType.ATTR) {
            // Not yet properly handled
            return;
        }

        Resource resource = getResource(element);
        if (resource != null) {
            if (!resource.reachable) {
                element.getParentNode().removeChild(element);
                return;
            }
            if (resource.type == ResourceType.DECLARE_STYLEABLE ||
                    resource.type == ResourceType.ATTR) {
                // Don't strip children of declare-styleable; we're not correctly
                // tracking field references of the R_styleable_attr fields yet
                return;
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                stripUnused((Element)child);
            }
        }
    }

    private static String getFieldName(Element element) {
        return getFieldName(element.getAttribute(ATTR_NAME));
    }

    @Nullable
    private Resource getResource(Element element) {
        ResourceType type = getResourceType(element);
        if (type != null) {
            String name = getFieldName(element);
            if (name != null) {
                return getResource(type, name);
            }
        }

        return null;
    }

    private static ResourceType getResourceType(Element element) {
        String tagName = element.getTagName();
        if (tagName.equals(TAG_ITEM)) {
            String typeName = element.getAttribute(ATTR_TYPE);
            if (!typeName.isEmpty()) {
                return ResourceType.getEnum(typeName);
            }
        } else if ("string-array".equals(tagName) || "integer-array".equals(tagName)) {
            return ResourceType.ARRAY;
        } else {
            return ResourceType.getEnum(tagName);
        }
        return null;
    }

    private void dropIdResources() {
        List<Resource> nonIds = Lists.newArrayListWithExpectedSize(mResources.size());
        for (Resource resource : mResources) {
            if (resource.type != ResourceType.ID) {
                nonIds.add(resource);
            }
        }
        mResources = nonIds;
    }

    private void findUnused() {
        List<Resource> roots = Lists.newArrayList();

        for (Resource resource : mResources) {
            if (resource.reachable && resource.type != ResourceType.ID
                    && resource.type != ResourceType.ATTR) {
                roots.add(resource);
            }
        }

        if (DEBUG) {
            System.out.println(
                    "The root reachable resources are: " + Joiner.on(",\n   ").join(roots));
        }

        Map<Resource,Boolean> seen = new IdentityHashMap<Resource,Boolean>(mResources.size());
        for (Resource root : roots) {
            visit(root, seen);
        }

        List<Resource> unused = Lists.newArrayListWithExpectedSize(mResources.size());
        for (Resource resource : mResources) {
            if (!resource.reachable && resource.type != ResourceType.ID) {
                unused.add(resource);
            }
        }

        mUnused = unused;
    }

    private static void visit(Resource root, Map<Resource, Boolean> seen) {
        if (seen.containsKey(root)) {
            return;
        }
        seen.put(root, Boolean.TRUE);
        root.reachable = true;
        if (root.references != null) {
            for (Resource referenced : root.references) {
                visit(referenced, seen);
            }
        }
    }

    private void dumpReferences() {
        if (DEBUG) {
            for (Resource resource : mResources) {
                if (resource.references != null) {
                    System.out.println(resource + " => " + resource.references);
                }
            }
        }
    }

    private void recordResources(File resDir)
            throws IOException, SAXException, ParserConfigurationException {
        File[] resourceFolders = resDir.listFiles();
        if (resourceFolders != null) {
            for (File folder : resourceFolders) {
                ResourceFolderType folderType = ResourceFolderType.getFolderType(folder.getName());
                if (folderType != null) {
                    recordResources(folderType, folder);
                }
            }
        }
    }

    private void recordResources(@NonNull ResourceFolderType folderType, File folder)
            throws ParserConfigurationException, SAXException, IOException {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String path = file.getPath();
                boolean isXml = endsWithIgnoreCase(path, DOT_XML);

                Resource from = null;
                // Record resource for the whole file
                if (folderType != ResourceFolderType.VALUES
                        && (isXml
                            || endsWith(path, DOT_PNG) //also true for endsWith(name, DOT_9PNG)
                            || endsWith(path, DOT_JPG)
                            || endsWith(path, DOT_GIF)
                            || endsWith(path, DOT_JPEG))) {
                    List<ResourceType> types = FolderTypeRelationship.getRelatedResourceTypes(
                            folderType);
                    ResourceType type = types.get(0);
                    assert type != ResourceType.ID : folderType;
                    String name = file.getName();
                    name = name.substring(0, name.indexOf('.'));
                    Resource resource = getResource(type, name);
                    if (resource != null) {
                        resource.addLocation(file);
                        from = resource;
                    }
                }

                if (isXml) {
                    // For value files, and drawables and colors etc also pull in resource
                    // references inside the file
                    recordResourcesUsages(file, from);
                }
            }
        }
    }

    private void recordMapping(@Nullable File mapping) throws IOException {
        if (mapping == null || !mapping.exists()) {
            return;
        }
        final String ARROW = " -> ";
        final String RESOURCE = ".R$";
        for (String line : Files.readLines(mapping, UTF_8)) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                continue;
            }
            int index = line.indexOf(RESOURCE);
            if (index == -1) {
                continue;
            }
            int arrow = line.indexOf(ARROW, index + 3);
            if (arrow == -1) {
                continue;
            }
            String typeName = line.substring(index + RESOURCE.length(), arrow);
            ResourceType type = ResourceType.getEnum(typeName);
            if (type == null) {
                continue;
            }
            int end = line.indexOf(':', arrow + ARROW.length());
            if (end == -1) {
                end = line.length();
            }
            String target = line.substring(arrow + ARROW.length(), end).trim();
            String ownerName = target.replace('.', '/'); // TODO: Are these EVER inner classes?
            mRenamedClasses.put(ownerName, type);
        }
    }

    private void recordManifestUsages(File manifest)
            throws IOException, ParserConfigurationException, SAXException {
        String xml = Files.toString(manifest, UTF_8);
        Document document = XmlUtils.parseDocument(xml, true);
        recordManifestUsages(document.getDocumentElement());
    }

    private void recordResourcesUsages(@NonNull File file, @Nullable Resource from)
            throws IOException, ParserConfigurationException, SAXException {
        String xml = Files.toString(file, UTF_8);
        Document document = XmlUtils.parseDocument(xml, true);
        recordResourceReferences(file, document.getDocumentElement(), from);
    }

    @Nullable
    private Resource getResource(@NonNull ResourceType type, @NonNull String name) {
        Map<String, Resource> nameMap = mTypeToName.get(type);
        if (nameMap != null) {
            return nameMap.get(name);
        }
        return null;
    }

    @Nullable
    private Resource getResource(@NonNull String possibleUrlReference) {
        ResourceUrl url = ResourceUrl.parse(possibleUrlReference);
        if (url != null && !url.framework) {
            return getResource(url.type, url.name);
        }

        return null;
    }

    private void recordManifestUsages(Node node) {
        short nodeType = node.getNodeType();
        if (nodeType == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Attr attr = (Attr) attributes.item(i);
                markReachable(getResource(attr.getValue()));
            }
        } else if (nodeType == Node.TEXT_NODE) {
            // Does this apply to any manifests??
            String text = node.getNodeValue().trim();
            markReachable(getResource(text));
        }

        NodeList children = node.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            recordManifestUsages(child);
        }
    }


    private void recordResourceReferences(@NonNull File file, Node node, @Nullable Resource from) {
        short nodeType = node.getNodeType();
        if (nodeType == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if (from != null) {
                NamedNodeMap attributes = element.getAttributes();
                for (int i = 0, n = attributes.getLength(); i < n; i++) {
                    Attr attr = (Attr) attributes.item(i);
                    Resource resource = getResource(attr.getValue());
                    if (resource != null) {
                        from.addReference(resource);
                    }
                }
            }

            Resource definition = getResource(element);
            if (definition != null) {
                from = definition;
                definition.addLocation(file);
            }

            String tagName = element.getTagName();
            if (TAG_STYLE.equals(tagName)) {
                if (element.hasAttribute(ATTR_PARENT)) {
                    String parent = element.getAttribute(ATTR_PARENT);
                    if (!parent.isEmpty() && !parent.startsWith(ANDROID_STYLE_RESOURCE_PREFIX) &&
                            !parent.startsWith(PREFIX_ANDROID)) {
                        String parentStyle = parent;
                        if (!parentStyle.startsWith(STYLE_RESOURCE_PREFIX)) {
                            parentStyle = STYLE_RESOURCE_PREFIX + parentStyle;
                        }
                        Resource ps = getResource(getFieldName(parentStyle));
                        if (ps != null && definition != null) {
                            definition.addReference(ps);
                        }
                    }
                } else {
                    // Implicit parent styles by name
                    String name = getFieldName(element);
                    while (true) {
                        int index = name.lastIndexOf('_');
                        if (index != -1) {
                            name = name.substring(0, index);
                            Resource ps = getResource(STYLE_RESOURCE_PREFIX + getFieldName(name));
                            if (ps != null && definition != null) {
                                definition.addReference(ps);
                            }
                        } else {
                            break;
                        }
                    }
                }
            }

            if (TAG_ITEM.equals(tagName)) {
                // In style? If so the name: attribute can be a reference
                if (element.getParentNode() != null
                        && element.getParentNode().getNodeName().equals(TAG_STYLE)) {
                    String name = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
                    if (!name.isEmpty() && !name.startsWith("android:")) {
                        Resource resource = getResource(ResourceType.ATTR, name);
                        if (definition == null) {
                            Element style = (Element) element.getParentNode();
                            definition = getResource(style);
                            if (definition != null) {
                                from = definition;
                                definition.addReference(resource);
                            }
                        }
                    }
                }
            }
        } else if (nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE) {
            String text = node.getNodeValue().trim();
            Resource textResource = getResource(getFieldName(text));
            if (textResource != null && from != null) {
                from.addReference(textResource);
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            recordResourceReferences(file, child, from);
        }
    }

    public static String getFieldName(@NonNull String styleName) {
        return styleName.replace('.', '_').replace('-', '_').replace(':', '_');
    }

    private static void markReachable(@Nullable Resource resource) {
        if (resource != null) {
            resource.reachable = true;
        }
    }

    private void recordUsages(File jarFile) throws IOException {
        if (!jarFile.exists()) {
            return;
        }
        ZipInputStream zis = null;
        try {
            FileInputStream fis = new FileInputStream(jarFile);
            try {
                zis = new ZipInputStream(fis);
                ZipEntry entry = zis.getNextEntry();
                while (entry != null) {
                    String name = entry.getName();
                    if (name.endsWith(DOT_CLASS)) {
                        byte[] bytes = ByteStreams.toByteArray(zis);
                        if (bytes != null) {
                            ClassReader classReader = new ClassReader(bytes);
                            classReader.accept(new UsageVisitor(), 0);
                        }
                    }

                    entry = zis.getNextEntry();
                }
            } finally {
                //noinspection deprecation
                Closeables.closeQuietly(fis);
            }
        } finally {
            //noinspection deprecation
            Closeables.closeQuietly(zis);
        }
    }

    private void gatherResourceValues(File file) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    gatherResourceValues(child);
                }
            }
        } else if (file.isFile() && file.getName().equals(SdkConstants.FN_RESOURCE_CLASS)) {
            parseResourceClass(file);
        }
    }

    // TODO: Use Lombok/ECJ here
    private void parseResourceClass(File file) throws IOException {
        String s = Files.toString(file, UTF_8);
        // Simple parser which handles only aapt's special R output
        int index = 0;
        int length = s.length();
        String classDeclaration = "public static final class ";
        while (true) {
            index = s.indexOf(classDeclaration, index);
            if (index == -1) {
                break;
            }
            int start = index + classDeclaration.length();
            int end = s.indexOf(' ', start);
            if (end == -1) {
                break;
            }
            String typeName = s.substring(start, end);
            ResourceType type = ResourceType.getEnum(typeName);
            if (type == null) {
                break;
            }

            index = end;

            // Find next declaration
            for (; index < length - 1; index++) {
                char c = s.charAt(index);
                if (Character.isWhitespace(c)) {
                    //noinspection UnnecessaryContinue
                    continue;
                } else if (c == '/') {
                    char next = s.charAt(index + 1);
                    if (next == '*') {
                        // Scan forward to comment end
                        end = index + 2;
                        while (end < length -2) {
                            c = s.charAt(end);
                            if (c == '*' && s.charAt(end + 1) == '/') {
                                end++;
                                break;
                            } else {
                                end++;
                            }
                        }
                        index = end;
                    } else if (next == '/') {
                        // Scan forward to next newline
                        assert false : s.substring(index - 1, index + 50); // we don't put line comments in R files
                    } else {
                        assert false : s.substring(index - 1, index + 50); // unexpected division
                    }
                } else if (c == 'p' && s.startsWith("public ", index)) {
                    if (type == ResourceType.STYLEABLE) {
                        start = s.indexOf(" int", index);
                        if (s.startsWith(" int[] ", start)) {
                            end = s.indexOf('=', start);
                            assert end != -1;
                            String styleable = s.substring(start, end).trim();
                            addResource(ResourceType.DECLARE_STYLEABLE, styleable, null);

                            // TODO: Read in all the action bar ints!
                            // For now, we're simply treating all R.attr fields as used
                        } else if (s.startsWith(" int ")) {
                            // Read these fields in and correlate with the attr R's. Actually
                            // we don't need this for anything; the local attributes are
                            // found by the R attr thing. I just need to record the class
                            // (style).
                            // public static final int ActionBar_background = 10;
                            // ignore - jump to end
                            index = s.indexOf(';', index);
                            if (index == -1) {
                                break;
                            }
                            // For now, we're simply treating all R.attr fields as used
                        }
                    } else {
                        start = s.indexOf(" int ", index);
                        if (start != -1) {
                            start += " int ".length();
                            //abc_fade_in=0x7f040000;
                            end = s.indexOf('=', start);
                            assert end != -1;
                            String name = s.substring(start, end).trim();
                            start = end + 1;
                            end = s.indexOf(';', start);
                            assert end != -1;
                            String value = s.substring(start, end).trim();
                            addResource(type, name, value);
                        }
                    }
                } else if (c == '}') {
                    // Done with resource class
                    break;
                }
            }
        }
    }

    private void addResource(@NonNull ResourceType type, @NonNull String name,
            @Nullable String value) {
        int realValue = value != null ? Integer.decode(value) : -1;
        Resource resource = new Resource(type, name, realValue);
        mResources.add(resource);
        if (realValue != -1) {
            mValueToResource.put(realValue, resource);
        }
        Map<String, Resource> nameMap = mTypeToName.get(type);
        if (nameMap == null) {
            nameMap = Maps.newHashMapWithExpectedSize(30);
            mTypeToName.put(type, nameMap);
        }
        nameMap.put(name, resource);

        // TODO: Assert that we don't set the same resource multiple times to different values.
        // Could happen if you pass in stale data!
    }

    private static class Resource {
        public ResourceType type;
        public String name;
        public int value;
        public boolean reachable;
        public List<Resource> references;
        public final List<File> declarations = Lists.newArrayList();

        private Resource(ResourceType type, String name, int value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return type + ":" + name + ":" + value;
        }

        @SuppressWarnings("RedundantIfStatement") // Generated by IDE
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Resource resource = (Resource) o;

            if (name != null ? !name.equals(resource.name) : resource.name != null) {
                return false;
            }
            if (type != resource.type) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        public void addLocation(@NonNull File file) {
            declarations.add(file);
        }

        public void addReference(@Nullable Resource resource) {
            if (resource != null) {
                if (references == null) {
                    references = Lists.newArrayList();
                } else if (references.contains(resource)) {
                    return;
                }
                references.add(resource);
            }
        }
    }

    private class UsageVisitor extends ClassVisitor {
        public UsageVisitor() {
            super(Opcodes.ASM4);
        }

        @Override
        public MethodVisitor visitMethod(int access, final String name,
                String desc, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM4) {
                @Override
                public void visitLdcInsn(Object cst) {
                    if (cst instanceof Integer) {
                        Integer value = (Integer)cst;
                        markReachable(mValueToResource.get(value));
                    }
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    if (opcode == Opcodes.GETSTATIC) {
                        ResourceType type = mRenamedClasses.get(owner);
                        if (type != null) {
                            Resource resource = getResource(type, name);
                            if (resource != null) {
                                markReachable(resource);
                            }
                        }
                    }
                }
            };
        }
    }

    @VisibleForTesting
    String dumpResourceModel() {
        StringBuilder sb = new StringBuilder(1000);
        Collections.sort(mResources, new Comparator<Resource>() {
            @Override
            public int compare(Resource resource1,
                    Resource resource2) {
                int delta = resource1.type.compareTo(resource2.type);
                if (delta != 0) {
                    return delta;
                }
                return resource1.name.compareTo(resource2.name);
            }
        });

        for (Resource resource : mResources) {
            sb.append("@").append(resource.type).append("/").append(resource.name)
                    .append(" : reachable=").append(resource.reachable);
            sb.append("\n");
            if (resource.references != null) {
                for (Resource referenced : resource.references) {
                    sb.append("    ");
                    sb.append("@").append(resource.type).append("/").append(resource.name);
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }
}
