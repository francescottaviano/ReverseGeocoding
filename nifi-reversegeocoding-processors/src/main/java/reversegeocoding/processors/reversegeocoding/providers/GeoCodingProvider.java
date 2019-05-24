package reversegeocoding.processors.reversegeocoding.providers;

import reversegeocoding.processors.reversegeocoding.City;

/**
 * Geocoding Provider Interface
 * */

public interface GeoCodingProvider {

    City resolve(City city);

}
