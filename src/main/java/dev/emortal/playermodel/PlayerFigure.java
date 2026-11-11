package dev.emortal.playermodel;

import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PlayerFigure extends Entity {

    private static final double FIGURE_SCALE = 2.0;

    private Map<String, Entity> displays = Map.of();
    private float displayYaw = Float.NaN;
    private @Nullable SkinLook look;

    public PlayerFigure() {
        super(EntityType.TEXT_DISPLAY);
        setInvisible(true);
        setNoGravity(true);
    }

    public PlayerFigure(SkinLook look) {
        this();
        setLook(look);
    }

    public PlayerFigure(CompletableFuture<@Nullable SkinLook> future) {
        this();
        future.whenComplete((result, error) -> {
            if (error != null) error.printStackTrace();
            if (result != null) scheduler().scheduleNextTick(() -> setLook(result));
        });
    }

    public @Nullable SkinLook getLook() {
        return look;
    }

    public void setLook(@Nullable SkinLook value) {
        this.look = value;
        displays.values().forEach(Entity::remove);

        if (value == null) displays = Map.of();
        else {
            displays = new HashMap<>();
            for (Map.Entry<String, CustomModelData> e : value.pieces.entrySet()) {
                displays.put(e.getKey(), display(e.getKey(), e.getValue()));
            }
        }

        displayYaw = Float.NaN;
        Instance instance = getInstance();
        if (instance == null) return;
        if (isRemoved()) return;
        face(getPosition().yaw());
        Pos displayPosition = getPosition().withView(0f, 0f);
        displays.values().forEach(d -> d.setInstance(instance, displayPosition));
    }

    @Override
    public CompletableFuture<Void> setInstance(Instance instance, Pos spawnPosition) {
        return super.setInstance(instance, spawnPosition).thenCompose(ignored -> {
            if (isRemoved()) return CompletableFuture.<Void>completedFuture(null);
            face(getPosition().yaw());
            Pos displayPosition = getPosition().withView(0f, 0f);
            return CompletableFuture.allOf(displays.values().stream()
                .map(entity -> entity.setInstance(instance, displayPosition))
                .toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public CompletableFuture<Void> teleport(Pos position, Vec velocity, long @Nullable [] chunks, int flags, boolean shouldConfirm) {
        return super.teleport(position, velocity, chunks, flags, shouldConfirm).thenCompose(ignored -> syncDisplays());
    }

    @Override
    public void setView(float yaw, float pitch, float headRotation) {
        super.setView(yaw, pitch, headRotation);
        face(getPosition().yaw());
    }

    @Override
    public void update(long time) {
        super.update(time);
        syncDisplays();
    }

    @Override
    public void despawn() {
        displays.values().forEach(Entity::remove);
        super.despawn();
    }

    private CompletableFuture<Void> syncDisplays() {
        if (isRemoved()) return CompletableFuture.completedFuture(null);
        face(getPosition().yaw());
        Pos displayPosition = getPosition().withView(0f, 0f);
        return CompletableFuture.allOf(displays.values().stream()
            .filter(d -> d.getInstance() == getInstance() && !d.getPosition().samePoint(displayPosition))
            .map(d -> d.teleport(displayPosition))
            .toArray(CompletableFuture[]::new));
    }

    private void face(float yaw) {
        if (yaw == displayYaw) return;
        displayYaw = yaw;
        double half = -Math.toRadians(yaw) / 2;
        float[] rotation = {0f, (float) Math.sin(half), 0f, (float) Math.cos(half)};
        displays.values().forEach(display ->
            display.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setLeftRotation(rotation)));
    }

    private static Entity display(String item, CustomModelData data) {
        Entity entity = new Entity(EntityType.ITEM_DISPLAY);
        entity.setNoGravity(true);
        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setPosRotInterpolationDuration(2);
            meta.setItemStack(ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, item)
                .set(DataComponents.CUSTOM_MODEL_DATA, data)
                .build());
            meta.setScale(new Vec(FIGURE_SCALE, FIGURE_SCALE, FIGURE_SCALE));
        });
        return entity;
    }

    public @Nullable Entity getDisplay(String name) {
        return displays.get(name);
    }
}