package com.udacity.security.service;

import com.udacity.image.service.IImageService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {


    private SecurityService securityService;

    private final String uuid = UUID.randomUUID().toString();

    private Sensor sensor;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private IImageService imageService;

    @BeforeEach
    void setUp(){
        securityService = new SecurityService(securityRepository,imageService);
        sensor = new Sensor(uuid, SensorType.DOOR);
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
    void ifSystemArmedAndSensorActivated_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void ifSystemArmedAndSensorActivatedAndPendingState_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifPendingAlarmAndSensorInactive_returnNoAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifAlarmIsActive_changeSensorShouldNotAffectAlarmState(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifSensorActivatedWhileActiveAndPendingAlarm_changeStatusToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void ifSensorDeactivatedWhileInactive_noChangesToAlarmState(AlarmStatus status) {
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifImageServiceIdentifiesCatWhileAlarmArmedHome_changeStatusToAlarm() {
        var catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(catImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifImageServiceIdentifiesNoCatImage_changeStatusToNoAlarmAsLongSensorsNotActive() {
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void ifSystemDisarmed_setNoAlarmState() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemArmed_resetSensorsToInactive(ArmingStatus status) {
        Set<Sensor> sensors = getAllSensors(3, true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);

        securityService.getSensors().forEach(sensor -> {
            assertFalse(sensor.getActive());
        });
    }

    @Test
    void ifSystemArmedHomeWhileImageServiceIdentifiesCat_changeStatusToAlarm() {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /*
     * System disarmed Test
     * */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM"})
    void ifSystemDisarmedAndSensorActivated_noChangesToArmingState(AlarmStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setArmingStatus(ArmingStatus.DISARMED);
    }

    @Test
    void ifAlarmStateAndSystemDisarmed_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    private Set<Sensor> getAllSensors(int count, boolean status) {
        var sensors =
                IntStream
                        .range(0, count)
                        .mapToObj(i -> new Sensor(uuid, SensorType.DOOR))
                        .collect(Collectors.toCollection(HashSet::new));

        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

}