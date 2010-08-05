package com.nookdevs.market;

public class AppInfo {
    String url;
    String version;
    String text;
    String title;
    String pkg;
    boolean installed=false;
    boolean updateAvailable=false;
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Title =");
        sb.append(title);
        sb.append(" URL =");
        sb.append(url);
        sb.append(" version=");
        sb.append(version);
        sb.append(" package=");
        sb.append(pkg);
        sb.append(" installed=");
        sb.append(installed);
        sb.append(" updateAvailable=");
        sb.append(updateAvailable);
        sb.append(" desc=");
        sb.append(text);
        sb.append("\n");
        return sb.toString();
    }
}
