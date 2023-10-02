package dev.emortal;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MinecraftPhysicsHandler {

    private final @NotNull BlockRigidBody groundObject;

    private final @NotNull btCollisionConfiguration collisionConfig;
    private final @NotNull btDynamicsWorld dynamicsWorld;
    private final @NotNull btDispatcher dispatcher;
    private final @NotNull btBroadphaseInterface broadphase;
    private final @NotNull btConstraintSolver constraintSolver;

    private final @NotNull List<BlockRigidBody> cubes = new ArrayList<>();

    public MinecraftPhysicsHandler(Instance instance) {
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        constraintSolver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);

        groundObject = addCube(new BlockRigidBody(
                instance,
                new Vector3(0, -1, 0),
                new Vector3(20, 1, 20),
                0,
                false,
                Block.AIR
        ));
    }

    public void update(float delta) {
        dynamicsWorld.stepSimulation(delta); // could do with being on a different thread, however bullet seems to really dislike that

        for (BlockRigidBody cube : cubes) {
            cube.updateEntity();
        }

    }

    public @NotNull BlockRigidBody addCube(BlockRigidBody cube) {
        addBody(cube.getRigidBody());
        cubes.add(cube);
        return cube;
    }
    public void addBody(btRigidBody body) {
        dynamicsWorld.addRigidBody(body);
    }

    public @NotNull BlockRigidBody getGroundObject() {
        return groundObject;
    }

    public @NotNull List<BlockRigidBody> getCubes() {
        return cubes;
    }


}
