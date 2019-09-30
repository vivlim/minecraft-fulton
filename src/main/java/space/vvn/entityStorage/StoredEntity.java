package space.vvn.entityStorage;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import lombok.Getter;
import lombok.val;

public class StoredEntity implements ConfigurationSerializable {
    @Getter private EntityType entityType;
    @Getter private String customName;

    public StoredEntity(Entity entity){
        this.entityType = entity.getType();
        this.customName = entity.getCustomName();
    }

    public StoredEntity(Map<String, Object> map){
        val entityTypeString = (String)map.get("entityType");
        this.entityType = EntityType.valueOf(entityTypeString);
        this.customName = (String)map.get("customName");
    }

    @Override public Map<String, Object> serialize(){
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("entityType", entityType.toString());
        map.put("customName", customName);
        return map;
    }

    @Override public boolean equals(Object o){
        if (!(o instanceof StoredEntity)){
            return false;
        }
        val compareTo = (StoredEntity)o;
        
        return compareTo.getCustomName() == this.customName
            && compareTo.getEntityType() == this.entityType;
    }
}