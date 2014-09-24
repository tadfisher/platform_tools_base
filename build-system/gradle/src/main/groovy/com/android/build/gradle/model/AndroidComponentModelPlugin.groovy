package com.android.build.gradle.model

import com.android.build.gradle.internal.dsl.BuildTypeFactory
import com.android.build.gradle.internal.dsl.GroupableProductFlavorDsl
import com.android.build.gradle.internal.dsl.GroupableProductFlavorFactory
import com.android.builder.core.BuilderConstants
import com.android.builder.core.DefaultBuildType
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.language.base.internal.LanguageRegistry
import org.gradle.model.Model
import org.gradle.model.RuleSource

/**
 * Created by chiur on 9/24/14.
 */
public class AndroidComponentModelPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        // Add project as an extension so that it can be used in model rules until Gradle provides
        // methods to replace project.file and project.container.
        project.extensions.add("projectModel", project)
    }

    @RuleSource
    static class Rules {
        @Model
        Project projectModel(ExtensionContainer extensions) {
            return extensions.getByType(Project)
        }

        @Model("android.buildTypes")
        NamedDomainObjectContainer<DefaultBuildType> buildTypes(ServiceRegistry serviceRegistry,
                Project project) {
            Instantiator instantiator = serviceRegistry.get(Instantiator.class)
            def buildTypeContainer = project.container(DefaultBuildType,
                    new BuildTypeFactory(instantiator,  project, project.getLogger()))

            // create default Objects, signingConfig first as its used by the BuildTypes.
            buildTypeContainer.create(BuilderConstants.DEBUG)
            buildTypeContainer.create(BuilderConstants.RELEASE)

            buildTypeContainer.whenObjectRemoved {
                throw new UnsupportedOperationException("Removing build types is not supported.")
            }
            return buildTypeContainer
        }

        @Model("android.productFlavors")
        NamedDomainObjectContainer<GroupableProductFlavorDsl> productFlavors(
                ServiceRegistry serviceRegistry,
                Project project) {
            Instantiator instantiator = serviceRegistry.get(Instantiator.class)
            def productFlavorContainer = project.container(GroupableProductFlavorDsl,
                    new GroupableProductFlavorFactory(instantiator, project, project.getLogger()))

            productFlavorContainer.whenObjectRemoved {
                throw new UnsupportedOperationException("Removing product flavors is not supported.")
            }

            return productFlavorContainer
        }
    }
}
