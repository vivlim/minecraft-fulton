package space.vvn.entityStorage;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import lombok.Getter;
import lombok.val;

public class StoredEntity implements ConfigurationSerializable {
    @Getter private EntityType EntityType;
    @Getter private String CustomName;

    public StoredEntity(Entity entity){
        this.EntityType = entity.getType();
        this.CustomName = entity.getCustomName();
    }

    public StoredEntity(Map<String, Object> map){
        this.EntityType = (EntityType)map.get("entityType");
        this.CustomName = (String)map.get("customName");
    }

    @Override public Map<String, Object> serialize(){
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("entityType", EntityType);
        map.put("customName", CustomName);
        return map;
    }

    @Override public boolean equals(Object o){
        if (!(o instanceof StoredEntity)){
            return false;
        }
        val compareTo = (StoredEntity)o;
        
        return compareTo.getCustomName() == this.CustomName
            && compareTo.getEntityType() == this.EntityType;
    }
}