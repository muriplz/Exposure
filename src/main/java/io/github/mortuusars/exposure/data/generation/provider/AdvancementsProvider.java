package io.github.mortuusars.exposure.data.generation.provider;

import com.google.common.collect.Sets;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.advancement.predicate.BooleanPredicate;
import io.github.mortuusars.exposure.advancement.predicate.CameraPredicate;
import io.github.mortuusars.exposure.advancement.trigger.CameraTakenShotTrigger;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.*;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AdvancementsProvider extends net.minecraft.data.advancements.AdvancementProvider
{
    private final Path PATH;
    public static final Logger LOGGER = LogManager.getLogger();

    public AdvancementsProvider(DataGenerator dataGenerator, ExistingFileHelper existingFileHelper) {
        super(dataGenerator, existingFileHelper);
        PATH = dataGenerator.getOutputFolder();
    }

    @Override
    public void run(CachedOutput cache) {
        Set<ResourceLocation> set = Sets.newHashSet();
        Consumer<Advancement> consumer = (advancement) -> {
            if (!set.add(advancement.getId())) {
                throw new IllegalStateException("Duplicate advancement " + advancement.getId());
            } else {
                Path path1 = getPath(PATH, advancement);

                try {
                    DataProvider.saveStable(cache, advancement.deconstruct().serializeToJson(), path1);
                }
                catch (IOException ioexception) {
                    LOGGER.error("Couldn't save advancement {}", path1, ioexception);
                }
            }
        };

        new MonobankAdvancements(this.fileHelper).accept(consumer);
    }

    private static Path getPath(Path pathIn, Advancement advancementIn) {
        return pathIn.resolve("data/" + advancementIn.getId().getNamespace() + "/advancements/" + advancementIn.getId().getPath() + ".json");
    }

    public static class MonobankAdvancements implements Consumer<Consumer<Advancement>>
    {
        private final ExistingFileHelper existingFileHelper;

        public MonobankAdvancements(ExistingFileHelper existingFileHelper) {
            this.existingFileHelper = existingFileHelper;
        }

        @Override
        public void accept(Consumer<Advancement> advancementConsumer) {

            Advancement exposure = Advancement.Builder.advancement()
                    .parent(new ResourceLocation("minecraft:adventure/root"))
                    .display(new ItemStack(Exposure.Items.CAMERA.get()),
                            Component.translatable("advancement.exposure.exposure.title"),
                            Component.translatable("advancement.exposure.exposure.description"),
                            null, FrameType.TASK, true, true, false)
                    .addCriterion("expose_film", new CameraTakenShotTrigger.TriggerInstance(EntityPredicate.Composite.ANY,
                            LocationPredicate.ANY, CameraPredicate.exposesFilm()))
                    .save(advancementConsumer, Exposure.resource("adventure/expose_film"), existingFileHelper);

            Advancement momentInTime = Advancement.Builder.advancement()
                    .parent(exposure)
                    .display(new ItemStack(Exposure.Items.PHOTOGRAPH.get()),
                            Component.translatable("advancement.exposure.get_photograph.title"),
                            Component.translatable("advancement.exposure.get_photograph.description"),
                            null, FrameType.TASK, true, true, false)
                    .addCriterion("get_photograph", InventoryChangeTrigger.TriggerInstance.hasItems(Exposure.Items.PHOTOGRAPH.get()))
                    .addCriterion("get_stacked_photographs", InventoryChangeTrigger.TriggerInstance.hasItems(Exposure.Items.STACKED_PHOTOGRAPHS.get()))
                    .requirements(RequirementsStrategy.OR)
                    .save(advancementConsumer, Exposure.resource("adventure/get_photograph"), existingFileHelper);

            Advancement lightsUp = Advancement.Builder.advancement()
                    .parent(exposure)
                    .display(new ItemStack(Items.REDSTONE_LAMP),
                            Component.translatable("advancement.exposure.lights_up.title"),
                            Component.translatable("advancement.exposure.lights_up.description"),
                            null, FrameType.TASK, true, true, true)
                    .addCriterion("flash_in_ancient_city", new CameraTakenShotTrigger.TriggerInstance(EntityPredicate.Composite.ANY,
                            LocationPredicate.inStructure(BuiltinStructures.ANCIENT_CITY),
                            new CameraPredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, null, true, true)))
                    .save(advancementConsumer, Exposure.resource("adventure/lights_up"), existingFileHelper);

            Advancement problem = Advancement.Builder.advancement()
                    .parent(lightsUp)
                    .display(new ItemStack(Items.END_STONE_BRICKS),
                            Component.translatable("advancement.exposure.we_have_a_problem.title"),
                            Component.translatable("advancement.exposure.we_have_a_problem.description"),
                            null, FrameType.TASK, true, true, true)
                    .addCriterion("photograph_in_end", new CameraTakenShotTrigger.TriggerInstance(EntityPredicate.Composite.ANY,
                            LocationPredicate.inDimension(Level.END), CameraPredicate.exposesFilm()))
                    .save(advancementConsumer, Exposure.resource("adventure/we_have_a_problem"), existingFileHelper);
        }
    }
}
