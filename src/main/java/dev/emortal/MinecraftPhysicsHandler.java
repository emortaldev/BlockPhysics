package dev.emortal;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

import java.util.ArrayList;
import java.util.List;

public class MinecraftPhysicsHandler {

    private final @NotNull List<BlockRigidBody> cubes = new ArrayList<>();

    private final PxScene scene;
    private final Instance instance;
    private final PxPhysics physics;
    private final PxFoundation foundation;

    public MinecraftPhysicsHandler(Instance instance) {
        this.instance = instance;

        int version = PxTopLevelFunctions.getPHYSICS_VERSION();

        int versionMajor = version >> 24;
        int versionMinor = (version >> 16) & 0xff;
        int versionMicro = (version >> 8) & 0xff;
        System.out.printf("PhysX %d.%d.%d\n", versionMajor, versionMinor, versionMicro);

        // create PhysX foundation object
        PxDefaultAllocator allocator = new PxDefaultAllocator();
        PxDefaultErrorCallback errorCb = new PxDefaultErrorCallback();
        foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb);

        // create PhysX main physics object
        PxTolerancesScale tolerances = new PxTolerancesScale();
        physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);


        // create a physics scene
        PxVec3 tmpVec = new PxVec3(0f, -9.81f, 0f);

        scene = CudaHelpers.createCudaEnabledScene(this, CudaHelpers.getCudaContextManager(this));

//        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
//        sceneDesc.setGravity(tmpVec);
//        sceneDesc.setCpuDispatcher(cpuDispatcher);
//        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
//        scene = physics.createScene(sceneDesc);

        // create a default material
        PxMaterial material = physics.createMaterial(0.5f, 0.5f, 0.5f);
        // create default simulation shape flags
        PxShapeFlags shapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));

        // create a few temporary objects used during setup
        PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxFilterData tmpFilterData = new PxFilterData(1, 1, 0, 0);

        // create a large static box with size 20x1x20 as ground
        PxBoxGeometry groundGeometry = new PxBoxGeometry(200f, 0.5f, 200f);   // PxBoxGeometry uses half-sizes
        PxShape groundShape = physics.createShape(groundGeometry, material, true, shapeFlags);
        PxRigidStatic ground = physics.createRigidStatic(tmpPose);
        groundShape.setSimulationFilterData(tmpFilterData);
        ground.attachShape(groundShape);
        scene.addActor(ground);

        groundGeometry.destroy();
//        boxGeometry.destroy();
        tmpFilterData.destroy();
        tmpPose.destroy();
        tmpVec.destroy();
        shapeFlags.destroy();
//        sceneDesc.destroy();
        tolerances.destroy();
    }


    public BlockRigidBody spawnCube(Point pos, Vec size, int mass, boolean visible, Block block) {
        BlockRigidBody cube = new BlockRigidBody(instance, physics, scene, mass, pos, size, visible, block);
        cubes.add(cube);
        return cube;
    }

    public void update(float delta) {
        scene.simulate(delta);
        scene.fetchResults(true);

        for (BlockRigidBody cube : cubes) {
            cube.updateEntity();
        }

    }

    public @NotNull List<BlockRigidBody> getCubes() {
        return cubes;
    }

    public PxFoundation getFoundation() {
        return foundation;
    }

    public PxPhysics getPhysics() {
        return physics;
    }
}
