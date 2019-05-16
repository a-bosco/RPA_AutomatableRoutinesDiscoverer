package au.edu.unimelb.rpadiscovery.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

public class Log {

    private static HashSet<String> tags;

    private static int detailsLevel;
    public static final int LEVEL_ESSENTIAL = 1;
    public static final int LEVEL_MAX_DETAIL = 10;


    private static boolean showTimestamp;
    private static boolean showTag;
    private static boolean debugSpace;

    private static SimpleDateFormat simpleDateFormat;
    private static final String DEFAULT_DATA_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"; // 2015-09-28T16:16:23.308


    private static Log ourInstance;

    public static Log getInstance() {
        if (ourInstance == null) {
            ourInstance = new Log();
        }
        return ourInstance;
    }


    public void println(String TAG, int detail, String input) {
        if ((detail > detailsLevel) || (!tags.isEmpty() && !tags.contains(TAG)))
            return;
        System.out.println(getPrint(TAG, detail, input));
    }

    public void println(String TAG, int detail, Object input) {
        if ((detail > detailsLevel) || (!tags.isEmpty() && !tags.contains(TAG)))
            return;
        System.out.println(getPrint(TAG, detail, input.toString()));
    }

    public void print(String TAG, int detail, String input) {
        System.out.print(getPrint(TAG, detail, input));
    }

    public void print(String TAG, int detail, Object input) {
        System.out.print(getPrint(TAG, detail, input.toString()));
    }

    public String getPrintln(String TAG, int detail, String input) {
        if ((detail > detailsLevel) || (!tags.isEmpty() && !tags.contains(TAG)))
            return "";
        return getPrint(TAG, detail, input) + "\n";
    }

    public String getPrintln(String TAG, int detail, Object input) {
        if ((detail > detailsLevel) || (!tags.isEmpty() && !tags.contains(TAG)))
            return "";
        return getPrint(TAG, detail, input.toString()) + "\n";
    }

    public String getPrint(String TAG, int detail, Object input) {
        return getPrint(TAG, detail, input.toString());
    }

    public String getPrint(String TAG, int detail, String input) {
        if (detail > detailsLevel || (!tags.isEmpty() && !tags.contains(TAG)))
            return "";
        if (!showTag) {
            return getBaseString(TAG, input, false);
        }
        return getBaseString(TAG, input, true);
    }


    public void println(String TAG, int detail) {
        if ((detail > detailsLevel) || (!tags.isEmpty() && !tags.contains(TAG)))
            return;
        System.out.println(getPrint(TAG, detail, ""));
    }

    public void print(String TAG, int detail) {
        System.out.print(getPrint(TAG, detail, ""));
    }

    public void simplePrint(String TAG, int detail, Object input){
        if(detail <= detailsLevel&&(tags.isEmpty()||tags.contains(TAG)))
            System.out.print(input.toString());
    }

    public String getPrintln(String TAG, int detail) {
        return getPrint(TAG, detail, "") + "\n";
    }

    public String getPrint(String TAG, int detail) {
        return getPrint(TAG, detail, "");
    }


    public String getBaseString(String TAG, String input, boolean tag) {
        String s = "" + ((showTimestamp) ? "[" + simpleDateFormat.format(Calendar.getInstance().getTime()) + "] - " : "")
                + ((tag) ? "<" + TAG + "> :" : "")
                + ((debugSpace) ? "*" + input + "*" : input);
        return s;
    }


    private Log() {
        this.tags = new HashSet<>();
        this.detailsLevel = LEVEL_MAX_DETAIL;
        this.showTag = true;
        this.showTimestamp = true;
        this.debugSpace = false;
        this.simpleDateFormat = new SimpleDateFormat(DEFAULT_DATA_FORMAT);
    }

    public void settingsLog(List<String> tag, boolean showTimestamp, boolean showTag, int detailsLevel, SimpleDateFormat simpleDateFormat) {
        this.tags = new HashSet<>(tag);
        this.detailsLevel = detailsLevel;
        this.showTag = showTag;
        this.showTimestamp = showTimestamp;
        if (simpleDateFormat != null)
            this.simpleDateFormat = simpleDateFormat;
        else {
            this.simpleDateFormat = new SimpleDateFormat(DEFAULT_DATA_FORMAT);

        }


    }

    public void settingsLog(List<String> tag, boolean showTimestamp, boolean showTag, int detailsLevel) {
        this.tags = new HashSet<>(tag);
        this.detailsLevel = detailsLevel;
        this.showTag = showTag;
        this.showTimestamp = showTimestamp;
    }

    public void settingsLog(List<String> tag, int detailsLevel) {
        this.tags = new HashSet<>(tag);
        this.detailsLevel = detailsLevel;

    }

    public boolean isDebugSpace() {
        return debugSpace;
    }

    public void setDebugSpace(boolean debugSpace) {
        Log.debugSpace = debugSpace;
    }

    public HashSet<String> getTags() {
        return tags;
    }

    public void setTags(HashSet<String> tags) {
        Log.tags = tags;
    }

    public int getDetailsLevel() {
        return detailsLevel;
    }

    public void setDetailsLevel(int detailsLevel) {
        Log.detailsLevel = detailsLevel;
    }

    public boolean isShowTimestamp() {
        return showTimestamp;
    }

    public void setShowTimestamp(boolean showTimestamp) {
        Log.showTimestamp = showTimestamp;
    }

    public boolean isShowTag() {
        return showTag;
    }

    public void setShowTag(boolean showTag) {
        Log.showTag = showTag;
    }

    public SimpleDateFormat getSimpleDateFormat() {
        return simpleDateFormat;
    }

    public void setSimpleDateFormat(SimpleDateFormat simpleDateFormat) {
        Log.simpleDateFormat = simpleDateFormat;
    }
}
