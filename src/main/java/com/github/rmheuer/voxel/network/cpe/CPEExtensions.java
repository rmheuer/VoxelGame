package com.github.rmheuer.voxel.network.cpe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CPEExtensions {
    public static final class ExtensionInfo {
        public final String name;
        public final int version;

        public ExtensionInfo(String name, int version) {
            this.name = name;
            this.version = version;
        }
    }

    public static final class ExtensionSet {
        private final Map<String, Integer> extensions;

        public ExtensionSet() {
            extensions = new HashMap<>();
        }

        public void add(String extName, int version) {
            extensions.merge(extName, version, Math::max);
        }

        public boolean has(String name, int version) {
            Integer supportedVer = extensions.get(name);
            return supportedVer != null && supportedVer == version;
        }
    }

    public static final byte HANDSHAKE_MAGIC_VALUE = 0x42;
    public static final List<ExtensionInfo> ALL_SUPPORTED = List.of(
            new ExtensionInfo("ClickDistance", 1),
            new ExtensionInfo("LongerMessages", 1),
            new ExtensionInfo("BulkBlockUpdate", 1),
            new ExtensionInfo("TextColors", 1)
    );

    public final boolean clickDistance;
    public final boolean longerMessages;
    public final boolean bulkBlockUpdate;
    public final boolean textColors;

    public CPEExtensions(ExtensionSet extensions) {
        clickDistance = extensions.has("ClickDistance", 1);
        longerMessages = extensions.has("LongerMessages", 1);
        bulkBlockUpdate = extensions.has("BulkBlockUpdate", 1);
        textColors = extensions.has("TextColors", 1);
    }
}
