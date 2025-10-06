package com.ars.gateway.dto;

@SuppressWarnings("ununsed")
public class CheckValidDeviceIdResponseDTO {
    private boolean isValid;
    private String deviceId;

    public CheckValidDeviceIdResponseDTO(String deviceId, boolean isValid) {
        this.deviceId = deviceId;
        this.isValid = isValid;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
