package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSettingDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private String key;
    private String value;
    private String type;
    private String category;
}
