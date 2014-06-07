# The revision history of android-power-assert-plugin

## v0.11.0 - 2014-06-08 08:01:42+0900

* Build with Android Gradle Plugin 0.11.+
* No code change from v0.10.8

## v0.10.8 - 2014-06-08 07:51:49+0900

* This is the last version that supports Android Gradle Plugin 0.10.+
* Fix build errors by specifying the minor version of Android Gradle Plugin
* No code change from v0.10.7

## v0.10.7 - 2014-06-03 08:06:13+0900

* Fix a compilation error in abstract methods

## v0.10.6 - 2014-05-22 22:46:02+0900

* Fix a runtime NPE in injected code (#8)
* Fix a compilation error in library projects (#9)

## v0.10.5 - 2014-05-22 08:26:40+0900

* Fix compilation errors

## v0.10.4 - 2014-05-18 20:11:17+0900

* No feature changes
* Revise logging; POWERASSERT_VERBOSE=1 is recommended in CI
  in order to show how this plugin spend time

## v0.10.3 - 2014-05-13 09:11:19+0900

* Fix a lot of compilation problems

## v0.10.2 - 2014-05-11 23:01:17+0900

* Embed source lines in assertion messages
* Support `android-library` modules

## v0.10.1 - 2014-05-10 18:10:29+0900

* Fix IllegalAccessError in androidTest

## v0.10.0 - 2014-05-10 12:00:00+0900

* Initial Release
