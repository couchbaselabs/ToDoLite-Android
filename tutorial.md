# Couchbase Connect Mobile Workshop Android

In this workshop, you will learn how to use Couchbase Lite along with Sync Gateway to build a ToDos app with a guest account mode, offline-first capabilities and syncing different ToDo lists to Sync Gateway.

This paper will guide you through the steps to build the application and know all the tips and tricks to building apps with a great look and feel using Couchbase Mobile for your Android project.

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/Android/Screen%20Shot%202015-07-27%20at%203.45.16%20PM.png)

## Couchbase Lite in-depth Presentation

See presentation slides [here](http://www.slideshare.net/Couchbase/mobile-workshop-couchbase-lite-indepth).

## 90 minutes: Hands on Couchbase Lite

### Getting started

Clone the `ToDoLite-Android` repository from GitHub and install the submodules:

    $ git clone https://github.com/couchbaselabs/ToDoLite-Android.git
    $ cd ToDoLite-Android
    $ git submodule init && git submodule update

In the step above, you also added the `couchbase-lite-android` and `couchbase-lite-java-core` submodules required for the project and now you are ready to begin. You should now be at the step below and see something like:

![](http://cl.ly/bTt7/git%20submodule%20git%20init.png)

And the ToDoLite-Android folder now will contain the files below:

![](http://cl.ly/bUlx/To-Do-Lite%20Android%20folder%20content.png)

Open Android Studio and select the menu `File\>New\>Import Project` 

![](http://cl.ly/bSYg/file_import_project.png)

Locate the ToDoLite-Android folder and import the project:  

![](http://cl.ly/bTxP/import%20project.png)

Run the app now to see Couchbase Lite working within the ToDoLite Android app.  Click on the green button to build the app.  

![](http://cl.ly/bRmh/Build%20ToDo-Lite.png)

Choose to launch the app on the device or using the Android emulator:

![](http://cl.ly/bUZe/Run%20Android%20App.png)

When complete, you will have a mobile app that looks like below:

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/ToDoLite.png)

This application has three screens, the **drawer** to display the lists, the **main screen** to display the tasks in a particular list and finally the **share screen** to share a list with other users.

![](http://i.gyazo.com/a5d4774bdc4ed02afe77f3841be5db18.gif)

The starting point of the workshop is located on the `workshop/start` branch:

```bash
$ git checkout origin/workshop/start
```

In the source code, you will find comments to help locate where the missing code is meant to go. For example:

	// WORKSHOP STEP 1: missing method to save a new List doc

### Introduction

The topics below are fundamental aspects for Couchbase Mobile. If you understand all of them and their purposes, you will be in a very good position after reading this tutorial.

- [Document](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/document/index.html): the primary entity stored in a database for Couchbase.
- [Revision](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/revision/index.html): with every change to a document, we get a new document revision.
- [View](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/view/index.html): persistent index of documents in a database, which you then query to find data.
- [Query](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/query/index.html): the action of looking up results from a View’s index.
- [Attachment](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/attachment/index.html): stores data associated with a document, but are not part of the document’s JSON object.

Throughout this tutorial, we will refer to the logs in LogCat to check everything is working as expected. You can filter logs on the `ToDoLite` Tag name and `com.couchbase.todolite` package name. Create a new Filter Configuration.

![](http://i.gyazo.com/daf65b5f80afe626877348635aefcead.gif)

### ToDoLite Data Model

In ToDoLite, there are 3 types of documents: **profile**, **list** and **task**. 
The task document holds a reference to the list it belongs to and a list has an owner and a members array property.

![](http://f.cl.ly/items/0r2I3p2C0I041G3P0C0C/Model.png)

### STEP 1: Create a database

**Make sure you are on the `workshop/start` branch. In Terminal, type: `git checkout origin/workshop/start`. Right now the app is not compiling. This is normal and you will fix that shortly.**

Within Android Studio, open `Application.java` under `ToDoLite-Android/ToDoLite/src/main/java/com/couchbase/todolite/Application.java`

![](http://cl.ly/bVhe/application-java%20file.png)

Notice there is a property called database of type Database. We will use this property throughout the application to get access to our database.

- Set up a new instance of the manager

```java
manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
```
- Create a new database called name `todos`

```java
database = manager.getDatabase(DATABASE_NAME);
```

The `Application.java` class has a `preferences` property of type `ToDoLitePreferences`. This class is used to store information to the shared preferences that you will reuse later.

In the `onCreate()` method, add the following:

- Set the user id to your name with the `setCurrentUserId()` setter method on the `preferences` property.
- Use the Profile’s `createProfile` class method to create a new Profile document with the user you chose above.
- Log the properties of the Profile document to the Console

Launch the app and log the properties of the Profile document to LogCat.

![](https://i.gyazo.com/83203bf679c2d41b18f1d9e5c9e8d5a8.gif)

### STEP 2: Working with HashMap\<String, Object\>

In this section you will learn how to save documents and consequently the document revisions as well.

In Couchbase Lite, a Document’s body takes the form of a JSON object where the data is a collection of key/value pairs. The values can be different types of data such as numbers, strings, arrays or even nested objects.

Open `List.java`:

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/document-list.png)

Add the necessary code within the `createNewList` method to persist a List document to the local Couchbase Lite database:

- Instantiate a new HashMap variable by:

```java
Map<String, Object> properties = new HashMap<String, Object>();
```

Now you can save a few properties by calling the put method on our HashMap object:

- `type` » the document type `list`

	```
	properties.put("type", "list");
	```

- `title` » parameter that’s passed to the function.

	```
	properties.put("title", title);
	```

- `created_at` » the `currentTimeString` variable.
- `members` » an empty `ArrayList` data type.

Add the owner key and value: 

- `owner` » the `userId` variable.

Create a new document using the `createDocument` method available on the [database](http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/database/index.html) object.

```java
Document document = database.createDocument();
```

With a new document created, use the [`putProperties`](http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/document/document/index.html#savedrevision-putpropertiesmapstring-object-properties) method to pass in the HashMap. This method creates a new revision and persists the document to the local database on the device.

```java
document.putProperties(properties);
```

Do not forget to return the saved document at the end of the `createNewList` method.

Now let us test if the `createNewList` method is working as expected. 
Open `MainActivity.java` and navigate to the `createNewList` method. 

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/list-createnewlist.png)

Within the onClick listener, call the `List.createNewList` method and pass in the database, title and currentUserId:

```
Document document = List.createNewList(application.getDatabase(), title, currentUserId);
```

Finally, add a log statement to check that the document was saved.

Run the app and create a couple of lists. Nothing will display in the UI just yet but you see the Log statement you added above. 
In the next section, we will learn how to query those documents.

![](https://i.gyazo.com/d974a80369ad4e2b3552fbd3bde5d441.gif)

### STEP 3: Creating Views

Couchbase [Views](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/view/index.html) enable indexing and querying of data within our document database.

The main component of a view is its **map function**. This function is written in the same native language as your mobile app which is most likely in Objective-C or Java and therefore it is very flexible. The map function takes a document's JSON as input and emits (outputs) any number of key/value pairs to be indexed. The view generates a complete index by calling the map function on every document in the database and adding each emitted key/value pair to the index, sorted then by the key itself.

You will find the `queryListsInDatabase` method in `List.java` and the objective is to add the missing code to index the List documents. The emit function will emit the List title as key and null as the value.

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/QueryListinDatabase.png)

In pseudo code, the map function will look like:

	var type = document.type;
	if document.type == "list"
	    emit(document.title, null)
	    
- Emit the title of the document within the 'map' function of the instance:

```
String type = (String) document.get("type");
if (DOC_TYPE.equals(type)) {
	emitter.emit(document.get("title"), document);
}
```

### STEP 4: Query Views

A query is the action of looking up results from a view's index. In Couchbase Lite, queries are objects of the Query class. To perform a query you create a Query instance, customize its properties (such as the key range or the maximum number of rows) and then run it. 
The result is a [QueryEnumerator](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/query/index.html), which provides a list of QueryRow objects where each one describes one row from the view's index.

Now that you have created the view to index List documents, you can query them accordingly. 
In `MainActivity.java`, add the missing code to the `setupTodoLists` method to run the query:
```
        Query query = List.queryListsInDatabase(application.getDatabase());
        try {
            QueryEnumerator qe = query.run();
            Iterator<QueryRow> queryIterator = qe.iterator();
            while (qe.hasNext()) {
                Log.d(Application.TAG, qe.next().toString());
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
```

Iterate on the result and print the title of every List document. If you saved List documents in Step 1, you should now see the titles in the LogCat.

![](http://i.gyazo.com/71c39cfdc9ed1aa5c90b1521906a92ef.gif)

The solution is on the `workshop/query_views` branch.

At this point, we could pass the result enumerator to an ArrayAdapter or RecyclerViewAdapter to display the lists on screen. 

However, we will jump slightly ahead of ourselves and use a [LiveQuery](http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/query/live-query/index.html) to have Reactive UI capabilities.

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/setuptodolists.png)

### STEP 5: A Recycler View meets a Live Query

Couchbase Lite provides live queries. Once created, a live query remains active and monitors for changes to the view's index thus notifying observers whenever the query results change. Live queries are very useful for driving UI components like lists.

We will use the query to populate a Recycler View with those documents. To have the UI automatically update when new documents are indexed, we will use a Live Query.

Open `LiveQueryRecyclerAdapter.java` from the 'java>com.couchbase.todolite>helper' and we will discuss the methods in this file:

![](http://cl.ly/image/3w0m352S0k0s/Screen%20Shot%202015-05-27%20at%2021.28.06.png)

There are a few things to note here that you will see over and over again when using View Queries with UI classes. The constructor takes a LiveQuery as the second parameter. We subsequently use the `addChangeListener` method to register a listener for changes to the view result (also called an `enumerator`). That is great because it means the adapter will get notified when it needs to redraw the Recycler View.

Next up, open `ListAdapter.java`:

![](http://cl.ly/image/2b0S2E0v1F1L/Screen%20Shot%202015-05-27%20at%2021.35.30.png)

The responsibility of this class is to bind the data from the document to the `viewHolder`. In particular, the `onCreateViewHolder` creates the view holder.

Now we understand the mechanics from Query » LiveQueryRecyclerAdapter » ListAdapter, we can use the technique to display the Query we wrote in Step 4 next.

### STEP 6: Using the ListsAdapter

Back in `setupTodoLists` method of `MainActivity.java`, we will need to make slight changes to accommodate for a live query instead of a simple query. Remove the snippet added in Step 4 since we are now using another Query technique.  There is a `liveQuery` property on the Main Activity class that we can use in `setupTodoLists`:

- Initialize the liveQuery with the query from Step 4 
(all queries have a `toLiveQuery` method we can use to convert the query into a Live Query).
```
liveQuery = List.queryListsInDatabase(application.getDatabase()).toLiveQuery();
```
- Create a new `listAdapter` variable of type ListAdapter and pass in the liveQuery object.
```
ListAdapter listAdapter = new ListAdapter(this, liveQuery);
```
- Click events on a row are handled by this class, use the `setOnItemClickListener` method passing in `this` as the argument.
```
listAdapter.setOnItemClickListener(this);
```
- Use the `setAdapter` method on the `recyclerView` variable to wire up the adapter to the Recycler View.
```
recyclerView.setAdapter(listAdapter);
```

Run the app on the emulator and start creating ToDo lists.  You can see the created items are now persisted and displayed in the Drawer.

![](http://i.gyazo.com/e7faa2e8a395a12bf4ce8315372f8a71.gif)

Solution is on branch `workshop/using_list_adapter`.

### STEP 7: Persist the Task document

Open `Task.java` and find the `createTask` method. Similarly to Step 1 & 2, complete the body of the function to persist the HashMap of properties in a document.

Instantiate a new HashMap variable:
```
Map<String, Object> properties = new HashMap<String, Object>();
```
Then add the following properties:
- `type` » the type of document, in this case `task`.
```
properties.put("type", DOC_TYPE);
```
- `title` » the title parameter passed in.
```
properties.put("title", title);
```
- `checked` » a boolean to track if a task has been completed, the default is `Boolean.FALSE`.
```
properties.put("checked", Boolean.FALSE);
```
- `created_at` » the `currentTimeString` variable.
```
properties.put("created_at", currentTimeString);
```
- `list_id` » the listId parameter passed in.
```
properties.put("list_id", listId);
```

So far, we have added valid JSON types similarly to Step 1. 

	//need steps on where to call it

Change the function return type to be a Document type and return a document instance. 

![](http://i.gyazo.com/68dfc680dc38813aa0c6ff144697ef4c.gif)

However, a Task document can have an image. In Couchbase Lite, all binary properties of documents are called attachments. The Document API does not allow for saving an attachment. To do so, we will have to go one step further and use the underlying ['Revision' API](http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/revision/index.html) to do so.

Solution is on branch `workshop/persist_task_document`.

### STEP 8: Working with Attachments and Revisions

To create a Revision, we must first create a Document:

- Create a new variable named `document` of type Document using the `createDocument` method.
- In turn, create a new variable named `revision` of type Revision with the document’s `createRevision` method.
- Call the `setUserProperties` passing in the properties HashMap. In this context, user properties represent any property except the `_id` and `rev`, those two properties are important to save the revision as we’ll see in a bit. If we called the `setProperties`, the `_id` and `rev` would get deleted in the process.
- If an image was passed in, use the `setAttachment` method on the revision to save it as attachment.
- Call `revision.save()` and this will create the new revision with the image attachment.

![](https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/Working%20with%20Attachments%20and%20Revisions.png)

Run the app and you should now be able to attach images to tasks:

![](http://i.gyazo.com/4b35a4bcf99bc57d3c47553b3ca973d4.gif)

The solution is on the `workshop/attachments_and_revisions` branch.

## Sync Gateway in-depth Presentation

The goal is to add the sync feature to our application. We will go through the steps to install Sync Gateway and get it running with Couchbase Server.

Then, we will all attempt to connect to the same instance of Sync Gateway running [here][3].

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

The solution is on the `workshop/replication` branch.

In the next section, you will add user authentication with Sync Gateway. You can choose to use Facebook Login or Basic Authentication for this workshop.

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