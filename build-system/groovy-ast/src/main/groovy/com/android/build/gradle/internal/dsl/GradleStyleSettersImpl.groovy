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

package com.android.build.gradle.internal.dsl

import com.google.common.base.CaseFormat
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.objectweb.asm.Opcodes.ACC_PUBLIC

/**
 * Implementation of {@link GradleStyleSetters}.
 */
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class GradleStyleSettersImpl implements ASTTransformation {
    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        ClassNode annotatedClass = astNodes[1] as ClassNode
        List<MethodNode> generatedSetters = generateSetters(annotatedClass)
        generatedSetters.each annotatedClass.&addMethod
    }

    List<MethodNode> generateSetters(ClassNode classNode) {
        def setters = classNode.methods.findAll {
            it.name.startsWith("set")  &&
                    (it.modifiers & ACC_PUBLIC) &&
                    it.parameters.length == 1
        }

        setters.collect{ MethodNode setter ->
            def name = CaseFormat.UPPER_CAMEL.to(
                    CaseFormat.LOWER_CAMEL,
                    setter.name[3..-1])

            if (classNode.methods.find {
                it.name == name &&
                        it.parameters.length == 1 &&
                        it.parameters[0].type == setter.parameters[0].type
            }) {
                println "Skipping $name(${setter.parameters[0].type.typeClass})"
                // Don't clash with existing methods.
                return null
            }

            def type = setter.parameters[0].type.typeClass

            def ast = new AstBuilder().buildFromSpec {
                method name, ACC_PUBLIC, Void.TYPE, {
                    parameters {
                        parameter "value": type
                    }
                    exceptions {}
                    block {
                        expression {
                            methodCall {
                                variable "this"
                                constant setter.name
                                argumentList {
                                    variable "value"
                                }
                            }
                        }
                    }
                }
            }

            // TODO: Generate varargs setters for collections.
            ast[0]
        }.findAll() // Remove nulls.
    }
}
