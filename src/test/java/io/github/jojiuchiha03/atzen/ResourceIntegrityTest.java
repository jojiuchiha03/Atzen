package io.github.jojiuchiha03.atzen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/** Asset/data checks without starting Minecraft, a server or a world. */
class ResourceIntegrityTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path ASSETS = RESOURCES.resolve("assets/atzen");

    private static JsonObject json(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
    }

    private static List<Path> files(Path path, String extension) throws IOException {
        try (var stream = Files.walk(path)) { return stream.filter(p -> p.toString().endsWith(extension)).toList(); }
    }

    @Test void everyJsonParsesAndEveryLocalModelAndTextureResolves() throws IOException {
        for (Path path : files(RESOURCES, ".json")) checkReferences(json(path));
    }

    private static void checkReferences(JsonElement element) {
        if (element.isJsonArray()) { element.getAsJsonArray().forEach(ResourceIntegrityTest::checkReferences); return; }
        if (!element.isJsonObject()) return;
        var object = element.getAsJsonObject();
        for (var entry : object.entrySet()) {
            var value = entry.getValue();
            if ((entry.getKey().equals("model") || entry.getKey().equals("parent")) && value.isJsonPrimitive()) {
                String id = value.getAsString();
                if (id.startsWith("atzen:")) assertTrue(Files.isRegularFile(ASSETS.resolve("models/" + id.substring(6) + ".json")), id);
            }
            if (entry.getKey().equals("textures")) for (var texture : value.getAsJsonObject().entrySet()) {
                String id = texture.getValue().getAsString();
                if (id.startsWith("atzen:")) assertTrue(Files.isRegularFile(ASSETS.resolve("textures/" + id.substring(6) + ".png")), id);
            }
            checkReferences(value);
        }
    }

    @Test void germanAndEnglishCoverItemsAndAllLiteralMessages() throws IOException {
        JsonObject de = json(ASSETS.resolve("lang/de_de.json")), en = json(ASSETS.resolve("lang/en_us.json"));
        assertEquals(en.keySet(), de.keySet());
        for (Path item : files(ASSETS.resolve("items"), ".json")) {
            String id = item.getFileName().toString().replace(".json", "");
            assertTrue(de.has("item.atzen." + id), id);
        }
        var keys = Pattern.compile("(?:translatable|translate)\\(\"([^\"]*atzen[^\"]*)\"\\s*[,)]");
        for (String sourceSet : List.of("main", "client")) for (Path source : files(Path.of("src", sourceSet, "java"), ".java")) {
            var matcher = keys.matcher(Files.readString(source));
            while (matcher.find()) assertTrue(de.has(matcher.group(1)), source + ": " + matcher.group(1));
        }
    }

    @Test void recipesAndEnchantmentsReferenceExistingItemsAndTranslations() throws IOException {
        Set<String> items = files(ASSETS.resolve("items"), ".json").stream()
                .map(p -> "atzen:" + p.getFileName().toString().replace(".json", "")).collect(Collectors.toSet());
        JsonObject translations = json(ASSETS.resolve("lang/en_us.json"));
        for (Path recipe : files(RESOURCES.resolve("data/atzen/recipe"), ".json")) {
            JsonObject value = json(recipe);
            if (value.has("ingredients")) for (var ingredient : value.getAsJsonArray("ingredients")) checkItem(ingredient.getAsString(), items);
            if (value.has("ingredient")) checkItem(value.get("ingredient").getAsString(), items);
            checkItem(value.getAsJsonObject("result").get("id").getAsString(), items);
        }
        for (Path enchantment : files(RESOURCES.resolve("data/atzen/enchantment"), ".json")) {
            JsonObject value = json(enchantment);
            assertTrue(translations.has(value.getAsJsonObject("description").get("translate").getAsString()));
            String tag = value.get("supported_items").getAsString().replace("#atzen:", "");
            for (var item : json(RESOURCES.resolve("data/atzen/tags/item/" + tag + ".json")).getAsJsonArray("values")) checkItem(item.getAsString(), items);
            assertTrue(value.get("max_level").getAsInt() > 0);
        }
    }

    private static void checkItem(String id, Set<String> items) {
        if (id.startsWith("atzen:")) assertTrue(items.contains(id), id);
    }

    @Test void inventorySpritesHaveTransparencyAndFishSkinsCoverTheirWholeAtlas() throws IOException {
        for (Path sprite : files(ASSETS.resolve("textures/item"), ".png")) {
            var image = ImageIO.read(sprite.toFile());
            assertTrue(Set.of(16, 32, 64).contains(image.getWidth()), sprite.toString());
            assertEquals(image.getWidth(), image.getHeight(), sprite.toString());
            int opaque = 0, transparent = 0;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha == 0) transparent++;
                if (alpha >= 250) opaque++;
            }
            assertTrue(opaque > 30 && transparent > 30, sprite.toString());
        }
        for (FishSpecies species : FishSpecies.values()) {
            var image = ImageIO.read(ASSETS.resolve("textures/entity/fish/" + species.id() + ".png").toFile());
            assertEquals(64, image.getWidth());
            assertEquals(64, image.getHeight());
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) assertEquals(255, image.getRGB(x, y) >>> 24, species.id());
        }
    }
}
