package com.ultramega.appliedflowanalytics.data;

import com.ultramega.appliedflowanalytics.resource.ResourceChangeKey;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowSnapshotPersistenceTest {
    private static HolderLookup.Provider registries;

    @TempDir
    private Path directory;

    @BeforeAll
    static void loadRegistries() {
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void readsExistingVersionThreeHistoryWithAnItemComponent() throws IOException {
        final var stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Flow history"));
        final var item = AEItemKey.of(stack);
        final var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        final var dictionary = new CompoundTag();
        dictionary.put("1", AEKey.CODEC.encodeStart(ops, item).getOrThrow());
        final var deltas = new CompoundTag();
        deltas.putLong("1", 64);
        deltas.putLong("-1", -17);
        deltas.putLong("999", 100);
        final var snapshot = new CompoundTag();
        snapshot.putInt("offset", 1);
        snapshot.put("deltas", deltas);
        final var snapshots = new ListTag();
        snapshots.add(snapshot);
        final var data = new CompoundTag();
        data.putInt("format_version", 3);
        data.putInt("interval_count", 3);
        data.put("items_map", dictionary);
        data.put("snapshots", snapshots);
        final var root = new CompoundTag();
        root.put("data", data);
        final Path path = this.directory.resolve("legacy.dat");
        NbtIo.write(root, path);

        final var loaded = FlowSnapshotNbtReader.read(path, registries);
        assertEquals(List.of(Map.of(), Map.of(
            new ResourceChangeKey(item, (short) 1), 64L,
            new ResourceChangeKey(item, (short) -1), -17L), Map.of()), loaded.getSnapshots());
    }

    @Test
    void restoresEarlyTicksAndKeepsSavedAndForkedHistoriesIndependent() throws IOException {
        final var water = AEFluidKey.of(Fluids.WATER);
        final var lava = AEFluidKey.of(Fluids.LAVA);
        final var iron = AEItemKey.of(Items.IRON_INGOT);
        final Path path = this.directory.resolve("history.dat");
        final Path forkPath = this.directory.resolve("fork.dat");

        final var expected = new FlowSnapshotData();
        for (int tick = 0; tick < 37; tick++) {
            expected.recordChange(water, 100);
            expected.recordChange(iron, -8);
            expected.tick();
        }
        expected.recordChange(lava, -17);
        final CompoundTag legacyData = expected.save(new CompoundTag(), registries);
        expected.save(path.toFile(), registries);
        FlowSnapshotData.flushPendingSaves();
        assertEquals(legacyData, NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()).getCompound("data"));

        final List<Runnable> reads = new ArrayList<>();
        final var live = FlowSnapshotData.open(path, registries, reads::add);
        assertFalse(live.pollLoad());
        for (int tick = 0; tick < 250; tick++) {
            if (tick % 6 == 0) {
                expected.recordChange(lava, 300);
                live.recordChange(lava, 300);
            }
            expected.tick();
            live.tick();
        }
        assertEquals(expected.getRecordedIntervals(), live.getRecordedIntervals());
        expected.recordChange(water, -55);
        live.recordChange(water, -55);
        final var fork = new FlowSnapshotData();
        live.copyTo(fork);
        final var expectedFork = new FlowSnapshotData();
        expected.copyTo(expectedFork);

        fork.tick();
        expectedFork.tick();
        live.save(path.toFile(), registries);
        fork.save(forkPath.toFile(), registries);
        live.recordChange(water, 22);
        reads.getFirst().run();
        FlowSnapshotData.flushPendingSaves();
        assertTrue(live.pollLoad());
        assertTrue(fork.pollLoad());

        assertHistoryEquals(expected, FlowSnapshotNbtReader.read(path, registries));
        expected.recordChange(water, 22);
        assertHistoryEquals(expected, live);
        assertHistoryEquals(expectedFork, FlowSnapshotNbtReader.read(forkPath, registries));
        assertHistoryEquals(expectedFork, fork);
    }

    @Test
    void leavesUnreadableHistoryIntactWhenSavedDuringLoad() throws IOException {
        final byte[] truncated = {0x1f, (byte) 0x8b, 8};
        final Path path = this.directory.resolve("broken.dat");
        Files.write(path, truncated);

        final List<Runnable> reads = new ArrayList<>();
        final var live = FlowSnapshotData.open(path, registries, reads::add);
        live.tick();
        live.save(path.toFile(), registries);
        reads.getFirst().run();
        FlowSnapshotData.flushPendingSaves();

        live.tick();
        live.save(path.toFile(), registries);
        FlowSnapshotData.flushPendingSaves();
        assertArrayEquals(truncated, Files.readAllBytes(path));
    }

    private static void assertHistoryEquals(final FlowSnapshotData expected, final FlowSnapshotData actual) {
        assertEquals(expected.getSnapshots(), actual.getSnapshots());
        assertArrayEquals(expected.getSamples(key -> true, 1, 209).inflow(), actual.getSamples(key -> true, 1, 209).inflow());
        assertArrayEquals(expected.getSamples(key -> true, 1, 209).outflow(), actual.getSamples(key -> true, 1, 209).outflow());
        // Advance copies to check both tick and second deltas pending at capture time.
        final var expectedNext = new FlowSnapshotData();
        final var actualNext = new FlowSnapshotData();
        expected.copyTo(expectedNext);
        actual.copyTo(actualNext);
        for (int tick = 0; tick < 20; tick++) {
            expectedNext.tick();
            actualNext.tick();
        }
        assertEquals(expectedNext.getSnapshots(), actualNext.getSnapshots());
        assertEquals(expectedNext.getRollingFlow(key -> true, 20), actualNext.getRollingFlow(key -> true, 20));
    }
}
