package com.edusistem.core.appversion.presentation.dtos;

import com.edusistem.core.appversion.domain.vo.AppVersionPolicy;

public record AppVersionResponse(String latestVersion, String minimumVersion) {

    public static AppVersionResponse from(AppVersionPolicy policy) {
        return new AppVersionResponse(policy.latest().toString(), policy.minimum().toString());
    }
}
