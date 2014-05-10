# Android Power Assert [![wercker status](https://app.wercker.com/status/b5ff7f4f8ec53e3bc8bed5d6435dc511/s/ "wercker status")](https://app.wercker.com/project/bykey/b5ff7f4f8ec53e3bc8bed5d6435dc511)

Power Asserts, invented in Groovy and being spread around other proramming languages, are becomming a fundamental feature for debugging and testing, especially in unit tests: you no longer need to learn a bunch of test machers such as `assertEquals()`.

This plugin is a Gradle build script to provie power asserts to Android to edit Java class files in compilation phases. No application code changes are required. All you have to do is to depend on `android-power-assert-plugin` and apply `android-power-assert` plugin in `build.gradle`, which automatically enables `assert` statements unless you makes `release` build.

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

// in a build.grdle of a module
apply plugin: 'android-power-assert'
```

```java
// in applications
assert x == 10; // it may throw AssertionError() with much useful information
```

# Author And License

Copyright 2014, FUJI Goro (gfx) <gfuji@cpan.org>. All rights reserved.

This library may be copied only under the terms of the Apache License 2.0, which may be found in the distribution.

# See Also

- [Groovy 1.7 Power Assert (Posted on December 11, 2009)](http://dontmindthelanguage.wordpress.com/2009/12/11/groovy-1-7-power-assert/)
- [Power Assert in JavaScript](https://github.com/twada/power-assert)
