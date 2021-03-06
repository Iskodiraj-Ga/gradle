/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.nativeplatform.fixtures

import org.gradle.api.internal.file.BaseDirFileResolver
import org.gradle.api.internal.file.TestFiles
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.time.Time
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory
import org.gradle.test.fixtures.file.TestFile
import org.junit.runner.RunWith

/**
 * Runs a test separately for each installed tool chain.
 */
@RunWith(SingleToolChainTestRunner.class)
abstract class AbstractInstalledToolChainIntegrationSpec extends AbstractIntegrationSpec {
    static AvailableToolChains.InstalledToolChain toolChain
    File initScript

    def setup() {
        initScript = file("init.gradle") << """
allprojects { p ->
    apply plugin: ${toolChain.pluginClass}

    model {
          toolChains {
            ${toolChain.buildScriptConfig}
          }
    }
}
"""
        executer.beforeExecute({
            usingInitScript(initScript)
        })
    }

    def NativeInstallationFixture installation(Object installDir, OperatingSystem os = OperatingSystem.current()) {
        return new NativeInstallationFixture(file(installDir), os)
    }

    def String withExecutableSuffix(Object path) {
        return path + OperatingSystem.current().getExecutableSuffix()
    }

    def String getExecutableExtension() {
        def suffix = OperatingSystem.current().executableSuffix
        return suffix.empty ? "" : suffix.substring(1)
    }

    def ExecutableFixture executable(Object path) {
        return toolChain.executable(file(path))
    }

    def LinkerOptionsFixture linkerOptionsFor(String taskName, TestFile projectDir = testDirectory) {
        return toolChain.linkerOptionsFor(projectDir.file("build/tmp/$taskName/options.txt"))
    }

    def TestFile objectFile(Object path) {
        return toolChain.objectFile(file(path))
    }

    def String withLinkLibrarySuffix(Object path) {
        return path + OperatingSystem.current().linkLibrarySuffix
    }

    def String getLinkLibrarySuffix() {
        return OperatingSystem.current().linkLibrarySuffix.substring(1)
    }

    def String withSharedLibrarySuffix(Object path) {
        return path + OperatingSystem.current().sharedLibrarySuffix
    }

    def String getSharedLibraryExtension() {
        return OperatingSystem.current().sharedLibrarySuffix.substring(1)
    }

    def SharedLibraryFixture sharedLibrary(Object path) {
        return toolChain.sharedLibrary(file(path))
    }

    def StaticLibraryFixture staticLibrary(Object path) {
        return toolChain.staticLibrary(file(path))
    }

    def NativeBinaryFixture resourceOnlyLibrary(Object path) {
        return toolChain.resourceOnlyLibrary(file(path))
    }

    def objectFileFor(File sourceFile, String rootObjectFilesDir = "build/objs/main/main${sourceType}") {
        return intermediateFileFor(sourceFile, rootObjectFilesDir, OperatingSystem.current().isWindows() ? ".obj" : ".o")
    }

    def intermediateFileFor(File sourceFile, String intermediateFilesDir, String intermediateFileSuffix) {
        File objectFile = new CompilerOutputFileNamingSchemeFactory(new BaseDirFileResolver(TestFiles.fileSystem(), testDirectory, TestFiles.getPatternSetFactory())).create()
            .withObjectFileNameSuffix(intermediateFileSuffix)
            .withOutputBaseFolder(file(intermediateFilesDir))
            .map(file(sourceFile))
        return file(getTestDirectory().toURI().relativize(objectFile.toURI()))
    }

    protected void maybeWait() {
        if (toolChain.visualCpp) {
            def now = Time.clock().currentTime
            def nextSecond = now % 1000
            Thread.sleep(1200 - nextSecond)
        }
    }
}
