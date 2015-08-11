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

## Sync Gatewayの詳細なプレゼンテーション

このセクションのゴールは、アプリケーションに同期機能を追加することです。Sync Gatewayのインストールを行います。ワークショップをグループで実施している場合、Sync GatewayとCouchbase Serverのデモインスタンスがすでにクラウド上で稼働していて、接続できる場合もあります。

プレゼンテーションスライドは[こちら](http://www.slideshare.net/Couchbase/mobile-workshop-sync-gateway-indepth-couchbase-connect-2015)にあります。

### ローカル環境にSync Gatewayをインストールする

Sync Gatewayをダウンロードし、ファイルをunzipします:

> http://www.couchbase.com/nosql-databases/downloads#Couchbase\_Mobile

Sync Gatwayのバイナリは**bin**フォルダにあり、サンプルの設定ファイルは**examples**フォルダにあります。このワークショップでは、このリポジトリのルートディレクトリにある、`sync-gateway-config.json`という設定ファイルを使用します。

上記の設定ファイルを指定して、Sync Gatewayを起動します:

        $ ~/Downloads/couchbase-sync-gateway/bin/sync_gateway ./sync-gateway-config.json

管理用ダッシュボードを開き、Sync Gatewayに保存されたドキュメントをモニタリングします。

        http://localhost:4985/_admin/

次のセクションでは、アプリケーション内部のローカルデータベースと、Sync Gateway間でドキュメントのpushおよびpullを行う同期のコードを記述します。

## 30分: ハンズオン、レプリケーション

### ステップ 9: 認証なしのレプリケーション

`MainActivity.java`内で、`setupReplications`という新規メソッドを作成し、push/pullレプリケーションを作成します:

- 新規のURLオブジェクトを作成します。稼働しているSync GatewayのURL文字列は、インストラクタから提供されたデモインスタンスか、お使いのマシン上で稼働している、`http://localhost:4984/todos/`で接続可能なローカルインスタンスとします (**注:** Androidのデフォルトエミュレータでアプリを起動している場合、ホスト名は`10.0.2.2`となり、Genymotionエミュレータの場合は`10.0.3.2`となります)。
- `createPullReplication`メソッドを利用し、pullレプリケーションを作成します。
- `createPushReplication`メソッドを利用し、pushレプリケーションを作成します。
- 両レプリケーションのcontinuousプロパティにtrueを設定します。
- 各レプリケーションの`start`メソッドを実行します。

最後に、`onCreate`メソッドから、`setupReplications`メソッドを呼び出します。

アプリを起動する前に、`com.couchbase.lite`パッケージと`Sync`タグにマッチするLogCatフィルタを追加しましょう。`Sync`タグはCouchbase Liteフレームワークがログに出力する、レプリケーションのフェーズに関連する様々なイベントを特定できます。

![](http://cl.ly/image/3w0f320i3Z0W/Screen%20Shot%202015-08-11%20at%2003.02.51.png)

アプリを起動しても、何もSync Gatewayには保存されません。なぜなら、GUESTアカウントを設定ファイルで無効にしているからです。コンソールには401 HTTPエラーが表示されます:

![](https://i.gyazo.com/c12de08a54472ed537d4c36f0da84fbc.gif)

次のセクションでは、Sync GatewayでのBasic認証を利用したユーザ認証を追加します。

### ステップ 10: Sync Gatewayのベーシック認証

Sync Gateway APIを利用すると、ユーザがアクセス可能なデータをレプリケートできるように、クライアント側でユーザの認証が可能です。ベーシック認証を利用する場合、そのユーザは事前にSync Gatewayデータベースに存在する必要があります。ユーザを作成するには二つの方法があります:

- 設定ファイル内の`users`フィールド配下。
- Admin REST APIの利用。

ステップ 1で選択したものと同じユーザIDでユーザを作成しましょう:

```bash
curl -vX POST -H 'Content-Type: application/json' \
                -d '{"name": "user id from step 1", "password": "your password"}' \
                http://localhost:4985/todos/_user/
```

ここで、リクエストが`4985` (管理用ポート)に送信されていることに注意しましょう、このポートはSync Gatewayが稼働している場所と同一の内部ネットワークからしかアクセスできません。ローカルで独自のSGインスタンスを起動しているなら、このリクエストは成功し、レスポンスは`201 Created`となるはずです:

```
* Connected to localhost (127.0.0.1) port 4985
> POST /todos/_user/ HTTP/1.1
> Host: localhost:4985
> User-Agent: curl/7.43.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 40
>
* upload completely sent off: 40 out of 40 bytes
< HTTP/1.1 201 Created
< Server: Couchbase Sync Gateway/1.1.0
< Date: Tue, 11 Aug 2015 00:27:55 GMT
< Content-Length: 0
< Content-Type: text/plain; charset=utf-8
```

もしアクセスできないインスタンスに接続している場合、インストラクタにアプリのサーバが接続できるように追加されていることを確認してください、あるいはこのcurlリクエストをSync Gatewayを起動しているマシンから実行してください。

Androidアプリに戻り、`Application.java`に新規メソッド、`setupReplicationsWithName`を作成し、ユーザ名とパスワードを指定します。

- 今度はAuthenticatorクラスを利用します、ユーザ名とパスワードを渡して、ベーシック認証のauthenticatorを作成しましょう。
- `setAuthenticator`メソッドを利用してレプリケーションにauthenticatorを設定します。
- `onCreate`メソッドから、`setupReplications`の代わりにこのメソッドを実行します。

アプリを起動する前に、`Sync`タグでログが有効になっているか確認してください。`Application.java`の`initDatabase`メソッドで、次の行が記述されていればログを有効にできます:

```java
Manager.enableLogging("Sync", Log.VERBOSE);
```

アプリを起動すると、LogCatにレプリケーション関連の出力が表示されるはずです。`START`、`RUNNING`、 `WAITING_FOR_CHANGES`、 `IDLE`といった様々なレプリケーションステータスが表示されます。期待通り動作している証拠です。

ローカルでSync Gatewayを稼働させている場合、`http://localhost:4985/_admin/`でアクセスできる管理UIから、ドキュメントを確認できるでしょう。

## Sync Gateway でのデータオーケストレーション

ここまでで、ReplicationとAuthenticatorクラスを利用してSync Gatewayでユーザを認証する方法を学んできました。最後に扱うコンポーネントはSync Functionです。これはSync Gatewayの設定ファイルの一部であり、ユーザのアクセスルールを定義します。

プレゼンテーションスライドは[こちら](http://www.slideshare.net/Couchbase/mobile-workshop-data-orchestration)にあります。

## 30分: データオーケストレーションのハンズオン

### ステップ 11: Share画面

プレゼンテーションで説明したように、Listドキュメントはチャネルにマッピングされ、ここにはTaskも追加されます。ListドキュメントはArrayList型の`members`プロパティを持っていて、Listを共有するユーザのIDを保持しています。

すべての`Profile`ドキュメントは`profiles`チャネルにマッピングされ、すべてのユーザがアクセスできます。

こうすることで、すべてのユーザプロファイルを表示し、ユーザがListを共有するユーザを選択できるようにしています。以前Recycler Viewを利用してQueryの結果を表示したことを思い出してください。今回は、ListView APIを利用します。

RecyclerViewでのLiveQueryと同様に、`LiveQueryAdapter.java`はLiveQueryの変更イベントとListView APIが結果を再描画するための接着剤として動作します。

![](http://cl.ly/image/2W3F001H2C3Q/Screen%20Shot%202015-05-27%20at%2023.29.26.png)

UserAdapterクラスはこのクラスを継承しています。`ShareActivity`の`onCreate`メソッド内で:

- `query`という名の新規変数をQuery型で作成し、`Profile`の`getQuery`クラスメソッドを実行します。
- `getQuery`メソッドではdatabaseとユーザIDを引数に指定します。
- `mAdapter`プロパティにLive Queryを設定します。
- atapterをListViewに設定します。

`UserAdapter`はListViewを生成するためのアダプタとして動作する内部クラスです。しかし、`getView`メソッドにはitemビューにデータをバインドするためのコードが足りません。

以下のように不足しているコードを追加します:

- `getItem`メソッドを利用して、新規の変数、`user`を`Document`型で作成します。
- `textView`のtextプロパティにこのドキュメントの`name`プロパティを設定します。

Document型の`mCurrentList`プロパティは選択されたList Documentへの参照です、ユーザIDが配列内に存在するかチェックしましょう。存在する場合、`checkBox`のcheckedプロパティをtrueに設定しましょう。

![](https://i.gyazo.com/a183dc056044784e7ae7589470cd8212.gif)

### ステップ 12: Listを共有する

`checkBox`オブジェクトのクリックリスナーを利用して、データをトグルし、UIを更新します。

インラインでリスナークラスをセットアップし、`List.addMemberToList`と`List.removeMemberFromList`を適切に呼び出してください。

どちらのメソッドもmembers配列プロパティへユーザを追加したり、削除するかに応じて、Listドキュメントを更新します。

次回pushレプリケーションが実行された際(もしくはcontinuousの場合直ちに)、Sync GatewayはこのListチャネルへのアクセスを更新し、データモデル内の変更を反映します。

### 最終結果のテスト

アプリを起動すると、`profiles`チャネルから他のユーザが参照でき、他の参加者とリストを共有できるようになります。以下のイメージの様に、ログインしているユーザの名前がナビゲーションドロワーに表示されます:

![](http://cl.ly/image/2m0H0U36252I/Screen%20Shot%202015-08-11%20at%2002.57.59.png)

![](https://i.gyazo.com/afed584972cd8ca002f7ceaa88f81c15.gif)

最終結果は`workshop/final`ブランチにあります。

## おめでとうございます、Couchbase Mobileが完了しました!

おめでとうございます、これでToDoLiteのメイン機能が実装できました! Couchbase Liteと、Sync Gatewayのsync機能の利用方法への理解が深まったと思います、是非、アプリ開発にSDKをご利用ください。Google Playストア上でCouchbase Mobileを利用したあなたのアプリがリリースされる日を楽しみにしています!
