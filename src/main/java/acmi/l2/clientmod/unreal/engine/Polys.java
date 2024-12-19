package acmi.l2.clientmod.unreal.engine;

import acmi.l2.clientmod.io.ObjectInput;
import acmi.l2.clientmod.io.annotation.ReadMethod;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.core.Object;

public class Polys extends Object {
    public Polygon[] polygons;

    public static class Polygon {
        public Vector origin;
        public Vector normal;
        public Vector textureU;
        public Vector textureV;
        public Vector[] vertexes;
        public int flags;
        public Object actor;
        public Object texture;
        public String itemName;
        public int iLink;
        public int iBrushPoly;
        public float unk0;
        public int unk1;

        @ReadMethod
        public final void readPolygon(ObjectInput<UnrealRuntimeContext> input) {
            vertexes = new Vector[input.readCompactInt()];
            input.getSerializerFactory().forClass(Vector.class).readObject(origin = new Vector(), input);
            input.getSerializerFactory().forClass(Vector.class).readObject(normal = new Vector(), input);
            input.getSerializerFactory().forClass(Vector.class).readObject(textureU = new Vector(), input);
            input.getSerializerFactory().forClass(Vector.class).readObject(textureV = new Vector(), input);
            for (int i = 0; i < vertexes.length; i++) {
                input.getSerializerFactory().forClass(Vector.class).readObject(vertexes[i] = new Vector(), input);
            }
            flags = input.readInt();
            actor = ((UnrealSerializerFactory) input.getSerializerFactory()).getOrCreateObject(input.getContext().getUnrealPackage().objectReference(input.readCompactInt()));
            texture = ((UnrealSerializerFactory) input.getSerializerFactory()).getOrCreateObject(input.getContext().getUnrealPackage().objectReference(input.readCompactInt()));
            itemName = input.getContext().getUnrealPackage().nameReference(input.readCompactInt());
            iLink = input.readCompactInt();
            iBrushPoly = input.readCompactInt();
            unk0 = input.readFloat();
            switch (input.getContext().getUnrealPackage().getVersion()) {
                case 0x060076:
                case 0x10007b:
                case 0x12007b:
                case 0x13007b:
                case 0x14007b:
                    break;
                default:
                    unk1 = input.readInt();
            }
        }
    }

    @ReadMethod
    public final void readPolys(ObjectInput<UnrealRuntimeContext> input) {
        input.readInt();
        polygons = new Polygon[input.readInt()];
        for (int i = 0; i < polygons.length; i++) {
            input.getSerializerFactory().forClass(Polygon.class).readObject(polygons[i] = new Polygon(), input);
        }
    }
}
