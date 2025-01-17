/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apicompiler

import androidx.privacysandbox.tools.testing.CompilationTestHelper
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationResult

/**
 * Compile the given sources using the PrivacySandboxKspCompiler.
 *
 * Default parameters will set required options like AIDL compiler path and use the latest
 * Android platform API stubs that support the Privacy Sandbox.
 */
fun compileWithPrivacySandboxKspCompiler(
    sources: List<Source>,
    addLibraryStubs: Boolean = true,
    extraProcessorOptions: Map<String, String> = mapOf(),
): TestCompilationResult {
    val provider = PrivacySandboxKspCompiler.Provider()

    val processorOptions = buildMap {
        val aidlPath = (System.getProperty("aidl_compiler_path")
            ?: throw IllegalArgumentException("aidl_compiler_path flag not set."))
        put("aidl_compiler_path", aidlPath)
        putAll(extraProcessorOptions)
    }

    return CompilationTestHelper.compileAll(
        if (addLibraryStubs) sources + syntheticSdkRuntimeLibraryStubs + syntheticUiLibraryStubs
        else sources,
        symbolProcessorProviders = listOf(provider),
        processorOptions = processorOptions,
    )
}

// SDK Runtime library is not available in AndroidX prebuilts, so while that's the case we use fake
// stubs to run our compilation tests.
private val syntheticSdkRuntimeLibraryStubs = listOf(
    Source.kotlin(
        "androidx/privacysandbox/sdkruntime/core/SandboxedSdkCompat.kt", """
        |package androidx.privacysandbox.sdkruntime.core
        |
        |import android.os.IBinder
        |
        |@Suppress("UNUSED_PARAMETER")
        |class SandboxedSdkCompat(sdkInterface: IBinder) {
        |    fun getInterface(): IBinder? = throw RuntimeException("Stub!")
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/sdkruntime/core/SandboxedSdkProviderCompat.kt", """
        |package androidx.privacysandbox.sdkruntime.core
        |
        |import android.content.Context
        |import android.os.Bundle
        |import android.view.View
        |
        |@Suppress("UNUSED_PARAMETER")
        |abstract class SandboxedSdkProviderCompat {
        |   var context: Context? = null
        |       private set
        |   fun attachContext(context: Context): Unit = throw RuntimeException("Stub!")
        |
        |   abstract fun onLoadSdk(params: Bundle): SandboxedSdkCompat
        |
        |   open fun beforeUnloadSdk() {}
        |
        |   abstract fun getView(
        |       windowContext: Context,
        |       params: Bundle,
        |       width: Int,
        |       height: Int
        |   ): View
        |}
        |""".trimMargin()
    ),
)

private val syntheticUiLibraryStubs = listOf(
    Source.kotlin(
        "androidx/privacysandbox/ui/core/SandboxedUiAdapter.kt", """
        |package androidx.privacysandbox.ui.core
        |
        |import android.content.Context
        |import android.view.View
        |import java.util.concurrent.Executor
        |
        |interface SandboxedUiAdapter {
        |  fun openSession(
        |      context: Context,
        |      initialWidth: Int,
        |      initialHeight: Int,
        |      isZOrderOnTop: Boolean,
        |      clientExecutor: Executor,
        |      client: SessionClient
        |  )
        |
        |
        |  interface Session {
        |    fun close()
        |    val view: View
        |  }
        |
        |  interface SessionClient {
        |    fun onSessionError(throwable: Throwable);
        |    fun onSessionOpened(session: Session);
        |  }
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/ui/core/SdkRuntimeUiLibVersions.kt", """
        |package androidx.privacysandbox.ui.core
        |
        |import androidx.annotation.RestrictTo
        |
        |object SdkRuntimeUiLibVersions {
        |    var clientVersion: Int = -1
        |        /**
        |         * @hide
        |         */
        |        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        |        set
        |
        |    const val apiVersion: Int = 1
        |}
        |""".trimMargin()
    ),
)
