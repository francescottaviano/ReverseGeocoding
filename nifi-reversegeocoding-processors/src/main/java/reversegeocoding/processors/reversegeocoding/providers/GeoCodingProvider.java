package reversegeocoding.processors.reversegeocoding.providers;

import reversegeocoding.processors.reversegeocoding.City;

public interface GeoCodingProvider {

    City resolve(City city);

}
