package com.github.gfx.powerassert

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
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
        assert android != null;

        android.lintOptions.disable "Assert" // assertions are now reliable

        android.packagingOptions {
            // exclude possibly-conflicting files in commons-lang3
            exclude 'META-INF/NOTICE.txt'
            exclude 'META-INF/LICENSE.txt'
        }

        android.buildTypes.all { BuildType buildType ->
            if (isAssertionsEnabled(buildType)) {
                project.dependencies."${buildType.name}Compile"(Empower.DEPENDENCIES)
            }
        }

        if (android instanceof AppExtension) {
            assert android.applicationVariants != null
            android.applicationVariants.all { ApplicationVariant variant ->
                if (isAssertionsEnabled(variant.buildType)) {
                    assert variant.javaCompile != null
                    variant.javaCompile.doLast {
                        def empower = new Empower(project)
                        empower.addClassPaths(variant.apkLibraries)
                        empower.addClassPaths([variant.javaCompile.destinationDir])

                        empower.process(variant)
                    }
                    assert variant.testVariant.javaCompile != null
                    variant.testVariant.javaCompile.doLast {
                        def empower = new Empower(project)
                        empower.addClassPaths(variant.apkLibraries)
                        empower.addClassPaths([variant.javaCompile.destinationDir])

                        empower.addClassPaths(variant.testVariant.apkLibraries)
                        empower.addClassPaths([variant.testVariant.javaCompile.destinationDir])
                        empower.process(variant.testVariant)
                    }
                }
            }
        } else if (android instanceof LibraryExtension) {
            assert android.libraryVariants != null;
            android.libraryVariants.all { LibraryVariant variant ->
                if (isAssertionsEnabled(variant.buildType)) {
                    assert variant.javaCompile != null
                    variant.javaCompile.doLast {
                        def empower = new Empower(project)
                        empower.addClassPaths(variant.testVariant.apkLibraries)
                        empower.addClassPaths([variant.javaCompile.destinationDir])

                        empower.process(variant)
                    }
                    assert variant.testVariant.javaCompile != null
                    variant.testVariant.javaCompile.doLast {
                        def empower = new Empower(project)
                        empower.addClassPaths(variant.testVariant.apkLibraries)
                        empower.addClassPaths([variant.javaCompile.destinationDir])

                        empower.addClassPaths([variant.testVariant.javaCompile.destinationDir])
                        empower.process(variant.testVariant)
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
