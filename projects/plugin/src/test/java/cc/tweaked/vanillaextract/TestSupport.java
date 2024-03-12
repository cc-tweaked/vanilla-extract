package cc.tweaked.vanillaextract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TestSupport {
    private TestSupport() {
    }

    public static List<String> getGradleArguments(ConfigurationCacheMode configurationCache, String... args) {
        List<String> argList = new ArrayList<>();
        Collections.addAll(argList, "-s", "-i", "--warning-mode=fail");
        switch (configurationCache) {
            case NONE -> {
            }
            case WITH_CACHE -> argList.add("--configuration-cache");
        }

        Collections.addAll(argList, args);
        return argList;
    }

    public enum ConfigurationCacheMode {
        NONE,
        WITH_CACHE
    }
}
