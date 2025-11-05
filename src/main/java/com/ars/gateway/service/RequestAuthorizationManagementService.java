package com.ars.gateway.service;

import java.util.List;

public interface RequestAuthorizationManagementService {
    void refreshPublicRequestsConfig();
    List<String> getDefaultPublicRequestsConfig();
}
