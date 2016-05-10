package acmi.l2.clientmod.unreal.engine;

import acmi.l2.clientmod.io.annotation.Compact;
import acmi.l2.clientmod.unreal.annotation.ObjectRef;
import acmi.l2.clientmod.unreal.core.Object;

public class Font extends Object {
    public Character[] characters;
    @ObjectRef
    public Object[] textureList;
    public int unk1,unk2;

    public static class Character{
        public int x,y,width,height;
        @Compact
        public int textureIndex;
    }
}
