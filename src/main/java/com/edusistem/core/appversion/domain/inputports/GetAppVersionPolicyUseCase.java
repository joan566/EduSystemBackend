package com.edusistem.core.appversion.domain.inputports;

import com.edusistem.core.appversion.domain.vo.AppVersionPolicy;

public interface GetAppVersionPolicyUseCase {

    /** Versiones vigentes de la app móvil; no depende del usuario autenticado. */
    AppVersionPolicy current();
}
