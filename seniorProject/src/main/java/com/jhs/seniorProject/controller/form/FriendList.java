package com.jhs.seniorProject.controller.form;

import com.jhs.seniorProject.domain.Friend;
import lombok.Data;

@Data
public class FriendList {
    String id;
    String name;

    public FriendList(Friend friend) {
        this.id = friend.getFriendId().getId();
        this.name = friend.getFriendId().getName();
    }
}