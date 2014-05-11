# Android Power Assert [![wercker status](https://app.wercker.com/status/b5ff7f4f8ec53e3bc8bed5d6435dc511/s/ "wercker status")](https://app.wercker.com/project/bykey/b5ff7f4f8ec53e3bc8bed5d6435dc511)

Power Asserts, invented in Groovy and being spread around other programming languages, are becoming a fundamental feature for debugging and testing, especially in unit tests: you no longer need to learn a bunch of test matchers such as `assertEquals()`.

This plugin is a Gradle build script to provide power asserts to Android to edit Java class files in compilation phases. No application code changes are required. All you have to do is to depend on `android-power-assert-plugin` and apply `android-power-assert` plugin in `build.gradle`, which automatically enables `assert` statements unless you makes `release` build.

# Usage

```groovy
// in the root build.gradle
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.github.gfx:android-power-assert-plugin:0.10.+'
    }
}

// in a build.gradle of a module
apply plugin: 'android-power-assert'
```

Then, use `assert` in your program:

```java
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

# Author And License

Copyright 2014, FUJI Goro (gfx) <gfuji@cpan.org>. All rights reserved.

This library may be copied only under the terms of the Apache License 2.0, which may be found in the distribution.

# See Also

- [Groovy 1.7 Power Assert (Posted on December 11, 2009)](http://dontmindthelanguage.wordpress.com/2009/12/11/groovy-1-7-power-assert/)
- [Power Assert in JavaScript](https://github.com/twada/power-assert)
