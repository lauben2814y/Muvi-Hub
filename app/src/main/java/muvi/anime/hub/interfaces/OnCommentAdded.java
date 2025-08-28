package muvi.anime.hub.interfaces;

import java.util.List;

import muvi.anime.hub.models.Comment;

public interface OnCommentAdded {
    void onCommentAdded(List<Comment> comments);
}
