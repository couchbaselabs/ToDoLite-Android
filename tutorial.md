# Couchbase Mobile Day Workshop Android

In this workshop, you will learn how to use Couchbase Lite along with Sync Gateway to build a ToDos app with a guest account mode, offline-first capabilities and syncing different ToDo lists to Sync Gateway.

This document will guide you through the steps to build the application using Couchbase Mobile.

## Hands on Couchbase Lite

### What is Included

This tutorial is based on a demo application, ToDo Lite, designed to show the
features of Couchbase Lite.  The full demo application is on the
master branch and is updated from time to time as new features are
added to Couchbase Lite.

You may be viewing this tutorial from a browser on github or on your
local system after cloning the repository or opening a zipfile.  In
any case, you should find the following layout in the tutorial:

	CONTRIBUTING.md
	finished/
	tutorial.md
	README.md
	initial/

The CONTRIBUTING.md and README.md are a simple copy of the master
branch overview and contribution instructions.  The tutorial.md is the
file containing these instructions.  The initial/ directory contains
the basic scaffolding of ToDo Lite which you will edit to add
functionality to.  The finished/ directory contains a completed
version of the application.  You will use the completed version in a
later portion of the lab with Couchbase Sync Gateway and you may want
to refer to it if you get stuck when going through the lab steps.

### Getting started

In the getting started, you will get the project set up locally,
verify it by running the `finished` project, then switch to the
`initial` project to follow the steps which will have you learn the
Couchbase Lite API basics.

If needed, clone the application from the ToDoLite-Android repository:

	git clone https://github.com/couchbaselabs/ToDoLite-Android
	git checkout workshop/CouchbaseDay

Within Android Studio, click:  File\>New\>Import Project 
![][image-15]

Locate the ToDoLite-Android/finished folder and import the project:
![][image-16]

Run the app now to see Couchbase Lite working within the ToDoLite Android app.  Click on the green button to build the app.  
![][image-19]

Choose to launch the app on the device or using the Android emulator:
![][image-18]

When complete, you will have a mobile app that looks like below:
![][image-17]

This application has three main screens, the drawer to display the List, the Main screen to display the Tasks in a particular screen and finally the Share screen to share a List with other users.

![][image-1]

We have tried to avoid having this workshop be just a series of
cut-n-paste sections of code so you will find below a series of steps
that will help you build the application.  If you find yourself in
trouble and want to skip a step or catch up, you can look at the
completed code in the `finished/` directory.

In the source code, you will find comments to help locate where the missing code is meant to go. For example:

	// WORKSHOP STEP 1: missing method to save a new List doc

### Introduction

The topics below are the fundamental aspects of Couchbase Mobile. If
you understand all of them and their purposes, you’ll be in a very
good spot after walking through this tutorial.

- [Document][7]: the primary entity stored in a database for Couchbase.
- [Revision][8]: with every change to a document, we get a new document revision.
- [View][9]: persistent index of documents in a database, which you then query to find data.
- [Query][10]: the action of looking up results from a View’s index.
- [Attachment][11]: stores data associated with a document, but are not part of the document’s JSON object.

Throughout this tutorial, we will refer to the logs in LogCat to check everything is working as expected. You can filter logs on the `ToDoLite` Tag name and `com.couchbase.todolite` package name. We create a new Filter Configuration.

![][image-2]

### ToDoLite Data Model

In ToDoLite, there are 3 types of documents: profile, list and task. 
The task document holds a reference to the list it belongs to and a list has an owner and a members array.

![][image-3]

### STEP 1: Create a database

Make sure you have imported the project from the `initial`
directory. Right now the App is not compiling. This is normal, you
just need to initialize the Database.  You'll do this over the course
of the following steps.  We have described what code needs to be
written, occasionally including some code snippets.  Some details and
exception handling are left to you as an exercise.

Open `Application.java` under ToDoLite-Android/ToDoLite/src/main/java/com/couchbase/todolite/Application.java
![][image-20]

Notice there is a property called database of type Database. We will use this property throughout the application to get access to our database.

- set up a new instance of the manager in the `initDatabase()` method
```
manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
```
- create a new database called name `todos`
```
database = manager.getDatabase(DATABASE_NAME);
```

Then, in the `onCreate()` method, you'll want to do a few things with
the profile that is being instantiated:

- set the user id to your name
- use the Profile’s createNewProfile class method to create a new Profile document with the user you chose above
- log the properties of the Profile document to the Console

Launch the app and log the properties of the Profile document to LogCat:

You'll see the complete solution in the equivalent files in the `finished` directory.

### STEP 2: Working with HashMap\<String, Object\>

This section we will learn how to save documents and consequently the document revisions as well.

In Couchbase Lite, a document’s body takes the form of a JSON object where the it is a collection a key/value pairs.   The values can be different types of data such as numbers, strings, arrays or even nested objects.

Locate and open the `document/List.java` file over at:  
ToDoLite-Android/ToDoLite/src/main/java/com/couchbase/todolite/document/List.java  
![][image-21]

Add the necessary code within the `createNewList` method to persist a List document to a local Couchbase Lite database.  Instantiate a new HashMap variable by:
```
Map<String, Object> properties = new HashMap<String, Object>();
```
Now we can save a few properties by calling the put method on our HashMap object: 
- `type` » the document type `list`
```
properties.put("type", "list");
```
- `title` » parameter that’s passed to the function.
```
properties.put("title", title);
```
- `created_at` » the `currentTimeString` variable.
- `members` » an empty `ArrayList`.

Create a new document using the `createDocument` method available on the [database][2] object.
```
Document document = database.createDocument();
```
With a new document created, use the [`putProperties` method][3] to pass in the HashMap. This method creates a new revision and persists the document to the local database on the device.
```
document.putProperties(properties);
```

Do not forget to return the saved document at the end of the `createNewList` method.

Now let us test if the `createNewList` method is working as expected. 
Open `MainActivity.java` and navigate to the `createNewList` method. 
![][image-22]

Within the onClick listener, call the `List.createNewList` method and pass in the database, title and currentUserId:
```
Document document = List.createNewList(application.getDatabase(), title, currentUserId);
```

Finally, add a log statement to check that the document was saved.

Run the app and create a couple of lists. Nothing will display in the UI just yet but you see the Log statement you added above. 
In the next section, we will learn how to query those documents.

![][image-5]

You'll see the complete solution in the equivalent files in the `finished` directory.

### STEP 3: Creating Views

Couchbase ['Views'][4] enable indexing and querying of data within out document database.

The main component of a view is its **map function**. This function is written in the same native language as your app which most likely Objective-C or Java therefore it is very flexible. The map function takes a document's JSON as input and emits (outputs) any number of key/value pairs to be indexed. The view generates a complete index by calling the map function on every document in the database and adding each emitted key/value pair to the index, sorted by the key.

You will find the `queryListsInDatabase` method in `List.java` and the objective is to add the missing code to index the List documents. The emit function will emit the List title as key and null as the value.
![][image-23]

In pseudo code, the map function will look like:

	var type = document.type;
	if document.type == "list"
	    emit(document.title, null)

You'll see the complete solution in the equivalent files in the `finished` directory.

### STEP 4: Query Views

A query is the action of looking up results from a view's index. In Couchbase Lite, queries are objects of the Query class. To perform a query you create one of these, customize its properties (such as the key range or the maximum number of rows) and then run it. 
The result is a ['QueryEnumerator'][5], which provides a list of QueryRow objects where each one describes one row from the view's index.

Now that you have created the view to index List documents, you can query them accordinly. 
In `MainActivity.java`, add the missing code to the `setupTodoLists` method to run the query.
![][image-24]


Iterate on the result and print the title of every List document. If you saved List documents in Step 1, you should now see the titles in the LogCat.

![][image-6]

You'll see the complete solution in the equivalent files in the `finished` directory.

At this point, we could pass the result enumerator to an ArrayAdapter or RecyclerViewAdapter to display the lists on screen. 

However, we will jump slightly ahead of ourselves and use a ['LiveQuery'][6] to have Reactive UI capabilities.

### STEP 5: A Recycler View meets a Live Query

Couchbase Lite provides live queries. Once created, a live query remains active and monitors for changes to the view's index thus notifying observers whenever the query results change. Live queries are very useful for driving UI components like lists.

We will use the query to populate a Recycler View with those documents. To have the UI automatically update when new documents are indexed, we will use a Live Query.

Open `LiveQueryRecyclerAdapter.java` and we will discuss the methods in this file:

![][image-7]

There are a few things to note here that you will see over and over again when using View Queries with UI classes. The constructor takes a LiveQuery as the second parameter. We subsequently use the `addChangeListener` method to register a listener for changes to the view result (also called an `enumerator`). That is great because it means the adapter will get notified when it needs to redraw the Recycler View.

Next up, open `ListAdapter.java`:

![][image-8]

The responsibility of this class is to bind the data from the document to the `viewHolder`. In particular, the `onCreateViewHolder` creates the view holder.

Now we understand the mechanics from Query » LiveQueryRecyclerAdapter » ListAdapter, we can use the technique to display the Query we wrote in Step 4.

### STEP 6: Using the ListsAdapter

Back in `setupTodoLists` method of `MainActivity.java`, we will need to make slight changes to accommodate for a live query instead of a simple query. There is a `liveQuery` property on the Main Activity class that we can use in `setupTodoLists`:

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

![][image-9]

You'll see the complete solution in the equivalent files in the `finished` directory.

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

![][image-10]

However, a Task document can have an image. In Couchbase Lite, all binary properties of documents are called attachments. The Document API does not allow for saving an attachment. To do so, we will have to go one step further and use the underlying ['Revision' API][12] to do so.

You'll see the complete solution in the equivalent files in the `finished` directory.

### STEP 8: Working with Attachments and Revisions

To create a Revision, we must first create a Document:

- Create a new variable named `document` of type Document using the `createDocument` method.
- In turn, create a new variable name revision of type Revision with the document’s `createRevision` method.
- Call the `setUserProperties` passing in the properties HashMap. In this context, user properties represent any property except the `_id` and `rev`, those two properties are important to save the revision as we’ll see in a bit. If we called the `setProperties`, the `_id` and `rev` would get deleted in the process.
- If an image was passed in, use the `setAttachment` method on the revision to save it as attachment.
- Call `revision.save()` and this will create the new revision with the image attachment.

Run the app and you should now be able to attach images to tasks:

![][image-11]

You'll see the complete solution in the equivalent files in the `finished` directory.

## Hands On Sync Gateway

In this section, our goal is to pick up a completed version of the
ToDo Lite demonstration code and configure a Sync Gateway backed by
Couchbase Server to store the documents.  Optionally, you can run
multiple instances of ToDo Lite to see your demo application
synchronize between multipled devices.

Note: this section of the workshop is separated by letters instead of
numbers.

### STEP A: Install Sync Gateway and Couchbase Server

(Side note: if you are constrained in resources, it is possible to use
Sync Gateway alone with memory backed storage.  This is known as
"wallace" and the Sync Gateway documentation covers the details.)


#### Install from Vagrant

If you are attending a workshop in person, you may obtain a software
distribution of a Vagrant configuration and an associated `.box`
file.  With Vagrant and VirtualBox installed locally, set up Sync
Gateway by running `vagrant up` from a shell prompt in the directory
with the `Vagrantfile` and the `Couchbase-Sync_Gateway.box` files.

After running `vagrant up`, you should be able to `vagrant ssh` to
connect to the VM.  Also, you should be able to reach your
[Couchbase Web UI][couchbase-web-ui] via the browser 
at [http://10.111.72.101:8091/index.html][couchbase-web-ui].  The
username is "Administrator" and the password is "password".

#### Install from Download

To set up Couchbase Sync Gateway on your own system, get Couchbase
Server and Sync Gateway binaries from the
[Couchbase download page](http://www.couchbase.com/nosql-databases/downloads).
Follow the setup wizard from Couchbase Server to set it up and install
the Sync Gateway binary for your platform per the
[installation instructions](http://developer.couchbase.com/documentation/mobile/1.1.0/develop/guides/sync-gateway/running-sync-gateway/installing-sync-gateway/index.html).

#### STEP B: Configure Sync Gateway

In this step, we will start with a minimal Sync Gateway configuration
for our environment and run the "finished" ToDo LIte application
against it.  It will not yet actually be syncing files.

As before, there is both an 'initial' and 'finished' configuration.
You will find a file named `sync-gateway-config.json` in both the VM
and in this repository.

First, copy the initial `sync-gateway-config.json` into your local
working directory.  Then, start Sync Gateway with either the provided shell
script (VM environment) or by running the `sync_gateway` executable
with the `sync-gateway-config.json` file as an argument.

When Sync Gateway starts, you should see some basic logging output.

```
2015-11-17T07:58:01.571Z Enabling logging: [CRUD REST+ Access]
2015-11-17T07:58:01.571Z ==== Couchbase Sync Gateway/1.1.1(10;2fff9eb) ====
2015-11-17T07:58:01.571Z Configured process to allow 4096 open file descriptors
2015-11-17T07:58:01.572Z Opening db /todos as bucket "todos", pool "default", server <walrus:>
2015-11-17T07:58:01.573Z Opening Walrus database todos on <walrus:>
2015-11-17T07:58:01.573Z Using default sync function 'channel(doc.channels)' for database "todos"
2015-11-17T07:58:01.646Z WARNING: No users have been defined in the 'todos' database, which means that you will not be able to get useful data out of the sync gateway over the standard port.  FIX: define users in the configuration json or via the REST API on the admin port, and grant users to channels via the admin_channels parameter. -- rest.emitAccessRelatedWarnings() at server_context.go:576
2015-11-17T07:58:01.646Z Starting admin server on 127.0.0.1:4985
2015-11-17T07:58:01.650Z Starting server on :4984 ...
```

You will notice that it may not be listening on the IP address we want
it to.  Also, you may notice that it is currently using the "walrus:"
memory backend, but it is otherwise configured for our ToDo Lite
application.  Next, we'll want to set up the Sync Gatway REST API to
listen on all IPs on the host.

Entering `ctrl-c` will stop the process.

Then edit the JSON file.  Add a new value for _interface_ set up to
listen on `0.0.0.0`, which indicates all IPs on the system.  You will
add  `"interface": "0.0.0.0:4984",` to the JSON.

Start Sync Gateway again and you should see:
```2015-11-17T08:06:49.206Z Starting server on 0.0.0.0:4984 ...```

### STEP C: Run the Completed ToDo Lite

From Android Studio, close any projects you may currently have open.
Select "Import project" from the quick start dialog and navigate to
the `finished/` directory.  Import the project there.

After import, open `Application.java` and verify that the IP address
is correct for the Sync Gateway you will connect to (_note_: the one on the
preconfigured VM is 10.111.72.101).  

When ToDo Lite runs, you will see some logged traffic on the Sync
Gateway node:
```
2015-11-17T14:17:13.177Z HTTP auth failed for username="oliver"
2015-11-17T14:17:13.177Z HTTP:  #001: GET /todos/_session
2015-11-17T14:17:13.177Z HTTP: #001:     --> 401 Invalid login  (0.1 ms)
2015-11-17T14:17:13.178Z HTTP auth failed for username="oliver"
2015-11-17T14:17:13.178Z HTTP:  #002: GET /todos/_session
2015-11-17T14:17:13.178Z HTTP: #002:     --> 401 Invalid login  (0.1 ms)
```

Since ToDo LIte is configured for authentication but Sync Gateway
does not have the "oliver" user built in to the app, it cannot
authenticate.  Add that user using the
[Sync Gateway REST API][sg-rest-useradd].  You will have to use the
admin port to access this REST API.  For security reasons, Sync
Gateway listens on two ports.  One is intended for administrative
access and should *only* be open to trusted networks.  By default, it
will be localhost only.

Adding the user should generate these log messages:
```
2015-11-17T14:23:11.474Z HTTP:  #003: POST /todos/_user/  (ADMIN)
> POST /todos/_user/ HTTP/1.1
> User-Agent: curl/7.19.7 (x86_64-redhat-linux-gnu) libcurl/7.19.7 NSS/3.16.2.3 Basic ECC zlib/1.2.3 libidn/1.18 libssh2/1.4.2
> Host: localhost:4985
> Accept: */*
> Content-Type: application/json
> Content-Length: 41
```

Once the user is added, if you run the application again, you will
note that "oliver" authenticates successfully.

However, we are not done yet.  We still need to configure Sync Gateway
to connect to the Couchbase Server bucket and store and retrieve
documents based on the user with a sync function.

The solution to adding the user is in a shell script in the
`finished/` directory.  

### STEP D: Complete the Sync Gateway Configuration

Verify your [Couchbase Web UI][couchbase-web-ui]] has a bucket named
"todos".  If does not exist, create it.  

Now, referring to the [documentation on config.json][sg-config-json]
edit the `sync-gateway-config.json` to change the server to
"http://localhost:8091" or wherever your Couchbase Server is.  Also
change it so user 'GUEST' access is disabled.  Finally, add a sync
function:
```
function(doc, oldDoc) {
  // NOTE this function is the same across the iOS, Android, and PhoneGap versions.
  if (doc.type == "task") {
    if (!doc.list_id) {
      throw({forbidden : "items must have a list_id"})
    }
    channel("list-"+doc.list_id);
  } else if (doc.type == "list") {
    channel("list-"+doc._id);
    if (!doc.owner) {
      throw({forbidden : "list must have an owner"})
    }
    if (oldDoc) {
      var oldOwnerName = oldDoc.owner.substring(oldDoc.owner.indexOf(":")+1);
      requireUser(oldOwnerName)
    }
    var ownerName = doc.owner.substring(doc.owner.indexOf(":")+1);
    access(ownerName, "list-"+doc._id);
    if (Array.isArray(doc.members)) {
      var memberNames = [];
      for (var i = doc.members.length - 1; i >= 0; i--) {
        memberNames.push(doc.members[i].substring(doc.members[i].indexOf(":")+1))
      };
      access(memberNames, "list-"+doc._id);
    }
  } else if (doc.type == "profile") {
    channel("profiles");
    var user = doc._id.substring(doc._id.indexOf(":")+1);
    if (user !== doc.user_id) {
      throw({forbidden : "profile user_id must match docid"})
    }
    requireUser(user);
    access(user, "profiles"); // TODO this should use roles
  }
}
```

Read through that sync function.  You'll notice that it handles
documents differently based on the type.  For instance "task"
documents must have an owner and a list_id and the "profile" user\_id
is required to match the docid.  

Start Sync Gateway with this new configuration file.

Before we can synchronize, the user "oliver" with the password
"letmein" needs to be added as you did earlier via the Admin REST
API:
```
2015-11-17T14:56:22.145Z Access: Computed channels for "oliver": !:1
2015-11-17T14:56:22.161Z Access: Computed roles for "oliver": 
< HTTP/1.1 201 Created
< Server: Couchbase Sync Gateway/1.1.1
< Date: Tue, 17 Nov 2015 14:56:22 GMT
< Content-Length: 0
< Content-Type: text/plain; charset=utf-8
< 
* Connection #0 to host localhost left intact
* Closing connection #0
```

You will also note in the [Couchbase Server Web UI][couchbase-web-ui]
that the "todos" bucket now has some documents in it related to Sync
Gateway's own management of data.

The completed configuration is in the `finished/` directory.

### STEP E:  Run the Completed ToDo Lite Against the Server

Once again, go back to the completed ToDo Lite Application and start
it.  This time, as you interact with the app, you should see data
being automatically synchronized to and from the server:
```
2015-11-17T15:00:06.884Z HTTP:  #002: GET /todos/_session  (as oliver)
2015-11-17T15:00:06.900Z HTTP:  #003: GET /todos/_session  (as oliver)
2015-11-17T15:00:06.925Z HTTP:  #004: GET /todos/_local/417e79ad1e103bb533eb9083f975c0f8523f7c83  (as oliver)
2015-11-17T15:00:06.925Z HTTP: #004:     --> 404 missing  (1.5 ms)
2015-11-17T15:00:06.959Z HTTP:  #005: GET /todos/_local/2d306925261478d9482423011c6dbfd168fc74d1  (as oliver)
2015-11-17T15:00:06.959Z HTTP: #005:     --> 404 missing  (0.5 ms)
2015-11-17T15:00:06.990Z HTTP:  #006: POST /todos/_changes  (as oliver)
2015-11-17T15:00:07.016Z HTTP:  #007: POST /todos/_changes  (as oliver)
2015-11-17T15:00:07.462Z HTTP:  #008: POST /todos/_revs_diff  (as oliver)
2015-11-17T15:00:07.504Z HTTP:  #009: PUT /todos/56a4997f-2b85-419e-a10d-1c3712748415?new_edits=false  (as oliver)
2015-11-17T15:00:07.539Z HTTP:  #010: POST /todos/_bulk_docs  (as oliver)
2015-11-17T15:00:07.633Z CRUD: 	Doc "p:oliver" in channels "{profiles}"
```

Congratulations on building the main features of Couchbase Mobile with
the ToDoLite app!  Now that you have a deeper understanding of
Couchbase Lite and how to use the sync features with Sync Gateway, you
can start using the SDKs in your own mobile apps.  We hope to see
Couchbase Mobile with your apps on the Google Play store soon!


[1]:	https://github.com/couchbaselabs/ToDoLite-Android
[2]:	http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/database/index.html
[3]:	http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/document/document/index.html#savedrevision-putpropertiesmapstring-object-properties
[4]: 	http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/view/index.html
[5]:	http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/query/index.html
[6]:	http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/query/live-query/index.html
[7]:	http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/document/index.html
[8]:	http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/revision/index.html
[9]:	http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/view/index.html
[10]:	http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/query/index.html
[11]:	http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/native-api/attachment/index.html
[12]:   http://developer.couchbase.com/mobile/develop/references/couchbase-lite/couchbase-lite/revision/index.html


[image-1]:	http://i.gyazo.com/a5d4774bdc4ed02afe77f3841be5db18.gif
[image-2]:	http://i.gyazo.com/daf65b5f80afe626877348635aefcead.gif
[image-3]:	http://f.cl.ly/items/0r2I3p2C0I041G3P0C0C/Model.png
[image-5]:	http://i.gyazo.com/332190d6d46fb059d7f0953bb938321f.gif
[image-6]:	http://i.gyazo.com/71c39cfdc9ed1aa5c90b1521906a92ef.gif
[image-7]:	http://cl.ly/image/3w0m352S0k0s/Screen%20Shot%202015-05-27%20at%2021.28.06.png
[image-8]:	http://cl.ly/image/2b0S2E0v1F1L/Screen%20Shot%202015-05-27%20at%2021.35.30.png
[image-9]:	http://i.gyazo.com/e7faa2e8a395a12bf4ce8315372f8a71.gif
[image-10]:	http://i.gyazo.com/68dfc680dc38813aa0c6ff144697ef4c.gif
[image-11]:	http://i.gyazo.com/4b35a4bcf99bc57d3c47553b3ca973d4.gif
[image-13]: http://cl.ly/bTt7/git%20submodule%20git%20init.png
[image-14]: http://cl.ly/bUlx/To-Do-Lite%20Android%20folder%20content.png
[image-15]: http://cl.ly/bSYg/file_import_project.png
[image-16]: https://i.gyazo.com/1ef1ccc2249de175530a8b19fcd11846.png
[image-x]: http://cl.ly/bU5d/ToLite%20App%20in%20Android.png
[image-17]: https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/ToDoLite.png
[image-18]: http://cl.ly/bUZe/Run%20Android%20App.png
[image-19]: http://cl.ly/bRmh/Build%20ToDo-Lite.png
[image-21]: https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/document-list.png
[image-22]: https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/list-createnewlist.png
[image-23]: https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/QueryListinDatabase.png
[image-24]: https://dl.dropboxusercontent.com/u/5618818/Couchbase/workshop/mobile/images/setuptodolists.png
[couchbase-web-ui]: http://10.111.72.101:8091/index.html
[couchbase-sg]: http://10.111.72.101:4984/index.html
[sg-rest-useradd]: http://developer.couchbase.com/documentation/mobile/current/develop/references/sync-gateway/admin-rest-api/user/post-user/index.html
[sg-config-json]: http://developer.couchbase.com/documentation/mobile/1.1.0/develop/guides/sync-gateway/configuring-sync-gateway/config-properties/index.html
