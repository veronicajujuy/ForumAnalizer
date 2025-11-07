package com.verovaldez.discord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvWriter implements Closeable {
    private final PrintWriter out;

    public CsvWriter(String path, List<String> headers) throws IOException {
        this.out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8));
        writeRow(headers);
    }

    public void writeRow(List<String> cols) {
        String row = String.join(",", cols.stream().map(CsvWriter::quote).toArray(String[]::new));
        out.println(row);
        out.flush();
    }
    private static String quote(String s) {
        if (s == null) s = "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
