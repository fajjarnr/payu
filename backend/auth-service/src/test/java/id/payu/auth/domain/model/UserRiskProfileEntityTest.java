package id.payu.auth.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRiskProfileEntityTest {

    @Test
    void addKnownIpShouldAssociateChildWithoutRecursiveHashing() {
        UserRiskProfileEntity profile = new UserRiskProfileEntity();
        profile.setUsername("k6stress-user");

        profile.addKnownIp("10.200.0.10");

        assertThat(profile.getKnownIps()).hasSize(1);
        UserKnownIpEntity knownIp = profile.getKnownIps().iterator().next();
        assertThat(knownIp.getIpAddress()).isEqualTo("10.200.0.10");
        assertThat(knownIp.getUserRiskProfile()).isSameAs(profile);
    }

    @Test
    void addKnownDeviceShouldAssociateChildWithoutRecursiveHashing() {
        UserRiskProfileEntity profile = new UserRiskProfileEntity();
        profile.setUsername("k6stress-user");

        profile.addKnownDevice("device-fingerprint-1234");

        assertThat(profile.getKnownDevices()).hasSize(1);
        UserKnownDeviceEntity knownDevice = profile.getKnownDevices().iterator().next();
        assertThat(knownDevice.getDeviceId()).isEqualTo("device-fingerprint-1234");
        assertThat(knownDevice.getUserRiskProfile()).isSameAs(profile);
    }
}