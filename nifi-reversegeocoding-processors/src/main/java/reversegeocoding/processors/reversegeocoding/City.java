package reversegeocoding.processors.reversegeocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.TimeZone;

/**
 * city class modeling cities provided by csv files
 */
public class City implements Serializable {

    @JsonProperty("City")
    private String name;
    @JsonProperty("Latitude")
    private String lat;
    @JsonProperty("Longitude")
    private String lon;
    private String country;
    private String timeOffset;

    public City(String name, String lat, String lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }

    public City() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTimeOffset() {
        return timeOffset;
    }

    public void setTimeOffset(String timeOffset) {
        this.timeOffset = timeOffset;
    }

    public void setTimeOffset(int timeOffset) {
        this.timeOffset = "";

        if (timeOffset >= 0) {
            this.timeOffset += "+";
            this.timeOffset += String.format("%02d", timeOffset);
        } else {
            this.timeOffset += String.format("%03d", timeOffset);
        }


    }
}
