package reversegeocoding.processors.reversegeocoding;

import java.util.TimeZone;

/**
 * City class modeling cities provided by csv files
 */

public class City {

    private String city;
    private String lat;
    private String lon;
    private String country;
    private TimeZone timeZone;

    public City() {
    }

    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }


    public String getCountry() {
        return country;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }
}
