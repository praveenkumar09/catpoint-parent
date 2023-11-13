package com.udacity.security.service;

import com.udacity.image.service.FakeImageService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.AlarmStatus;
import com.udacity.security.data.ArmingStatus;
import com.udacity.security.data.SecurityRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {


    private SecurityService securityService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private FakeImageService imageService;

    @Mock
    private StatusListener statusListener;

    @BeforeEach
    void setUp(){
        securityService = new SecurityService(securityRepository,imageService);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,names = {"ARMED_HOME","ARMED_AWAY"})
    void setArmingStatusToArmed_changeStatusToArmed(ArmingStatus status) {
        securityService.setArmingStatus(status);
        verify(securityRepository,times(1)).setArmingStatus(status);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,names = {"DISARMED"})
    void setArmingStatusToDisarmed_changeStatusToNoAlarm(ArmingStatus status) {
        securityService.setArmingStatus(status);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository,times(1)).setArmingStatus(status);
    }

    @Test
    void addStatusListener() {
    }

    @Test
    void removeStatusListener() {
    }

    @Test
    void setAlarmStatus() {
    }

    @Test
    void changeSensorActivationStatus() {
    }

    @Test
    void processImage() {
    }

    @Test
    void getAlarmStatus() {
    }

    @Test
    void getSensors() {
    }

    @Test
    void addSensor() {
    }

    @Test
    void removeSensor() {
    }

    @Test
    void getArmingStatus() {
    }
}