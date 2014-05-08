# Android Power Assert [![wercker status](https://app.wercker.com/status/b5ff7f4f8ec53e3bc8bed5d6435dc511/s/ "wercker status")](https://app.wercker.com/project/bykey/b5ff7f4f8ec53e3bc8bed5d6435dc511)

Power Assert, invented in Groovy and being spread around other proramming languages, is becomming a fundamental feature for debugging and testing, especially in unit tests: you no longer need to learn a bunch of test machers such as `assertEquals()`.

# Usage

**This library is not yet available in Maven Central.**

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.github.gfx:android-power-assert-plugin:0.10.+'
    }
}

apply plugin: 'android-power-assert'
```

# Author And License

Copyright 2014, FUJI Goro (gfx) <gfuji@cpan.org>. All rights reserved.

This library may be copied only under the terms of the Apache License 2.0, which may be found in the distribution.

# See Also

- [Groovy 1.7 Power Assert (Posted on December 11, 2009)](http://dontmindthelanguage.wordpress.com/2009/12/11/groovy-1-7-power-assert/)
- [Power Assert in JavaScript](https://github.com/twada/power-assert)
