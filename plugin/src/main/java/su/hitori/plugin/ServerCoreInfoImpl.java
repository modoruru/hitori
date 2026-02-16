package su.hitori.plugin;

import su.hitori.api.ServerCoreInfo;

final class ServerCoreInfoImpl implements ServerCoreInfo {

    private final boolean folia;

    ServerCoreInfoImpl() {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        }
        catch (ClassNotFoundException _) {
            folia = false;
        }

        this.folia = folia;
    }

    @Override
    public boolean isFolia() {
        return folia;
    }

}
