package de.lme.heartnhealth4u;
 interface IShimmerSimServiceCallback {
    /**
     * Called when the service has a new value for you.
     */
    oneway void onShimmerSimEvent( int eventID, long timestamp, float sensorValue, char label );
}