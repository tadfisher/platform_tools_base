/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.lint.checks;
/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.XmlParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.ast.AstVisitor;
import lombok.ast.ClassDeclaration;
import lombok.ast.Expression;
import lombok.ast.Identifier;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;
import lombok.ast.StrictListAccessor;
import lombok.ast.TypeMember;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableReference;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_EXPORTED;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.xml.AndroidManifest.ATTRIBUTE_NAME;
import static com.android.xml.AndroidManifest.NODE_ACTION;
import static com.android.xml.AndroidManifest.NODE_ACTIVITY;
import static com.android.xml.AndroidManifest.NODE_APPLICATION;
import static com.android.xml.AndroidManifest.NODE_DATA;
import static com.android.xml.AndroidManifest.NODE_INTENT;
import static com.android.xml.AndroidManifest.NODE_MANIFEST;
import static com.android.xml.AndroidManifest.NODE_METADATA;

/**
 * Check if the usage of App Indexing is correct.
 */
public class AppIndexingApiDetectorV2 extends Detector
  implements Detector.XmlScanner, Detector.JavaScanner {

  private static final Implementation IMPLEMENTATION_JAVA =
    new Implementation(
      AppIndexingApiDetectorV2.class,
      EnumSet.of(Scope.JAVA_FILE, Scope.MANIFEST),
      Scope.JAVA_FILE_SCOPE, Scope.MANIFEST_SCOPE);

  public static final Issue ISSUE_APP_INDEXING_ERROR =
    Issue.create(
      "AppIndexingCodeError", //$NON-NLS-1$
      "Wrong Usage of App Indexing",
      "Ensure the app can be linked and indexable, "
      + "by integration with App Indexing for Google Search.",
      Category.USABILITY,
      5,
      Severity.ERROR,
      IMPLEMENTATION_JAVA)
      .addMoreInfo("https://g.co/AppIndexing");

  public static final Issue ISSUE_APP_INDEXING_WARNING =
    Issue.create(
      "AppIndexingCodeWarning", //$NON-NLS-1$
      "Missing App Indexing Support",
      "Ensure the app can be linked and indexable, "
      + "by integration with App Indexing for Google Search.",
      Category.USABILITY,
      5,
      Severity.WARNING,
      IMPLEMENTATION_JAVA)
      .addMoreInfo("https://g.co/AppIndexing");

  private static final String APP_INDEX_START = "start"; //$NON-NLS-1$
  private static final String APP_INDEX_END = "end"; //$NON-NLS-1$
  private static final String CLIENT_CONNECT = "connect"; //$NON-NLS-1$
  private static final String CLIENT_DISCONNECT = "disconnect"; //$NON-NLS-1$
  private static final String ADD_API = "addApi"; //$NON-NLS-1$
  private static final String ACTIVITY = "Activity"; //$NON-NLS-1$

  private List<String> activitiesToCheck = Lists.newArrayList();
  private Map<ClassDeclaration, Location> activitiesNeedsIndex = Maps.newHashMap();
  private Map<MethodInvocation, Location> startMethod = Maps.newHashMap();
  private Map<MethodInvocation, Location> endMethod = Maps.newHashMap();
  private boolean hasConnectMethod = false;
  private boolean hasDisconnectMethod = false;
  private boolean hasAddAppIndexApi = false;
  private Map<Location, String> startClient = Maps.newHashMap();
  private Map<Location, String> endClient = Maps.newHashMap();
  private Map<Location, String> clientField = Maps.newHashMap();

  /** Constructs a new {@link AppIndexingApiDetectorV2} */
  public AppIndexingApiDetectorV2() {}

  // ---- Implements XmlScanner ----

  @Override
  @Nullable
  public Collection<String> getApplicableElements() {
    return Collections.singletonList(NODE_APPLICATION);
  }

  @Override
  public void visitElement(@NonNull XmlContext context, @NonNull Element application) {
    // check GMS
    if (!hasGMS(application)) {
      context.report(ISSUE_APP_INDEXING_ERROR, application, context.getLocation(application),
                     "GMS should be added to the app");
    }

    List<Element> activities = extractChildrenByName(application, NODE_ACTIVITY);
    boolean applicationHasActionView = false;
    for (Element activity : activities) {
      List<Element> intents = extractChildrenByName(activity, NODE_INTENT);
      boolean activityHasActionView = false;
      for (Element intent : intents) {
        boolean actionView = hasActionView(intent);
        if (actionView) {
          activityHasActionView = true;
        }
      }
      if (activityHasActionView) {
        applicationHasActionView = true;
        if (activity.hasAttributeNS(ANDROID_URI, ATTR_EXPORTED)) {
          Attr exported = activity.getAttributeNodeNS(ANDROID_URI, ATTR_EXPORTED);
          if (!exported.getValue().equals("true")) {
            // Report error if the activity supporting action view is not exported.
            context.report(ISSUE_APP_INDEXING_ERROR, activity, context.getLocation(activity),
                           "Activity supporting ACTION_VIEW isn't exported correctly");
          }
        }
      }
    }
    if (!applicationHasActionView) {
      // Report warning if there're no
      context.report(ISSUE_APP_INDEXING_WARNING, application, context.getLocation(application),
                     "Application should has at least one Activity supporting ACTION_VIEW");
    }
  }

  /**
   * Check if the intent filter supports action view.
   * @param intent the intent filter
   * @return true if it does
   */
  private static boolean hasActionView(Element intent) {
    List<Element> actions = extractChildrenByName(intent, NODE_ACTION);
    for (Element action : actions) {
      if (action.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
        Attr attr = action.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
        if (attr.getValue().equals("android.intent.action.VIEW")) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks whether the application tag has GMS support
   * GMS support means the metadata tag indicating GMS version.
   * @param application The application tag.
   * @return True if the application has GMS support.
   */
  private static boolean hasGMS(Element application) {
    List<Element> children = LintUtils.getChildren(application);
    for (Element metadata : children) {
      if (metadata.getNodeName().equals(NODE_METADATA)) {
        if (metadata.hasAttributeNS(ANDROID_URI, ATTR_NAME)) {
          Attr attr = metadata.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
          if (attr.getValue().equals("com.google.android.gms.version")) {
            return true;
          }
        }
      }
    }
    return false;
  }

  // ---- Implements JavaScanner ----

  @Nullable
  @Override
  public List<String> getApplicableMethodNames() {
    return Arrays.asList(APP_INDEX_START, APP_INDEX_END,
                         CLIENT_CONNECT, CLIENT_DISCONNECT, ADD_API);
  }

  @Override
  public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
                          @NonNull MethodInvocation node) {
    if (node.astName().astValue().equals(APP_INDEX_START)) {
      // check operand and arguments.
      String operand = node.astOperand().toString();
      StrictListAccessor<Expression, MethodInvocation> args = node.astArguments();
      if ((operand.equals("AppIndex.AppIndexApi") || operand.endsWith(".AppIndex.AppIndexApi"))
          && args.size()== 2 && args.first() instanceof VariableReference
          && args.last() instanceof VariableReference) {
        startMethod.put(node, context.getLocation(node.astName()));
        startClient.put(context.getLocation(node.astArguments().first()),
                        node.astArguments().first().toString());
      }
    } else if (node.astName().astValue().equals(APP_INDEX_END)) {
      String operand = node.astOperand().toString();
      StrictListAccessor<Expression, MethodInvocation> args = node.astArguments();
      if ((operand.equals("AppIndex.AppIndexApi") || operand.endsWith(".AppIndex.AppIndexApi"))
          && args.size()== 2 && args.first() instanceof VariableReference
          && args.last() instanceof VariableReference) {
        endMethod.put(node, context.getLocation(node.astName()));
        endClient.put(context.getLocation(node.astArguments().first()),
                      node.astArguments().first().toString());
      }
    } else if (node.astName().astValue().equals(CLIENT_CONNECT)
               && node.rawOperand() instanceof VariableReference && node.astArguments().size() == 0) {
      hasConnectMethod = true;
    } else if (node.astName().astValue().equals(CLIENT_DISCONNECT)
               && node.rawOperand() instanceof VariableReference && node.astArguments().size() == 0) {
      hasDisconnectMethod = true;
    }else if (node.astName().astValue().equals(ADD_API)) {
      StrictListAccessor<Expression, MethodInvocation> args = node.astArguments();
      if (args.size() == 1 && args.first().toString().equals("AppIndex.APP_INDEX_API")) {
        hasAddAppIndexApi = true;
      }
    }
  }

  @Override
  public void afterCheckFile(@NonNull Context context) {
    if (context instanceof JavaContext) {
      JavaContext javaContext = (JavaContext) context;
      ClassDeclaration classDeclaration = getClassDeclaration(javaContext);
      if (classDeclaration != null && classDeclaration.astExtending() != null
          && classDeclaration.astExtending().getTypeName().endsWith(ACTIVITY)) {
        // finds the activity classes that need app activity annotation
        getActivitiesToCheck(context);

        // app indexing API used but no support in manifest
        boolean hasIntent = activitiesToCheck.contains(classDeclaration.astName().astValue());
        if (!hasIntent) {
          for (Map.Entry<MethodInvocation, Location> method : startMethod.entrySet()) {
            javaContext.report(
              ISSUE_APP_INDEXING_ERROR,
              method.getValue(),
              "Missing app indexing support in manifest");
          }
          for (Map.Entry<MethodInvocation, Location> method : endMethod.entrySet()) {
            if (classDeclaration == JavaContext.findSurroundingClass(method.getKey())) {
              javaContext.report(
                ISSUE_APP_INDEXING_ERROR,
                method.getValue(),
                "Missing app indexing support in manifest");
            }
          }
          return;
        }

        // collects Activity class that needs app indexing.
        activitiesNeedsIndex.put(classDeclaration,
                                 javaContext.getLocation(classDeclaration.astName()));

        collectGoogleApiClient(classDeclaration, javaContext);
      }
    }
  }

  @Override
  public void afterCheckProject(@NonNull Context context) {
    boolean haveAllJavaFile = context.getScope().contains(Scope.ALL_JAVA_FILES);
    if (!haveAllJavaFile) {
      return;
    }

    // `AppIndex.AppIndexApi.start` and `AppIndex.AppIndexApi.end` should exist
    if (startMethod.isEmpty() && endMethod.isEmpty()) {
      for (Map.Entry<ClassDeclaration, Location> activity : activitiesNeedsIndex.entrySet()) {
        context.report(ISSUE_APP_INDEXING_WARNING, activity.getValue(),
                       "Missing app indexing api call");
      }
      return;
    }

    // GoogleApiClient `connect` should exist
    if (!hasConnectMethod) {
      for (Map.Entry<Location, String> client : startClient.entrySet()) {
        context.report(ISSUE_APP_INDEXING_ERROR, client.getKey(),
                       String.format("GoogleApiClient `%1$s` has not connected", client.getValue()));
      }
    }

    // GoogleApiClient `disconnect` should exist
    if (!hasDisconnectMethod) {
      for (Map.Entry<Location, String> client : endClient.entrySet()) {
        context.report(ISSUE_APP_INDEXING_ERROR, client.getKey(), String.format(
          "GoogleApiClient `%1$s` should disconnect afterwards", client.getValue()));
      }
    }

    // `AppIndex.AppIndexApi.end` should pair with `AppIndex.AppIndexApi.start`
    if (endMethod.isEmpty()) {
      for (Map.Entry<MethodInvocation, Location> method : startMethod.entrySet()) {
        context.report(ISSUE_APP_INDEXING_ERROR, method.getValue(),
                       "Missing corresponding `AppIndex.AppIndexApi.end` method");
      }
    }

    // `AppIndex.AppIndexApi.start` should pair with `AppIndex.AppIndexApi.end`
    if (startMethod.isEmpty()) {
      for (Map.Entry<MethodInvocation, Location> method : endMethod.entrySet()) {
        context.report(ISSUE_APP_INDEXING_ERROR, method.getValue(),
                       "Missing corresponding `AppIndex.AppIndexApi.start` method");
      }
    }

    // GoogleApiClient.Builder should addApi(AppIndex.APP_INDEX_API)
    if (!hasAddAppIndexApi) {
      for (Map.Entry<Location, String> client : clientField.entrySet()) {
        context.report(ISSUE_APP_INDEXING_WARNING, client.getKey(),
                       String.format("GoogleApiClient `%1$s`'s Builder may need to add app indexing API",
                                     client.getValue()));
      }
    }
  }

  /**
   * Gets names of activities which needs app indexing.
   * i.e. the activities have data tag in their intent filters.
   * @param context The context to check in.
   */
  private void getActivitiesToCheck(Context context) {
    List<File> manifestFiles = context.getProject().getManifestFiles();
    XmlParser xmlParser = context.getDriver().getClient().getXmlParser();
    if (xmlParser != null) {
      for (File manifest : manifestFiles) {
        XmlContext xmlContext =
          new XmlContext(context.getDriver(), context.getProject(),
                         null, manifest, null, xmlParser);
        Document doc = xmlParser.parseXml(xmlContext);
        if (doc != null) {
          List<Element> children = LintUtils.getChildren(doc);
          for (Element child : children) {
            if (child.getNodeName().equals(NODE_MANIFEST)) {
              List<Element> apps = getChildrenByName(child, NODE_APPLICATION);
              for (Element app : apps) {
                List<Element> acts = getChildrenByName(app, NODE_ACTIVITY);
                for (Element act : acts) {
                  List<Element> intents = getChildrenByName(act, NODE_INTENT);
                  for (Element intent : intents) {
                    List<Element> data = getChildrenByName(intent, NODE_DATA);
                    if (!data.isEmpty() && act.hasAttributeNS(
                      ANDROID_URI, ATTRIBUTE_NAME)) {
                      Attr attr = act.getAttributeNodeNS(
                        ANDROID_URI, ATTRIBUTE_NAME);
                      activitiesToCheck.add(attr.getValue().substring(1));
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Collects Activity class's GoogleApiClient members.
   * @param classDeclaration The Activity class that needs app indexing.
   * @param context The Java context.
   */
  private void collectGoogleApiClient(ClassDeclaration classDeclaration, JavaContext context) {
    NormalTypeBody body = classDeclaration.astBody();
    if (body != null) {
      for (TypeMember member : body.astMembers()) {
        if (member instanceof VariableDeclaration) {
          VariableDefinition definition = ((VariableDeclaration) member).astDefinition();
          String typeName = definition.astTypeReference().getTypeName();
          if (typeName.equals("GoogleApiClient") || typeName.endsWith(".GoogleApiClient")) {
            Identifier name = definition.astVariables().first().astName();
            clientField.put(context.getLocation(name), name.astValue());
          }
        }
      }
    }
  }

  /**
   * Gets children elements by name
   * @param parent The parent element.
   * @param name The element name.
   * @return All the children elements with the name.
   */
  private static List<Element> getChildrenByName(Element parent, String name) {
    List<Element> result = Lists.newArrayList();
    List<Element> children = LintUtils.getChildren(parent);
    for (Element child : children) {
      if (child.getNodeName().equals(name)) {
        result.add(child);
      }
    }
    return result;
  }

  /**
   * Gets the class given a Java file context.
   * @param context The Java context.
   * @return The class declared in the file.
   */
  private static ClassDeclaration getClassDeclaration(JavaContext context) {
    Node compilationUnit = context.getCompilationUnit();
    List<Node> nodeList;
    if (compilationUnit != null) {
      nodeList = compilationUnit.getChildren();
      for (Node node : nodeList) {
        if (node instanceof ClassDeclaration) {
          return (ClassDeclaration) node;
        }
      }
    }
    return null;
  }

  private static List<Element> extractChildrenByName(@NonNull Element node,
                                                     @NonNull String name) {
    List<Element> result = Lists.newArrayList();
    List<Element> children = LintUtils.getChildren(node);
    for (Element child : children) {
      if (child.getNodeName().equals(name)) {
        result.add(child);
      }
    }
    return result;
  }
}
