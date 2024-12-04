package com.autel.sdk.debugtools.beans;

/**
 * 子级Item实体类
 */
public class ChildItem {
    /**
     * 该子级类所对应父级类的名称
     */
    protected int parentId;

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
}
