package main

import (
    "net/http"
    "github.com/fjl/go-couchdb"
    "io/ioutil"
    "encoding/json"
)

func checkErr(err error) {
    if err != nil {
        panic(err)
    }
}

type Info struct {
    Seq int `json:"update_seq"`
}

type Profile struct {
    Type        string `json:"type"`
    DeviceToken string `json:"device_token"`
}

type List struct {
    Type    string `json:"type"`
    Owner   string `json:"owner"`
    Members []string `json:"members"`
    Rev     string `json:"_rev,omitempty"`
}

type Task struct {
    Type   string `json:"type"`
    ListId string `json:"list_id"`
}

func main() {

    client, err := couchdb.NewClient("http://localhost:4985/", nil)

    if err != nil {
        checkErr(err)
    }

    db := client.DB("todos")

    resp, err := http.Get("http://localhost:4985/todos/")
    defer resp.Body.Close()
    jsonData, err := ioutil.ReadAll(resp.Body)
    var info Info
    json.Unmarshal(jsonData, &info)

    feed, err := db.Changes(couchdb.Options{"feed": "continuous", "since": info.Seq, "include_docs": true})

    if err != nil {
        checkErr(err)
    }

    for feed.Next() {

        deviceTokens := HandleChangeEvent(feed.ID, db)
        NotifyUsers(deviceTokens)

    }

    for {

    }
}