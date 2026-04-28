package com.inventory.system.service.courier;

public class CourierProviderException extends RuntimeException {

    public CourierProviderException(String message) {
        super(message);
    }

    public CourierProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
