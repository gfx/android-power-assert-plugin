package com.github.gfx.powerassert

import com.android.build.gradle.*
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.builder.DefaultBuildType
import com.android.builder.model.BuildType
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

// see http://www.gradle.org/docs/current/userguide/custom_plugins.html

public class PowerAssertPlugin implements Plugin<Project> {
    static final String TAG = 'PowerAssert'
    static final int VERBOSE = Integer.valueOf(System.getenv('POWERASSERT_VERBOSE') ?: '0')

    static boolean empower = true

    /**
     * If you call <code>setEmpower(false)</code>, this plugin enable assertions but
     * does not add extra information around assertions.
     * Use this if you have problems in android-power-assert-plugin.
     * @param enabled
     */
    public static void setEmpower(boolean enabled) {
        empower = enabled
    }

    @Override
    void apply(Project project) {
        checkAndroidPlugin(project)

        BaseExtension android = project.android

        android.lintOptions.disable "Assert" // assertions are now reliable

        android.packagingOptions {
            // exclude possibly-conflicting files in commons-lang3
            exclude 'META-INF/NOTICE.txt'
            exclude 'META-INF/LICENSE.txt'
        }

        android.buildTypes.all { DefaultBuildType buildType ->
            if (isAssertionsEnabled(buildType)) {
                project.dependencies."${buildType.name}Compile"(Empower.DEPENDENCIES)
            }
        }

        if (android instanceof AppExtension) {
            android.applicationVariants.all { ApplicationVariant variant ->
                if (isAssertionsEnabled(variant.buildType)) {
                    variant.javaCompile.doLast {
                        Empower.enablePowerAssert(project, variant, variant.apkLibraries)
                    }
                    variant.testVariant.javaCompile.doLast {
                        List<File> libs = new ArrayList<>(variant.apkLibraries)
                        libs.addAll(variant.testVariant.apkLibraries)
                        Empower.enablePowerAssert(project, variant.testVariant, libs)
                    }
                }
            }
        } else if (android instanceof LibraryExtension) {
            android.libraryVariants.all { LibraryVariant variant ->
                if (isAssertionsEnabled(variant.buildType)) {
                    variant.javaCompile.doLast {
                        Empower.enablePowerAssert(project, variant, variant.testVariant.apkLibraries)
                    }
                    variant.testVariant.javaCompile.doLast {
                        Empower.enablePowerAssert(project, variant.testVariant, variant.testVariant.apkLibraries)
                    }
                }
            }
        } else {
            throw new GradleException("Unknown extension: ${android}");
        }
    }

    private static boolean isAssertionsEnabled(BuildType variant) {
        // TODO: make it customizable
        return variant.name != "release"
    }

    private static void checkAndroidPlugin(Project project) {
        if (!(project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(LibraryPlugin))) {
            throw new GradleException('No android plugin detected')
        }
    }
}
