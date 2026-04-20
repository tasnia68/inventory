package com.inventory.system.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class StorefrontModuleDisabledException extends RuntimeException {

    public StorefrontModuleDisabledException() {
        super("Storefront module is not enabled for this tenant");
    }
}