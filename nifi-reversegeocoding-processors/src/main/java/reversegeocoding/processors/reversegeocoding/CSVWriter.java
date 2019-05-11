package reversegeocoding.processors.reversegeocoding;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class CSVWriter {
    private BufferedWriter bw;

    private String fieldSeparator;

    public CSVWriter(OutputStream out, String fieldSeparator) {
        this.bw = new BufferedWriter(new OutputStreamWriter(out));
        this.fieldSeparator = fieldSeparator;
    }

    public void closeFile() throws IOException {
        this.bw.close();
    }

    public void writeLine(List<String> values) throws IOException {
        StringBuilder line = new StringBuilder();
        int i = 0;
        while (i < values.size() - 1) {
            line.append(values.get(i)).append(fieldSeparator);
            i++;
        }
        line.append(values.get(i)).append("\n");

        this.bw.write(line.toString());
    }


    public BufferedWriter getBw() {
        return bw;
    }

    public String getFieldSeparator() {
        return fieldSeparator;
    }
}
