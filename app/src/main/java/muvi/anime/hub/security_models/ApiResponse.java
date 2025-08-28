package muvi.anime.hub.security_models;


// Generic API Response wrapper
public class ApiResponse<T> {
    public boolean success;
    public T data;
    public Pagination pagination;
    public Integer total;
    public String error;
    public String details;
}
