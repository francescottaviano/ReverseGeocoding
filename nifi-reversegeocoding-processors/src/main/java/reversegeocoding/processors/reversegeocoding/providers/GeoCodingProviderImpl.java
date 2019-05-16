package reversegeocoding.processors.reversegeocoding.providers;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public abstract class GeoCodingProviderImpl implements GeoCodingProvider {

    protected String prettyOffset(double offset) {
        NumberFormat formatter = new DecimalFormat("#00.00");
        String prettyOffset = "";
        String offsetStr = "";

        if (offset >= 0) {
            prettyOffset += "+";
        }
        offsetStr += formatter.format(offset);
        prettyOffset += String.format("%s", offsetStr.replace(".",""));
        return prettyOffset;
    }

}
