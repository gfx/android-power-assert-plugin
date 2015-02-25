package com.github.gfx.powerassert
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.api.TestVariant
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
            prepareAppExtension(android)
        } else if (android instanceof LibraryExtension) {
            prepareLibraryExtension(android)
        } else {
            throw new GradleException("Unknown extension: ${android}");
        }
    }

    static void prepareAppExtension(AppExtension android) {
        assert android.applicationVariants != null
        android.applicationVariants.all { ApplicationVariant variant ->
            if (isAssertionsEnabled(variant.buildType)) {
                assert variant.javaCompile != null
                variant.javaCompile.doLast {
                    def empower = new Empower(project)
                    empower.addClassPaths([variant.javaCompile.destinationDir])
                    empower.addClassPaths(variant.apkLibraries)

                    empower.process(variant)
                }
            }
        }

        assert android.testVariants != null
        android.testVariants.all { TestVariant testVariant ->
            assert testVariant.javaCompile != null
            testVariant.javaCompile.doLast {
                def variant = testVariant.testedVariant as ApplicationVariant
                def empower = new Empower(project)
                empower.addClassPaths([variant.javaCompile.destinationDir])
                empower.addClassPaths(variant.apkLibraries)

                empower.addClassPaths([testVariant.javaCompile.destinationDir])
                empower.addClassPaths(testVariant.apkLibraries)

                empower.process(testVariant)
            }
        }
    }

    static void prepareLibraryExtension(LibraryExtension android) {
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
            }
        }
        android.testVariants.all { TestVariant testVariant ->
            assert testVariant.javaCompile != null
            testVariant.javaCompile.doLast {
                def variant = testVariant.testedVariant as LibraryVariant
                def empower = new Empower(project)
                empower.addClassPaths(testVariant.apkLibraries)
                empower.addClassPaths([variant.javaCompile.destinationDir])

                empower.addClassPaths([testVariant.javaCompile.destinationDir])
                empower.process(testVariant)
            }
        }
    }

    static boolean isAssertionsEnabled(BuildType variant) {
        // TODO: make it customizable
        return variant.name != "release"
    }

    static void checkAndroidPlugin(Project project) {
        if (!(project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(LibraryPlugin))) {
            throw new GradleException('No android plugin detected')
        }
    }
}
