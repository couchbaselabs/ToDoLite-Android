## ToDo Lite for Android

[![Join the chat at https://gitter.im/couchbase/mobile](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/couchbase/mobile?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A shared todo app that shows how to use the [Couchbase Lite Android](https://github.com/couchbase/couchbase-lite-android) framework to embed a nonrelational ("NoSQL") document-oriented database in an Android app and sync it with [Couchbase Server](http://www.couchbase.com/nosql-databases/couchbase-server) in a public or private cloud.

![screenshot](http://f.cl.ly/items/1K2e200t2D3s1l0i473e/ToDoLite.gif)

## Get the code

```
$ git clone https://github.com/couchbaselabs/ToDoLite-Android.git
$ cd ToDoLite-Android
```

## Build and run the app

* Import the project into Android Studio by selecting `build.gradle` or `settings.gradle` from the root of the project.
* Run the app using the "play" or "debug" button.

## Point to your own Sync Gateway

1. [Download Sync Gateway](http://www.couchbase.com/nosql-databases/downloads#couchbase-mobile).
2. Start Sync Gateway with the configuration file in the root of this project.

    ```bash
    ~/Downloads/couchbase-sync-gateway/bin/sync_gateway sync-gateway-config.json
    ```

3. Open **Application.java** and update the `SYNC_URL_HTTP` constant to point to your Sync Gateway instance.

    ```java
    private static final String SYNC_URL_HTTP = "http://localhost:4984/todolite";
    ```

    You can use the `adb reverse tcp:4984 tcp:4984` command to open the port access from the host to the Android emulator. This command is only available on devices running android 5.0+ (API 21).

4. Log in with your Facebook account.
5. Add lists and tasks and they should be visible on the Sync Gateway Admin UI on [http://localhost:4985/_admin/](http://localhost:4985/_admin/).

## Community

If you have any comments or suggestions, please join [our forum](https://forums.couchbase.com/c/mobile) and let us know.

## License

Released under the Apache license, 2.0.

Copyright 2011-2014, Couchbase, Inc.
