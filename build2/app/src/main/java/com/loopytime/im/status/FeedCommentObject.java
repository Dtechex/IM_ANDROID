package com.loopytime.im.status;

import java.io.Serializable;

public class FeedCommentObject implements Serializable {
    public String comment,post_id,uid, created_at, uname, upic, uphone, _id, parent_id;

    public FeedCommentObject(String comment, String post_id, String parent_id, String created_at, String uname, String upic, String uphone, String _id, String uid) {
        this.comment = comment;
        this.uid = uid;
        this.post_id = post_id;
        this.parent_id = parent_id;
        this.created_at = created_at;
        this.uname = uname;
        this.upic = upic;
        this.uphone = uphone;
        this._id = _id;
    }
}
