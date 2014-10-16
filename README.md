## ToDo Lite for Android

A shared todo app that shows how to use the uses the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) framework to embed a nonrelational ("NoSQL") document-oriented database in an Android app and sync it with [Couchbase Server](http://www.couchbase.com/nosql-databases/couchbase-server) in a public or private cloud.

The app is available for download in [Google Play](https://play.google.com/store/apps/details?id=com.couchbase.todolite&hl=en).

![screenshot](http://cl.ly/image/0C2N2F1X3J2a/todolite_screenshot.png)

## Prequisites

* [Android Studio](http://developer.android.com/sdk/installing/studio.html) ([compatible version list](https://github.com/couchbase/couchbase-lite-android#building-couchbase-lite-master-branch-from-source))

## Get the code

```
$ git clone https://github.com/couchbaselabs/ToDoLite-Android.git
$ git submodule init && git submodule update
```

## Build and run the app

* Import the project into your Android Studio by selecting `build.gradle` or `settings.gradle` from the root of the project.
* Run the app using the "play" or "debug" button.

## Run the Unit Tests

See [Running Unit Tests for Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android/wiki/Running-unit-tests-for-couchbase-lite-android) for instructions.

## Point to your own Sync Gateway (Optional)

By default, the app is pointed to a [Sync Gateway](https://github.com/couchbase/sync_gateway) instance hosted by Couchbase on a demo server.  

If you want to user your own Sync Gateway instance, you can change the URL in the `SYNC_URL` variable to point to your instance.  You'll want to use the following [Sync Gateway Config](https://github.com/couchbaselabs/ToDoLite-iOS/blob/master/sync-gateway-config.json)

## Community

If you have any comments or suggestions, please join [our mailing list](https://groups.google.com/forum/#!forum/mobile-couchbase) and let us know.

## License

Released under the Apache license, 2.0.

Copyright 2011-2014, Couchbase, Inc.
