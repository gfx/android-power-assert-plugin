# Android Power Assert [![Build Status](https://travis-ci.org/gfx/android-power-assert-plugin.svg?branch=master)](https://travis-ci.org/gfx/android-power-assert-plugin)

Power Assert is a language extension which adds extra information when assertions fail. This feature has been invented in Groovy and being spread around other programming languages, and is becoming a fundamental feature for debugging and testing, especially in unit tests: you no longer need to learn a bunch of test matchers such as [assertEquals()](http://developer.android.com/reference/junit/framework/Assert.html).

This library, `android-power-assert`, is a Gradle plugin to provide power asserts to Android by editing Java class files in compilation phases. To use power assert, all you have to do is to depend on `android-power-assert-plugin` and apply `android-power-assert` plugin in `build.gradle`, which automatically enables `assert` statements unless you makes `release` build. Thus you can use [assert statements](http://docs.oracle.com/javase/8/docs/technotes/guides/language/assert.html) both in applications and unit tests.

# Usage

This plugin uses `assert` statements in Java by applying this plugin in `build.gradle`.

```groovy
// in the root build.gradle
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:0.10.+'
        classpath 'com.github.gfx:android-power-assert-plugin:0.10.+'
    }
}
```

```groovy
// in a build.gradle of a module
apply plugin: 'android'
apply plugin: 'android-power-assert'
```

Then, use `assert` in your program:

```java
// in example's MainActivity.java
void onButton2Click() {
    assert findViewById(android.R.id.list).getVisibility() == View.VISIBLE;
}
```

This shows the following output when `findViewById()` returns null:

```
java.lang.NullPointerException:
25:     void onButton2Click() {
26>         assert findViewById(android.R.id.list).getVisibility() == View.VISIBLE;
26:     }
com.github.gfx.powerassert.example.MainActivity.findViewById()=<null>
(...stacktrace...)
```

# Logging

`POWERASSERT_VERBOSE=1` shows debug logs including how the plugin spends time in bytecode modification.

`POWERASSERT_VERBOSE=2` shows all the extra source code so it might be useful to debug this plugin.

# Compatibility

The minor version of this plugin should match with the minor version of Android Gradle Plugin.

 hat is, android-power-assert-plugin v0.10.x is compatible with Android Gradle Plugin v0.10.x,
and android-power-assert-plugin v0.11.x is compatible with Android Gradle Plugin v.11.x.

# Author And License

Copyright 2014, FUJI Goro (gfx) <gfuji@cpan.org>. All rights reserved.

This library may be copied only under the terms of the Apache License 2.0, which may be found in the distribution.

# See Also

- [Groovy 1.7 Power Assert (Posted on December 11, 2009)](http://dontmindthelanguage.wordpress.com/2009/12/11/groovy-1-7-power-assert/)
- [Power Assert in JavaScript](https://github.com/twada/power-assert)
