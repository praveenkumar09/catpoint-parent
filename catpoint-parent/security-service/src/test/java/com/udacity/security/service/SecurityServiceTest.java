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

    private Sensor sensor;

    private Set<Sensor> sensors;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private IImageService imageService;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("a", SensorType.DOOR);
        Sensor sensorB = new Sensor("b", SensorType.DOOR);
        Sensor sensorC = new Sensor("c", SensorType.DOOR);
        sensors = new HashSet<>();
        sensors.add(sensor);
        sensors.add(sensorB);
        sensors.add(sensorC);
    }

    // case 1
    @Test
    public void ifAlarmArmed_sensorActivated_systemPendingAlarm() {
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    public void idAlarmArmed_sensorActivated_pendingAlarm_setAlarm() {
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void ifPendingAlarm_allSensorInactive_returnToNoAlarm() {
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void ifAlarmActive_changeSensorState_notAffectAlarmState(boolean state) {
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, state);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    public void ifAlreadyActive_activateSensor_systemPendingState_changeToAlarmState() {
        when(securityRepository.getSensors()).thenReturn(sensors);
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void ifAlreadyInactive_deactiveSensor_noChangeToAlarmState() {
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    public void ifCatImage_armedHome_systemAlarmState() {
        BufferedImage catImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(catImage, 50.0f)).thenReturn(true);
        securityService.processImage(catImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void ifNotCatImage_changeToNoAlarm_asLongAsSensorsNotActive() {
        BufferedImage notCatImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.processImage(notCatImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void ifDisarmedSystem_setToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void ifArmedSystem_resetSensorsToInactive(ArmingStatus state) {
        sensors.forEach(sensor -> sensor.setActive(true));
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(state);
        securityRepository.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"DISARMED", "ARMED_AWAY"})
    public void ifDisarmedOrArmedAway_detectCat_armedAndAlarm(ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
        BufferedImage catImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(catImage, 50.0f)).thenReturn(true);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void ifDeActiveActiveSensor_pendingAlarm_toNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
}