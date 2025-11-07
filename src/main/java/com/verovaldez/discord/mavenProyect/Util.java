package com.verovaldez.discord.mavenProyect;

public class Util {
    public static String escapeCsv(String s) {
        if (s == null) return "";
        return s.length() > 4000 ? s.substring(0, 4000) + "…" : s;
    }
}
