package ypfunding.common.util;

public class LogUtil {
    private LogUtil(){

    }

    public static void log(Object message){
        System.out.println(message);
    }

    public static void logError(Object message){
        System.err.println(message);
    }

}
