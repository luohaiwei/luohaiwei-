package org.soft.base.server.ctrl.dao;

public interface SplitDao {
    public int allPages(int rows , int size);

    public int getBegin(int current , int size);
}
