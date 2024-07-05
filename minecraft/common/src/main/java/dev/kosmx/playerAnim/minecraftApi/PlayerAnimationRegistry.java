package dev.kosmx.playerAnim.minecraftApi;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Load resources from <code>assets/{modid}/player_animation</code>
 * <br>
 * The animation identifier:
 * <table border="1">
 *   <tr>
 *     <td> namespace </td> <td> Mod namespace </td>
 *   </tr>
 *   <tr>
 *     <td> path </td> <td> Animation name, not the filename </td>
 *   </tr>
 * </table>
 * <br>
 * Use {@link PlayerAnimationRegistry#getAnimation(ResourceLocation)} to fetch an animation
 * <br><br>
 * Extra animations can be added by ResourcePack(s) or other mods
 */
@Environment(EnvType.CLIENT)
public final class PlayerAnimationRegistry {

    private static final HashMap<ResourceLocation, KeyframeAnimation> animations = new HashMap<>();

    /**
     * Get an animation from the registry, using Identifier(MODID, animation_name) as key
     * @param identifier identifier
     * @return animation, <code>null</code> if no animation
     */
    @Nullable
    public static KeyframeAnimation getAnimation(@NotNull ResourceLocation identifier) {
        return animations.get(identifier);
    }

    /**
     * Get Optional animation from registry
     * @param identifier identifier
     * @return Optional animation
     */
    @NotNull
    public static Optional<KeyframeAnimation> getAnimationOptional(@NotNull ResourceLocation identifier) {
        return Optional.ofNullable(getAnimation(identifier));
    }

    /**
     * @return an unmodifiable map of all the animations
     */
    public static Map<ResourceLocation, KeyframeAnimation> getAnimations() {
        return Map.copyOf(animations);
    }

    /**
     * Returns the animations of a specific mod/namespace
     * @param modid namespace (assets/modid)
     * @return map of path and animations
     */
    public static Map<String, KeyframeAnimation> getModAnimations(String modid) {
        HashMap<String, KeyframeAnimation> map = new HashMap<>();
        for (Map.Entry<ResourceLocation, KeyframeAnimation> entry: animations.entrySet()) {
            if (entry.getKey().getNamespace().equals(modid)) {
                map.put(entry.getKey().getPath(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Load animations using ResourceManager
     * Internal use only!
     */
    @ApiStatus.Internal
    public static void resourceLoaderCallback(@NotNull ResourceManager manager, Logger logger) {
        animations.clear();
        for (var resource: manager.listResources("player_animation", location -> location.getPath().endsWith(".json")).entrySet()) {
            try (var input = resource.getValue().open()) {

                //Deserialize the animation json. GeckoLib animation json can contain multiple animations.
                for (var animation : AnimationSerializing.deserializeAnimation(input)) {

                    //Save the animation for later use.
                    animations.put(new ResourceLocation(resource.getKey().getNamespace(), PlayerAnimationRegistry.serializeTextToString((String) animation.extraData.get("name")).toLowerCase(Locale.ROOT)), animation);
                }
            } catch(IOException e) {
                logger.error("Error while loading payer animation: " + resource.getKey());
                logger.error(e.getMessage());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String sStackTrace = sw.toString(); // stack trace as a string
                logger.error(sStackTrace);
            }
        }
    }


    /**
     * Helper function to convert animation name to string
     */
    public static String serializeTextToString(String arg) {
        try {
            var component = Component.Serializer.fromJson(arg, new HolderLookup.Provider() {
                @Override
                public @NotNull Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                    return Stream.empty();
                }

                @Override
                public <T> @NotNull Optional<HolderLookup.RegistryLookup<T>> lookup(@NotNull ResourceKey<? extends Registry<? extends T>> resourceKey) {
                    return Optional.empty();
                }
            });
            if (component != null) {
                return component.getString();
            }
        } catch(Exception ignored) { }
        return arg.replace("\"", "");
    }
}
