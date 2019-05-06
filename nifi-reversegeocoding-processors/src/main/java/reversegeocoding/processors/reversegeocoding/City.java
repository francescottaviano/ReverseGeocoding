package reversegeocoding.processors.reversegeocoding;

import java.util.TimeZone;

public class City {

    private String name;
    private String lat;
    private String lon;
    private String country;
    private TimeZone timeZone;

    public City(String name, String lat, String lon, String country, TimeZone timeZone) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.country = country;
        this.timeZone = timeZone;

    }

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

}
