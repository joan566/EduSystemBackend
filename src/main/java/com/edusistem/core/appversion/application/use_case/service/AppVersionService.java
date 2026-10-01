package com.edusistem.core.appversion.application.use_case.service;

import com.edusistem.core.appversion.domain.inputports.GetAppVersionPolicyUseCase;
import com.edusistem.core.appversion.domain.vo.AppVersionPolicy;

/** Las versiones salen de la configuración del entorno; se validan una sola vez al arrancar. */
public class AppVersionService implements GetAppVersionPolicyUseCase {

    private final AppVersionPolicy policy;

    public AppVersionService(AppVersionPolicy policy) {
        this.policy = policy;
    }

    @Override
    public AppVersionPolicy current() {
        return policy;
    }
}
