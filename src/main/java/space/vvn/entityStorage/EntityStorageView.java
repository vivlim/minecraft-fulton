package space.vvn.entityStorage;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import lombok.var;

@AllArgsConstructor
public class EntityStorageView {
    @Getter private Player player;
    @Getter private World world;
    @Getter private JavaPlugin plugin;

    private String getStoredEntitiesConfigKey(){
        return String.format("player.%s.stored-entities.%s", player, world);
    }

    public List<StoredEntity> getStoredEntities(){
        val config = this.plugin.getConfig();
        String settingKey = getStoredEntitiesConfigKey();

        List<?> configList = config.getList(settingKey);

        List<StoredEntity> storedEntities = (List<StoredEntity>) configList;
        if (storedEntities == null){
            storedEntities = new LinkedList<StoredEntity>();
        }
        return storedEntities;
    }

    public void saveStoredEntities(List<StoredEntity> list){
        val config = this.plugin.getConfig();
        String settingKey = getStoredEntitiesConfigKey();
        config.set(settingKey, list);
        this.plugin.saveConfig();
    }

    public boolean removeStoredEntity(StoredEntity entity){
        val storedEntities = getStoredEntities();

        for (var i = 0; i < storedEntities.size(); i++){
            val e = storedEntities.get(i);

            if (e == entity){
                storedEntities.remove(i);
                saveStoredEntities(storedEntities);
                return true;
            }
        }

        return false;
    }

    public void storeEntity(Entity entity){
        List<StoredEntity> storedEntities = getStoredEntities();

        StoredEntity storedEntity = new StoredEntity(entity);
        storedEntities.add(storedEntity);

        saveStoredEntities(storedEntities);
    }
}