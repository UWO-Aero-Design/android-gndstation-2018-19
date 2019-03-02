public interface GeneralAircraft {

    double readRadioSignalStrength();
    void updateRadioSignalStrength(double currentRadioSignalStrengthRssi);

    double readBlueToothStrength();
    void updateBlueToothStrength(double currentBlueToothStrength);

    double readPlaneYaw();
    void updatePlaneYaw(double currentPlaneYawValue);

    double readPlanePitch();
    void updatePlanePitch(double currentPlanePitchValue);

    double readPlaneRoll();
    void updatePlaneRoll(double currentPlaneRollValue);

    double readPlaneHeight();
    void updatePlaneHeight(double currentPlaneHeightValue);

    double readPlaneSpeed();
    void updatePlaneSpeed(double currentPlaneSpeedValue);

    double readPlaneAltitude();
    void updatePlaneAltitude(double currentPlaneAltitudeValue);

    double readPlaneLatitude();
    void updatePlaneLatitude(double currentPlaneLatitudeValue);

    double readPlaneLongitude();
    void updatePlaneLongitude(double currentPlaneLongitudeValue);

}
