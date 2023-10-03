package dev.emortal;

import de.fabmax.physxjni.Platform;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.physics.*;

public class CudaHelpers {

    private static PxCudaContextManager cudaMgr;

    public static boolean isAvailable(MinecraftPhysicsHandler physicsHandler) {
        if (Platform.getPlatform() == Platform.MACOS || Platform.getPlatform() == Platform.MACOS_ARM64) {
            // no CUDA support on macOS
            return false;
        }
        return PxCudaTopLevelFunctions.GetSuggestedCudaDeviceOrdinal(physicsHandler.getFoundation()) >= 0;
    }

    public static PxCudaContextManager getCudaContextManager(MinecraftPhysicsHandler physicsHandler) {
        if (cudaMgr != null && cudaMgr.contextIsValid()) {
            return cudaMgr;
        }

        if (!isAvailable(physicsHandler)) {
            System.err.println("CUDA is not available or disabled on this platform");
            return null;
        }

        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxCudaContextManagerDesc desc = PxCudaContextManagerDesc.createAt(mem, MemoryStack::nmalloc);
            cudaMgr = PxCudaTopLevelFunctions.CreateCudaContextManager(physicsHandler.getFoundation(), desc);
            if (cudaMgr == null || !cudaMgr.contextIsValid()) {
                System.err.println("Failed creating CUDA context, no CUDA capable GPU?");
                cudaMgr = null;
            }
            return cudaMgr;
        }
    }

    public static PxScene createCudaEnabledScene(MinecraftPhysicsHandler physicsHandler, PxCudaContextManager cudaMgr) {
        if (cudaMgr == null) {
            throw new IllegalArgumentException("PxCudaContextManager must be non-null");
        }

        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxDefaultCpuDispatcher cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(16);

            PxSceneDesc sceneDesc = PxSceneDesc.createAt(mem, MemoryStack::nmalloc, physicsHandler.getPhysics().getTolerancesScale());
            sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
            sceneDesc.setCpuDispatcher(cpuDispatcher);
            sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
            sceneDesc.setCudaContextManager(cudaMgr);
            sceneDesc.setStaticStructure(PxPruningStructureTypeEnum.eDYNAMIC_AABB_TREE);
            sceneDesc.getFlags().raise(PxSceneFlagEnum.eENABLE_PCM);
            sceneDesc.getFlags().raise(PxSceneFlagEnum.eENABLE_GPU_DYNAMICS);
            sceneDesc.setBroadPhaseType(PxBroadPhaseTypeEnum.eGPU);
            sceneDesc.setSolverType(PxSolverTypeEnum.eTGS);
            return physicsHandler.getPhysics().createScene(sceneDesc);
        }
    }
}