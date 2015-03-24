## ToDo Lite for Android

[![Join the chat at https://gitter.im/couchbase/couchbase-lite-java-core](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/couchbase/couchbase-lite-java-core?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A shared todo app that shows how to use the uses the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) framework to embed a nonrelational ("NoSQL") document-oriented database in an Android app and sync it with [Couchbase Server](http://www.couchbase.com/nosql-databases/couchbase-server) in a public or private cloud.

The app is available for download in [Google Play](https://play.google.com/store/apps/details?id=com.couchbase.todolite&hl=en).

![screenshot](http://cl.ly/image/0C2N2F1X3J2a/todolite_screenshot.png)

## Prequisites

* [Android Studio](http://developer.android.com/sdk/installing/studio.html) ([compatible version list](https://github.com/couchbase/couchbase-lite-android#building-couchbase-lite-master-branch-from-source))

## Get the code

```
$ git clone https://github.com/couchbaselabs/ToDoLite-Android.git
$ cd ToDoLite-Android
$ git submodule init && git submodule update
```

## Build and run the app

* Import the project into your Android Studio by selecting `build.gradle` or `settings.gradle` from the root of the project.
* Run the app using the "play" or "debug" button.

## Run the Unit Tests

See [Running Unit Tests for Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android/wiki/Running-unit-tests-for-couchbase-lite-android) for instructions.

## Point to your own Sync Gateway (Optional)

There are three build variants: `debug`, `dev`, `release`.

You can change the build variant in the Build Variants tab in Android Studio:

![img](http://f.cl.ly/items/3q413k063O061I2g2j1x/Screen%20Shot%202015-03-24%20at%2022.02.11.png)

The `debug` and `release` variants point to a [Sync Gateway](https://github.com/couchbase/sync_gateway) instance hosted by Couchbase on a demo server. 

If you want to use your own Sync Gateway instance, you can select the `dev` build variant and make sure to have `sync_url_http` and `sync_url_https` set in `local.properties` in the **root directory** of the project. For example, if you're running the app on the android emulator it would be:

```
sync_url_http=http://10.0.2.2:4984/todos/
sync_url_https=https://10.0.2.2:4984/todos/
```

If you're running the app on a `Genymotion` emulator, the IP address would be `10.0.3.2`. Note the `local.properties` file is not committed to git. You can now use the `dev` build variant for developing with SyncGateway running locally and your emulator of choice.

You'll want to use the following [Sync Gateway Config](https://github.com/couchbaselabs/ToDoLite-iOS/blob/master/sync-gateway-config.json)

## Community

If you have any comments or suggestions, please join [our mailing list](https://groups.google.com/forum/#!forum/mobile-couchbase) and let us know.

## License

Released under the Apache license, 2.0.

Copyright 2011-2014, Couchbase, Inc.
