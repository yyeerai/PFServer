package net.minecraftforge.registries;

import cn.pfcraft.server.PFServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistryNamespacedDefaultedByKey;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

class NamespacedDefaultedWrapper<V extends IForgeRegistryEntry<V>> extends RegistryNamespacedDefaultedByKey<ResourceLocation, V> implements ILockableRegistry
{
    private boolean locked = false;
    private ForgeRegistry<V> delegate;

    private NamespacedDefaultedWrapper(ForgeRegistry<V> owner)
    {
        super(null);
        this.delegate = owner;
    }

    @Override
    public void register(int id, ResourceLocation key, V value)
    {
        if (locked)
            throw new IllegalStateException("Can not register to a locked registry. Modder should use Forge Register methods.");
        Validate.notNull(value);

        if (value.getRegistryName() == null)
            value.setRegistryName(key);

        int realId = this.delegate.add(id, value);
        if (realId != id && id != -1)
            PFServer.LOGGER.warn("Registered object did not get ID it asked for. Name: {} Type: {} Expected: {} Got: {}", key, value.getRegistryType().getName(), id, realId);
    }

    @Override
    public void putObject(ResourceLocation key, V value)
    {
        register(-1, key, value);
    }

    @Override
    public void validateKey()
    {
        this.delegate.validateKey();
    }

    // Reading Functions
    @Override
    @Nullable
    public V getObject(@Nullable ResourceLocation name)
    {
        return this.delegate.getValue(name);
    }

    @Override
    @Nullable
    public ResourceLocation getNameForObject(V value)
    {
        return this.delegate.getKey(value);
    }

    @Override
    public boolean containsKey(ResourceLocation key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public int getIDForObject(@Nullable V value)
    {
        return this.delegate.getID(value);
    }

    @Override
    @Nullable
    public V getObjectById(int id)
    {
        return this.delegate.getValue(id);
    }

    @Override
    public Iterator<V> iterator()
    {
        return this.delegate.iterator();
    }

    @Override
    public Set<ResourceLocation> getKeys()
    {
        return this.delegate.getKeys();
    }

    @Override
    @Nullable
    public V getRandomObject(Random random)
    {
        Collection<V> values = this.delegate.getValuesCollection();
        return values.stream().skip(random.nextInt(values.size())).findFirst().orElse(this.delegate.getDefault());
    }

    //internal
    @Override
    public void lock(){ this.locked = true; }

    public static class Factory<V extends IForgeRegistryEntry<V>> implements IForgeRegistry.CreateCallback<V>
    {
        public static final ResourceLocation ID = new ResourceLocation("forge", "registry_defaulted_wrapper");
        @Override
        public void onCreate(IForgeRegistryInternal<V> owner, RegistryManager stage)
        {
            owner.setSlaveMap(ID, new NamespacedDefaultedWrapper<V>((ForgeRegistry<V>)owner));
        }
    }
}
