package org.djtmk.beeauction.mysql;

import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.mysql.impl.MySQLManager;
import org.djtmk.beeauction.mysql.impl.SQLiteManager;

public class DatabaseManagerFactory {

    private final BeeAuction plugin;

    public DatabaseManagerFactory(BeeAuction plugin) {
        this.plugin = plugin;
    }

    public AsyncDatabaseManager createDatabaseManager() {
        String databaseType = plugin.getConfig().getString("database.type", "sqlite");
        if (databaseType.equalsIgnoreCase("mysql")) {
            return new MySQLManager(plugin);
        } else {
            return new SQLiteManager(plugin);
        }
    }
}
