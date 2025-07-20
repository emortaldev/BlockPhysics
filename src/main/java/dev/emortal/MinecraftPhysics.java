package dev.emortal;

import com.github.stephengold.joltjni.*;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EPhysicsUpdateError;
import com.github.stephengold.joltjni.readonly.ConstPlane;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import dev.emortal.objects.MinecraftPhysicsObject;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class MinecraftPhysics {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftPhysics.class);

    public static final int objLayerMoving = 0;
    public static final int objLayerNonMoving = 1;

    private boolean paused = false;

    private @NotNull PhysicsSystem physicsSystem;
    private @NotNull TempAllocator tempAllocator;
    private @NotNull JobSystem jobSystem;

    public static final Tag<MinecraftPhysicsObject> PHYSICS_BLOCK_TAG = Tag.Transient("physicsblock");
    public static final Tag<Body> PLAYER_RIGID_BODY_TAG = Tag.Transient("playerbody");

    private final @NotNull List<MinecraftPhysicsObject> objects = new CopyOnWriteArrayList<>();
    private final @NotNull Map<Body, MinecraftPhysicsObject> objectMap = new ConcurrentHashMap<>();
    private final Instance instance;

    public MinecraftPhysics(Instance instance) {
        this.instance = instance;

        instance.scheduler().submitTask(new Supplier<>() {
            boolean first = true;
            long lastRan = System.nanoTime();

            @Override
            public TaskSchedule get() {
                if (first) {
                    init();
                    first = false;
                }

                long diff = System.nanoTime() - lastRan;
                float deltaTime = diff / 1_000_000_000f;

                lastRan = System.nanoTime();
                if (paused) return TaskSchedule.tick(1);
                update(deltaTime);

                return TaskSchedule.tick(1);
            }
        });
    }

    private void init() {
        // https://github.com/stephengold/jolt-jni-docs/blob/master/java-apps/src/main/java/com/github/stephengold/sportjolt/javaapp/sample/console/HelloJoltJni.java
        //Jolt.setTraceAllocations(true); // to log Jolt-JNI heap allocations
        JoltPhysicsObject.startCleaner(); // to reclaim native memory
        Jolt.registerDefaultAllocator(); // tell Jolt Physics to use malloc/free
        Jolt.installDefaultAssertCallback();
        Jolt.installDefaultTraceCallback();
        boolean success = Jolt.newFactory();
        assert success;
        Jolt.registerTypes();

        // For simplicity, use a single broadphase layer:
        int numBpLayers = 1;
        int numObjLayers = 2;

        ObjectLayerPairFilterTable ovoFilter = new ObjectLayerPairFilterTable(numObjLayers);
        // Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(objLayerMoving, objLayerMoving);
        // Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(objLayerMoving, objLayerNonMoving);
        // Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(objLayerNonMoving, objLayerNonMoving);

        // Map both object layers to broadphase layer 0:
        BroadPhaseLayerInterfaceTable layerMap = new BroadPhaseLayerInterfaceTable(numObjLayers, numBpLayers);
        layerMap.mapObjectToBroadPhaseLayer(objLayerMoving, 0);
        layerMap.mapObjectToBroadPhaseLayer(objLayerNonMoving, 0);

        // Rules for colliding object layers with broadphase layers:
        ObjectVsBroadPhaseLayerFilter ovbFilter = new ObjectVsBroadPhaseLayerFilterTable(layerMap, numBpLayers, ovoFilter, numObjLayers);

        physicsSystem = new PhysicsSystem();

        // Set high limits, even though this sample app uses only 2 bodies:
        int maxBodies = 5_000;
        int numBodyMutexes = 0; // 0 means "use the default number"
        int maxBodyPairs = 65_536;
        int maxContacts = 20_480;
        physicsSystem.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts, layerMap, ovbFilter, ovoFilter);
        physicsSystem.optimizeBroadPhase();

        tempAllocator = new TempAllocatorMalloc();
        int numWorkerThreads = Runtime.getRuntime().availableProcessors();
        jobSystem = new JobSystemThreadPool(Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, numWorkerThreads); // use all available processors

        // Default: -9.81f
        // Minecraft: -31.36f
        physicsSystem.setGravity(new Vec3(0, -17f, 0));

        BodyInterface bi = physicsSystem.getBodyInterface();

        // add static plane
        float groundY = 0f;
        Vec3Arg normal = Vec3.sAxisY();
        ConstPlane plane = new Plane(normal, -groundY);
        ConstShape floorShape = new PlaneShape(plane);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(objLayerNonMoving);
        bcs.setShape(floorShape);
        Body floor = bi.createBody(bcs);
        bi.addBody(floor, EActivation.DontActivate);
    }

    private void update(float delta) {
        if (physicsSystem == null) return;

        int steps = 1;
        int errors = physicsSystem.update(delta, steps, tempAllocator, jobSystem);
        assert errors == EPhysicsUpdateError.None : errors;

        for (MinecraftPhysicsObject object : objects) {
            object.update();
        }
    }

    public @NotNull List<MinecraftPhysicsObject> getObjects() {
        return objects;
    }

    public void addObject(MinecraftPhysicsObject object) {
        objects.add(object);
        objectMap.put(object.getBody(), object);
    }
    public void removeObject(MinecraftPhysicsObject object) {
        objects.remove(object);
        objectMap.remove(object.getBody());
    }

    public @Nullable MinecraftPhysicsObject getObjectByBody(Body physicsObject) {
        return objectMap.get(physicsObject);
    }

//    public List<PhysicsRayTestResult> raycastEntity(@NotNull Point startPoint, @NotNull Point direction, double maxDistance) {
//        Point endPoint = startPoint.add(direction.asVec().normalize().mul(maxDistance));
//        List<PhysicsRayTestResult> results = getPhysicsSystem().rayTest(new Vector3f((float) startPoint.x(), (float) startPoint.y(), (float) startPoint.z()), new Vector3f((float) endPoint.x(), (float) endPoint.y(), (float) endPoint.z()));
//
//        return results;
//    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public Instance getInstance() {
        return instance;
    }

    public PhysicsSystem getPhysicsSystem() {
        return physicsSystem;
    }

    public BodyInterface getBodyInterface() {
        return getPhysicsSystem().getBodyInterface();
    }
}
