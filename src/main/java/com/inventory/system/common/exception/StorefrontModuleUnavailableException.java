package com.inventory.system.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StorefrontModuleUnavailableException extends RuntimeException {

    public StorefrontModuleUnavailableException() {
        super("Storefront is not available for this tenant");
    }
}