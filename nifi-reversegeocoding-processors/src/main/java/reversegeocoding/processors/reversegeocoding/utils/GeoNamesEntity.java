package reversegeocoding.processors.reversegeocoding.utils;

/**
 * GeoNames class modeling GeoNames results
 * */

public class GeoNamesEntity {
    private String sunrise;
    private double lng;
    private double lat;
    private String countryCode;
    private double gmtOffset;
    private double rawOffset;
    private String sunset;
    private String timezoneId;
    private double dstOffset;
    private String countryName;
    private String time;

    public GeoNamesEntity() {
    }

    public String getSunrise() {
        return sunrise;
    }

    public void setSunrise(String sunrise) {
        this.sunrise = sunrise;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public double getGmtOffset() {
        return gmtOffset;
    }

    public void setGmtOffset(double gmtOffset) {
        this.gmtOffset = gmtOffset;
    }

    public double getRawOffset() {
        return rawOffset;
    }

    public void setRawOffset(double rawOffset) {
        this.rawOffset = rawOffset;
    }

    public String getSunset() {
        return sunset;
    }

    public void setSunset(String sunset) {
        this.sunset = sunset;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public void setTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    public double getDstOffset() {
        return dstOffset;
    }

    public void setDstOffset(double dstOffset) {
        this.dstOffset = dstOffset;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
