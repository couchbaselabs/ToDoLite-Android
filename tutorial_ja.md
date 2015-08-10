# Couchbase Connect Mobile Workshop Android

このワークショップでは、Couchbase LiteをSync Gatewayと組み合わせて、ゲストアカウントモードで、オフラインファーストの特性を持ち、様々なToDoリストをSync Gatewayへと同期する、ToDoアプリの開発方法を学びます。

このドキュメントはアプリケーション開発手順を解説し、Couchbase Mobileを利用して素晴らしいルックアンドフィールを持つアプリを開発する際のティップスや、つまずきやすい点についても解説していきます。

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/Android/Screen%20Shot%202015-07-27%20at%203.45.16%20PM.png)

## Couchbase Liteの詳細なプレゼンテーション

プレゼンテーションスライドは[こちら](http://www.slideshare.net/Couchbase/mobile-workshop-couchbase-lite-indepth)にあります。

## 90 minutes: Couchbase Liteハンズオン

### 環境設定

`ToDoLite-Android`リポジトリをGitHubからクローンし、サブモジュールをインストールします:

    $ git clone https://github.com/couchbaselabs/ToDoLite-Android.git
    $ cd ToDoLite-Android
    $ git submodule init && git submodule update

上記の手順では、プロジェクトで必要な、`couchbase-lite-android` と `couchbase-lite-java-core`サブモジュールも追加されます。コンソール上では以下のように表示されているでしょう:

![](http://cl.ly/bTt7/git%20submodule%20git%20init.png)

そして、ToDoLite-Androidフォルダには以下のファイルが格納されています:

![](http://cl.ly/bUlx/To-Do-Lite%20Android%20folder%20content.png)

Android Studioを起動し、`File\>New\>Import Project`メニューを選択します

![](http://cl.ly/bSYg/file_import_project.png)

ToDoLite-Androidフォルダを見つけて、プロジェクトをインポートします:

![](http://cl.ly/bTxP/import%20project.png)

アプリを実行し、ToDoLite Androidアプリ内でCouchbase Liteが動作していることを確認しましょう。アプリをビルドするには緑のボタンをクリックします。

![](http://cl.ly/bRmh/Build%20ToDo-Lite.png)

アプリを実行するデバイスを選択するか、Androidエミュレータを利用します:

![](http://cl.ly/bUZe/Run%20Android%20App.png)

完了すると、以下のようなモバイルアプリが起動します:

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/ToDoLite.png)

このアプリケーションには、リストを表示する**drawer**、特定のリスト内のタスクを表示する**main screen**、リストを他のユーザとシェアするための**share screen**の、3つの画面があります。

![](http://i.gyazo.com/a5d4774bdc4ed02afe77f3841be5db18.gif)

このワークショップの開始点は`workshop/start`にあります:

```bash
$ git checkout origin/workshop/start
```

ソースコードでは、不足しているコードをコメントで探すことができます。例えば:

        // WORKSHOP STEP 1: missing method to save a new List doc

### Introduction

Couchbase Mobileの基本となるトピックを以下に記載します。 これらのオブジェクトと利用用途を理解できたら、このチュートリアルを実施後に、非常に有用な知識が身に付いているでしょう。

- [Document](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/document/index.html): Couchbaseデータベース内に保存されるプライマリなエンティティ。
- [Revision](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/revision/index.html): ドキュメントに変更を加えると、新規revisionが作成される。
- [View](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/view/index.html): データベース内のドキュメントに対する永続的なインデックス、これを利用してクエリを実行しデータを探す。
- [Query](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/query/index.html): Viewインデックスから結果をルックアップするアクション。
- [Attachment](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/attachment/index.html): ドキュメントのJSONオブジェクトの一部としてではなく、ドキュメントに関連するデータを保存する。

このチュートリアルでは、物事が期待するように動作しているかを確認するために、LogCatのログを利用します。`ToDoLite`タグと、`com.couchbase.todolite`パッケージ名でログをフィルタリングできます。新規のフィルタ設定を作成しましょう。

![](http://i.gyazo.com/daf65b5f80afe626877348635aefcead.gif)

### ToDoLiteデータモデル

ToDoLiteには、3種類のドキュメントがあります: **profile**、 **list**、 **task**。
Taskドキュメントは所属するListへの参照を保持し、Listは所有者(owner)とメンバの配列プロパティを保持します。

![](http://f.cl.ly/items/0r2I3p2C0I041G3P0C0C/Model.png)

### ステップ 1: データベースを作成する

**`workshop/start`ブランチで作業していることを確認してください。ターミナルで: `git checkout origin/workshop/start` を実行します。この時点でアプリはコンパイルされません。これは通常の動作で、まもなくコンパイルできるようになります。**

Android Studioで、`ToDoLite-Android/ToDoLite/src/main/java/com/couchbase/todolite/Application.java`にある、`Application.java`を開きます。

![](http://cl.ly/bVhe/application-java%20file.png)

そこにDatabase型のdatabaseというプロパティがあることを確認してください。データベースへのアクセスを取得するために、アプリケーションを通してこのプロパティを使用します。

- managerの新規インスタンスセットアップ

```java
manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
```

- `todos`という名前で新規データベースを作成します

```java
database = manager.getDatabase(DATABASE_NAME);
```

`Application.java`クラスには、`ToDoLitePreferences`型の`preferences`プロパティがあります。このクラスは、shared preferencesに情報を保存し、後から再利用するために使用します。

`onCreate()`メソッド内で、以下を追加します:

- `preferences`プロパティの`setCurrentUserId()`セッタメソッドを利用し、あなたのお名前でユーザIDを設定してください。
- Profileの`createProfile`クラスメソッドを利用し、新規Profileドキュメントを上記で選んだユーザで作成します。
- ProfileドキュメントのプロパティをConsoleにログ出力します。

アプリを起動し、ProfileドキュメントのプロパティをLogCatに出力しましょう。

![](https://i.gyazo.com/83203bf679c2d41b18f1d9e5c9e8d5a8.gif)

### ステップ 2: HashMap\<String, Object\> を利用する

このセクションでは、ドキュメントの保存方法、およびドキュメントのリビジョンについて学習します。

Couchbase Liteでは、DocumentのボディはJSONオブジェクト形式となり、データはkey/valueペアのコレクションです。値は、数値、文字列、配列、入れ子オブジェクトなど様々なデータ型を利用できます。

`List.java`を開きます:

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/document-list.png)

`createNewList`メソッド内に必要なコードを追記し、ListドキュメントをローカルのCouchbase Liteデータベースに永続化します:

- 新規のHashMap変数を以下のように作成します:

```java
Map<String, Object> properties = new HashMap<String, Object>();
```

このHashMapオブジェクトのputメソッドを呼び出し、いくつかのプロパティを保存しましょう:

- `type` » ドキュメン の型として`list`を設定します。

        ```
        properties.put("type", "list");
        ```

- `title` » メソッドに渡されたパラメータを利用します。

        ```
        properties.put("title", title);
        ```

- `created_at` » `currentTimeString` 変数を利用します。
- `members` » 空の`ArrayList`データ型を利用します。

ownerのkey/valueを追加します:

- `owner` » `userId`変数を利用します。

[database](http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/database/index.html)オブジェクトで利用可能な、`createDocument`メソッドを利用して、新規のドキュメントを作成します。

```java
Document document = database.createDocument();
```

新規ドキュメントが作成できたら、[`putProperties`](http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/document/document/index.html#savedrevision-putpropertiesmapstring-object-properties)メソッドを利用して、HashMapを渡します。このメソッドは新規のリビジョンを作成し、デバイス上のローカルデータベースにそのドキュメントを永続化します。

```java
document.putProperties(properties);
```

`createNewList`メソッドの最後で、保存したドキュメントを返すことを忘れないようにしましょう。

期待通り`createNewList`メソッドが動作するかテストしましょう。
`MainActivity.java`を開き、`createNewList`メソッドを探してください。

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/list-createnewlist.png)

onClickリスナー内で、`List.createNewList`メソッドを実行し、database、title、currentUserIdを渡します:

```
Document document = List.createNewList(application.getDatabase(), title, currentUserId);
```

最後に、ドキュメントが保存されたことを確認するためのログを出力しましょう。

アプリを実行していくつかListを作成してみましょう。UIにはまだ何も表示されませんが、上記で追加したログが出力されるはずです。
次のセクションでは、これらのドキュメントのクエリ方法を学習します。

![](https://i.gyazo.com/d974a80369ad4e2b3552fbd3bde5d441.gif)

### ステップ 3: Viewの作成

Couchbaseの[View](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/view/index.html)を利用するとドキュメントデータベース内のデータをインデクシングし、クエリすることができます。

Viewの主なコンポートは、**map関数**です。この関数はアプリの開発言語と同じ言語 - Objective-CまたはJavaなど - で記述でき、非常にフレキシブルです。ドキュメントのJSONを入力とし、任意の数のkey/valueペアをインデックス用にemit(出力)します。Viewはデータベース内のすべてのドキュメントに対しmap関数を実行し、emitされた各key/valueペアをインデックスに追加することで、keyでソートされた、完全なインデックスを生成します。

`List.java`には`queryListsInDatabase`メソッドがあります、ここに不足しているコードを追加し、Listドキュメントをインデクシングしましょう。emit関数では、Listのtitleをkeyとして、valueにはnullを出力します。

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/QueryListinDatabase.png)

map関数の実装を疑似コードで示すと次のようになります:

        var type = document.type;
        if document.type == "list"
            emit(document.title, null)

- このインスタンスの'map'関数で、ドキュメントのtitleをemitします:

```
String type = (String) document.get("type");
if (DOC_TYPE.equals(type)) {
        emitter.emit(document.get("title"), document);
}
```

### STEP 4: Viewのクエリ

クエリはViewインデックスから結果をルックアップするアクションです。Couchbase Liteでは、クエリはQueryクラスのオブジェクトです。クエリを実行するには、これを生成し、プロパティを変更して(キーの範囲や最大行数など)、実行します。
結果は[QueryEnumerator](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/query/index.html)となり、Viewインデックスの各行を表すQueryRowオブジェクトのリストを提供します。

ListドキュメントをインデクシングするViewを作成したので、クエリを実行してみましょう。
`MainActivity.java`で、クエリを実行するために不足しているコードを、`setupTodoLists`メソッドに追加しましょう:

```java
Query listQuery = List.queryListsInDatabase(application.getDatabase());
try {
    QueryEnumerator rowsEnumerator = listQuery.run();
    for (QueryRow queryRow : rowsEnumerator) {
        Document document = queryRow.getDocument();
        Log.d(TAG, (String) document.getProperty("title"));
    }
} catch (CouchbaseLiteException e) {
    e.printStackTrace();
}
```

結果をイテレートし、各Listドキュメントのtitleを出力しましょう。ステップ 1でListドキュメントを保存していれば、LogCatにそれらのtitleが出力されるはずです。

![](http://i.gyazo.com/71c39cfdc9ed1aa5c90b1521906a92ef.gif)

この時点で、ArrayAdapterやRecyclerViewAdapterに結果のenumeratorを渡し、画面にリストを表示することができます。

しかしながら、ここではもう少し先に進んで、[LiveQuery](http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/query/live-query/index.html)を利用し、リアクティブなUIを作成しましょう。

### ステップ 5: 再利用可能なViewを利用したLive Query

Couchbase LiteではLive Queryを利用できます。一度作成すると、Live Queryは継続して動作し、Viewインデックスの変更を監視し、クエリの結果が変わる際にObserverへ通知します。Live QueryはlistのようなUIコンポーネントを扱う際に非常に便利です。

`java>com.couchbase.todolite>helper`にある、`LiveQueryRecyclerAdapter.java`を開き、このファイル内のメソッド見てみましょう:

![](http://cl.ly/image/3w0m352S0k0s/Screen%20Shot%202015-05-27%20at%2021.28.06.png)

ここでは、ViewクエリをUIクラスで利用する際に繰り返し見ることになる、重要な点がいくつかあります。コンストラクタはLiveQueryを第二の引数として受け取ります。そしてViewの結果の変化に対するリスナーを登録するために、`addChangeListener`メソッドを実行します(`enumerator`も呼び出します)。Recycler Viewを再描画する必要がある際にアダプタで通知を受信できるため、非常に便利です。

次に、`ListAdapter.java`を開きます:

![](http://cl.ly/image/2b0S2E0v1F1L/Screen%20Shot%202015-05-27%20at%2021.35.30.png)

このクラスの責務はドキュメントのデータを`viewHolder`にバインドすることです。`onCreateViewHolder`では、view holderを作成しています。

Query » LiveQueryRecyclerAdapter » ListAdapterの構造が理解できたところで、このテクニックを利用し、次のセクションでは、ステップ 4で記述したクエリを表示してみましょう。

### ステップ 6: ListsAdapterを利用する

`MainActivity.java`の`setupTodoLists`メソッドに戻り、単純なクエリの代わりにLive Queryを利用するように若干修正が必要です。異なるクエリテクニックを利用するので、ステップ 4で追加したコードは削除します。Main Activityクラスには、`setupTodoLists`で利用できる、`liveQuery`プロパティがあります:

- ステップ4のクエリをliveQueryで初期化しましょう (すべてのクエリは`toLiveQuery`メソッドがあり、クエリをLive Queryに変換できます)。

```java
liveQuery = List.queryListsInDatabase(application.getDatabase()).toLiveQuery();
```

- 新規の`listAdapter`変数をListAdapter型で作成し、liveQueryオブジェクトを渡します。

```java
ListAdapter listAdapter = new ListAdapter(this, liveQuery);
```

- 行へのクリックイベントはこのクラスで処理されます、`setOnItemClickListener`を呼び出し、引数に`this`を指定します。

```java
listAdapter.setOnItemClickListener(this);
```

- `recyclerView`変数の`setAdapter`メソッドを利用し、アダプタをRecycler Viewに接続しましょう。

```
recyclerView.setAdapter(listAdapter);
```

アプリをエミュレータで起動し、ToDoリストを作成してみましょう。作成したアイテムが保存され、Drawerに表示される様子が確認できます。

![](http://i.gyazo.com/e7faa2e8a395a12bf4ce8315372f8a71.gif)

### ステップ 7: Taskドキュメントの永続化

`Task.java`を開き、`createTask`メソッドを見つけてください。ステップ1、2と同様に、ドキュメント内のプロパティのHashMapを保存する関数の中身を完成させましょう。

- 新規のHashMap変数を作成します:

```java
Map<String, Object> properties = new HashMap<String, Object>();
```

そして、以下のプロパティを追加しましょう:

- `type` » ドキュメントの型を、`task`とします。

```java
properties.put("type", DOC_TYPE);
```

- `title` » 引数のtitleを利用します。

```java
properties.put("title", title);
```

- `checked` » Taskが完了したかどうかを記録するbooleanの値です、デフォルトは`Boolean.FALSE`とします。

```java
properties.put("checked", Boolean.FALSE);
```

- `created_at` » `currentTimeString`を設定します。

```java
properties.put("created_at", currentTimeString);
```

- `list_id` » 引数のlistIdを設定します。

```java
properties.put("list_id", listId);
```

ここまでは、ステップ1と同様の有効なJSON型を追加しました。

しかし、Taskドキュメントでは画像を保持することもできます。Couchbase Liteでは、ドキュメントのすべてのバイナリプロパティはアタッチメントと呼びます。ドキュメントAPIではアタッチメントを保存することはできません。保存するには、もう少し踏み込んで、内部の['Revision' API](http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/revision/index.html)を利用する必要があります。


### ステップ 8: アタッチメントとリビジョンを利用する

Revisionを作成するにはまず、Documentを作成する必要があります:

- `createDocument`メソッドを利用して、`document`という名前で`Document`型の新しい変数を作成します。
- そして、documentの`createRevision`メソッドを実行し、`Revision`型の`revision`という新規の変数を作成します。
- プロパティのHashMapを渡して、`setUserProperties`を実行します。この流れでは、userプロパティが、`_id`と`rev`以外のすべてのプロパティを表現します。これら二つのプロパティはこの後すぐに登場するように、リビジョンを保存する上で非常に重要なものです。`setProperties`を実行すると、`_id`と`rev`は処理中に削除されます。
- 渡された画像は、`InputStream`に変換しましょう。

```java
ByteArrayOutputStream out = new ByteArrayOutputStream();
image.compress(Bitmap.CompressFormat.JPEG, 50, out);
ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
```

- revisionの`setAttachment`を利用してこれをアタッチメントとして保存します。
- `revision.save()`を実行し、画像のアタッチメントを持つ新規リビジョンを作成します。

アプリを起動すると、Taskに画像を添付できるようになります:

![](https://i.gyazo.com/2aa53f81b4bc724eed43d9dbf1d14480.gif)

## Sync Gateway in-depth Presentation

The goal is to add the sync feature to your application. You will go through the steps to install Sync Gateway and get it running with Couchbase Server.

Then, we will all attempt to connect to the same instance of Sync Gateway.

See presentation slides [here](http://www.slideshare.net/Couchbase/mobile-workshop-sync-gateway-indepth-couchbase-connect-2015).

## 30 minutes: Hands-on, Replications

### STEP 9: Replications without authentication

In `MainActivity.java`, create a new method called `startReplications` to create the push/pull replications:

- Initialize a new URL object. The string URL for this tutorial is `http://todolite-syncgateway.cluster.com`
- Initialize the pull replication with the `createPullReplication` method.
- Initialize the push replication with the `createPushReplication  ` method.
- Set the continuous property to true on both replications.
- Call the `start` method on each replication.

Finally, call the `startReplications` method in the `onCreate` method.

If you run the app, nothing is saved to the Sync Gateway. That’s because we disabled the GUEST account in the configuration file.  You can see the 401 HTTP errors in the console:

[missing gif]()

In the next section, you will add user authentication with Sync Gateway with Basic Authentication.

### STEP 10: Sync Gateway Basic Authentication

Currently, the functionality to create a user with a username/password is not implemented in ToDoLite-iOS or ToDoLite-Android.

To register users on Sync Gateway, we can use the Admin REST API `_user` endpoint. The Admin REST API is available on port `4985` and can only be accessed on the internal network that Sync Gateway is running on. That is a good use case for using an app server to proxy the request to Sync Gateway.

For this workshop, the endpoint is `/signup` on port `8080`:

        curl -vX POST -H 'Content-Type: application/json' \
                -d '{"name": "your username", "password": "your password"}' \
                http://localhost:8080/signup

You should get a 200 OK if the user was created successfully.

        * Hostname was NOT found in DNS cache
        *   Trying ::1...
        * Connected to localhost (::1) port 8080 (#0)
        > POST /signup HTTP/1.1
        > User-Agent: curl/7.37.1
        > Host: localhost:8080
        > Accept: */*
        > Content-Type: application/json
        > Content-Length: 49
        >
        * upload completely sent off: 49 out of 49 bytes
        < HTTP/1.1 200 OK
        < Content-Type: application/json
        < Date: Mon, 01 Jun 2015 21:57:32 GMT
        < Content-Length: 0
        <
        * Connection #0 to host localhost left intact

Back in the Android app in Application.java, create a new method `setupReplicationWithName` method to provide the username and password:

- this time use the Authenticator class to create an authenticator of type basic auth passing in the name and password
- wire up the authenticator to the replications using the `setAuthenticator` method
- call the refactored method in `onCreate`

Notice in LogCat that the documents are now syncing to Sync Gateway.


The solution is on the `workshop/replication_basic_auth` branch.

## Data orchestration with Sync Gateway

So far, you have learned how to use the Replication and Authenticator classes to authenticate as a user with Sync Gateway. The last component we will discuss is the Sync Function. This is part of Sync Gateway’s configuration file and defines the access rules for users.

See presentation slides [here](http://www.slideshare.net/Couchbase/mobile-workshop-data-orchestration).

## 30 minutes: Hands-on, Data orchestration

### STEP 11: The Share View

As we saw in the presentation, a List document is mapped to a channel to which the Tasks are also added. The List document has a `members` property of type ArrayList holding the ids of the users to share the list with.

All Profile documents are mapped to the `profiles` channel and all users have access to it.

That way, we can display all the user Profiles and let the user pick who to share the List with. Remember earlier we used a Recycler View to display a Query result. This time, we will use the ListView api.

Similarly to the LiveQuery for the RecyclerView, the `LiveQueryAdapter.java` serves as the glue between the LiveQuery change events and the ListView API to redraw the results.

![](http://cl.ly/image/2W3F001H2C3Q/Screen%20Shot%202015-05-27%20at%2023.29.26.png)

The UserAdapter class inherits from this class. In the `onCreate` method of the ShareActivity:

- Create a new variable called `query` of type Query and the `getQuery`.
- The `getQuery` takes the database and user id as parameters.
- Initialize the `mAdapter` property passing in the live query.
- Wire up the adapter to the ListView.

The `UserAdapter` is an inner class to serve as the adapter to populate the ListView. But the `getView` method is missing some code to bind the data to the item view.

Where the code is missing add the following:
- Initialize a new `user` variable of type `Document` using the `getItem` method.
- Set the text property on the `textView` to the `name` property of the document.

The `mCurrentList` property of type document refers to the List Document that was selected, check if the user id is in the array. If it’s the case then set the checked property of `checkBox` to true.


The solution is on the `populating_list_items` branch.

### STEP 12: Sharing a List

Now we will use a click listener on the `checkBox` object to toggle the data and update the UI.

Setup the listener class inline and call the `List.addMemberToList` and `List.removeMemberFromList` accordingly.

Both methods update the List document according to wether it should add or remove the User from the members array property.

Next time a push replication (or immediately if it’s continuous) occurs, Sync Gateway will update the access to this List channel to reflect the change in the data model.

The solution is on the `workshop/final` branch.

### Testing the final result

Run the app, you can now see the different users from the `profiles` channel and share lists with other attendees.

![][image-20]

The result is on the `workshop/final` branch.

## Congratulations!  Couchbase Mobile now complete

Congratulations on building the main features of Couchbase Mobile with the ToDoLite app!  Now that you have a deeper understanding of Couchbase Lite and how to use the sync features with Sync Gateway, you can start using the SDKs in your own mobile apps.  Hope to see Couchbase Mobile with your apps on Google Play store soon!
