package xyz.immortius.chunkbychunk.common.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.blockEntities.WorldForgeBlockEntity;
import xyz.immortius.chunkbychunk.common.blockEntities.WorldScannerBlockEntity;
import xyz.immortius.chunkbychunk.config.ChunkByChunkConfig;
import xyz.immortius.chunkbychunk.interop.Services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@JeiPlugin
public class CBCJeiPlugin implements IModPlugin {
    public static final RecipeType<WorldForgeRecipe> WORLD_FORGE =
            RecipeType.create(ChunkByChunkConstants.MOD_ID, "worldforge", WorldForgeRecipe.class);

    public static final RecipeType<WorldScannerRecipe> WORLD_SCANNER =
            RecipeType.create(ChunkByChunkConstants.MOD_ID, "worldscanner", WorldScannerRecipe.class);

    public static final RecipeType<WorldMenderRecipe> WORLD_MENDER =
            RecipeType.create(ChunkByChunkConstants.MOD_ID, "worldmender", WorldMenderRecipe.class);

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new WorldForgeRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new WorldScannerRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new WorldMenderRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registerWorldForgeRecipes(registration);
        registerWorldScannerRecipes(registration);
        registerWorldMenderRecipes(registration);
        registerSpawnerInfo(registration);
    }

    /**
     * Info page on every chunk spawner explaining how to take it apart, showing the player's own key bindings
     */
    private void registerSpawnerInfo(IRecipeRegistration registration) {
        List<ItemStack> spawners = new ArrayList<>();
        spawners.add(Services.PLATFORM.spawnChunkBlockItem().getDefaultInstance());
        spawners.add(Services.PLATFORM.unstableChunkSpawnBlockItem().getDefaultInstance());
        spawners.addAll(Services.PLATFORM.biomeThemeBlockItems());
        registration.addItemStackInfo(spawners,
                Component.translatable("jei.chunkbychunk.spawner.disassemble", Component.keybind("key.sneak"), Component.keybind("key.attack")));
    }

    private void registerWorldMenderRecipes(IRecipeRegistration registration) {
        registration.addRecipes(WORLD_MENDER, Arrays.asList(
                new WorldMenderRecipe(Services.PLATFORM.worldCoreBlockItem().getDefaultInstance()),
                new WorldMenderRecipe(Services.PLATFORM.unstableChunkSpawnBlockItem().getDefaultInstance()),
                new WorldMenderRecipe(Services.PLATFORM.spawnChunkBlockItem().getDefaultInstance())
        ));
        registration.addRecipes(WORLD_MENDER, Services.PLATFORM.biomeThemeBlockItems().stream().map(WorldMenderRecipe::new).toList());
    }

    private void registerWorldScannerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(WORLD_SCANNER, WorldScannerBlockEntity.FUEL.entrySet().stream().map(entry -> new WorldScannerRecipe(entry.getKey().getDefaultInstance(), entry.getValue().get())).toList());
    }

    private void registerWorldForgeRecipes(IRecipeRegistration registration) {
        registration.addRecipes(WORLD_FORGE, WorldForgeBlockEntity.FUEL_TAGS.entrySet().stream().map(tagInfo -> {
            int inputSize = determineForgeInput(tagInfo.getValue().get());
            ItemStack output = determineForgeOutput(tagInfo.getValue().get());
            return new WorldForgeRecipe(registration.getJeiHelpers().getIngredientManager().getAllItemStacks().stream().filter(item -> item.is(tagInfo.getKey())).map(x -> (inputSize > 1) ? x.copyWithCount(inputSize) : x).toList(), tagInfo.getValue().get(), output);
        }).filter(r -> !r.getInputItems().isEmpty()).toList());
        registration.addRecipes(WORLD_FORGE, WorldForgeBlockEntity.FUEL.entrySet().stream().map(fuelInfo -> {
            ItemStack output = determineForgeOutput(fuelInfo.getValue().get());
            return new WorldForgeRecipe(Collections.singletonList(fuelInfo.getKey().getDefaultInstance()), fuelInfo.getValue().get(), output);
        }).toList());
    }

    private int determineForgeInput(int fuelValue) {
        return Math.max(1, ChunkByChunkConfig.get().getWorldForge().getFragmentFuelCost() / fuelValue);
    }

    @NotNull
    private ItemStack determineForgeOutput(int fuelValue) {
        int count = fuelValue / ChunkByChunkConfig.get().getWorldForge().getFragmentFuelCost();
        ItemStack output = Services.PLATFORM.worldFragmentItem().getDefaultInstance();
        if (count > 1) {
            output = output.copyWithCount(count);
        }
        return output;
    }
}
