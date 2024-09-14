package org.addns.conf;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author YahocenMiniPC
 */
public class Command {

    /**
     * 单例实例
     */
    private static volatile Command instance;

    private final List<String> params;

    private Command(String[] args) {
        this.params = Arrays.stream(args).map(String::trim).toList();
    }

    /**
     * 获取单例实例的方法。
     * @param args 程序入口参数
     */
    public static void initInstance(String[] args) {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Command(args);
                }
            }
        }
    }

    public static String get(Option option) {
        if (instance == null) {
            throw new IllegalStateException("Command not initialized");
        }
        if(option == Option.HELP || option == Option.VERSION) {
            return option.defaultValue;
        }
        Optional<Integer> paramIndex = Arrays.stream(option.names).map(k -> instance.params.indexOf(k)).filter(i -> i != -1).findFirst();
        return paramIndex.map(integer -> instance.params.get(integer + 1)).orElse(option.defaultValue);
    }

    public static boolean contains(Option option) {
        if (instance == null) {
            throw new IllegalStateException("Command not initialized");
        }
        return Arrays.stream(option.names).anyMatch(k -> instance.params.contains(k));
    }

    public static boolean isHelp() {
        return contains(Option.HELP);
    }

    public static boolean isVersion() {
        return contains(Option.VERSION);
    }

    public static void usage(PrintStream stream) {
        StringBuilder usageBuilder = new StringBuilder();
        usageBuilder.append("Usage: ").append(Constant.NAME).append(" [options]\n\n");
        usageBuilder.append("Options:\n");
        for (Option option : Option.values()) {
            StringBuilder namesBuilder = new StringBuilder();
            for (int i = 0; i < option.names.length; i++) {
                if (i > 0) {
                    namesBuilder.append(", ");
                }
                if (option.names[i].startsWith("-")) {
                    namesBuilder.append(option.names[i]);
                } else {
                    namesBuilder.append("--").append(option.names[i]);
                }
            }
            String names = namesBuilder.toString();
            usageBuilder.append(String.format("  %s   %s\n", names, option.describe));
        }
        stream.println(usageBuilder);
    }

    public static void version(PrintStream stream) {
        stream.println(Option.VERSION.defaultValue);
    }

    public static enum Option {

        /**
         * 自定义参数
         */
        CONF(new String[]{"-c", "--conf"}, "指定配置文件", Constant.CONFIG_PATH),
        STOP(new String[]{"-s", "--stop"}, "停止服务", null),
        /**
         * 系统参数
         */
        HELP(new String[]{"-h", "--help"}, "显示帮助信息", null),
        VERSION(new String[]{"-v", "--version"}, "显示版本信息", Constant.VERSION),
        ;

        Option(String[] names, String describe, String defaultValue) {
            this.names = names;
            this.describe = describe;
            this.defaultValue = defaultValue;
        }

        final String[] names;

        final String describe;

        final String defaultValue;

        public String get() {
            return Command.get(this);
        }

        public boolean is() {
            return Command.contains(this);
        }

    }

    public static void main(String[] args) {
        Command.initInstance(new String[]{"-v"});
        if(Option.HELP.is()) {
            Command.usage(System.out);
            System.exit(0);
        }
        if(Option.VERSION.is()) {
            Command.version(System.out);
            System.exit(0);
        }
        //其他业务逻辑
    }

}
