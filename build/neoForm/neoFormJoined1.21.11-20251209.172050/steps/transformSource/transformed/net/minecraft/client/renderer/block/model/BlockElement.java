package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BlockElement(
    Vector3fc from, Vector3fc to, Map<Direction, BlockElementFace> faces, @Nullable BlockElementRotation rotation, boolean shade, int lightEmission, net.neoforged.neoforge.client.model.ExtraFaceData faceData
) {
    private static final boolean DEFAULT_RESCALE = false;
    private static final float MIN_EXTENT = -16.0F;
    private static final float MAX_EXTENT = 32.0F;

    public BlockElement {
        faces.values().forEach(face -> face.parent().setValue(this));
    }

    public BlockElement(Vector3fc from, Vector3fc to, Map<Direction, BlockElementFace> faces, @Nullable BlockElementRotation rotation, boolean shade, int lightEmission) {
        this(from, to, faces, rotation, shade, lightEmission, net.neoforged.neoforge.client.model.ExtraFaceData.DEFAULT);
    }

    public BlockElement(Vector3fc p_405527_, Vector3fc p_405454_, Map<Direction, BlockElementFace> p_362722_) {
        this(p_405527_, p_405454_, p_362722_, null, true, 0);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Deserializer implements JsonDeserializer<BlockElement> {
        private static final boolean DEFAULT_SHADE = true;
        private static final int DEFAULT_LIGHT_EMISSION = 0;
        private static final String FIELD_SHADE = "shade";
        private static final String FIELD_LIGHT_EMISSION = "light_emission";
        private static final String FIELD_ROTATION = "rotation";
        private static final String FIELD_ORIGIN = "origin";
        private static final String FIELD_ANGLE = "angle";
        private static final String FIELD_X = "x";
        private static final String FIELD_Y = "y";
        private static final String FIELD_Z = "z";
        private static final String FIELD_AXIS = "axis";
        private static final String FIELD_RESCALE = "rescale";
        private static final String FIELD_FACES = "faces";
        private static final String FIELD_TO = "to";
        private static final String FIELD_FROM = "from";

        public BlockElement deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonobject = json.getAsJsonObject();
            Vector3f vector3f = getPosition(jsonobject, "from");
            Vector3f vector3f1 = getPosition(jsonobject, "to");
            BlockElementRotation blockelementrotation = this.getRotation(jsonobject);
            Map<Direction, BlockElementFace> map = this.getFaces(context, jsonobject);
            if (jsonobject.has("shade") && !GsonHelper.isBooleanValue(jsonobject, "shade")) {
                throw new JsonParseException("Expected 'shade' to be a Boolean");
            } else {
                boolean flag = GsonHelper.getAsBoolean(jsonobject, "shade", true);
                int i = 0;
                if (jsonobject.has("light_emission")) {
                    boolean flag1 = GsonHelper.isNumberValue(jsonobject, "light_emission");
                    if (flag1) {
                        i = GsonHelper.getAsInt(jsonobject, "light_emission");
                    }

                    if (!flag1 || i < 0 || i > 15) {
                        throw new JsonParseException("Expected 'light_emission' to be an Integer between (inclusive) 0 and 15");
                    }
                }

                var faceData = net.neoforged.neoforge.client.model.ExtraFaceData.read(jsonobject.get("neoforge_data"), net.neoforged.neoforge.client.model.ExtraFaceData.DEFAULT);
                return new BlockElement(vector3f, vector3f1, map, blockelementrotation, flag, i, faceData);
            }
        }

        private @Nullable BlockElementRotation getRotation(JsonObject json) {
            if (!json.has("rotation")) {
                return null;
            } else {
                JsonObject jsonobject = GsonHelper.getAsJsonObject(json, "rotation");
                Vector3f vector3f = getVector3f(jsonobject, "origin");
                vector3f.mul(0.0625F);
                BlockElementRotation.RotationValue blockelementrotation$rotationvalue;
                if (!jsonobject.has("axis") && !jsonobject.has("angle")) {
                    if (!jsonobject.has("x") && !jsonobject.has("y") && !jsonobject.has("z")) {
                        throw new JsonParseException("Missing rotation value, expected either 'axis' and 'angle' or 'x', 'y' and 'z'");
                    }

                    float f2 = GsonHelper.getAsFloat(jsonobject, "x", 0.0F);
                    float f3 = GsonHelper.getAsFloat(jsonobject, "y", 0.0F);
                    float f1 = GsonHelper.getAsFloat(jsonobject, "z", 0.0F);
                    blockelementrotation$rotationvalue = new BlockElementRotation.EulerXYZRotation(f2, f3, f1);
                } else {
                    Direction.Axis direction$axis = this.getAxis(jsonobject);
                    float f = GsonHelper.getAsFloat(jsonobject, "angle");
                    blockelementrotation$rotationvalue = new BlockElementRotation.SingleAxisRotation(direction$axis, f);
                }

                boolean flag = GsonHelper.getAsBoolean(jsonobject, "rescale", false);
                return new BlockElementRotation(vector3f, blockelementrotation$rotationvalue, flag);
            }
        }

        private Direction.Axis getAxis(JsonObject json) {
            String s = GsonHelper.getAsString(json, "axis");
            Direction.Axis direction$axis = Direction.Axis.byName(s.toLowerCase(Locale.ROOT));
            if (direction$axis == null) {
                throw new JsonParseException("Invalid rotation axis: " + s);
            } else {
                return direction$axis;
            }
        }

        private Map<Direction, BlockElementFace> getFaces(JsonDeserializationContext context, JsonObject json) {
            Map<Direction, BlockElementFace> map = this.filterNullFromFaces(context, json);
            if (map.isEmpty()) {
                throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
            } else {
                return map;
            }
        }

        private Map<Direction, BlockElementFace> filterNullFromFaces(JsonDeserializationContext context, JsonObject json) {
            Map<Direction, BlockElementFace> map = Maps.newEnumMap(Direction.class);
            JsonObject jsonobject = GsonHelper.getAsJsonObject(json, "faces");

            for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                Direction direction = this.getFacing(entry.getKey());
                map.put(direction, context.deserialize(entry.getValue(), BlockElementFace.class));
            }

            return map;
        }

        private Direction getFacing(String name) {
            Direction direction = Direction.byName(name);
            if (direction == null) {
                throw new JsonParseException("Unknown facing: " + name);
            } else {
                return direction;
            }
        }

        private static Vector3f getPosition(JsonObject json, String field) {
            Vector3f vector3f = getVector3f(json, field);
            if (!(vector3f.x() < -16.0F)
                && !(vector3f.y() < -16.0F)
                && !(vector3f.z() < -16.0F)
                && !(vector3f.x() > 32.0F)
                && !(vector3f.y() > 32.0F)
                && !(vector3f.z() > 32.0F)) {
                return vector3f;
            } else {
                throw new JsonParseException("'" + field + "' specifier exceeds the allowed boundaries: " + vector3f);
            }
        }

        private static Vector3f getVector3f(JsonObject json, String field) {
            JsonArray jsonarray = GsonHelper.getAsJsonArray(json, field);
            if (jsonarray.size() != 3) {
                throw new JsonParseException("Expected 3 " + field + " values, found: " + jsonarray.size());
            } else {
                float[] afloat = new float[3];

                for (int i = 0; i < afloat.length; i++) {
                    afloat[i] = GsonHelper.convertToFloat(jsonarray.get(i), field + "[" + i + "]");
                }

                return new Vector3f(afloat[0], afloat[1], afloat[2]);
            }
        }
    }
}
