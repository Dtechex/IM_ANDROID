package com.loopytime.im.status;

import java.io.Serializable;

public class FeedObject implements Serializable {
    public String text1, text2, media, created_at, uname, upic, uphone, _id, uid, comment_count, reaction_count,media_type;
    public int size,status;
    public FeedCommentObject feedComment;

    public FeedObject(String text1, String text2, String media, String created_at, String uname, String upic, String uphone, String _id, String uid, FeedCommentObject feedComment, String comment_count, String reaction_count, int size, String media_type, int status) {
        this.text1 = text1;
        this.media_type = media_type;
        this.status=status;
        this.size = size;
        this.uid = uid;
        this.text2 = text2;
        this.media = media;
        this.created_at = created_at;
        this.uname = uname;
        this.upic = upic;
        this.uphone = uphone;
        this._id = _id;
        this.feedComment = feedComment;
        this.comment_count = comment_count;
        this.reaction_count = reaction_count;
    }
}
