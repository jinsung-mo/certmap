package com.authservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuth2CodeRequest {
    private String code;
}
