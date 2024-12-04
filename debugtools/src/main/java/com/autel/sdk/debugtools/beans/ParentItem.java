package com.autel.sdk.debugtools.beans;

public abstract class ParentItem {
    /**
     * 是否展开，默认否
     */
    protected boolean isExpand = false;
    public boolean isExpand() {
        return isExpand;
    }

    public void setExpand(boolean expand) {
        isExpand = expand;
    }

    public abstract void printMessage();
}
