package com.sq_yan.magic_storage.menu;

public enum SortMode {
    NONE, NAME, COUNT, MOD;

    public SortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
