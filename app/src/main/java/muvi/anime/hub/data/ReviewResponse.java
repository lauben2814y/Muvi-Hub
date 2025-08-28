package muvi.anime.hub.data;

public class ReviewResponse {
    private boolean isInReview;

    public ReviewResponse(boolean isInReview) {
        this.isInReview = isInReview;
    }

    public boolean isInReview() {
        return isInReview;
    }

    public void setInReview(boolean inReview) {
        isInReview = inReview;
    }
}
