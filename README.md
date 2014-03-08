## ToDo Lite for Android

A shared todo app that shows how to use the uses the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) framework to embed a nonrelational ("NoSQL") document-oriented database in an Android app and sync it with [Couchbase Server][CBS] in "the cloud".

## Prequisites

* [Android Studio](http://developer.android.com/sdk/installing/studio.html)
* (optional) [Sync Gateway](https://github.com/couchbaselabs/sync_gateway) to use the sync feature.

## Build and run the app

* Import the project into your Android Studio by selecting `build.gradle` or `settings.gradle` from the root of the project.
* Configure Sync Gateway URL by openning `app/src/main/java/com/couchbase/todolite/Application.java` and modifying SYNC_URL variable pointing to your Sync Gateway url. You can skip this step if you do not want to use the sync feature.
* Run the app using the "play" or "debug" button.

## Configure and run Sync Gateway (Optional)

To use Sync feature, download and run [Sync Gateway](https://github.com/couchbaselabs/sync_gateway) on your desire server. A sample Sync Gateway Configuration to use with the ToDo Lite app is available at `./sync-gateway-config.json`.

## Community

If you have any comments or suggestions, please join [our mailing list](https://groups.google.com/forum/#!forum/mobile-couchbase) and let us know.

## License

Released under the Apache license, 2.0.

Copyright 2011-2014, Couchbase, Inc.
