package org.addns.util;

import org.addns.conf.Constant;

import java.io.IOException;
import java.util.logging.*;

/**
 * @author YahocenMiniPC
 */
public class LogUtil {

    private static final Logger LOGGER = Logger.getLogger(Constant.NAME);

    static {
        // 设置全局日志级别
        LOGGER.setLevel(Level.ALL);
        try {
            //创建文件目录
            if(!FileUtil.exist(Constant.LOG_PATH)) {
                FileUtil.mkdir(Constant.LOG_PATH);
            }
            // 文件处理器
            FileHandler fileHandler = new FileHandler(Constant.LOG_NAME, Constant.LOG_LIMIT, Constant.LOG_COUNT, true);
            fileHandler.setEncoding("UTF-8");
            // 设置文件日志级别
            fileHandler.setLevel(Level.INFO);
            // 可以自定义格式器
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            // 控制台处理器
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setEncoding("UTF-8");
            // 设置控制台日志级别
            consoleHandler.setLevel(Level.ALL);
            LOGGER.addHandler(consoleHandler);
            // 避免日志重复
            LOGGER.setUseParentHandlers(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private LogUtil() {
        throw new AssertionError("No instances for you!");
    }

    /**
     * Logs a FINEST level message.
     *
     * @param message The message to log.
     */
    public static void trace(String message) {
        LOGGER.log(Level.FINEST, message);
    }

    /**
     * Logs a FINE level message.
     *
     * @param message The message to log.
     */
    public static void debug(String message) {
        LOGGER.log(Level.FINE, message);
    }

    /**
     * Logs an info level message.
     *
     * @param message The message to log.
     */
    public static void info(String message, Object... args) {
        LOGGER.log(Level.INFO, String.format(message, args));
    }

    /**
     * Logs a warning level message.
     *
     * @param message The message to log.
     */
    public static void warn(String message, Object... args) {
        LOGGER.log(Level.WARNING, String.format(message, args));
    }

    /**
     * Logs an error level message.
     *
     * @param message The message to log.
     */
    public static void error(String message, Object... args) {
        LOGGER.log(Level.SEVERE, String.format(message, args));
    }

    /**
     * Logs a FINEST level message with an exception.
     *
     * @param message The message to log.
     * @param throwable The exception to log.
     */
    public static void trace(String message, Throwable throwable) {
        LOGGER.log(Level.FINEST, message, throwable);
    }

    /**
     * Logs a FINE level message with an exception.
     *
     * @param message The message to log.
     * @param throwable The exception to log.
     */
    public static void debug(String message, Throwable throwable) {
        LOGGER.log(Level.FINE, message, throwable);
    }

    /**
     * Logs an info level message with an exception.
     *
     * @param message The message to log.
     * @param throwable The exception to log.
     */
    public static void info(String message, Throwable throwable) {
        LOGGER.log(Level.INFO, message, throwable);
    }

    /**
     * Logs a warning level message with an exception.
     *
     * @param message The message to log.
     * @param throwable The exception to log.
     */
    public static void warn(String message, Throwable throwable, Object... args) {
        LOGGER.log(Level.WARNING, String.format(message, args), throwable);
    }

    /**
     * Logs an error level message with an exception.
     *
     * @param message The message to log.
     * @param throwable The exception to log.
     */
    public static void error(String message, Throwable throwable, Object... args) {
        LOGGER.log(Level.SEVERE, String.format(message, args), throwable);
    }

}