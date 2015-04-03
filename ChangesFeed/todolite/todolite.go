package main

import (

    "github.com/alexjlockwood/gcm"
    "fmt"
    "github.com/fjl/go-couchdb"
)

func HandleChangeEvent(ID string, db *couchdb.DB) (deviceTokens []string) {

    deviceTokens = []string{}

    var list List
    if err := db.Get(ID, &list, nil); err != nil {
        fmt.Println(err)
    } else {
        var profile Profile
        db.Get(list.Owner, &profile, nil)
        deviceTokens = append(deviceTokens, profile.DeviceToken)


        for _, userId := range list.Members {
            var profile Profile
            db.Get(userId, &profile, nil)
            deviceTokens = append(deviceTokens, profile.DeviceToken)
        }
    }

    var task Task
    if err := db.Get(ID, &task, nil); err != nil {
        fmt.Println(err)
    } else {
        var list List
        db.Get(task.ListId, &list, nil)

        fmt.Println(list.Owner)

        var profile Profile
        db.Get(list.Owner, &profile, nil)

        fmt.Println(profile.Type)

        deviceTokens = append(deviceTokens, profile.DeviceToken)


        for _, userId := range list.Members {
            var profile Profile
            db.Get(userId, &profile, nil)
            deviceTokens = append(deviceTokens, profile.DeviceToken)
        }
    }

    return deviceTokens
}


func NotifyUsers(deviceTokens []string) (error) {
    sender := &gcm.Sender{ApiKey: "AIzaSyBEHLA1FR4OlCRQE1vPv_mfqQaIF0ICZeA"}
    message := gcm.NewMessage(nil, deviceTokens...)
    resp, err := sender.Send(message, 2)
    fmt.Println(resp)
    if err != nil {
        return fmt.Errorf("Error sending notifications: %s", err)
    }
    return nil
}


/*
  This method doesn't work at the moment
  Get back an unexpected end of input
  */
//        var list List
//        if err := json.Unmarshal(feed.Doc, &list); err != nil {
//            fmt.Println(err)
//        } else {
//            fmt.Println(list.Owner)
//        }

//        var list List
//        db.Get(feed.ID, &list, nil)
//        fmt.Println(list)

//        sendGCM()