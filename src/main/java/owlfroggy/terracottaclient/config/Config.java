package owlfroggy.terracottaclient.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import owlfroggy.terracottaclient.TCClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class Config {
    // initialize here to set a default value
    @ConfigValue(key = "api_enabled", order = 10)
    public static boolean apiEnabled = true;

    @ConfigValue(key = "highlight_library_items", order = 20)
    public static boolean highlightLibraryItems = true;

    @ConfigValue(key = "connection_message_type", order = 30)
    public static ConnectionMessageMode connectionMessageMode = ConnectionMessageMode.VERBOSE;


    private static Path CONFIG_FILE_PATH = TCClient.getConfigPath().resolve("config.json");


    public static void load() {
        try {
            String contents = Files.readString(CONFIG_FILE_PATH);
            JsonObject json = JsonParser.parseString(contents).getAsJsonObject();
            Set<String> jsonKeys = json.keySet();

            for (Field f : Config.class.getFields()) {
                ConfigValue annotation = f.getAnnotation(ConfigValue.class);
                if (annotation == null) continue;
                if (!jsonKeys.contains(annotation.key())) continue;

                Class<?> type = f.getType();
                try {
                    if (type == boolean.class) {
                        f.set(null,json.get(annotation.key()).getAsBoolean());
                    } else if (type.isEnum()) {
                        f.set(null,Enum.valueOf(
                            type.asSubclass(Enum.class),
                            json.get(annotation.key()).getAsString()
                        ));
                    } else {
                        throw new RuntimeException("No clue how to deserialize a %s".formatted(type));
                    }
                } catch (RuntimeException e) {
                    TCClient.LOGGER.error("Missing or invalid value for config value for key {}. Its last known value or default value will be used. {}\n{}",annotation.key(),e.getMessage(),e.getStackTrace());
                }
            }
        } catch (Exception e) {
            TCClient.LOGGER.error("Could not read config file. All values will use their defaults. {}\n{}",e.getMessage(),e.getStackTrace());
        }
    }

    public static void write() {
        JsonObject json = new JsonObject();
        for (Field f : Config.class.getFields()) {
            ConfigValue annotation = f.getAnnotation(ConfigValue.class);
            if (annotation == null) continue;

            Class<?> type = f.getType();
            try {
                if (type == boolean.class) {
                    json.addProperty(annotation.key(), (Boolean)f.get(null));
                } else if (type.isEnum()) {
                    json.addProperty(annotation.key(), ((Enum<?>)f.get(null)).name() );
                } else {
                    throw new RuntimeException("No clue how to serialize a %s".formatted(type));
                }
            } catch (Exception e) {
                TCClient.LOGGER.error("Could not serialize config value for key {}. It will not be included in the output file. {}\n{}",annotation.key(),e.getMessage(),e.getStackTrace());
            }
        }

        String serialized = json.toString();
        try {
            Files.writeString(CONFIG_FILE_PATH,serialized);
        } catch (IOException e) {
            TCClient.LOGGER.error("Could not write token file: {}\n{}",e.getMessage(),e.getStackTrace());
        }
    }

}
