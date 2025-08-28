package muvi.anime.hub.security_models;

public class RegisterDeviceResponse {
    public boolean success;
    public String message;
    public String deviceId;
    public boolean isNewDevice;
    public String signatureMethod; // 'simple' or 'dynamic'
}
