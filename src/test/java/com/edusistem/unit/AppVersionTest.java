package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusistem.core.appversion.domain.vo.AppVersion;
import com.edusistem.core.appversion.domain.vo.AppVersionPolicy;
import org.junit.jupiter.api.Test;

class AppVersionTest {

    private static AppVersion v(String value) {
        return AppVersion.parse(value);
    }

    @Test
    void comparesMinorVersions() {
        assertThat(v("1.0.0")).isLessThan(v("1.1.0"));
        assertThat(v("1.1.0")).isLessThan(v("1.2.0"));
        assertThat(v("1.0.0").isOlderThan(v("1.1.0"))).isTrue();
    }

    @Test
    void comparesNumericallyNotAsStrings() {
        // como texto "1.10.0" < "1.9.0"; como versión es mayor
        assertThat(v("1.10.0")).isGreaterThan(v("1.9.0"));
    }

    @Test
    void equalVersionsAreEqual() {
        assertThat(v("1.2.0")).isEqualByComparingTo(v("1.2.0"));
        assertThat(v("1.2.0")).isEqualTo(v("1.2.0"));
        assertThat(v("1.2.0").isOlderThan(v("1.2.0"))).isFalse();
    }

    @Test
    void majorVersionDominates() {
        assertThat(v("2.0.0")).isGreaterThan(v("1.9.9"));
    }

    @Test
    void comparesPatchVersions() {
        assertThat(v("1.2.1")).isGreaterThan(v("1.2.0"));
    }

    @Test
    void keepsTextFormat() {
        assertThat(v("1.10.0")).hasToString("1.10.0");
        assertThat(v(" 2.0.3 ")).hasToString("2.0.3");
    }

    @Test
    void rejectsInvalidFormats() {
        for (String invalid : new String[] {"1.2", "1.2.0.1", "a.b.c", "-1.0.0", "v1.2.0", "1.2.0+5", "", " "}) {
            assertThatThrownBy(() -> AppVersion.parse(invalid)).as(invalid).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> AppVersion.parse(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void policyRequiresMinimumNotGreaterThanLatest() {
        assertThat(new AppVersionPolicy(v("1.2.0"), v("1.1.0")).minimum()).isEqualTo(v("1.1.0"));
        assertThat(new AppVersionPolicy(v("1.2.0"), v("1.2.0")).latest()).isEqualTo(v("1.2.0"));
        assertThatThrownBy(() -> new AppVersionPolicy(v("1.9.0"), v("1.10.0")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
