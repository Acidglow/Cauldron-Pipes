package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.jfr.event.ChunkGenerationEvent;
import net.minecraft.util.profiling.jfr.event.ChunkRegionReadEvent;
import net.minecraft.util.profiling.jfr.event.ChunkRegionWriteEvent;
import net.minecraft.util.profiling.jfr.event.ClientFpsEvent;
import net.minecraft.util.profiling.jfr.event.NetworkSummaryEvent;
import net.minecraft.util.profiling.jfr.event.PacketReceivedEvent;
import net.minecraft.util.profiling.jfr.event.PacketSentEvent;
import net.minecraft.util.profiling.jfr.event.ServerTickTimeEvent;
import net.minecraft.util.profiling.jfr.event.StructureGenerationEvent;
import net.minecraft.util.profiling.jfr.event.WorldLoadFinishedEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class JfrProfiler implements JvmProfiler {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String ROOT_CATEGORY = "Minecraft";
    public static final String WORLD_GEN_CATEGORY = "World Generation";
    public static final String TICK_CATEGORY = "Ticking";
    public static final String NETWORK_CATEGORY = "Network";
    public static final String STORAGE_CATEGORY = "Storage";
    private static final List<Class<? extends Event>> CUSTOM_EVENTS = List.of(
        ChunkGenerationEvent.class,
        ChunkRegionReadEvent.class,
        ChunkRegionWriteEvent.class,
        PacketReceivedEvent.class,
        PacketSentEvent.class,
        NetworkSummaryEvent.class,
        ServerTickTimeEvent.class,
        ClientFpsEvent.class,
        StructureGenerationEvent.class,
        WorldLoadFinishedEvent.class
    );
    private static final String FLIGHT_RECORDER_CONFIG = "/flightrecorder-config.jfc";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd-HHmmss")
        .toFormatter(Locale.ROOT)
        .withZone(ZoneId.systemDefault());
    private static final JfrProfiler INSTANCE = new JfrProfiler();
    @Nullable Recording recording;
    private int currentFPS;
    private float currentAverageTickTimeServer;
    private final Map<String, NetworkSummaryEvent.SumAggregation> networkTrafficByAddress = new ConcurrentHashMap<>();
    private final Runnable periodicClientFps = () -> new ClientFpsEvent(this.currentFPS).commit();
    private final Runnable periodicServerTickTime = () -> new ServerTickTimeEvent(this.currentAverageTickTimeServer).commit();
    private final Runnable periodicNetworkSummary = () -> {
        Iterator<NetworkSummaryEvent.SumAggregation> iterator = this.networkTrafficByAddress.values().iterator();

        while (iterator.hasNext()) {
            iterator.next().commitEvent();
            iterator.remove();
        }
    };

    private JfrProfiler() {
        CUSTOM_EVENTS.forEach(FlightRecorder::register);
        this.registerPeriodicEvents();
        FlightRecorder.addListener(new FlightRecorderListener() {
            @Override
            public void recordingStateChanged(Recording recording) {
                switch (recording.getState()) {
                    case STOPPED:
                        JfrProfiler.this.registerPeriodicEvents();
                    case NEW:
                    case DELAYED:
                    case RUNNING:
                    case CLOSED:
                }
            }
        });
    }

    void registerPeriodicEvents() {
        addPeriodicEvent(ClientFpsEvent.class, this.periodicClientFps);
        addPeriodicEvent(ServerTickTimeEvent.class, this.periodicServerTickTime);
        addPeriodicEvent(NetworkSummaryEvent.class, this.periodicNetworkSummary);
    }

    private static void addPeriodicEvent(Class<? extends Event> eventClass, Runnable hook) {
        FlightRecorder.removePeriodicEvent(hook);
        FlightRecorder.addPeriodicEvent(eventClass, hook);
    }

    public static JfrProfiler getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean start(Environment p_185307_) {
        URL url = JfrProfiler.class.getResource("/flightrecorder-config.jfc");
        if (url == null) {
            LOGGER.warn("Could not find default flight recorder config at {}", "/flightrecorder-config.jfc");
            return false;
        } else {
            try {
                boolean flag;
                try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    flag = this.start(bufferedreader, p_185307_);
                }

                return flag;
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to start flight recorder using configuration at {}", url, ioexception);
                return false;
            }
        }
    }

    @Override
    public Path stop() {
        if (this.recording == null) {
            throw new IllegalStateException("Not currently profiling");
        } else {
            this.networkTrafficByAddress.clear();
            Path path = this.recording.getDestination();
            this.recording.stop();
            return path;
        }
    }

    @Override
    public boolean isRunning() {
        return this.recording != null;
    }

    @Override
    public boolean isAvailable() {
        return FlightRecorder.isAvailable();
    }

    private boolean start(Reader reader, Environment environment) {
        if (this.isRunning()) {
            LOGGER.warn("Profiling already in progress");
            return false;
        } else {
            try {
                Configuration configuration = Configuration.create(reader);
                String s = DATE_TIME_FORMATTER.format(Instant.now());
                this.recording = Util.make(new Recording(configuration), p_415069_ -> {
                    CUSTOM_EVENTS.forEach(p_415069_::enable);
                    p_415069_.setDumpOnExit(true);
                    p_415069_.setToDisk(true);
                    p_415069_.setName(String.format(Locale.ROOT, "%s-%s-%s", environment.getDescription(), SharedConstants.getCurrentVersion().name(), s));
                });
                Path path = Paths.get(String.format(Locale.ROOT, "debug/%s-%s.jfr", environment.getDescription(), s));
                FileUtil.createDirectoriesSafe(path.getParent());
                this.recording.setDestination(path);
                this.recording.start();
                this.setupSummaryListener();
            } catch (ParseException | IOException ioexception) {
                LOGGER.warn("Failed to start jfr profiling", (Throwable)ioexception);
                return false;
            }

            LOGGER.info(
                "Started flight recorder profiling id({}):name({}) - will dump to {} on exit or stop command",
                this.recording.getId(),
                this.recording.getName(),
                this.recording.getDestination()
            );
            return true;
        }
    }

    private void setupSummaryListener() {
        FlightRecorder.addListener(new FlightRecorderListener() {
            final SummaryReporter summaryReporter = new SummaryReporter(() -> JfrProfiler.this.recording = null);

            @Override
            public void recordingStateChanged(Recording p_455049_) {
                if (p_455049_ == JfrProfiler.this.recording) {
                    switch (p_455049_.getState()) {
                        case STOPPED:
                            this.summaryReporter.recordingStopped(p_455049_.getDestination());
                            FlightRecorder.removeListener(this);
                        case NEW:
                        case DELAYED:
                        case RUNNING:
                        case CLOSED:
                    }
                }
            }
        });
    }

    @Override
    public void onClientTick(int p_454943_) {
        if (ClientFpsEvent.TYPE.isEnabled()) {
            this.currentFPS = p_454943_;
        }
    }

    @Override
    public void onServerTick(float p_185300_) {
        if (ServerTickTimeEvent.TYPE.isEnabled()) {
            this.currentAverageTickTimeServer = p_185300_;
        }
    }

    @Override
    public void onPacketReceived(ConnectionProtocol p_294286_, PacketType<?> p_320639_, SocketAddress p_185304_, int p_185302_) {
        if (PacketReceivedEvent.TYPE.isEnabled()) {
            new PacketReceivedEvent(p_294286_.id(), p_320639_.flow().id(), p_320639_.id().toString(), p_185304_, p_185302_).commit();
        }

        if (NetworkSummaryEvent.TYPE.isEnabled()) {
            this.networkStatFor(p_185304_).trackReceivedPacket(p_185302_);
        }
    }

    @Override
    public void onPacketSent(ConnectionProtocol p_295940_, PacketType<?> p_320751_, SocketAddress p_185325_, int p_185323_) {
        if (PacketSentEvent.TYPE.isEnabled()) {
            new PacketSentEvent(p_295940_.id(), p_320751_.flow().id(), p_320751_.id().toString(), p_185325_, p_185323_).commit();
        }

        if (NetworkSummaryEvent.TYPE.isEnabled()) {
            this.networkStatFor(p_185325_).trackSentPacket(p_185323_);
        }
    }

    private NetworkSummaryEvent.SumAggregation networkStatFor(SocketAddress remoteAddress) {
        return this.networkTrafficByAddress.computeIfAbsent(remoteAddress.toString(), NetworkSummaryEvent.SumAggregation::new);
    }

    @Override
    public void onRegionFileRead(RegionStorageInfo p_326253_, ChunkPos p_326199_, RegionFileVersion p_326089_, int p_325934_) {
        if (ChunkRegionReadEvent.TYPE.isEnabled()) {
            new ChunkRegionReadEvent(p_326253_, p_326199_, p_326089_, p_325934_).commit();
        }
    }

    @Override
    public void onRegionFileWrite(RegionStorageInfo p_326009_, ChunkPos p_326210_, RegionFileVersion p_326516_, int p_326455_) {
        if (ChunkRegionWriteEvent.TYPE.isEnabled()) {
            new ChunkRegionWriteEvent(p_326009_, p_326210_, p_326516_, p_326455_).commit();
        }
    }

    @Override
    public @Nullable ProfiledDuration onWorldLoadedStarted() {
        if (!WorldLoadFinishedEvent.TYPE.isEnabled()) {
            return null;
        } else {
            WorldLoadFinishedEvent worldloadfinishedevent = new WorldLoadFinishedEvent();
            worldloadfinishedevent.begin();
            return p_382689_ -> worldloadfinishedevent.commit();
        }
    }

    @Override
    public @Nullable ProfiledDuration onChunkGenerate(ChunkPos p_185313_, ResourceKey<Level> p_185314_, String p_185315_) {
        if (!ChunkGenerationEvent.TYPE.isEnabled()) {
            return null;
        } else {
            ChunkGenerationEvent chunkgenerationevent = new ChunkGenerationEvent(p_185313_, p_185314_, p_185315_);
            chunkgenerationevent.begin();
            return p_382687_ -> chunkgenerationevent.commit();
        }
    }

    @Override
    public @Nullable ProfiledDuration onStructureGenerate(ChunkPos p_383140_, ResourceKey<Level> p_382829_, Holder<Structure> p_383041_) {
        if (!StructureGenerationEvent.TYPE.isEnabled()) {
            return null;
        } else {
            StructureGenerationEvent structuregenerationevent = new StructureGenerationEvent(p_383140_, p_383041_, p_382829_);
            structuregenerationevent.begin();
            return p_382685_ -> {
                structuregenerationevent.success = p_382685_;
                structuregenerationevent.commit();
            };
        }
    }
}
