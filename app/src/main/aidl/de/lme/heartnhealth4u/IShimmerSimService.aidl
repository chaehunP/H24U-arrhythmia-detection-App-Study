package de.lme.heartnhealth4u;

import de.lme.heartnhealth4u.IShimmerSimServiceCallback;

interface IShimmerSimService {
	/**
     * Often you want to allow a service to call back to its clients.
     * This shows how to do so, by registering a callback interface with
     * the service.
     */
    void registerCallback(IShimmerSimServiceCallback cb);
    
    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(IShimmerSimServiceCallback cb);
}